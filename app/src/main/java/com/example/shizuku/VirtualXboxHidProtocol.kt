package com.example.shizuku

import com.example.xinput.XInputState
import kotlin.math.roundToInt

/**
 * Pure protocol encoder for spoofing an Xbox controller over Android's
 * `/system/bin/hid` (/dev/uhid) and `/system/bin/uinput` (/dev/uinput) tools.
 */
object VirtualXboxHidProtocol {

    const val MICROSOFT_VID = 0x045E // 1118
    const val XBOX_360_USB_PID = 0x028E // 654 -> matches Vendor_045e_Product_028e.kl
    const val XBOX_UHID_PID = 0x0B20 // 2848 -> routes cleanly through Generic.kl as Xbox Wireless Controller

    const val UHID_NAME = "Xbox Wireless Controller"
    const val UINPUT_NAME = "Microsoft X-Box 360 pad"

    /**
     * Standard 14-byte-report HID Gamepad descriptor matching Android Generic.kl:
     *  - Report ID 1 (1 byte)
     *  - 15 buttons + 1 padding bit (2 bytes)
     *  - 4-bit Hat switch (1..8, null state) + 4-bit padding (1 byte)
     *  - 4 signed 16-bit stick axes: X (0x30), Y (0x31), Z (0x32), Rz (0x35) (8 bytes)
     *  - 2 unsigned 8-bit triggers: Brake (0xC5), Accelerator (0xC4) (2 bytes)
     */
    val UHID_DESCRIPTOR: IntArray = intArrayOf(
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x05,             // Usage (Gamepad)
        0xA1, 0x01,             // Collection (Application)
        0x85, 0x01,             //   Report ID (1)

        // 15 buttons + 1 padding bit
        0x05, 0x09,             //   Usage Page (Button)
        0x19, 0x01,             //   Usage Minimum (Button 1)
        0x29, 0x0F,             //   Usage Maximum (Button 15)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x0F,             //   Report Count (15)
        0x81, 0x02,             //   Input (Data, Var, Abs)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x03,             //   Input (Const, Var, Abs)

        // D-Pad Hat Switch (4 bits) + 4 bits padding
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x39,             //   Usage (Hat Switch)
        0x15, 0x01,             //   Logical Minimum (1)
        0x25, 0x08,             //   Logical Maximum (8)
        0x35, 0x00,             //   Physical Minimum (0)
        0x46, 0x3B, 0x01,       //   Physical Maximum (315)
        0x65, 0x14,             //   Unit (Eng Rot: Angular Pos)
        0x75, 0x04,             //   Report Size (4)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x42,             //   Input (Data, Var, Abs, Null)
        0x35, 0x00,             //   Physical Minimum (0)
        0x45, 0x00,             //   Physical Maximum (0)
        0x65, 0x00,             //   Unit (None)
        0x75, 0x04,             //   Report Size (4)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x03,             //   Input (Const, Var, Abs)

        // Thumbsticks: X (LX), Y (LY), Z (RX), Rz (RY)
        0x09, 0x30,             //   Usage (X)
        0x09, 0x31,             //   Usage (Y)
        0x09, 0x32,             //   Usage (Z)
        0x09, 0x35,             //   Usage (Rz)
        0x16, 0x01, 0x80,       //   Logical Minimum (-32767)
        0x26, 0xFF, 0x7F,       //   Logical Maximum (32767)
        0x75, 0x10,             //   Report Size (16)
        0x95, 0x04,             //   Report Count (4)
        0x81, 0x02,             //   Input (Data, Var, Abs)

        // Triggers: Brake (LT), Accelerator (RT)
        0x05, 0x02,             //   Usage Page (Simulation Controls)
        0x09, 0xC5,             //   Usage (Brake)
        0x09, 0xC4,             //   Usage (Accelerator)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF, 0x00,       //   Logical Maximum (255)
        0x75, 0x08,             //   Report Size (8)
        0x95, 0x02,             //   Report Count (2)
        0x81, 0x02,             //   Input (Data, Var, Abs)

        0xC0                    // End Collection
    )

    fun buildUhidRegisterJson(deviceId: Int = 1): String = buildString(512) {
        append("{\"id\":").append(deviceId)
        append(",\"command\":\"register\"")
        append(",\"name\":\"").append(escapeJsonString(UHID_NAME)).append('"')
        append(",\"vid\":").append(MICROSOFT_VID)
        append(",\"pid\":").append(XBOX_UHID_PID)
        append(",\"bus\":\"bluetooth\"")
        append(",\"descriptor\":[")
        for (i in UHID_DESCRIPTOR.indices) {
            if (i > 0) append(',')
            append(UHID_DESCRIPTOR[i])
        }
        append("]}")
    }

    fun buildUhidDelayJson(deviceId: Int = 1, durationMs: Int = 100): String {
        return "{\"id\":$deviceId,\"command\":\"delay\",\"duration\":$durationMs}"
    }

    /**
     * Converts XInputState fields into the exact 14-byte HID report matching [UHID_DESCRIPTOR].
     */
    fun buildUhidReportBytes(
        buttonsMask: Int,
        leftTrigger: Float,
        rightTrigger: Float,
        thumbLX: Float,
        thumbLY: Float,
        thumbRX: Float,
        thumbRY: Float,
        hatX: Float,
        hatY: Float
    ): IntArray {
        var hidButtons = 0
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_A) != 0) hidButtons = hidButtons or (1 shl 0)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_B) != 0) hidButtons = hidButtons or (1 shl 1)
        // bit 2 = BTN_C (skipped in Generic.kl)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_X) != 0) hidButtons = hidButtons or (1 shl 3)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_Y) != 0) hidButtons = hidButtons or (1 shl 4)
        // bit 5 = BTN_Z (skipped in Generic.kl)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_LEFT_SHOULDER) != 0) hidButtons = hidButtons or (1 shl 6)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_RIGHT_SHOULDER) != 0) hidButtons = hidButtons or (1 shl 7)
        if (leftTrigger >= 0.5f) hidButtons = hidButtons or (1 shl 8)
        if (rightTrigger >= 0.5f) hidButtons = hidButtons or (1 shl 9)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_BACK) != 0) hidButtons = hidButtons or (1 shl 10)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_START) != 0) hidButtons = hidButtons or (1 shl 11)
        // bit 12 = BTN_MODE
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_LEFT_THUMB) != 0) hidButtons = hidButtons or (1 shl 13)
        if ((buttonsMask and XInputState.XINPUT_GAMEPAD_RIGHT_THUMB) != 0) hidButtons = hidButtons or (1 shl 14)

        val hat = encodeHatSwitch(hatX, hatY)
        val lx = scaleStickInt16(thumbLX)
        val ly = scaleStickInt16(thumbLY)
        val rx = scaleStickInt16(thumbRX)
        val ry = scaleStickInt16(thumbRY)
        val lt = (leftTrigger.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        val rt = (rightTrigger.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)

        return intArrayOf(
            0x01,
            hidButtons and 0xFF,
            (hidButtons ushr 8) and 0xFF,
            hat and 0x0F,
            lx and 0xFF,
            (lx ushr 8) and 0xFF,
            ly and 0xFF,
            (ly ushr 8) and 0xFF,
            rx and 0xFF,
            (rx ushr 8) and 0xFF,
            ry and 0xFF,
            (ry ushr 8) and 0xFF,
            lt,
            rt
        )
    }

    fun buildUhidReportJson(
        deviceId: Int = 1,
        reportBytes: IntArray
    ): String = buildString(128) {
        append("{\"id\":").append(deviceId)
        append(",\"command\":\"report\",\"report\":[")
        for (i in reportBytes.indices) {
            if (i > 0) append(',')
            append(reportBytes[i] and 0xFF)
        }
        append("]}")
    }

    fun buildUinputRegisterJson(deviceId: Int = 1): String = buildString(1024) {
        val evBits = intArrayOf(EV_KEY, EV_ABS)
        val keyBits = intArrayOf(
            BTN_A, BTN_B, BTN_X, BTN_Y,
            BTN_TL, BTN_TR, BTN_SELECT, BTN_START,
            BTN_THUMBL, BTN_THUMBR
        )
        val absBits = intArrayOf(
            ABS_X, ABS_Y, ABS_Z,
            ABS_RX, ABS_RY, ABS_RZ,
            ABS_HAT0X, ABS_HAT0Y
        )

        append("{\"id\":").append(deviceId)
        append(",\"command\":\"register\"")
        append(",\"name\":\"").append(escapeJsonString(UINPUT_NAME)).append('"')
        append(",\"vid\":").append(MICROSOFT_VID)
        append(",\"pid\":").append(XBOX_360_USB_PID)
        append(",\"bus\":\"usb\"")
        append(",\"configuration\":[")
        append("{\"type\":\"UI_SET_EVBIT\",\"data\":[").append(evBits.joinToString(",")).append("]},")
        append("{\"type\":\"UI_SET_KEYBIT\",\"data\":[").append(keyBits.joinToString(",")).append("]},")
        append("{\"type\":\"UI_SET_ABSBIT\",\"data\":[").append(absBits.joinToString(",")).append("]}")
        append("]")
        append(",\"abs_info\":[")
        append(absInfoJson(ABS_X, -32768, 32767, 0, 0)).append(',')
        append(absInfoJson(ABS_Y, -32768, 32767, 0, 0)).append(',')
        append(absInfoJson(ABS_Z, 0, 255, 0, 0)).append(',')
        append(absInfoJson(ABS_RX, -32768, 32767, 0, 0)).append(',')
        append(absInfoJson(ABS_RY, -32768, 32767, 0, 0)).append(',')
        append(absInfoJson(ABS_RZ, 0, 255, 0, 0)).append(',')
        append(absInfoJson(ABS_HAT0X, -1, 1, 0, 0)).append(',')
        append(absInfoJson(ABS_HAT0Y, -1, 1, 0, 0))
        append("]}")
    }

    fun buildUinputInjectJson(deviceId: Int = 1, events: List<Int>): String = buildString(256) {
        append("{\"id\":").append(deviceId)
        append(",\"command\":\"inject\",\"events\":[")
        for (i in events.indices) {
            if (i > 0) append(',')
            append(events[i])
        }
        append("]}")
    }

    private fun absInfoJson(
        code: Int,
        min: Int,
        max: Int,
        fuzz: Int,
        flat: Int
    ): String {
        return "{\"code\":$code,\"info\":{\"value\":0,\"minimum\":$min,\"maximum\":$max,\"fuzz\":$fuzz,\"flat\":$flat,\"resolution\":0}}"
    }

    private fun escapeJsonString(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    fun encodeHatSwitch(hatX: Float, hatY: Float): Int {
        val hx = when {
            hatX > 0.5f -> 1
            hatX < -0.5f -> -1
            else -> 0
        }
        val hy = when {
            hatY > 0.5f -> 1
            hatY < -0.5f -> -1
            else -> 0
        }
        return when {
            hx == 0 && hy == -1 -> 1 // N
            hx == 1 && hy == -1 -> 2 // NE
            hx == 1 && hy == 0 -> 3  // E
            hx == 1 && hy == 1 -> 4  // SE
            hx == 0 && hy == 1 -> 5  // S
            hx == -1 && hy == 1 -> 6 // SW
            hx == -1 && hy == 0 -> 7 // W
            hx == -1 && hy == -1 -> 8 // NW
            else -> 0 // Null / Centered
        }
    }

    fun scaleStickInt16(value: Float): Int {
        return (value.coerceIn(-1f, 1f) * 32767f).roundToInt().coerceIn(-32767, 32767)
    }

    // Linux input event constants (matches uinput + Vendor_045e_Product_028e.kl)
    const val EV_SYN = 0
    const val EV_KEY = 1
    const val EV_ABS = 3
    const val SYN_REPORT = 0

    const val BTN_A = 304
    const val BTN_B = 305
    const val BTN_X = 307
    const val BTN_Y = 308
    const val BTN_TL = 310
    const val BTN_TR = 311
    const val BTN_SELECT = 314
    const val BTN_START = 315
    const val BTN_THUMBL = 317
    const val BTN_THUMBR = 318

    const val ABS_X = 0
    const val ABS_Y = 1
    const val ABS_Z = 2     // LT in Vendor_045e_Product_028e.kl
    const val ABS_RX = 3    // Right Stick X in Vendor_045e_Product_028e.kl
    const val ABS_RY = 4    // Right Stick Y in Vendor_045e_Product_028e.kl
    const val ABS_RZ = 5    // RT in Vendor_045e_Product_028e.kl
    const val ABS_HAT0X = 16
    const val ABS_HAT0Y = 17
}
