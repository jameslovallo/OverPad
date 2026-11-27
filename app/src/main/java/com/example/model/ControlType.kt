package com.example.model

import android.view.KeyEvent

enum class ButtonShape(val label: String) {
    CIRCLE("Circle"),
    ROUNDED("Rounded"),
    PILL("Pill"),
    SQUARE("Square")
}

/**
 * Fixed, non-remappable XInput gamepad controls + the Settings overlay button.
 * Each gamepad control maps immutably to its canonical XInput / Android input target.
 */
enum class ControlType(
    val displayName: String,
    val shortLabel: String,
    val xinputBinding: String,
    val androidCodeSummary: String,
    val baseWidthDp: Float,
    val baseHeightDp: Float,
    val canHide: Boolean = true,
    val supportsDragAim: Boolean = false,
    val supportsShape: Boolean = true,
    val supportsToggle: Boolean = true,
    val defaultShape: ButtonShape = ButtonShape.CIRCLE,
    val keyCode: Int? = null
) {
    LEFT_STICK(
        displayName = "Left Thumbstick",
        shortLabel = "LS",
        xinputBinding = "XInput sThumbLX / sThumbLY",
        androidCodeSummary = "AXIS_X / AXIS_Y",
        baseWidthDp = 112f,
        baseHeightDp = 112f,
        supportsShape = false,
        supportsToggle = false
    ),
    RIGHT_STICK(
        displayName = "Right Thumbstick",
        shortLabel = "RS",
        xinputBinding = "XInput sThumbRX / sThumbRY",
        androidCodeSummary = "AXIS_Z / AXIS_RZ",
        baseWidthDp = 112f,
        baseHeightDp = 112f,
        supportsShape = false,
        supportsToggle = false
    ),
    DPAD(
        displayName = "D-Pad",
        shortLabel = "D-PAD",
        xinputBinding = "XInput DPAD_UP/DOWN/LEFT/RIGHT",
        androidCodeSummary = "AXIS_HAT_X/Y + DPAD",
        baseWidthDp = 116f,
        baseHeightDp = 116f,
        supportsShape = true,
        supportsToggle = false,
        defaultShape = ButtonShape.CIRCLE
    ),
    BUTTON_A(
        displayName = "A Button",
        shortLabel = "A",
        xinputBinding = "XINPUT_GAMEPAD_A (0x1000)",
        androidCodeSummary = "KEYCODE_BUTTON_A",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.CIRCLE,
        keyCode = KeyEvent.KEYCODE_BUTTON_A
    ),
    BUTTON_B(
        displayName = "B Button",
        shortLabel = "B",
        xinputBinding = "XINPUT_GAMEPAD_B (0x2000)",
        androidCodeSummary = "KEYCODE_BUTTON_B",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.CIRCLE,
        keyCode = KeyEvent.KEYCODE_BUTTON_B
    ),
    BUTTON_X(
        displayName = "X Button",
        shortLabel = "X",
        xinputBinding = "XINPUT_GAMEPAD_X (0x4000)",
        androidCodeSummary = "KEYCODE_BUTTON_X",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.CIRCLE,
        keyCode = KeyEvent.KEYCODE_BUTTON_X
    ),
    BUTTON_Y(
        displayName = "Y Button",
        shortLabel = "Y",
        xinputBinding = "XINPUT_GAMEPAD_Y (0x8000)",
        androidCodeSummary = "KEYCODE_BUTTON_Y",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.CIRCLE,
        keyCode = KeyEvent.KEYCODE_BUTTON_Y
    ),
    BUTTON_LB(
        displayName = "Left Bumper",
        shortLabel = "LB",
        xinputBinding = "XINPUT_GAMEPAD_LEFT_SHOULDER (0x0100)",
        androidCodeSummary = "KEYCODE_BUTTON_L1",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.PILL,
        keyCode = KeyEvent.KEYCODE_BUTTON_L1
    ),
    BUTTON_RB(
        displayName = "Right Bumper",
        shortLabel = "RB",
        xinputBinding = "XINPUT_GAMEPAD_RIGHT_SHOULDER (0x0200)",
        androidCodeSummary = "KEYCODE_BUTTON_R1",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.PILL,
        keyCode = KeyEvent.KEYCODE_BUTTON_R1
    ),
    TRIGGER_LT(
        displayName = "Left Trigger (Hold+Drag Aim)",
        shortLabel = "LT",
        xinputBinding = "XInput bLeftTrigger (0..255)",
        androidCodeSummary = "AXIS_LTRIGGER + L2",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        supportsDragAim = true,
        defaultShape = ButtonShape.ROUNDED,
        keyCode = KeyEvent.KEYCODE_BUTTON_L2
    ),
    TRIGGER_RT(
        displayName = "Right Trigger (Hold+Drag Aim)",
        shortLabel = "RT",
        xinputBinding = "XInput bRightTrigger (0..255)",
        androidCodeSummary = "AXIS_RTRIGGER + R2",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        supportsDragAim = true,
        defaultShape = ButtonShape.ROUNDED,
        keyCode = KeyEvent.KEYCODE_BUTTON_R2
    ),
    BUTTON_L3(
        displayName = "Left Stick Click",
        shortLabel = "L",
        xinputBinding = "XINPUT_GAMEPAD_LEFT_THUMB (0x0040)",
        androidCodeSummary = "KEYCODE_BUTTON_THUMBL",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.CIRCLE,
        keyCode = KeyEvent.KEYCODE_BUTTON_THUMBL
    ),
    BUTTON_R3(
        displayName = "Right Stick Click",
        shortLabel = "R",
        xinputBinding = "XINPUT_GAMEPAD_RIGHT_THUMB (0x0080)",
        androidCodeSummary = "KEYCODE_BUTTON_THUMBR",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.CIRCLE,
        keyCode = KeyEvent.KEYCODE_BUTTON_THUMBR
    ),
    BUTTON_BACK(
        displayName = "Back / View",
        shortLabel = "BACK",
        xinputBinding = "XINPUT_GAMEPAD_BACK (0x0020)",
        androidCodeSummary = "KEYCODE_BUTTON_SELECT",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.PILL,
        keyCode = KeyEvent.KEYCODE_BUTTON_SELECT
    ),
    BUTTON_START(
        displayName = "Start / Menu",
        shortLabel = "START",
        xinputBinding = "XINPUT_GAMEPAD_START (0x0010)",
        androidCodeSummary = "KEYCODE_BUTTON_START",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        defaultShape = ButtonShape.PILL,
        keyCode = KeyEvent.KEYCODE_BUTTON_START
    ),
    SETTINGS(
        displayName = "Overlay Settings",
        shortLabel = "CFG",
        xinputBinding = "Opens Preset & Layout HUD",
        androidCodeSummary = "OverPad Menu",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        canHide = false,
        supportsShape = true,
        supportsToggle = false,
        defaultShape = ButtonShape.CIRCLE
    );

    fun sizeDpFor(shape: ButtonShape): Pair<Float, Float> {
        if (!supportsShape) return baseWidthDp to baseHeightDp
        return when (shape) {
            ButtonShape.CIRCLE, ButtonShape.SQUARE -> {
                val side = ((baseWidthDp + baseHeightDp) / 2f).coerceAtLeast(48f)
                side to side
            }
            ButtonShape.ROUNDED -> baseWidthDp to baseHeightDp
            ButtonShape.PILL -> maxOf(baseWidthDp, baseHeightDp * 1.45f) to baseHeightDp
        }
    }
}
