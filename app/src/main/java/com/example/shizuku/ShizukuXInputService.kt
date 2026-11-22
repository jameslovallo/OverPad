package com.example.shizuku

import android.annotation.SuppressLint
import android.hardware.input.InputManager
import android.os.Process
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.Keep
import com.example.xinput.XInputState
import java.io.File
import java.lang.reflect.Method
import kotlin.math.abs

/**
 * Remote privileged service launched inside Shizuku (shell UID 2000 or root UID 0).
 * Translates OverPad's XInput state into real system-wide KeyEvent & MotionEvent injections
 * via Android's INJECT_EVENTS privilege.
 */
@Keep
class ShizukuXInputService() : IXInputInjector.Stub() {

    private var inputManagerInstance: Any? = null
    private var injectMethod: Method? = null

    private var prevButtonsMask = 0
    private var prevLtDown = false
    private var prevRtDown = false
    private var prevLX = 0f
    private var prevLY = 0f
    private var prevRX = 0f
    private var prevRY = 0f
    private var prevLT = 0f
    private var prevRT = 0f
    private var prevHatX = 0f
    private var prevHatY = 0f

    private var motionDownTime = 0L

    init {
        initInjectorReflection()
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun initInjectorReflection() {
        runCatching {
            // Android 14+ moved injectInputEvent to InputManagerGlobal
            val globalClass = runCatching {
                Class.forName("android.hardware.input.InputManagerGlobal")
            }.getOrNull()

            if (globalClass != null) {
                val getInstance = globalClass.getMethod("getInstance")
                val instance = getInstance.invoke(null)
                val method = globalClass.getMethod(
                    "injectInputEvent",
                    InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )
                inputManagerInstance = instance
                injectMethod = method
                return
            }

            // Android 7 - 13
            val imClass = InputManager::class.java
            val getInstance = imClass.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            val method = imClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            inputManagerInstance = instance
            injectMethod = method
        }
    }

    override fun getUid(): Int = Process.myUid()

    override fun isUinputWritable(): Boolean {
        val f = File("/dev/uinput")
        return f.exists() && f.canWrite()
    }

    override fun sendGamepadState(
        buttonsMask: Int,
        leftTrigger: Float,
        rightTrigger: Float,
        thumbLX: Float,
        thumbLY: Float,
        thumbRX: Float,
        thumbRY: Float,
        hatX: Float,
        hatY: Float
    ) {
        val now = SystemClock.uptimeMillis()

        // 1. KeyEvents for all button bit transitions
        val changedMask = prevButtonsMask xor buttonsMask
        if (changedMask != 0) {
            for ((bit, keyCode) in BUTTON_BIT_TO_KEYCODE) {
                if ((changedMask and bit) != 0) {
                    val isDown = (buttonsMask and bit) != 0
                    injectKey(keyCode, isDown, now)
                }
            }
            prevButtonsMask = buttonsMask
        }

        // Digital L2 / R2 key events alongside analog trigger axes (for maximum game compatibility)
        val ltDown = leftTrigger >= 0.5f
        if (ltDown != prevLtDown) {
            injectKey(KeyEvent.KEYCODE_BUTTON_L2, ltDown, now)
            prevLtDown = ltDown
        }
        val rtDown = rightTrigger >= 0.5f
        if (rtDown != prevRtDown) {
            injectKey(KeyEvent.KEYCODE_BUTTON_R2, rtDown, now)
            prevRtDown = rtDown
        }

        // 2. MotionEvent for analog sticks, triggers, and D-Pad hat
        val returningToZero =
            (thumbLX == 0f && prevLX != 0f) ||
                (thumbLY == 0f && prevLY != 0f) ||
                (thumbRX == 0f && prevRX != 0f) ||
                (thumbRY == 0f && prevRY != 0f) ||
                (leftTrigger == 0f && prevLT != 0f) ||
                (rightTrigger == 0f && prevRT != 0f) ||
                (hatX == 0f && prevHatX != 0f) ||
                (hatY == 0f && prevHatY != 0f)

        val axisChanged = returningToZero ||
            abs(thumbLX - prevLX) > AXIS_EPSILON ||
                abs(thumbLY - prevLY) > AXIS_EPSILON ||
                abs(thumbRX - prevRX) > AXIS_EPSILON ||
                abs(thumbRY - prevRY) > AXIS_EPSILON ||
                abs(leftTrigger - prevLT) > AXIS_EPSILON ||
                abs(rightTrigger - prevRT) > AXIS_EPSILON ||
                abs(hatX - prevHatX) > AXIS_EPSILON ||
                abs(hatY - prevHatY) > AXIS_EPSILON

        if (axisChanged) {
            prevLX = thumbLX
            prevLY = thumbLY
            prevRX = thumbRX
            prevRY = thumbRY
            prevLT = leftTrigger
            prevRT = rightTrigger
            prevHatX = hatX
            prevHatY = hatY

            if (motionDownTime == 0L) {
                motionDownTime = now
            }

            val props = arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_UNKNOWN
                }
            )
            val coords = arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = 0f
                    y = 0f
                    setAxisValue(MotionEvent.AXIS_X, thumbLX)
                    setAxisValue(MotionEvent.AXIS_Y, thumbLY)
                    setAxisValue(MotionEvent.AXIS_Z, thumbRX)
                    setAxisValue(MotionEvent.AXIS_RZ, thumbRY)
                    setAxisValue(MotionEvent.AXIS_LTRIGGER, leftTrigger)
                    setAxisValue(MotionEvent.AXIS_BRAKE, leftTrigger)
                    setAxisValue(MotionEvent.AXIS_RTRIGGER, rightTrigger)
                    setAxisValue(MotionEvent.AXIS_GAS, rightTrigger)
                    setAxisValue(MotionEvent.AXIS_HAT_X, hatX)
                    setAxisValue(MotionEvent.AXIS_HAT_Y, hatY)
                }
            )

            val motion = MotionEvent.obtain(
                motionDownTime,
                now,
                MotionEvent.ACTION_MOVE,
                1,
                props,
                coords,
                0,
                0,
                1f,
                1f,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK,
                0
            )
            try {
                injectEvent(motion)
            } finally {
                motion.recycle()
            }
        }
    }

    override fun resetAll() {
        sendGamepadState(0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        motionDownTime = 0L
    }

    override fun destroy() {
        runCatching { resetAll() }
        System.exit(0)
    }

    private fun injectKey(keyCode: Int, isDown: Boolean, eventTime: Long) {
        val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        val event = KeyEvent(
            eventTime,
            eventTime,
            action,
            keyCode,
            0,
            0,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            0,
            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD
        )
        injectEvent(event)
    }

    private fun injectEvent(event: InputEvent) {
        val manager = inputManagerInstance ?: return
        val method = injectMethod ?: return
        runCatching {
            method.invoke(manager, event, INJECT_INPUT_EVENT_MODE_ASYNC)
        }
    }

    companion object {
        private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        private const val AXIS_EPSILON = 0.004f

        private val BUTTON_BIT_TO_KEYCODE = listOf(
            XInputState.XINPUT_GAMEPAD_DPAD_UP to KeyEvent.KEYCODE_DPAD_UP,
            XInputState.XINPUT_GAMEPAD_DPAD_DOWN to KeyEvent.KEYCODE_DPAD_DOWN,
            XInputState.XINPUT_GAMEPAD_DPAD_LEFT to KeyEvent.KEYCODE_DPAD_LEFT,
            XInputState.XINPUT_GAMEPAD_DPAD_RIGHT to KeyEvent.KEYCODE_DPAD_RIGHT,
            XInputState.XINPUT_GAMEPAD_START to KeyEvent.KEYCODE_BUTTON_START,
            XInputState.XINPUT_GAMEPAD_BACK to KeyEvent.KEYCODE_BUTTON_SELECT,
            XInputState.XINPUT_GAMEPAD_LEFT_THUMB to KeyEvent.KEYCODE_BUTTON_THUMBL,
            XInputState.XINPUT_GAMEPAD_RIGHT_THUMB to KeyEvent.KEYCODE_BUTTON_THUMBR,
            XInputState.XINPUT_GAMEPAD_LEFT_SHOULDER to KeyEvent.KEYCODE_BUTTON_L1,
            XInputState.XINPUT_GAMEPAD_RIGHT_SHOULDER to KeyEvent.KEYCODE_BUTTON_R1,
            XInputState.XINPUT_GAMEPAD_A to KeyEvent.KEYCODE_BUTTON_A,
            XInputState.XINPUT_GAMEPAD_B to KeyEvent.KEYCODE_BUTTON_B,
            XInputState.XINPUT_GAMEPAD_X to KeyEvent.KEYCODE_BUTTON_X,
            XInputState.XINPUT_GAMEPAD_Y to KeyEvent.KEYCODE_BUTTON_Y
        )
    }
}
