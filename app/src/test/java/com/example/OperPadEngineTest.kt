package com.example

import com.example.model.ControlType
import com.example.model.DefaultPresets
import com.example.model.GridSnapper
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
}
