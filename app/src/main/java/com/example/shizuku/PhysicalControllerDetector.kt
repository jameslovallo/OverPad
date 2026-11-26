package com.example.shizuku

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhysicalControllerInfo(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int
)

data class PhysicalControllerState(
    val controllers: List<PhysicalControllerInfo> = emptyList()
) {
    val isConnected: Boolean
        get() = controllers.isNotEmpty()

    val primaryName: String?
        get() = controllers.firstOrNull()?.name
}

/**
 * Pure detection helpers to distinguish real physical gamepads from OverPad's
 * own Shizuku-registered virtual Xbox device and non-gamepad system devices.
 */
object PhysicalControllerDetector {

    private val JOYSTICK_AXES = intArrayOf(
        android.view.MotionEvent.AXIS_X,
        android.view.MotionEvent.AXIS_Y,
        android.view.MotionEvent.AXIS_Z,
        android.view.MotionEvent.AXIS_RX,
        android.view.MotionEvent.AXIS_RY,
        android.view.MotionEvent.AXIS_RZ,
        android.view.MotionEvent.AXIS_HAT_X,
        android.view.MotionEvent.AXIS_HAT_Y
    )

    fun isPhysicalControllerCandidate(
        deviceId: Int,
        name: String,
        vendorId: Int,
        productId: Int,
        sources: Int,
        isVirtual: Boolean,
        isEnabled: Boolean,
        hasJoystickAxis: Boolean,
        ownVirtualDeviceId: Int,
        isOwnVirtualOverlayStarting: Boolean
    ): Boolean {
        if (deviceId <= 0 || isVirtual || !isEnabled) return false
        if (ownVirtualDeviceId > 0 && deviceId == ownVirtualDeviceId) return false

        // During the brief window while OverPad's own virtual bridge is registering in InputManager
        // before virtualDeviceId is published back over Binder, avoid misclassifying our own spoofed pad.
        if (isOwnVirtualOverlayStarting && ownVirtualDeviceId <= 0 && vendorId == VirtualXboxHidProtocol.MICROSOFT_VID) {
            if (productId == VirtualXboxHidProtocol.XBOX_UHID_PID && name == VirtualXboxHidProtocol.UHID_NAME) {
                return false
            }
            if (productId == VirtualXboxHidProtocol.XBOX_360_USB_PID && name == VirtualXboxHidProtocol.UINPUT_NAME) {
                return false
            }
        }

        val hasGamepadSource = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val hasJoystickSource = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        return hasGamepadSource || (hasJoystickSource && hasJoystickAxis)
    }

    fun scanConnectedControllers(
        ownVirtualDeviceId: Int,
        isOwnVirtualOverlayStarting: Boolean
    ): PhysicalControllerState {
        val detected = mutableListOf<PhysicalControllerInfo>()
        for (id in InputDevice.getDeviceIds()) {
            val dev = InputDevice.getDevice(id) ?: continue
            val isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                dev.isEnabled
            } else {
                true
            }
            val hasJoystickAxis = dev.motionRanges.any { range ->
                range.isFromSource(InputDevice.SOURCE_JOYSTICK) && range.axis in JOYSTICK_AXES
            }
            val name = dev.name ?: "Game Controller"
            if (isPhysicalControllerCandidate(
                    deviceId = id,
                    name = name,
                    vendorId = dev.vendorId,
                    productId = dev.productId,
                    sources = dev.sources,
                    isVirtual = dev.isVirtual,
                    isEnabled = isEnabled,
                    hasJoystickAxis = hasJoystickAxis,
                    ownVirtualDeviceId = ownVirtualDeviceId,
                    isOwnVirtualOverlayStarting = isOwnVirtualOverlayStarting
                )
            ) {
                detected += PhysicalControllerInfo(
                    deviceId = id,
                    name = name,
                    vendorId = dev.vendorId,
                    productId = dev.productId
                )
            }
        }
        return PhysicalControllerState(controllers = detected)
    }
}

class PhysicalControllerMonitor(
    context: Context,
    private val ownVirtualDeviceIdProvider: () -> Int,
    private val isOverlayActiveProvider: () -> Boolean,
    private val onStateChanged: (PhysicalControllerState) -> Unit = {}
) {
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(PhysicalControllerState())
    val state: StateFlow<PhysicalControllerState> = _state.asStateFlow()

    private var listenerRegistered = false

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            refresh()
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            refresh()
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            refresh()
        }
    }

    fun initialize() {
        if (!listenerRegistered) {
            runCatching {
                inputManager?.registerInputDeviceListener(deviceListener, mainHandler)
                listenerRegistered = true
            }
        }
        refresh()
    }

    fun refresh(): PhysicalControllerState {
        val next = PhysicalControllerDetector.scanConnectedControllers(
            ownVirtualDeviceId = ownVirtualDeviceIdProvider(),
            isOwnVirtualOverlayStarting = isOverlayActiveProvider()
        )
        val previous = _state.value
        _state.value = next
        if (next != previous) {
            onStateChanged(next)
        }
        return next
    }
}
