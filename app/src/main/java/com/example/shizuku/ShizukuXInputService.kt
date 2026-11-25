package com.example.shizuku

import android.annotation.SuppressLint
import android.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.Keep
import com.example.xinput.XInputState
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Remote privileged service launched inside Shizuku (shell UID 2000 or root UID 0).
 *
 * Emulates a real connected Xbox hardware controller in Android's InputManager by spawning
 * Android's built-in `/system/bin/uinput` (/dev/uinput) or `/system/bin/hid` (/dev/uhid)
 * virtual device bridges so cloud streaming apps (GeForce NOW, Xbox Cloud, Steam Link)
 * detect a physical controller, falling back to InputManager.injectInputEvent when locked.
 */
@Keep
class ShizukuXInputService() : IXInputInjector.Stub() {

    enum class BackendMode {
        STANDBY,
        UINPUT_XBOX360,
        UHID_XBOX,
        INJECT_EVENTS_ONLY
    }

    private val lock = Any()

    private var inputManagerInstance: Any? = null
    private var injectMethod: Method? = null

    private var backendMode: BackendMode = BackendMode.STANDBY
    private var virtualDeviceId: Int = -1
    private var diagnosticsMessage: String = "Overlay is off — virtual Xbox controller disconnected."

    private var bridgeProcess: Process? = null
    private var bridgeWriter: BufferedWriter? = null
    @Volatile
    private var lastProcessError: String? = null

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
    private var prevUinputLX = 0
    private var prevUinputLY = 0
    private var prevUinputLT = 0
    private var prevUinputRX = 0
    private var prevUinputRY = 0
    private var prevUinputRT = 0
    private var prevUinputHatX = 0
    private var prevUinputHatY = 0
    private var motionDownTime = 0L

    init {
        initInjectorReflection()
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun initInjectorReflection() {
        runCatching {
            val globalClass = runCatching {
                Class.forName("android.hardware.input.InputManagerGlobal")
            }.getOrNull()

            if (globalClass != null) {
                val instance = globalClass.getMethod("getInstance").invoke(null)
                val method = globalClass.getMethod(
                    "injectInputEvent",
                    InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )
                inputManagerInstance = instance
                injectMethod = method
                return
            }

            val imClass = InputManager::class.java
            val instance = imClass.getMethod("getInstance").invoke(null)
            val method = imClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
            inputManagerInstance = instance
            injectMethod = method
        }
    }

    override fun getUid(): Int = android.os.Process.myUid()

    override fun isUinputWritable(): Boolean {
        val f = File("/dev/uinput")
        return f.exists() && f.canWrite()
    }

    override fun getBackendMode(): String = synchronized(lock) { backendMode.name }

    override fun getVirtualDeviceId(): Int = synchronized(lock) {
        if (virtualDeviceId > 0 && InputDevice.getDevice(virtualDeviceId) == null) {
            virtualDeviceId = findMatchingGamepadDeviceId(
                preferredVendor = VirtualXboxHidProtocol.MICROSOFT_VID,
                preferredProducts = setOf(
                    VirtualXboxHidProtocol.XBOX_360_USB_PID,
                    VirtualXboxHidProtocol.XBOX_UHID_PID
                ),
                excludedIds = emptySet()
            )
        }
        virtualDeviceId
    }

    override fun getDiagnostics(): String = synchronized(lock) { diagnosticsMessage }

    override fun ensureVirtualGamepad(): Boolean = synchronized(lock) {
        if (bridgeProcess?.isAlive == true && virtualDeviceId > 0 && InputDevice.getDevice(virtualDeviceId) != null) {
            return true
        }
        closeBridgeProcessLocked(virtualDeviceId)
        resetTrackingStateLocked()

        val uinputError: String?
        if (tryStartUinputXbox360Locked()) {
            return true
        } else {
            uinputError = lastProcessError
        }

        val uhidError: String?
        if (tryStartUhidXboxLocked()) {
            return true
        } else {
            uhidError = lastProcessError
        }

        backendMode = BackendMode.INJECT_EVENTS_ONLY
        virtualDeviceId = -1
        diagnosticsMessage = buildString {
            append("Using INJECT_EVENTS fallback (no kernel virtual controller). ")
            if (!uinputError.isNullOrBlank()) append("uinput: $uinputError. ")
            if (!uhidError.isNullOrBlank()) append("uhid: $uhidError.")
        }.trim()
        false
    }

    override fun disconnectVirtualGamepad() = synchronized(lock) {
        val prevId = virtualDeviceId
        if (backendMode != BackendMode.STANDBY) {
            runCatching { sendGamepadState(0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) }
        }
        closeBridgeProcessLocked(prevId)
        resetTrackingStateLocked()
        backendMode = BackendMode.STANDBY
        virtualDeviceId = -1
        diagnosticsMessage = "Overlay is off — virtual Xbox controller disconnected."
    }

    private fun tryStartUinputXbox360Locked(): Boolean {
        val pb = buildBridgeProcessBuilder(
            jarPath = "/system/framework/uinput.jar",
            mainClass = "com.android.commands.uinput.Uinput",
            fallbackBin = "/system/bin/uinput"
        )
        return startBridgeAndVerifyLocked(
            processBuilder = pb,
            mode = BackendMode.UINPUT_XBOX360,
            registerJson = VirtualXboxHidProtocol.buildUinputRegisterJson(1),
            delayJson = VirtualXboxHidProtocol.buildUhidDelayJson(1, 100),
            expectedPid = VirtualXboxHidProtocol.XBOX_360_USB_PID,
            sendNeutralState = {
                writeLineLocked(
                    VirtualXboxHidProtocol.buildUinputInjectJson(
                        1,
                        listOf(
                            VirtualXboxHidProtocol.EV_ABS, VirtualXboxHidProtocol.ABS_X, 0,
                            VirtualXboxHidProtocol.EV_ABS, VirtualXboxHidProtocol.ABS_Y, 0,
                            VirtualXboxHidProtocol.EV_ABS, VirtualXboxHidProtocol.ABS_Z, 0,
                            VirtualXboxHidProtocol.EV_ABS, VirtualXboxHidProtocol.ABS_RX, 0,
                            VirtualXboxHidProtocol.EV_ABS, VirtualXboxHidProtocol.ABS_RY, 0,
                            VirtualXboxHidProtocol.EV_ABS, VirtualXboxHidProtocol.ABS_RZ, 0,
                            VirtualXboxHidProtocol.EV_SYN, VirtualXboxHidProtocol.SYN_REPORT, 0
                        )
                    )
                )
            }
        )
    }

    private fun tryStartUhidXboxLocked(): Boolean {
        val pb = buildBridgeProcessBuilder(
            jarPath = "/system/framework/hid.jar",
            mainClass = "com.android.commands.hid.Hid",
            fallbackBin = "/system/bin/hid"
        )
        return startBridgeAndVerifyLocked(
            processBuilder = pb,
            mode = BackendMode.UHID_XBOX,
            registerJson = VirtualXboxHidProtocol.buildUhidRegisterJson(1),
            delayJson = VirtualXboxHidProtocol.buildUhidDelayJson(1, 100),
            expectedPid = VirtualXboxHidProtocol.XBOX_UHID_PID,
            sendNeutralState = {
                val neutral = VirtualXboxHidProtocol.buildUhidReportBytes(
                    0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
                )
                writeLineLocked(VirtualXboxHidProtocol.buildUhidReportJson(1, neutral))
            }
        )
    }

    private fun buildBridgeProcessBuilder(
        jarPath: String,
        mainClass: String,
        fallbackBin: String
    ): ProcessBuilder {
        val jarExists = File(jarPath).exists()
        val appProcess = listOf("/system/bin/app_process", "/system/bin/app_process64", "/system/bin/app_process32")
            .firstOrNull { File(it).exists() }
        return if (jarExists && appProcess != null) {
            // Invoke app_process directly without /system/bin/sh wrapper so Process.destroyForcibly()
            // kills the exact process holding /dev/uinput or /dev/uhid.
            ProcessBuilder(listOf(appProcess, "/system/bin", mainClass, "-")).apply {
                environment()["CLASSPATH"] = jarPath
            }
        } else {
            ProcessBuilder(listOf("sh", "-c", "exec $fallbackBin -"))
        }
    }

    private fun startBridgeAndVerifyLocked(
        processBuilder: ProcessBuilder,
        mode: BackendMode,
        registerJson: String,
        delayJson: String,
        expectedPid: Int,
        sendNeutralState: () -> Unit
    ): Boolean {
        lastProcessError = null
        val beforeIds = InputDevice.getDeviceIds().toSet()
        val proc = runCatching {
            processBuilder
                .redirectErrorStream(true)
                .start()
        }.getOrElse { err ->
            lastProcessError = err.message ?: "spawn failed"
            return false
        }

        // Drain stdout+stderr asynchronously so subprocess pipes never block
        Thread {
            runCatching {
                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.isNotBlank()) {
                            lastProcessError = line.take(140)
                        }
                    }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }

        val writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
        bridgeProcess = proc
        bridgeWriter = writer

        val wrote = runCatching {
            writer.write(registerJson)
            writer.newLine()
            writer.write(delayJson)
            writer.newLine()
            writer.flush()
            true
        }.getOrDefault(false)

        if (!wrote) {
            closeBridgeProcessLocked()
            return false
        }

        // Poll InputDevice registration for up to ~1600ms
        var resolvedId = -1
        for (attempt in 0 until 22) {
            if (!proc.isAlive) {
                break
            }
            resolvedId = findMatchingGamepadDeviceId(
                preferredVendor = VirtualXboxHidProtocol.MICROSOFT_VID,
                preferredProducts = setOf(expectedPid),
                excludedIds = beforeIds
            )
            if (resolvedId > 0) break
            runCatching { Thread.sleep(75L) }
        }

        if (!proc.isAlive) {
            if (lastProcessError.isNullOrBlank()) {
                lastProcessError = "exited (${runCatching { proc.exitValue() }.getOrDefault(-1)})"
            }
            closeBridgeProcessLocked()
            return false
        }

        if (resolvedId <= 0) {
            // One final check in case InputReader added it without exact VID/PID match on OEM ROM
            resolvedId = findAnyNewGamepadDeviceId(beforeIds)
        }

        if (resolvedId <= 0) {
            lastProcessError = lastProcessError ?: "kernel did not expose InputDevice"
            closeBridgeProcessLocked()
            return false
        }

        backendMode = mode
        virtualDeviceId = resolvedId
        val devName = InputDevice.getDevice(resolvedId)?.name ?: mode.name
        diagnosticsMessage = "Registered '$devName' (Device #$resolvedId) via ${mode.name}"
        runCatching { sendNeutralState() }
        return true
    }

    private fun findMatchingGamepadDeviceId(
        preferredVendor: Int,
        preferredProducts: Set<Int>,
        excludedIds: Set<Int>
    ): Int {
        for (id in InputDevice.getDeviceIds()) {
            val dev = InputDevice.getDevice(id) ?: continue
            val isGamepad = (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (dev.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (!isGamepad) continue
            if (dev.vendorId == preferredVendor && dev.productId in preferredProducts) {
                if (excludedIds.isEmpty() || id !in excludedIds) {
                    return id
                }
            }
        }
        return -1
    }

    private fun findAnyNewGamepadDeviceId(beforeIds: Set<Int>): Int {
        for (id in InputDevice.getDeviceIds()) {
            if (id in beforeIds) continue
            val dev = InputDevice.getDevice(id) ?: continue
            val isGamepad = (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (dev.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isGamepad) return id
        }
        return -1
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
    ) = synchronized(lock) {
        if (backendMode == BackendMode.STANDBY) {
            return@synchronized
        }
        if (backendMode != BackendMode.INJECT_EVENTS_ONLY && bridgeProcess?.isAlive != true) {
            closeBridgeProcessLocked()
            backendMode = BackendMode.INJECT_EVENTS_ONLY
            virtualDeviceId = -1
        }

        when (backendMode) {
            BackendMode.STANDBY -> Unit
            BackendMode.UHID_XBOX -> sendViaUhidLocked(
                buttonsMask, leftTrigger, rightTrigger,
                thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY
            )
            BackendMode.UINPUT_XBOX360 -> sendViaUinputLocked(
                buttonsMask, leftTrigger, rightTrigger,
                thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY
            )
            BackendMode.INJECT_EVENTS_ONLY -> sendViaInjectEventsLocked(
                buttonsMask, leftTrigger, rightTrigger,
                thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY
            )
        }
    }

    private fun sendViaUhidLocked(
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
        if (!hasStateChanged(buttonsMask, leftTrigger, rightTrigger, thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY)) {
            return
        }
        updatePrevState(buttonsMask, leftTrigger, rightTrigger, thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY)

        val report = VirtualXboxHidProtocol.buildUhidReportBytes(
            buttonsMask = buttonsMask,
            leftTrigger = leftTrigger,
            rightTrigger = rightTrigger,
            thumbLX = thumbLX,
            thumbLY = thumbLY,
            thumbRX = thumbRX,
            thumbRY = thumbRY,
            hatX = hatX,
            hatY = hatY
        )
        if (!writeLineLocked(VirtualXboxHidProtocol.buildUhidReportJson(1, report))) {
            fallbackToInjectEventsLocked(
                buttonsMask, leftTrigger, rightTrigger,
                thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY
            )
        }
    }

    private fun sendViaUinputLocked(
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
        val events = mutableListOf<Int>()
        val changedMask = prevButtonsMask xor buttonsMask
        if (changedMask != 0) {
            for ((bit, linuxCode) in XINPUT_BIT_TO_LINUX_KEY) {
                if ((changedMask and bit) != 0) {
                    events.add(VirtualXboxHidProtocol.EV_KEY)
                    events.add(linuxCode)
                    events.add(if ((buttonsMask and bit) != 0) 1 else 0)
                }
            }
        }

        val lx = VirtualXboxHidProtocol.scaleStickInt16(thumbLX)
        val ly = VirtualXboxHidProtocol.scaleStickInt16(thumbLY)
        val rx = VirtualXboxHidProtocol.scaleStickInt16(thumbRX)
        val ry = VirtualXboxHidProtocol.scaleStickInt16(thumbRY)
        val lt = (leftTrigger.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        val rt = (rightTrigger.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        val hx = when {
            hatX > 0.5f -> 1
            hatX < -0.5f -> -1
            else -> 0
        }
        val hy = when {
            hatY > 0.5f -> 1
            hatY < -0.5f -> -1
            else -> 0
        }

        if (lx != prevUinputLX) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_X); events.add(lx)
        }
        if (ly != prevUinputLY) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_Y); events.add(ly)
        }
        if (lt != prevUinputLT) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_Z); events.add(lt)
        }
        if (rx != prevUinputRX) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_RX); events.add(rx)
        }
        if (ry != prevUinputRY) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_RY); events.add(ry)
        }
        if (rt != prevUinputRT) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_RZ); events.add(rt)
        }
        if (hx != prevUinputHatX) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_HAT0X); events.add(hx)
        }
        if (hy != prevUinputHatY) {
            events.add(VirtualXboxHidProtocol.EV_ABS); events.add(VirtualXboxHidProtocol.ABS_HAT0Y); events.add(hy)
        }

        if (events.isEmpty()) return

        updatePrevState(buttonsMask, leftTrigger, rightTrigger, thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY)
        prevUinputLX = lx
        prevUinputLY = ly
        prevUinputLT = lt
        prevUinputRX = rx
        prevUinputRY = ry
        prevUinputRT = rt
        prevUinputHatX = hx
        prevUinputHatY = hy
        events.add(VirtualXboxHidProtocol.EV_SYN)
        events.add(VirtualXboxHidProtocol.SYN_REPORT)
        events.add(0)

        if (!writeLineLocked(VirtualXboxHidProtocol.buildUinputInjectJson(1, events))) {
            fallbackToInjectEventsLocked(
                buttonsMask, leftTrigger, rightTrigger,
                thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY
            )
        }
    }

    private fun fallbackToInjectEventsLocked(
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
        closeBridgeProcessLocked()
        backendMode = BackendMode.INJECT_EVENTS_ONLY
        virtualDeviceId = -1
        sendViaInjectEventsLocked(
            buttonsMask, leftTrigger, rightTrigger,
            thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY
        )
    }

    private fun sendViaInjectEventsLocked(
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

        val changedMask = prevButtonsMask xor buttonsMask
        if (changedMask != 0) {
            for ((bit, keyCode) in BUTTON_BIT_TO_KEYCODE) {
                if ((changedMask and bit) != 0) {
                    injectKey(keyCode, (buttonsMask and bit) != 0, now)
                }
            }
            prevButtonsMask = buttonsMask
        }

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

        if (hasAxesChanged(leftTrigger, rightTrigger, thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY)) {
            prevLX = thumbLX
            prevLY = thumbLY
            prevRX = thumbRX
            prevRY = thumbRY
            prevLT = leftTrigger
            prevRT = rightTrigger
            prevHatX = hatX
            prevHatY = hatY

            if (motionDownTime == 0L) motionDownTime = now

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

    private fun hasStateChanged(
        buttonsMask: Int,
        leftTrigger: Float,
        rightTrigger: Float,
        thumbLX: Float,
        thumbLY: Float,
        thumbRX: Float,
        thumbRY: Float,
        hatX: Float,
        hatY: Float
    ): Boolean {
        val ltDown = leftTrigger >= 0.5f
        val rtDown = rightTrigger >= 0.5f
        return buttonsMask != prevButtonsMask ||
            ltDown != prevLtDown ||
            rtDown != prevRtDown ||
            hasAxesChanged(leftTrigger, rightTrigger, thumbLX, thumbLY, thumbRX, thumbRY, hatX, hatY)
    }

    private fun hasAxesChanged(
        leftTrigger: Float,
        rightTrigger: Float,
        thumbLX: Float,
        thumbLY: Float,
        thumbRX: Float,
        thumbRY: Float,
        hatX: Float,
        hatY: Float
    ): Boolean {
        val returningToZero =
            (thumbLX == 0f && prevLX != 0f) ||
                (thumbLY == 0f && prevLY != 0f) ||
                (thumbRX == 0f && prevRX != 0f) ||
                (thumbRY == 0f && prevRY != 0f) ||
                (leftTrigger == 0f && prevLT != 0f) ||
                (rightTrigger == 0f && prevRT != 0f) ||
                (hatX == 0f && prevHatX != 0f) ||
                (hatY == 0f && prevHatY != 0f)

        return returningToZero ||
            abs(thumbLX - prevLX) > AXIS_EPSILON ||
            abs(thumbLY - prevLY) > AXIS_EPSILON ||
            abs(thumbRX - prevRX) > AXIS_EPSILON ||
            abs(thumbRY - prevRY) > AXIS_EPSILON ||
            abs(leftTrigger - prevLT) > AXIS_EPSILON ||
            abs(rightTrigger - prevRT) > AXIS_EPSILON ||
            abs(hatX - prevHatX) > AXIS_EPSILON ||
            abs(hatY - prevHatY) > AXIS_EPSILON
    }

    private fun updatePrevState(
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
        prevButtonsMask = buttonsMask
        prevLtDown = leftTrigger >= 0.5f
        prevRtDown = rightTrigger >= 0.5f
        prevLX = thumbLX
        prevLY = thumbLY
        prevRX = thumbRX
        prevRY = thumbRY
        prevLT = leftTrigger
        prevRT = rightTrigger
        prevHatX = hatX
        prevHatY = hatY
    }

    private fun writeLineLocked(line: String): Boolean {
        val writer = bridgeWriter ?: return false
        return runCatching {
            writer.write(line)
            writer.newLine()
            writer.flush()
            true
        }.getOrDefault(false)
    }

    private fun closeBridgeProcessLocked(previousDeviceId: Int = virtualDeviceId) {
        runCatching { bridgeWriter?.flush() }
        runCatching { bridgeWriter?.close() }
        bridgeWriter = null
        val proc = bridgeProcess
        bridgeProcess = null
        if (proc != null) {
            runCatching { proc.outputStream.close() }
            val graceful = runCatching { proc.waitFor(50L, TimeUnit.MILLISECONDS) }.getOrDefault(false)
            if (!graceful || proc.isAlive) {
                runCatching { proc.destroyForcibly() }
                runCatching { proc.waitFor(180L, TimeUnit.MILLISECONDS) }
            }
        }

        fun hasLingeringVirtualGamepad(): Boolean {
            if (previousDeviceId > 0 && InputDevice.getDevice(previousDeviceId) != null) return true
            return findMatchingGamepadDeviceId(
                preferredVendor = VirtualXboxHidProtocol.MICROSOFT_VID,
                preferredProducts = setOf(
                    VirtualXboxHidProtocol.XBOX_360_USB_PID,
                    VirtualXboxHidProtocol.XBOX_UHID_PID
                ),
                excludedIds = emptySet()
            ) > 0
        }

        if (hasLingeringVirtualGamepad()) {
            var cleared = false
            for (i in 0 until 8) {
                if (!hasLingeringVirtualGamepad()) {
                    cleared = true
                    break
                }
                runCatching { Thread.sleep(35L) }
            }
            if (!cleared) {
                // Safety backstop: kill any orphaned Android uinput/hid bridge holding the device fd
                runCatching {
                    val killer = ProcessBuilder(
                        "pkill",
                        "-9",
                        "-f",
                        "com\\.android\\.commands\\.(uinput\\.Uinput|hid\\.Hid)"
                    ).start()
                    killer.waitFor(400L, TimeUnit.MILLISECONDS)
                }
                for (i in 0 until 10) {
                    if (!hasLingeringVirtualGamepad()) break
                    runCatching { Thread.sleep(40L) }
                }
            }
        }
    }

    private fun resetTrackingStateLocked() {
        prevButtonsMask = 0
        prevLtDown = false
        prevRtDown = false
        prevLX = 0f
        prevLY = 0f
        prevRX = 0f
        prevRY = 0f
        prevLT = 0f
        prevRT = 0f
        prevHatX = 0f
        prevHatY = 0f
        prevUinputLX = 0
        prevUinputLY = 0
        prevUinputLT = 0
        prevUinputRX = 0
        prevUinputRY = 0
        prevUinputRT = 0
        prevUinputHatX = 0
        prevUinputHatY = 0
        motionDownTime = 0L
    }

    override fun resetAll() = synchronized(lock) {
        if (backendMode != BackendMode.STANDBY) {
            sendGamepadState(0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
        motionDownTime = 0L
    }

    override fun destroy() {
        disconnectVirtualGamepad()
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
        private const val AXIS_EPSILON = 0.0005f

        private val XINPUT_BIT_TO_LINUX_KEY = listOf(
            XInputState.XINPUT_GAMEPAD_A to VirtualXboxHidProtocol.BTN_A,
            XInputState.XINPUT_GAMEPAD_B to VirtualXboxHidProtocol.BTN_B,
            XInputState.XINPUT_GAMEPAD_X to VirtualXboxHidProtocol.BTN_X,
            XInputState.XINPUT_GAMEPAD_Y to VirtualXboxHidProtocol.BTN_Y,
            XInputState.XINPUT_GAMEPAD_LEFT_SHOULDER to VirtualXboxHidProtocol.BTN_TL,
            XInputState.XINPUT_GAMEPAD_RIGHT_SHOULDER to VirtualXboxHidProtocol.BTN_TR,
            XInputState.XINPUT_GAMEPAD_BACK to VirtualXboxHidProtocol.BTN_SELECT,
            XInputState.XINPUT_GAMEPAD_START to VirtualXboxHidProtocol.BTN_START,
            XInputState.XINPUT_GAMEPAD_LEFT_THUMB to VirtualXboxHidProtocol.BTN_THUMBL,
            XInputState.XINPUT_GAMEPAD_RIGHT_THUMB to VirtualXboxHidProtocol.BTN_THUMBR
        )

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
