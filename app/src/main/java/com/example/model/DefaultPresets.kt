package com.example.model

object DefaultPresets {
    const val ID_FPS_PRO = "preset_fps_pro"
    const val ID_CONSOLE_FULL = "preset_console_full"
    const val ID_ACTION_ARENA = "preset_action_arena"
    const val ID_RETRO_2D = "preset_retro_2d"

    val all: List<ControllerPreset> = listOf(
        fpsPro(),
        consoleFull(),
        actionArena(),
        retro2D()
    )

    fun byId(id: String): ControllerPreset? = all.firstOrNull { it.id == id }

    fun fpsPro(): ControllerPreset = ControllerPreset(
        id = ID_FPS_PRO,
        name = "FPS Pro (Touch-Aim)",
        description = "Left stick movement, mouse-like right-side look swipe, and LT/RT hold+swipe aim.",
        category = PresetCategory.FPS,
        isBuiltIn = true,
        opacity = 0.78f,
        gridStepDp = 24,
        snapToGrid = true,
        fpsRightZoneEnabled = true,
        fpsLookSensitivity = 2.60f,
        triggerDragAimEnabled = true,
        invertRightY = false,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.14f, 0.68f, scale = 1.15f, visible = true),
            ControlPlacement(ControlType.RIGHT_STICK, 0.84f, 0.72f, scale = 0.95f, visible = false),
            ControlPlacement(ControlType.DPAD, 0.25f, 0.36f, scale = 0.80f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.12f, 0.24f, scale = 1.10f, visible = true),
            ControlPlacement(ControlType.TRIGGER_RT, 0.87f, 0.44f, scale = 1.25f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.23f, 0.16f, scale = 0.90f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.76f, 0.18f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.88f, 0.78f, scale = 1.05f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.78f, 0.82f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.75f, 0.64f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.90f, 0.22f, scale = 0.90f, visible = true),
            ControlPlacement(ControlType.BUTTON_L3, 0.07f, 0.45f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.BUTTON_R3, 0.67f, 0.82f, scale = 0.85f, visible = false),
            ControlPlacement(ControlType.BUTTON_BACK, 0.42f, 0.12f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.58f, 0.12f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.12f, scale = 0.95f, visible = true)
        )
    )

    fun consoleFull(): ControllerPreset = ControllerPreset(
        id = ID_CONSOLE_FULL,
        name = "Console Full Pad",
        description = "Standard twin-stick XInput gamepad with D-Pad, ABXY, bumpers, triggers, L3/R3, and menu buttons.",
        category = PresetCategory.CONSOLE,
        isBuiltIn = true,
        opacity = 0.80f,
        gridStepDp = 24,
        snapToGrid = true,
        fpsRightZoneEnabled = false,
        fpsLookSensitivity = 2.25f,
        triggerDragAimEnabled = true,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.13f, 0.48f, scale = 1.05f, visible = true),
            ControlPlacement(ControlType.DPAD, 0.26f, 0.76f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.RIGHT_STICK, 0.73f, 0.76f, scale = 1.05f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.86f, 0.64f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.93f, 0.52f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.79f, 0.52f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.86f, 0.40f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.11f, 0.22f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.22f, 0.18f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.89f, 0.22f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.TRIGGER_RT, 0.78f, 0.18f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_L3, 0.08f, 0.78f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.BUTTON_R3, 0.92f, 0.82f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.BUTTON_BACK, 0.41f, 0.14f, scale = 0.90f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.59f, 0.14f, scale = 0.90f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.14f, scale = 0.95f, visible = true)
        )
    )

    fun actionArena(): ControllerPreset = ControllerPreset(
        id = ID_ACTION_ARENA,
        name = "Action / Arena",
        description = "Oversized face buttons and quick-reach triggers for third-person combat and brawlers.",
        category = PresetCategory.ACTION,
        isBuiltIn = true,
        opacity = 0.75f,
        gridStepDp = 24,
        snapToGrid = true,
        fpsRightZoneEnabled = false,
        fpsLookSensitivity = 2.40f,
        triggerDragAimEnabled = true,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.14f, 0.64f, scale = 1.20f, visible = true),
            ControlPlacement(ControlType.RIGHT_STICK, 0.72f, 0.78f, scale = 0.90f, visible = true),
            ControlPlacement(ControlType.DPAD, 0.12f, 0.30f, scale = 0.80f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.84f, 0.72f, scale = 1.20f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.92f, 0.56f, scale = 1.15f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.76f, 0.56f, scale = 1.15f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.84f, 0.40f, scale = 1.10f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.25f, 0.18f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.35f, 0.18f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.75f, 0.18f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_RT, 0.88f, 0.22f, scale = 1.05f, visible = true),
            ControlPlacement(ControlType.BUTTON_L3, 0.08f, 0.86f, scale = 0.80f, visible = false),
            ControlPlacement(ControlType.BUTTON_R3, 0.62f, 0.84f, scale = 0.80f, visible = false),
            ControlPlacement(ControlType.BUTTON_BACK, 0.42f, 0.12f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.58f, 0.12f, scale = 0.85f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.12f, scale = 0.95f, visible = true)
        )
    )

    fun retro2D(): ControllerPreset = ControllerPreset(
        id = ID_RETRO_2D,
        name = "Retro / 2D Platformer",
        description = "Focused D-Pad + ABXY + LB/RB layout with analog sticks and triggers hidden.",
        category = PresetCategory.RETRO,
        isBuiltIn = true,
        opacity = 0.85f,
        gridStepDp = 24,
        snapToGrid = true,
        fpsLookSensitivity = 2.40f,
        triggerDragAimEnabled = true,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.15f, 0.45f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.RIGHT_STICK, 0.80f, 0.45f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.DPAD, 0.16f, 0.66f, scale = 1.30f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.84f, 0.72f, scale = 1.20f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.92f, 0.58f, scale = 1.20f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.76f, 0.58f, scale = 1.15f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.84f, 0.44f, scale = 1.15f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.14f, 0.22f, scale = 1.10f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.86f, 0.22f, scale = 1.10f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.25f, 0.20f, scale = 0.95f, visible = false),
            ControlPlacement(ControlType.TRIGGER_RT, 0.75f, 0.20f, scale = 0.95f, visible = false),
            ControlPlacement(ControlType.BUTTON_L3, 0.10f, 0.86f, scale = 0.85f, visible = false),
            ControlPlacement(ControlType.BUTTON_R3, 0.90f, 0.86f, scale = 0.85f, visible = false),
            ControlPlacement(ControlType.BUTTON_BACK, 0.40f, 0.14f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.60f, 0.14f, scale = 0.95f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.14f, scale = 0.95f, visible = true)
        )
    )
}
