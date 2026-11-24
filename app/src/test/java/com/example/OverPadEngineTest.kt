package com.example

import com.example.data.ProfileBackupCodec
import com.example.model.AppPresetBinding
import com.example.model.ButtonShape
import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.DefaultPresets
import com.example.model.GridSnapper
import com.example.model.PresetCategory
import com.example.shizuku.ShizukuConnectionStage
import com.example.shizuku.ShizukuStatus
import com.example.xinput.DPadDirectionState
import com.example.xinput.XInputControllerEngine
import com.example.xinput.XInputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverPadEngineTest {

    @Test
    fun defaultPresets_includeFpsAndSelectiveVisibility() {
        val fps = DefaultPresets.fpsPro()
        assertTrue(fps.fpsRightZoneEnabled)
        assertTrue(fps.triggerDragAimEnabled)
        assertFalse(fps.placementFor(ControlType.RIGHT_STICK).visible)
        assertTrue(fps.placementFor(ControlType.LEFT_STICK).visible)
        assertTrue(fps.placementFor(ControlType.TRIGGER_RT).visible)

        val retro = DefaultPresets.retro2D()
        assertFalse(retro.placementFor(ControlType.LEFT_STICK).visible)
        assertFalse(retro.placementFor(ControlType.RIGHT_STICK).visible)
        assertTrue(retro.placementFor(ControlType.DPAD).visible)
        assertTrue(retro.placementFor(ControlType.SETTINGS).visible)

        DefaultPresets.all.forEach { preset ->
            assertEquals(GridSnapper.DEFAULT_GRID_ROWS, preset.gridRows)
            preset.controls.forEach { placement ->
                assertEquals(1.0f, placement.scale, 0.0001f)
            }
        }
    }

    @Test
    fun triggerHoldAndDragAim_keepsTriggerDepressedWhileSteeringRightStick() {
        val engine = XInputControllerEngine()

        // Press and hold RT while dragging up-right
        engine.updateTriggerWithDragAim(
            trigger = ControlType.TRIGGER_RT,
            pressed = true,
            dragStickX = 0.65f,
            dragStickY = -0.40f
        )

        val heldState = engine.state.value
        assertEquals(1.0f, heldState.rightTrigger, 0.001f)
        assertEquals(255, heldState.rightTriggerByte)
        assertEquals(0.65f, heldState.thumbRX, 0.001f)
        assertEquals(-0.40f, heldState.thumbRY, 0.001f)
        assertTrue("RT+Drag Aim" in heldState.activeSources)

        // Release RT resets both trigger and its drag-aim vector
        engine.updateTriggerWithDragAim(ControlType.TRIGGER_RT, pressed = false)
        val releasedState = engine.state.value
        assertEquals(0f, releasedState.rightTrigger, 0.001f)
        assertEquals(0, releasedState.rightTriggerByte)
        assertEquals(0f, releasedState.thumbRX, 0.001f)
        assertEquals(0f, releasedState.thumbRY, 0.001f)
    }

    @Test
    fun simultaneousMultiTouch_combinesLeftStickFpsLookDpadAndButtons() {
        val engine = XInputControllerEngine()

        engine.updateLeftStick(-0.8f, 0.5f)
        engine.updateFpsLookZoneStick(0.45f, -0.25f)
        engine.setButtonPressed(ControlType.BUTTON_A, true)
        engine.updateDPad(DPadDirectionState(up = true, right = true))

        val state = engine.state.value
        assertEquals(-0.8f, state.thumbLX, 0.001f)
        assertEquals(0.5f, state.thumbLY, 0.001f)
        assertEquals(0.45f, state.thumbRX, 0.001f)
        assertEquals(-0.25f, state.thumbRY, 0.001f)
        assertTrue((state.buttonsMask and XInputState.XINPUT_GAMEPAD_A) != 0)
        assertTrue((state.buttonsMask and XInputState.XINPUT_GAMEPAD_DPAD_UP) != 0)
        assertTrue((state.buttonsMask and XInputState.XINPUT_GAMEPAD_DPAD_RIGHT) != 0)
        assertEquals(20, state.toBinaryPacket().size)

        engine.resetAll()
        engine.updateLeftMoveZoneStick(0.7f, -0.6f)
        val touchMoveState = engine.state.value
        assertEquals(0.7f, touchMoveState.thumbLX, 0.001f)
        assertEquals(-0.6f, touchMoveState.thumbLY, 0.001f)
        assertTrue("Left Touch Move" in touchMoveState.activeSources)
    }

    @Test
    fun gridSnapper_centersGridOnScreenCenterWithAspectProportionalColumns() {
        assertEquals(36, GridSnapper.computeGridColumns(20, 1600f, 900f))
        assertEquals(44, GridSnapper.computeGridColumns(20, 2000f, 900f))
        assertEquals(27, GridSnapper.computeGridColumns(20, 1200f, 900f))

        assertTrue(GridSnapper.gridLineFractions(20).any { kotlin.math.abs(it - 0.5f) < 0.0001f })
        assertTrue(GridSnapper.gridLineFractions(27).any { kotlin.math.abs(it - 0.5f) < 0.0001f })
        assertTrue(GridSnapper.gridLineFractions(15).any { kotlin.math.abs(it - 0.5f) < 0.0001f })

        val (snappedX, snappedY) = GridSnapper.snapPosition(
            rawXFraction = 0.505f,
            rawYFraction = 0.495f,
            widthPx = 1200f,
            heightPx = 900f,
            gridRows = 15,
            enabled = true
        )
        assertEquals(0.50f, snappedX, 0.0001f)
        assertEquals(0.50f, snappedY, 0.0001f)

        assertEquals(1.25f, GridSnapper.snapScale(1.23f), 0.001f)
        assertEquals(0.80f, GridSnapper.snapOpacity(0.79f), 0.001f)
    }

    @Test
    fun shizukuStatus_resolvesStagesAndModeLabelsCorrectly() {
        assertEquals(
            ShizukuConnectionStage.NOT_INSTALLED,
            ShizukuStatus(isShizukuInstalled = false, isBinderAlive = false).stage
        )
        assertEquals(
            ShizukuConnectionStage.SERVICE_STOPPED,
            ShizukuStatus(isShizukuInstalled = true, isBinderAlive = false).stage
        )
        assertEquals(
            ShizukuConnectionStage.PERMISSION_REQUIRED,
            ShizukuStatus(isShizukuInstalled = true, isBinderAlive = true, isPermissionGranted = false).stage
        )
        assertEquals(
            ShizukuConnectionStage.CONNECTING,
            ShizukuStatus(
                isShizukuInstalled = true,
                isBinderAlive = true,
                isPermissionGranted = true,
                isServiceConnected = false
            ).stage
        )
        val shellConnected = ShizukuStatus(
            isShizukuInstalled = true,
            isBinderAlive = true,
            isPermissionGranted = true,
            isServiceConnected = true,
            remoteUid = 2000
        )
        assertEquals(ShizukuConnectionStage.CONNECTED, shellConnected.stage)
        assertTrue(shellConnected.modeLabel.contains("2000"))

        val rootFallback = shellConnected.copy(
            remoteUid = 0,
            uinputWritable = true,
            backendMode = "INJECT_EVENTS_ONLY"
        )
        assertFalse(rootFallback.isHardwareSpoofed)
        assertTrue(rootFallback.modeLabel.contains("Root UID 0"))
        assertTrue(rootFallback.modeLabel.contains("INJECT_EVENTS_ONLY"))

        val standby = shellConnected.copy(
            virtualDeviceId = -1,
            backendMode = "STANDBY"
        )
        assertFalse(standby.isHardwareSpoofed)
        assertTrue(standby.modeLabel.contains("Standby"))
        assertTrue(standby.modeLabel.contains("Overlay Off"))

        val hardwareSpoofed = shellConnected.copy(
            virtualDeviceId = 14,
            backendMode = "UHID_XBOX"
        )
        assertTrue(hardwareSpoofed.isHardwareSpoofed)
        assertTrue(hardwareSpoofed.modeLabel.contains("Spoofed Xbox Pad #14"))
        assertTrue(hardwareSpoofed.modeLabel.contains("UHID_XBOX"))
        assertEquals(
            rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID,
            com.example.shizuku.ShizukuBridgeManager.SHIZUKU_PACKAGE
        )
    }

    @Test
    fun engineCallback_emitsStateUpdatesForShizukuForwarding() {
        val engine = XInputControllerEngine()
        val captured = mutableListOf<XInputState>()
        engine.onStateUpdated = { captured.add(it) }

        engine.setButtonPressed(ControlType.BUTTON_X, true)
        engine.updateLeftStick(0.5f, -0.5f)
        engine.resetAll()

        assertEquals(3, captured.size)
        assertTrue((captured[0].buttonsMask and XInputState.XINPUT_GAMEPAD_X) != 0)
        assertEquals(0.5f, captured[1].thumbLX, 0.001f)
        assertEquals(0, captured[2].buttonsMask)
        assertEquals(0f, captured[2].thumbLX, 0.001f)
    }

    @Test
    fun virtualXboxHidProtocol_encodes14ByteReportsAndRegisterPayloads() {
        val neutral = com.example.shizuku.VirtualXboxHidProtocol.buildUhidReportBytes(
            0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
        )
        assertEquals(14, neutral.size)
        assertEquals(0x01, neutral[0])
        assertEquals(0, neutral[1])
        assertEquals(0, neutral[2])
        assertEquals(0, neutral[3]) // Null hat

        val active = com.example.shizuku.VirtualXboxHidProtocol.buildUhidReportBytes(
            buttonsMask = XInputState.XINPUT_GAMEPAD_A or
                XInputState.XINPUT_GAMEPAD_X or
                XInputState.XINPUT_GAMEPAD_RIGHT_SHOULDER or
                XInputState.XINPUT_GAMEPAD_START,
            leftTrigger = 1.0f,
            rightTrigger = 0.5f,
            thumbLX = 1.0f,
            thumbLY = -1.0f,
            thumbRX = 0.5f,
            thumbRY = 0f,
            hatX = 1f,
            hatY = -1f
        )
        assertEquals(14, active.size)
        // Byte 1 bits: A (bit 0 = 1), X (bit 3 = 8), RB (bit 7 = 128) => 137
        assertEquals(1 or 8 or 128, active[1])
        // Byte 2 bits: L2 (bit 8 -> bit 0 = 1), R2 (bit 9 -> bit 1 = 2), START (bit 11 -> bit 3 = 8) => 11
        assertEquals(1 or 2 or 8, active[2])
        // Hat NE = 2
        assertEquals(2, active[3])
        // LT = 255, RT = 128
        assertEquals(255, active[12])
        assertEquals(128, active[13])

        val uhidReg = com.example.shizuku.VirtualXboxHidProtocol.buildUhidRegisterJson(1)
        assertTrue(uhidReg.contains("\"command\":\"register\""))
        assertTrue(uhidReg.contains("\"Xbox Wireless Controller\""))
        assertTrue(uhidReg.contains("\"bus\":\"bluetooth\""))

        val uhidReport = com.example.shizuku.VirtualXboxHidProtocol.buildUhidReportJson(1, active)
        assertTrue(uhidReport.contains("\"command\":\"report\""))
        assertTrue(uhidReport.contains("\"report\":[1,137,11,2,"))

        val uinputReg = com.example.shizuku.VirtualXboxHidProtocol.buildUinputRegisterJson(1)
        assertTrue(uinputReg.contains("\"UI_SET_ABSBIT\""))
        assertTrue(uinputReg.contains("\"Microsoft X-Box 360 pad\""))
        assertTrue(uinputReg.contains("\"abs_info\":["))

        val uinputInject = com.example.shizuku.VirtualXboxHidProtocol.buildUinputInjectJson(1, listOf(1, 304, 1, 0, 0, 0))
        assertTrue(uinputInject.contains("\"command\":\"inject\""))
        assertTrue(uinputInject.contains("\"events\":[1,304,1,0,0,0]"))
    }

    @Test
    fun fpsMouseLookFilter_isRefreshRateIndependentWith2xTo4xSensitivityAndNoMidSwipeThrottle() {
        val fps = DefaultPresets.fpsPro()
        val console = DefaultPresets.consoleFull()
        assertTrue(fps.triggerDragAimEnabled)
        assertTrue(console.triggerDragAimEnabled)
        assertTrue(fps.fpsLookSensitivity in 2.0f..4.0f)
        assertTrue(console.fpsLookSensitivity in 2.0f..4.0f)

        // 1. Sub-jitter micro wobble stays exact zero
        val jitterFilter = com.example.xinput.TouchMouseLookFilter()
        jitterFilter.onPointerDown(1000L)
        val (jx, jy) = jitterFilter.onPointerMove(0.02f, -0.02f, 1016L, sensitivity = 2.5f)
        assertEquals(0f, jx, 0.0001f)
        assertEquals(0f, jy, 0.0001f)

        // 2. Same physical thumb speed at 60Hz (16ms steps) vs 120Hz (8ms steps) reaches matching deflection,
        // and interleaved 12ms decayIfIdle checks during a 60Hz swipe must not slow it down
        val filter60 = com.example.xinput.TouchMouseLookFilter()
        val filter120 = com.example.xinput.TouchMouseLookFilter()
        filter60.onPointerDown(0L)
        filter120.onPointerDown(0L)

        var out60 = 0f to 0f
        for (frame in 1..6) {
            val t = frame * 16L
            assertEquals(null, filter60.decayIfIdle(t - 2L, sensitivity = 2.5f, antiDeadzone = 0.14f))
            out60 = filter60.onPointerMove(1.4f, 0f, t, sensitivity = 2.5f, antiDeadzone = 0.14f)
        }
        var out120 = 0f to 0f
        for (frame in 1..12) {
            out120 = filter120.onPointerMove(0.7f, 0f, frame * 8L, sensitivity = 2.5f, antiDeadzone = 0.14f)
        }
        assertEquals(out60.first, out120.first, 0.04f)
        assertEquals(0f, out60.second, 0.001f)

        // 3. Sensitivity clamps to [2.0x, 4.0x]
        val belowMin = com.example.xinput.TouchMouseLookFilter().apply { onPointerDown(0L) }
            .onPointerMove(1.4f, 0f, 16L, sensitivity = 0.5f, antiDeadzone = 0.14f)
        val atMin = com.example.xinput.TouchMouseLookFilter().apply { onPointerDown(0L) }
            .onPointerMove(1.4f, 0f, 16L, sensitivity = 2.0f, antiDeadzone = 0.14f)
        assertEquals(atMin.first, belowMin.first, 0.001f)

        val aboveMax = com.example.xinput.TouchMouseLookFilter().apply { onPointerDown(0L) }
            .onPointerMove(1.4f, 0f, 16L, sensitivity = 6.0f, antiDeadzone = 0.14f)
        val atMax = com.example.xinput.TouchMouseLookFilter().apply { onPointerDown(0L) }
            .onPointerMove(1.4f, 0f, 16L, sensitivity = 4.0f, antiDeadzone = 0.14f)
        assertEquals(atMax.first, aboveMax.first, 0.001f)
        assertTrue(atMax.first > atMin.first)

        // 4. Fast flick saturates smoothly to max deflection in diagonal direction
        val flickFilter = com.example.xinput.TouchMouseLookFilter()
        flickFilter.onPointerDown(0L)
        val (fx, fy) = flickFilter.onPointerMove(18f, -18f, 12L, sensitivity = 2.5f, antiDeadzone = 0.14f)
        assertTrue(fx > 0.65f)
        assertTrue(fy < -0.65f)

        // 5. Idle decay glides velocity back to exact zero smoothly after idle threshold (~34ms) and then stops emitting
        var settledToZero = false
        for (step in 1..20) {
            val decayed = flickFilter.decayIfIdle(12L + 36L + step * 12L, sensitivity = 2.5f, antiDeadzone = 0.14f)
            if (decayed != null && decayed.first == 0f && decayed.second == 0f) {
                settledToZero = true
                break
            }
        }
        assertTrue(settledToZero)
        assertEquals(null, flickFilter.decayIfIdle(500L, sensitivity = 2.5f))

        // 6. Progressive precision + acceleration curve: slow tracking stays controlled near anti-deadzone
        // while fast swipes accelerate non-linearly instead of saturating prematurely
        fun steadyOutputForSpeed(dpPerSec: Float): Float {
            val f = com.example.xinput.TouchMouseLookFilter().apply { onPointerDown(0L) }
            val stepDp = dpPerSec * 0.016f
            var out = 0f
            for (frame in 1..8) {
                out = f.onPointerMove(stepDp, 0f, frame * 16L, sensitivity = 2.5f, antiDeadzone = 0.14f).first
            }
            return out
        }
        val slowAim = steadyOutputForSpeed(60f)
        val mediumPan = steadyOutputForSpeed(300f)
        val fastTurn = steadyOutputForSpeed(600f)
        assertTrue("Slow aim ($slowAim) should stay in fine micro-aim band", slowAim in 0.145f..0.22f)
        assertTrue("Turn acceleration should ramp steeper at higher speed", (fastTurn - mediumPan) > (mediumPan - slowAim))

        // 7. Direction reversal snaps cleanly past zero without sluggish glide lag
        val reversalFilter = com.example.xinput.TouchMouseLookFilter().apply { onPointerDown(0L) }
        for (i in 1..4) {
            reversalFilter.onPointerMove(6.4f, 0f, i * 16L, sensitivity = 2.5f, antiDeadzone = 0.14f)
        }
        val (revX, _) = reversalFilter.onPointerMove(-6.4f, 0f, 80L, sensitivity = 2.5f, antiDeadzone = 0.14f)
        assertTrue("Immediate reversal should turn negative on first opposing swipe ($revX)", revX < -0.14f)
    }

    @Test
    fun controlDragHelper_snapsAcrossGridStepsAndPrioritizesOverlappingSmallButtons() {
        // 1. Continuous total-delta dragging crosses centered division intersections cleanly (2:1 -> 40 cols x 20 rows)
        val startX = 0.20f
        val startY = 0.50f
        val widthPx = 2000f
        val heightPx = 1000f
        val gridRows = 20

        val smallMove = com.example.ui.gamepad.ControlDragHelper.computeDraggedPlacement(
            startXFraction = startX,
            startYFraction = startY,
            totalDeltaXPx = 55f,
            totalDeltaYPx = 0f,
            canvasWidthPx = widthPx,
            canvasHeightPx = heightPx,
            gridRows = gridRows,
            snapToGrid = true
        )
        assertEquals(0.225f, smallMove.first, 0.001f)
        assertEquals(0.50f, smallMove.second, 0.001f)

        val continuedMove = com.example.ui.gamepad.ControlDragHelper.computeDraggedPlacement(
            startXFraction = startX,
            startYFraction = startY,
            totalDeltaXPx = 200f,
            totalDeltaYPx = -100f,
            canvasWidthPx = widthPx,
            canvasHeightPx = heightPx,
            gridRows = gridRows,
            snapToGrid = true
        )
        assertEquals(0.30f, continuedMove.first, 0.001f)
        assertEquals(0.40f, continuedMove.second, 0.001f)

        // 2. Scaled-down editor hit-testing keeps 48dp minimum target and lets small L3 win over adjacent large LEFT_STICK
        val preset = DefaultPresets.consoleFull()
            .let { base ->
                base.updatePlacement(base.placementFor(ControlType.LEFT_STICK).copy(xFraction = 0.20f, yFraction = 0.60f, visible = true))
                    .updatePlacement(base.placementFor(ControlType.BUTTON_L3).copy(xFraction = 0.26f, yFraction = 0.60f, visible = true))
            }

        val hitL3 = com.example.ui.gamepad.ControlDragHelper.findHitControl(
            touchX = 0.26f * 1000f,
            touchY = 0.60f * 500f,
            controls = preset.controls,
            selectedControl = ControlType.LEFT_STICK,
            canvasWidthPx = 1000f,
            canvasHeightPx = 500f,
            effectiveScaleMultiplier = 0.45f,
            densityPx = 2.0f
        )
        assertEquals(ControlType.BUTTON_L3, hitL3?.type)

        val hitStick = com.example.ui.gamepad.ControlDragHelper.findHitControl(
            touchX = 0.16f * 1000f,
            touchY = 0.60f * 500f,
            controls = preset.controls,
            selectedControl = ControlType.BUTTON_L3,
            canvasWidthPx = 1000f,
            canvasHeightPx = 500f,
            effectiveScaleMultiplier = 0.45f,
            densityPx = 2.0f
        )
        assertEquals(ControlType.LEFT_STICK, hitStick?.type)
    }

    @Test
    fun buttonShapeAndToggle_computeEffectiveDimensionsAndPreserveDefaults() {
        // Face button A defaults to CIRCLE and toggleable = false
        val defaultA = ControlPlacement(type = ControlType.BUTTON_A, xFraction = 0.8f, yFraction = 0.7f)
        assertEquals(ButtonShape.CIRCLE, defaultA.resolvedShape())
        assertFalse(defaultA.toggleable)
        assertEquals(54f, defaultA.effectiveWidthDp(), 0.01f)
        assertEquals(54f, defaultA.effectiveHeightDp(), 0.01f)

        // Setting PILL on a face button expands width proportionally while keeping height
        val pillA = defaultA.copy(shape = ButtonShape.PILL, toggleable = true, scale = 1.5f)
        assertEquals(ButtonShape.PILL, pillA.resolvedShape())
        assertTrue(pillA.toggleable)
        assertEquals(54f * 1.45f * 1.5f, pillA.effectiveWidthDp(), 0.01f)
        assertEquals(54f * 1.5f, pillA.effectiveHeightDp(), 0.01f)

        // All buttons share the same 54dp base scaling across shapes (including triggers)
        val buttonTypes = listOf(
            ControlType.BUTTON_A, ControlType.BUTTON_B, ControlType.BUTTON_X, ControlType.BUTTON_Y,
            ControlType.BUTTON_LB, ControlType.BUTTON_RB,
            ControlType.TRIGGER_LT, ControlType.TRIGGER_RT,
            ControlType.BUTTON_L3, ControlType.BUTTON_R3,
            ControlType.BUTTON_BACK, ControlType.BUTTON_START,
            ControlType.SETTINGS
        )
        buttonTypes.forEach { type ->
            assertEquals(54f, type.baseWidthDp, 0.01f)
            assertEquals(54f, type.baseHeightDp, 0.01f)
            assertEquals(54f to 54f, type.sizeDpFor(ButtonShape.CIRCLE))
            assertEquals(54f to 54f, type.sizeDpFor(ButtonShape.ROUNDED))
            assertEquals(54f to 54f, type.sizeDpFor(ButtonShape.SQUARE))
            assertEquals(54f * 1.45f to 54f, type.sizeDpFor(ButtonShape.PILL))
        }

        val squareRt = ControlPlacement(
            type = ControlType.TRIGGER_RT,
            xFraction = 0.85f,
            yFraction = 0.25f,
            shape = ButtonShape.SQUARE
        )
        assertEquals(54f, squareRt.effectiveWidthDp(), 0.01f)
        assertEquals(54f, squareRt.effectiveHeightDp(), 0.01f)

        // D-Pad supports shape customization while toggleable remains false
        assertTrue(ControlType.DPAD.supportsShape)
        assertFalse(ControlType.DPAD.supportsToggle)
        val defaultDpad = ControlPlacement(type = ControlType.DPAD, xFraction = 0.2f, yFraction = 0.6f)
        assertEquals(ButtonShape.CIRCLE, defaultDpad.resolvedShape())
        assertEquals(116f, defaultDpad.effectiveWidthDp(), 0.01f)
        assertEquals(116f, defaultDpad.effectiveHeightDp(), 0.01f)
        val pillDpad = defaultDpad.copy(shape = ButtonShape.PILL)
        assertEquals(ButtonShape.PILL, pillDpad.resolvedShape())
        assertEquals(116f * 1.45f, pillDpad.effectiveWidthDp(), 0.01f)
        assertEquals(116f, pillDpad.effectiveHeightDp(), 0.01f)

        // Non-shapeable sticks ignore shape overrides and stay square
        val stick = ControlPlacement(
            type = ControlType.LEFT_STICK,
            xFraction = 0.2f,
            yFraction = 0.6f,
            shape = ButtonShape.PILL
        )
        assertFalse(ControlType.LEFT_STICK.supportsShape)
        assertFalse(ControlType.LEFT_STICK.supportsToggle)
        assertEquals(112f, stick.effectiveWidthDp(), 0.01f)
        assertEquals(112f, stick.effectiveHeightDp(), 0.01f)
    }

    @Test
    fun profileBackupCodec_roundTripsFullProfilesBindingsAndSinglePreset() {
        val customPreset = DefaultPresets.fpsPro()
            .copy(
                id = "preset_custom_abc123",
                name = "Arena Custom",
                category = PresetCategory.CUSTOM,
                isBuiltIn = false,
                gridRows = 24,
                opacity = 0.85f
            )
            .let { base ->
                base.updatePlacement(
                    base.placementFor(ControlType.BUTTON_A).copy(
                        shape = ButtonShape.PILL,
                        toggleable = true,
                        scale = 1.25f
                    )
                )
            }
        val builtInPreset = DefaultPresets.retro2D()
        val bindings = listOf(
            AppPresetBinding(
                packageName = "com.example.game",
                appLabel = "Arena Shooter",
                presetId = customPreset.id
            )
        )

        val encoded = ProfileBackupCodec.encodeBackup(
            presets = listOf(builtInPreset, customPreset),
            bindings = bindings,
            defaultPresetId = customPreset.id,
            exportedAtEpochMs = 1700000000000L
        )
        val decoded = ProfileBackupCodec.decodeBackup(encoded)

        assertEquals(2, decoded.presets.size)
        assertEquals(customPreset.id, decoded.defaultPresetId)
        assertEquals(1, decoded.bindings.size)
        assertEquals("com.example.game", decoded.bindings.first().packageName)

        val restoredCustom = decoded.presets.first { it.id == customPreset.id }
        assertEquals("Arena Custom", restoredCustom.name)
        assertEquals(24, restoredCustom.gridRows)
        assertEquals(0.85f, restoredCustom.opacity, 0.001f)
        val restoredA = restoredCustom.placementFor(ControlType.BUTTON_A)
        assertEquals(ButtonShape.PILL, restoredA.shape)
        assertTrue(restoredA.toggleable)
        assertEquals(1.25f, restoredA.scale, 0.001f)

        // Single-preset export roundtrip
        val singleJson = ProfileBackupCodec.encodeSinglePreset(customPreset, exportedAtEpochMs = 1700000000000L)
        val decodedSingle = ProfileBackupCodec.decodeBackup(singleJson)
        assertEquals(1, decodedSingle.presets.size)
        assertEquals(customPreset.id, decodedSingle.presets.first().id)

        // Invalid JSON raises IllegalArgumentException
        var threw = false
        try {
            ProfileBackupCodec.decodeBackup("{not valid json")
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun sliderStepping_snapsOpacitySensitivityAndDeadzoneToStepGrid() {
        assertEquals(0.80f, GridSnapper.snapOpacity(0.79f), 0.0001f)
        assertEquals(0.15f, GridSnapper.snapOpacity(0.05f), 0.0001f)
        assertEquals(1.00f, GridSnapper.snapOpacity(1.20f), 0.0001f)

        assertEquals(2.60f, com.example.xinput.TouchMouseLookFilter.snapSensitivity(2.58f), 0.0001f)
        assertEquals(2.00f, com.example.xinput.TouchMouseLookFilter.snapSensitivity(1.20f), 0.0001f)
        assertEquals(4.00f, com.example.xinput.TouchMouseLookFilter.snapSensitivity(5.50f), 0.0001f)

        assertEquals(0.14f, com.example.xinput.TouchMouseLookFilter.snapAntiDeadzone(0.149f), 0.0001f)
        assertEquals(0.00f, com.example.xinput.TouchMouseLookFilter.snapAntiDeadzone(-0.10f), 0.0001f)
        assertEquals(0.28f, com.example.xinput.TouchMouseLookFilter.snapAntiDeadzone(0.50f), 0.0001f)
    }

    @Test
    fun dpadFlickController_supportsSwipeFromAnywhereLeashReversalAndTapFallback() {
        val density = 2f
        val size = 232 // 116dp * 2

        // 1. Touch dead-center and flick up by 10dp (20px) -> immediate UP
        val flick = com.example.ui.gamepad.DPadFlickController()
        flick.onDown(116f, 116f)
        val tinyWobble = flick.onMove(116f, 110f, density, uptimeMs = 10L)
        assertFalse(tinyWobble.isFlickActive)
        assertEquals(DPadDirectionState(), tinyWobble.direction)

        val flickUp = flick.onMove(122f, 92f, density, uptimeMs = 20L) // slightly angled ~14 deg up-right -> clean UP
        assertTrue(flickUp.isFlickActive)
        assertTrue(flickUp.directionChanged)
        assertEquals(DPadDirectionState(up = true), flickUp.direction)

        // Very fast lift at 30ms (only 10ms elapsed) preserves minimum flick pulse
        val fastUpOutcome = flick.onUp(size, size, uptimeMs = 30L)
        assertTrue(fastUpOutcome.shouldDeferClearMs in 1L..45L)
        assertEquals(null, fastUpOutcome.tapPulseDirection)

        // 2. Touch left edge and swipe right -> fires RIGHT (never misfires LEFT)
        flick.onDown(24f, 116f)
        val swipeRight = flick.onMove(52f, 116f, density, uptimeMs = 100L)
        assertTrue(swipeRight.isFlickActive)
        assertEquals(DPadDirectionState(right = true), swipeRight.direction)

        // Continue dragging far right (90px past anchor -> leash follows), then jitter back 4dp (8px) -> stays RIGHT
        flick.onMove(160f, 116f, density, uptimeMs = 130L)
        val jitterHold = flick.onMove(152f, 116f, density, uptimeMs = 140L)
        assertEquals(DPadDirectionState(right = true), jitterHold.direction)

        // Slide back left past leashed anchor + threshold -> flips to LEFT smoothly without lifting
        val reverseLeft = flick.onMove(80f, 116f, density, uptimeMs = 160L)
        assertEquals(DPadDirectionState(left = true), reverseLeft.direction)

        // Held long enough -> instant clear on release
        val heldOutcome = flick.onUp(size, size, uptimeMs = 240L)
        assertEquals(0L, heldOutcome.shouldDeferClearMs)

        // 3. Deliberate 45-degree diagonal flick triggers both DOWN and RIGHT
        flick.onDown(116f, 116f)
        val diag = flick.onMove(140f, 140f, density, uptimeMs = 300L)
        assertEquals(DPadDirectionState(down = true, right = true), diag.direction)

        // 4. Quick tap on outer bottom arm without swiping yields a tap pulse on lift
        flick.onDown(116f, 200f)
        val quickTapOutcome = flick.onUp(size, size, uptimeMs = 340L)
        assertEquals(DPadDirectionState(down = true), quickTapOutcome.tapPulseDirection)

        // 5. Stationary hold on outer left arm engages after dwell timer
        flick.onDown(24f, 116f)
        assertEquals(DPadDirectionState(left = true), flick.onHoldDwellElapsed(size, size, uptimeMs = 490L))

        // 6. Pill-proportioned D-Pad outer tap normalization maps top/right icons cleanly
        assertEquals(
            DPadDirectionState(up = true),
            com.example.ui.gamepad.DPadFlickController.outerArmDirectionAt(168f, 40f, width = 336, height = 232)
        )
        assertEquals(
            DPadDirectionState(right = true),
            com.example.ui.gamepad.DPadFlickController.outerArmDirectionAt(290f, 116f, width = 336, height = 232)
        )
    }

    @Test
    fun buttonIconSize_neverExceedsHalfOfButtonHeightAcrossAllShapesAndScales() {
        val iconButtons = listOf(
            ControlType.TRIGGER_LT, ControlType.TRIGGER_RT,
            ControlType.BUTTON_LB, ControlType.BUTTON_RB,
            ControlType.BUTTON_L3, ControlType.BUTTON_R3,
            ControlType.BUTTON_BACK, ControlType.BUTTON_START,
            ControlType.SETTINGS
        )
        val scales = listOf(0.55f, 0.85f, 1.0f, 1.35f, 1.65f)
        iconButtons.forEach { type ->
            ButtonShape.entries.forEach { shape ->
                scales.forEach { scale ->
                    val placement = ControlPlacement(
                        type = type,
                        xFraction = 0.5f,
                        yFraction = 0.5f,
                        scale = scale,
                        shape = shape
                    )
                    val w = placement.effectiveWidthDp()
                    val h = placement.effectiveHeightDp()
                    val iconSize = com.example.ui.gamepad.buttonIconSizeDp(w, h)
                    assertTrue(
                        "Icon height ($iconSize) must be <= 50% of button height ($h) for $type/$shape/$scale",
                        iconSize <= h * 0.50f + 0.0001f
                    )
                    assertTrue(
                        "Icon size ($iconSize) must also fit within button width ($w) for $type/$shape/$scale",
                        iconSize <= w * 0.50f + 0.0001f
                    )
                }
            }
        }
    }
}
