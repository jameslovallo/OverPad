package com.example

import com.example.model.ControlType
import com.example.model.DefaultPresets
import com.example.model.GridSnapper
import com.example.shizuku.ShizukuConnectionStage
import com.example.shizuku.ShizukuStatus
import com.example.xinput.DPadDirectionState
import com.example.xinput.XInputControllerEngine
import com.example.xinput.XInputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperPadEngineTest {

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
    fun gridSnapper_snapsPositionsAndScalesCleanly() {
        val (snappedX, snappedY) = GridSnapper.snapPosition(
            rawXFraction = 0.51f,
            rawYFraction = 0.49f,
            widthPx = 1000f,
            heightPx = 500f,
            stepPx = 50f,
            enabled = true
        )
        assertEquals(0.50f, snappedX, 0.001f)
        assertEquals(0.50f, snappedY, 0.001f)

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
    }
}
