package com.example.xinput

import android.view.KeyEvent
import kotlin.math.roundToInt

/**
 * Canonical XInput 1.4 gamepad snapshot paired with standard Android KeyEvent / MotionEvent axes.
 */
data class XInputState(
    val buttonsMask: Int = 0,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val thumbLX: Float = 0f,
    val thumbLY: Float = 0f,
    val thumbRX: Float = 0f,
    val thumbRY: Float = 0f,
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,
    val hatX: Float = 0f,
    val hatY: Float = 0f,
    val activeKeyCodes: Set<Int> = emptySet(),
    val activeSources: Set<String> = emptySet(),
    val packetCounter: Long = 0L
) {
    val leftTriggerByte: Int get() = (leftTrigger.coerceIn(0f, 1f) * 255f).roundToInt()
    val rightTriggerByte: Int get() = (rightTrigger.coerceIn(0f, 1f) * 255f).roundToInt()

    val shortLX: Short get() = floatToShortAxis(thumbLX)
    val shortLY: Short get() = floatToShortAxis(thumbLY)
    val shortRX: Short get() = floatToShortAxis(thumbRX)
    val shortRY: Short get() = floatToShortAxis(thumbRY)

    fun isPressed(keyCode: Int): Boolean = keyCode in activeKeyCodes

    /**
     * Formats a genuine 20-byte XINPUT_GAMEPAD report packet (Type 0x00, Size 0x14).
     */
    fun toBinaryPacket(): ByteArray {
        val buf = ByteArray(20)
        buf[0] = 0x00 // Input report
        buf[1] = 0x14 // Report size 20 bytes
        buf[2] = (buttonsMask and 0xFF).toByte()
        buf[3] = ((buttonsMask ushr 8) and 0xFF).toByte()
        buf[4] = leftTriggerByte.toByte()
        buf[5] = rightTriggerByte.toByte()
        writeShortLE(buf, 6, shortLX)
        writeShortLE(buf, 8, shortLY)
        writeShortLE(buf, 10, shortRX)
        writeShortLE(buf, 12, shortRY)
        // Remaining 6 bytes reserved (0x00)
        return buf
    }

    fun toHexReportSummary(): String {
        val bytes = toBinaryPacket()
        return bytes.take(14).joinToString(" ") { "%02X".format(it) }
    }

    companion object {
        const val XINPUT_GAMEPAD_DPAD_UP = 0x0001
        const val XINPUT_GAMEPAD_DPAD_DOWN = 0x0002
        const val XINPUT_GAMEPAD_DPAD_LEFT = 0x0004
        const val XINPUT_GAMEPAD_DPAD_RIGHT = 0x0008
        const val XINPUT_GAMEPAD_START = 0x0010
        const val XINPUT_GAMEPAD_BACK = 0x0020
        const val XINPUT_GAMEPAD_LEFT_THUMB = 0x0040
        const val XINPUT_GAMEPAD_RIGHT_THUMB = 0x0080
        const val XINPUT_GAMEPAD_LEFT_SHOULDER = 0x0100
        const val XINPUT_GAMEPAD_RIGHT_SHOULDER = 0x0200
        const val XINPUT_GAMEPAD_A = 0x1000
        const val XINPUT_GAMEPAD_B = 0x2000
        const val XINPUT_GAMEPAD_X = 0x4000
        const val XINPUT_GAMEPAD_Y = 0x8000

        private fun floatToShortAxis(value: Float): Short {
            val clamped = value.coerceIn(-1f, 1f)
            return if (clamped >= 0f) {
                (clamped * 32767f).roundToInt().toShort()
            } else {
                (clamped * 32768f).roundToInt().toShort()
            }
        }

        private fun writeShortLE(buf: ByteArray, offset: Int, value: Short) {
            val v = value.toInt()
            buf[offset] = (v and 0xFF).toByte()
            buf[offset + 1] = ((v ushr 8) and 0xFF).toByte()
        }

        fun keyCodeDisplay(code: Int): String = when (code) {
            KeyEvent.KEYCODE_BUTTON_A -> "A"
            KeyEvent.KEYCODE_BUTTON_B -> "B"
            KeyEvent.KEYCODE_BUTTON_X -> "X"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y"
            KeyEvent.KEYCODE_BUTTON_L1 -> "LB"
            KeyEvent.KEYCODE_BUTTON_R1 -> "RB"
            KeyEvent.KEYCODE_BUTTON_L2 -> "LT"
            KeyEvent.KEYCODE_BUTTON_R2 -> "RT"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
            KeyEvent.KEYCODE_BUTTON_START -> "START"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "BACK"
            KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
            else -> KeyEvent.keyCodeToString(code)
        }
    }
}
