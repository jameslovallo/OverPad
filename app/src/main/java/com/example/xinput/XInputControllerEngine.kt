package com.example.xinput

import android.view.KeyEvent
import com.example.model.ControlType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.abs

data class DPadDirectionState(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false
)

enum class DriverCapabilityStatus(val title: String, val detail: String) {
    SANDBOX_AND_OVERLAY_READY(
        title = "Virtual XInput Engine Active",
        detail = "Generating 120Hz XInput 1.4 reports & Android gamepad axes. System-wide kernel /dev/uinput injection requires root or ADB shell permissions on standard Android; test live in Sandbox or forward via UDP Bridge."
    ),
    UINPUT_WRITABLE(
        title = "Kernel /dev/uinput Writable",
        detail = "Direct virtual Xbox 360 HID descriptor path (/dev/uinput) is accessible on this device."
    )
}

/**
 * Thread-safe multi-source XInput state aggregator.
 * Simultaneously merges Left Stick, Right Stick, FPS Right-Side Look Drag,
 * LT/RT Trigger Hold+Drag Aim, D-Pad, and fixed gamepad buttons without pointer conflicts.
 */
class XInputControllerEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private var leftStickX = 0f
    private var leftStickY = 0f

    private var directRightStickX = 0f
    private var directRightStickY = 0f

    private var fpsZoneStickX = 0f
    private var fpsZoneStickY = 0f

    private var ltDragStickX = 0f
    private var ltDragStickY = 0f

    private var rtDragStickX = 0f
    private var rtDragStickY = 0f

    private var dpad = DPadDirectionState()
    private val pressedControls = mutableSetOf<ControlType>()
    private var packetCounter = 0L

    @Volatile
    private var udpEnabled = false
    @Volatile
    private var udpHost = "127.0.0.1"
    @Volatile
    private var udpPort = 26760
    private var udpSocket: DatagramSocket? = null

    private val _state = MutableStateFlow(XInputState())
    val state: StateFlow<XInputState> = _state.asStateFlow()

    val driverCapability: DriverCapabilityStatus = detectCapability()

    private fun detectCapability(): DriverCapabilityStatus {
        val uinput = File("/dev/uinput")
        return if (uinput.exists() && uinput.canWrite()) {
            DriverCapabilityStatus.UINPUT_WRITABLE
        } else {
            DriverCapabilityStatus.SANDBOX_AND_OVERLAY_READY
        }
    }

    fun configureUdpRelay(enabled: Boolean, host: String, port: Int) {
        udpEnabled = enabled
        udpHost = host.ifBlank { "127.0.0.1" }
        udpPort = port.coerceIn(1024, 65535)
        if (!enabled) {
            runCatching { udpSocket?.close() }
            udpSocket = null
        }
    }

    fun updateLeftStick(x: Float, y: Float) {
        synchronized(lock) {
            leftStickX = x.coerceIn(-1f, 1f)
            leftStickY = y.coerceIn(-1f, 1f)
            publishLocked()
        }
    }

    fun updateRightStickDirect(x: Float, y: Float) {
        synchronized(lock) {
            directRightStickX = x.coerceIn(-1f, 1f)
            directRightStickY = y.coerceIn(-1f, 1f)
            publishLocked()
        }
    }

    fun updateFpsLookZoneStick(x: Float, y: Float) {
        synchronized(lock) {
            fpsZoneStickX = x.coerceIn(-1f, 1f)
            fpsZoneStickY = y.coerceIn(-1f, 1f)
            publishLocked()
        }
    }

    fun updateTriggerWithDragAim(
        trigger: ControlType,
        pressed: Boolean,
        dragStickX: Float = 0f,
        dragStickY: Float = 0f
    ) {
        synchronized(lock) {
            if (pressed) {
                pressedControls.add(trigger)
            } else {
                pressedControls.remove(trigger)
            }
            when (trigger) {
                ControlType.TRIGGER_LT -> {
                    ltDragStickX = if (pressed) dragStickX.coerceIn(-1f, 1f) else 0f
                    ltDragStickY = if (pressed) dragStickY.coerceIn(-1f, 1f) else 0f
                }
                ControlType.TRIGGER_RT -> {
                    rtDragStickX = if (pressed) dragStickX.coerceIn(-1f, 1f) else 0f
                    rtDragStickY = if (pressed) dragStickY.coerceIn(-1f, 1f) else 0f
                }
                else -> Unit
            }
            publishLocked()
        }
    }

    fun setButtonPressed(control: ControlType, pressed: Boolean) {
        if (control == ControlType.SETTINGS) return
        synchronized(lock) {
            if (pressed) {
                pressedControls.add(control)
            } else {
                pressedControls.remove(control)
            }
            publishLocked()
        }
    }

    fun updateDPad(direction: DPadDirectionState) {
        synchronized(lock) {
            dpad = direction
            publishLocked()
        }
    }

    fun resetAll() {
        synchronized(lock) {
            leftStickX = 0f
            leftStickY = 0f
            directRightStickX = 0f
            directRightStickY = 0f
            fpsZoneStickX = 0f
            fpsZoneStickY = 0f
            ltDragStickX = 0f
            ltDragStickY = 0f
            rtDragStickX = 0f
            rtDragStickY = 0f
            dpad = DPadDirectionState()
            pressedControls.clear()
            publishLocked()
        }
    }

    private fun publishLocked() {
        val combinedRX = (directRightStickX + fpsZoneStickX + ltDragStickX + rtDragStickX).coerceIn(-1f, 1f)
        val combinedRY = (directRightStickY + fpsZoneStickY + ltDragStickY + rtDragStickY).coerceIn(-1f, 1f)

        var mask = 0
        val keys = mutableSetOf<Int>()

        if (dpad.up) {
            mask = mask or XInputState.XINPUT_GAMEPAD_DPAD_UP
            keys += KeyEvent.KEYCODE_DPAD_UP
        }
        if (dpad.down) {
            mask = mask or XInputState.XINPUT_GAMEPAD_DPAD_DOWN
            keys += KeyEvent.KEYCODE_DPAD_DOWN
        }
        if (dpad.left) {
            mask = mask or XInputState.XINPUT_GAMEPAD_DPAD_LEFT
            keys += KeyEvent.KEYCODE_DPAD_LEFT
        }
        if (dpad.right) {
            mask = mask or XInputState.XINPUT_GAMEPAD_DPAD_RIGHT
            keys += KeyEvent.KEYCODE_DPAD_RIGHT
        }

        if (ControlType.BUTTON_START in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_START
            keys += KeyEvent.KEYCODE_BUTTON_START
        }
        if (ControlType.BUTTON_BACK in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_BACK
            keys += KeyEvent.KEYCODE_BUTTON_SELECT
        }
        if (ControlType.BUTTON_L3 in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_LEFT_THUMB
            keys += KeyEvent.KEYCODE_BUTTON_THUMBL
        }
        if (ControlType.BUTTON_R3 in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_RIGHT_THUMB
            keys += KeyEvent.KEYCODE_BUTTON_THUMBR
        }
        if (ControlType.BUTTON_LB in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_LEFT_SHOULDER
            keys += KeyEvent.KEYCODE_BUTTON_L1
        }
        if (ControlType.BUTTON_RB in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_RIGHT_SHOULDER
            keys += KeyEvent.KEYCODE_BUTTON_R1
        }
        if (ControlType.BUTTON_A in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_A
            keys += KeyEvent.KEYCODE_BUTTON_A
        }
        if (ControlType.BUTTON_B in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_B
            keys += KeyEvent.KEYCODE_BUTTON_B
        }
        if (ControlType.BUTTON_X in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_X
            keys += KeyEvent.KEYCODE_BUTTON_X
        }
        if (ControlType.BUTTON_Y in pressedControls) {
            mask = mask or XInputState.XINPUT_GAMEPAD_Y
            keys += KeyEvent.KEYCODE_BUTTON_Y
        }

        val ltDown = ControlType.TRIGGER_LT in pressedControls
        val rtDown = ControlType.TRIGGER_RT in pressedControls
        if (ltDown) keys += KeyEvent.KEYCODE_BUTTON_L2
        if (rtDown) keys += KeyEvent.KEYCODE_BUTTON_R2

        val hatX = when {
            dpad.left && !dpad.right -> -1f
            dpad.right && !dpad.left -> 1f
            else -> 0f
        }
        val hatY = when {
            dpad.up && !dpad.down -> -1f
            dpad.down && !dpad.up -> 1f
            else -> 0f
        }

        val sources = mutableSetOf<String>()
        if (abs(leftStickX) > 0.02f || abs(leftStickY) > 0.02f) sources += "Left Stick"
        if (abs(directRightStickX) > 0.02f || abs(directRightStickY) > 0.02f) sources += "Right Stick"
        if (abs(fpsZoneStickX) > 0.02f || abs(fpsZoneStickY) > 0.02f) sources += "FPS Look Zone"
        if (ltDown && (abs(ltDragStickX) > 0.02f || abs(ltDragStickY) > 0.02f)) sources += "LT+Drag Aim"
        if (rtDown && (abs(rtDragStickX) > 0.02f || abs(rtDragStickY) > 0.02f)) sources += "RT+Drag Aim"

        packetCounter += 1L
        val next = XInputState(
            buttonsMask = mask,
            leftTrigger = if (ltDown) 1f else 0f,
            rightTrigger = if (rtDown) 1f else 0f,
            thumbLX = leftStickX,
            thumbLY = leftStickY,
            thumbRX = combinedRX,
            thumbRY = combinedRY,
            dpadUp = dpad.up,
            dpadDown = dpad.down,
            dpadLeft = dpad.left,
            dpadRight = dpad.right,
            hatX = hatX,
            hatY = hatY,
            activeKeyCodes = keys,
            activeSources = sources,
            packetCounter = packetCounter
        )
        _state.value = next

        if (udpEnabled) {
            val packetBytes = next.toBinaryPacket()
            val targetHost = udpHost
            val targetPort = udpPort
            scope.launch {
                runCatching {
                    val socket = udpSocket ?: DatagramSocket().also { udpSocket = it }
                    val address = InetAddress.getByName(targetHost)
                    socket.send(DatagramPacket(packetBytes, packetBytes.size, address, targetPort))
                }
            }
        }
    }
}
