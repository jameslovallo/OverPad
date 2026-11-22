package com.example.model

import android.view.KeyEvent

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
    val keyCode: Int? = null
) {
    LEFT_STICK(
        displayName = "Left Thumbstick",
        shortLabel = "LS",
        xinputBinding = "XInput sThumbLX / sThumbLY",
        androidCodeSummary = "AXIS_X / AXIS_Y",
        baseWidthDp = 112f,
        baseHeightDp = 112f
    ),
    RIGHT_STICK(
        displayName = "Right Thumbstick",
        shortLabel = "RS",
        xinputBinding = "XInput sThumbRX / sThumbRY",
        androidCodeSummary = "AXIS_Z / AXIS_RZ",
        baseWidthDp = 112f,
        baseHeightDp = 112f
    ),
    DPAD(
        displayName = "D-Pad",
        shortLabel = "D-PAD",
        xinputBinding = "XInput DPAD_UP/DOWN/LEFT/RIGHT",
        androidCodeSummary = "AXIS_HAT_X/Y + DPAD",
        baseWidthDp = 116f,
        baseHeightDp = 116f
    ),
    BUTTON_A(
        displayName = "A Button",
        shortLabel = "A",
        xinputBinding = "XINPUT_GAMEPAD_A (0x1000)",
        androidCodeSummary = "KEYCODE_BUTTON_A",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        keyCode = KeyEvent.KEYCODE_BUTTON_A
    ),
    BUTTON_B(
        displayName = "B Button",
        shortLabel = "B",
        xinputBinding = "XINPUT_GAMEPAD_B (0x2000)",
        androidCodeSummary = "KEYCODE_BUTTON_B",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        keyCode = KeyEvent.KEYCODE_BUTTON_B
    ),
    BUTTON_X(
        displayName = "X Button",
        shortLabel = "X",
        xinputBinding = "XINPUT_GAMEPAD_X (0x4000)",
        androidCodeSummary = "KEYCODE_BUTTON_X",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        keyCode = KeyEvent.KEYCODE_BUTTON_X
    ),
    BUTTON_Y(
        displayName = "Y Button",
        shortLabel = "Y",
        xinputBinding = "XINPUT_GAMEPAD_Y (0x8000)",
        androidCodeSummary = "KEYCODE_BUTTON_Y",
        baseWidthDp = 54f,
        baseHeightDp = 54f,
        keyCode = KeyEvent.KEYCODE_BUTTON_Y
    ),
    BUTTON_LB(
        displayName = "Left Bumper",
        shortLabel = "LB",
        xinputBinding = "XINPUT_GAMEPAD_LEFT_SHOULDER (0x0100)",
        androidCodeSummary = "KEYCODE_BUTTON_L1",
        baseWidthDp = 74f,
        baseHeightDp = 48f,
        keyCode = KeyEvent.KEYCODE_BUTTON_L1
    ),
    BUTTON_RB(
        displayName = "Right Bumper",
        shortLabel = "RB",
        xinputBinding = "XINPUT_GAMEPAD_RIGHT_SHOULDER (0x0200)",
        androidCodeSummary = "KEYCODE_BUTTON_R1",
        baseWidthDp = 74f,
        baseHeightDp = 48f,
        keyCode = KeyEvent.KEYCODE_BUTTON_R1
    ),
    TRIGGER_LT(
        displayName = "Left Trigger (Hold+Drag Aim)",
        shortLabel = "LT",
        xinputBinding = "XInput bLeftTrigger (0..255)",
        androidCodeSummary = "AXIS_LTRIGGER + L2",
        baseWidthDp = 78f,
        baseHeightDp = 52f,
        supportsDragAim = true,
        keyCode = KeyEvent.KEYCODE_BUTTON_L2
    ),
    TRIGGER_RT(
        displayName = "Right Trigger (Hold+Drag Aim)",
        shortLabel = "RT",
        xinputBinding = "XInput bRightTrigger (0..255)",
        androidCodeSummary = "AXIS_RTRIGGER + R2",
        baseWidthDp = 78f,
        baseHeightDp = 52f,
        supportsDragAim = true,
        keyCode = KeyEvent.KEYCODE_BUTTON_R2
    ),
    BUTTON_L3(
        displayName = "Left Stick Click",
        shortLabel = "L3",
        xinputBinding = "XINPUT_GAMEPAD_LEFT_THUMB (0x0040)",
        androidCodeSummary = "KEYCODE_BUTTON_THUMBL",
        baseWidthDp = 48f,
        baseHeightDp = 48f,
        keyCode = KeyEvent.KEYCODE_BUTTON_THUMBL
    ),
    BUTTON_R3(
        displayName = "Right Stick Click",
        shortLabel = "R3",
        xinputBinding = "XINPUT_GAMEPAD_RIGHT_THUMB (0x0080)",
        androidCodeSummary = "KEYCODE_BUTTON_THUMBR",
        baseWidthDp = 48f,
        baseHeightDp = 48f,
        keyCode = KeyEvent.KEYCODE_BUTTON_THUMBR
    ),
    BUTTON_BACK(
        displayName = "Back / View",
        shortLabel = "BACK",
        xinputBinding = "XINPUT_GAMEPAD_BACK (0x0020)",
        androidCodeSummary = "KEYCODE_BUTTON_SELECT",
        baseWidthDp = 60f,
        baseHeightDp = 48f,
        keyCode = KeyEvent.KEYCODE_BUTTON_SELECT
    ),
    BUTTON_START(
        displayName = "Start / Menu",
        shortLabel = "START",
        xinputBinding = "XINPUT_GAMEPAD_START (0x0010)",
        androidCodeSummary = "KEYCODE_BUTTON_START",
        baseWidthDp = 60f,
        baseHeightDp = 48f,
        keyCode = KeyEvent.KEYCODE_BUTTON_START
    ),
    SETTINGS(
        displayName = "Overlay Settings",
        shortLabel = "CFG",
        xinputBinding = "Opens Preset & Layout HUD",
        androidCodeSummary = "OverPad Menu",
        baseWidthDp = 48f,
        baseHeightDp = 48f,
        canHide = false
    )
}
