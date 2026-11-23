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
        gridRows = GridSnapper.DEFAULT_GRID_ROWS,
        snapToGrid = true,
        fpsRightZoneEnabled = true,
        fpsLookSensitivity = 2.60f,
        triggerDragAimEnabled = true,
        invertRightY = false,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.14f, 0.70f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.RIGHT_STICK, 0.84f, 0.75f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.DPAD, 0.28f, 0.40f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.12f, 0.25f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_RT, 0.88f, 0.40f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.24f, 0.15f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.76f, 0.15f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.86f, 0.80f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.76f, 0.80f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.76f, 0.65f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.90f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_L3, 0.08f, 0.45f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_R3, 0.66f, 0.80f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.BUTTON_BACK, 0.40f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.60f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.10f, scale = 1.0f, visible = true)
        )
    )

    fun consoleFull(): ControllerPreset = ControllerPreset(
        id = ID_CONSOLE_FULL,
        name = "Console Full Pad",
        description = "Standard twin-stick XInput gamepad with D-Pad, ABXY, bumpers, triggers, L3/R3, and menu buttons.",
        category = PresetCategory.CONSOLE,
        isBuiltIn = true,
        opacity = 0.80f,
        gridRows = GridSnapper.DEFAULT_GRID_ROWS,
        snapToGrid = true,
        fpsRightZoneEnabled = false,
        fpsLookSensitivity = 2.25f,
        triggerDragAimEnabled = true,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.14f, 0.50f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.DPAD, 0.28f, 0.75f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.RIGHT_STICK, 0.72f, 0.75f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.86f, 0.70f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.94f, 0.55f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.78f, 0.55f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.86f, 0.40f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.12f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.24f, 0.15f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.88f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_RT, 0.76f, 0.15f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_L3, 0.08f, 0.80f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_R3, 0.92f, 0.85f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_BACK, 0.40f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.60f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.10f, scale = 1.0f, visible = true)
        )
    )

    fun actionArena(): ControllerPreset = ControllerPreset(
        id = ID_ACTION_ARENA,
        name = "Action / Arena",
        description = "Balanced face buttons and quick-reach triggers for third-person combat and brawlers.",
        category = PresetCategory.ACTION,
        isBuiltIn = true,
        opacity = 0.75f,
        gridRows = GridSnapper.DEFAULT_GRID_ROWS,
        snapToGrid = true,
        fpsRightZoneEnabled = false,
        fpsLookSensitivity = 2.40f,
        triggerDragAimEnabled = true,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.14f, 0.65f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.RIGHT_STICK, 0.70f, 0.75f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.DPAD, 0.12f, 0.30f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.86f, 0.75f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.94f, 0.60f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.78f, 0.60f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.86f, 0.45f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.24f, 0.15f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.34f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.76f, 0.15f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_RT, 0.88f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_L3, 0.08f, 0.85f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.BUTTON_R3, 0.60f, 0.85f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.BUTTON_BACK, 0.40f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.60f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.10f, scale = 1.0f, visible = true)
        )
    )

    fun retro2D(): ControllerPreset = ControllerPreset(
        id = ID_RETRO_2D,
        name = "Retro / 2D Platformer",
        description = "Focused D-Pad + ABXY + LB/RB layout with analog sticks and triggers hidden.",
        category = PresetCategory.RETRO,
        isBuiltIn = true,
        opacity = 0.85f,
        gridRows = GridSnapper.DEFAULT_GRID_ROWS,
        snapToGrid = true,
        fpsLookSensitivity = 2.40f,
        triggerDragAimEnabled = true,
        controls = listOf(
            ControlPlacement(ControlType.LEFT_STICK, 0.16f, 0.45f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.RIGHT_STICK, 0.80f, 0.45f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.DPAD, 0.16f, 0.65f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_A, 0.86f, 0.75f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_B, 0.94f, 0.60f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_X, 0.78f, 0.60f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_Y, 0.86f, 0.45f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_LB, 0.14f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_RB, 0.86f, 0.20f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.TRIGGER_LT, 0.26f, 0.20f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.TRIGGER_RT, 0.74f, 0.20f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.BUTTON_L3, 0.10f, 0.85f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.BUTTON_R3, 0.90f, 0.85f, scale = 1.0f, visible = false),
            ControlPlacement(ControlType.BUTTON_BACK, 0.40f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.BUTTON_START, 0.60f, 0.10f, scale = 1.0f, visible = true),
            ControlPlacement(ControlType.SETTINGS, 0.50f, 0.10f, scale = 1.0f, visible = true)
        )
    )
}
