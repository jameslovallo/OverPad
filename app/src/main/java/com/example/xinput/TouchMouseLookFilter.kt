package com.example.xinput

import kotlin.math.exp
import kotlin.math.hypot

/**
 * Translates touchscreen swipe deltas (`dp`) into smooth, frame-rate-independent,
 * mobile-shooter Right Thumbstick (RX, RY) output using a VSync-paced spring anchor.
 *
 * Why this stays smooth with minimal intervention:
 * 1. No `dx / dt` division on touch timestamps: raw `dp` deltas accumulate directly into a
 *    trailing virtual stick offset, eliminating touch-IC timing jitter, 0-delta multi-touch
 *    artifacts, and attack/glide mode switching.
 * 2. Display-locked exponential relaxation (`stepFrame` via `withFrameNanos`) drains the anchor
 *    smoothly at the exact VSync interval (`8.33ms` @ 120Hz, `16.67ms` @ 60Hz) with midpoint
 *    sampling so 60Hz and 120Hz produce matching turn rates and silky low-speed tracking.
 * 3. Balanced vertical pitch gain (`VERTICAL_SENSITIVITY_GAIN = 1.50f`) and a linear-clean curve
 *    keep both horizontal and vertical aiming predictable and responsive.
 */
class TouchMouseLookFilter {

    private var offsetX = 0f
    private var offsetY = 0f
    private var lastSampleTimeMs = 0L
    private var lastEmittedZero = true

    @Synchronized
    fun onPointerDown(uptimeMs: Long = 0L): Pair<Float, Float> {
        offsetX = 0f
        offsetY = 0f
        lastSampleTimeMs = uptimeMs
        lastEmittedZero = true
        return 0f to 0f
    }

    /**
     * Accumulates raw finger displacement (`dp`) without dividing by noisy touch timestamps.
     * Call [stepFrame] once per VSync frame to emit stick output and advance the spring anchor.
     */
    @Synchronized
    fun accumulateDelta(
        dxDp: Float,
        dyDp: Float,
        sensitivity: Float
    ) {
        if (dxDp == 0f && dyDp == 0f) return
        offsetX += dxDp
        offsetY += dyDp * VERTICAL_SENSITIVITY_GAIN

        val maxLeash = fullStickDp(sensitivity) * LEASH_HEADROOM
        val mag = hypot(offsetX, offsetY)
        if (mag > maxLeash && mag > 0f) {
            val scale = maxLeash / mag
            offsetX *= scale
            offsetY *= scale
        }
    }

    /**
     * Advances the trailing spring anchor by one VSync frame (`dtMs`) and returns the
     * screen-oriented stick deflection `(rx, ry)`, or `null` if already settled at zero.
     */
    @Synchronized
    fun stepFrame(
        dtMs: Float,
        sensitivity: Float,
        antiDeadzone: Float = DEFAULT_ANTI_DEADZONE
    ): Pair<Float, Float>? {
        val beforeMag = hypot(offsetX, offsetY)
        if (beforeMag < ZERO_SNAP_DP) {
            offsetX = 0f
            offsetY = 0f
            return if (lastEmittedZero) {
                null
            } else {
                lastEmittedZero = true
                0f to 0f
            }
        }

        val clampedDt = dtMs.coerceIn(1f, 50f)
        val decay = exp(-clampedDt / TAU_MS)
        val afterX = offsetX * decay
        val afterY = offsetY * decay

        // Midpoint sampling across the frame keeps 60Hz/90Hz/120Hz steady-state deflection identical
        val midX = 0.5f * (offsetX + afterX)
        val midY = 0.5f * (offsetY + afterY)

        if (hypot(afterX, afterY) < ZERO_SNAP_DP) {
            offsetX = 0f
            offsetY = 0f
        } else {
            offsetX = afterX
            offsetY = afterY
        }

        return mapOffsetToStick(midX, midY, sensitivity, antiDeadzone)
    }

    /**
     * Convenience wrapper for deterministic stepping in unit tests or non-VSync callers.
     */
    @Synchronized
    fun onPointerMove(
        dxDp: Float,
        dyDp: Float,
        uptimeMs: Long,
        sensitivity: Float,
        antiDeadzone: Float = DEFAULT_ANTI_DEADZONE
    ): Pair<Float, Float> {
        val rawDt = if (lastSampleTimeMs == 0L) 16L else (uptimeMs - lastSampleTimeMs)
        val dtMs = rawDt.coerceIn(1L, 50L).toFloat()
        lastSampleTimeMs = uptimeMs

        accumulateDelta(dxDp, dyDp, sensitivity)
        return stepFrame(dtMs, sensitivity, antiDeadzone) ?: (0f to 0f)
    }

    @Synchronized
    fun decayIfIdle(
        nowUptimeMs: Long,
        sensitivity: Float,
        antiDeadzone: Float = DEFAULT_ANTI_DEADZONE
    ): Pair<Float, Float>? {
        val rawDt = (nowUptimeMs - lastSampleTimeMs).coerceIn(1L, 50L).toFloat()
        lastSampleTimeMs = nowUptimeMs
        return stepFrame(rawDt, sensitivity, antiDeadzone)
    }

    @Synchronized
    fun reset(): Pair<Float, Float> {
        offsetX = 0f
        offsetY = 0f
        lastSampleTimeMs = 0L
        lastEmittedZero = true
        return 0f to 0f
    }

    private fun mapOffsetToStick(
        ox: Float,
        oy: Float,
        sensitivity: Float,
        antiDeadzone: Float
    ): Pair<Float, Float> {
        val mag = hypot(ox, oy)
        if (mag < ZERO_SNAP_DP) {
            lastEmittedZero = true
            return 0f to 0f
        }

        val clampedBoost = antiDeadzone.coerceIn(0f, MAX_ANTI_DEADZONE)
        val fullDp = fullStickDp(sensitivity)
        val scaled = (mag / fullDp).coerceIn(0f, 1f)

        // Smooth ease-in over sub-millimeter noise, then clean linear response above it
        val boostRamp = (scaled / ANTI_DEADZONE_RAMP_THRESHOLD).coerceIn(0f, 1f)
        val effectiveBoost = clampedBoost * boostRamp * (2f - boostRamp)
        val magnitude = (effectiveBoost + (1f - effectiveBoost) * scaled).coerceIn(0f, 1f)

        val outX = ((ox / mag) * magnitude).coerceIn(-1f, 1f)
        val outY = ((oy / mag) * magnitude).coerceIn(-1f, 1f)
        lastEmittedZero = false
        return outX to outY
    }

    private fun fullStickDp(sensitivity: Float): Float {
        val clampedSens = sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        return BASE_FULL_STICK_DP / clampedSens
    }

    companion object {
        const val MIN_SENSITIVITY = 1.0f
        const val MAX_SENSITIVITY = 6.0f
        const val DEFAULT_SENSITIVITY = 2.5f
        const val SENSITIVITY_STEP = 0.10f
        const val SENSITIVITY_STEPS = 49

        const val DEFAULT_ANTI_DEADZONE = 0.14f
        const val MIN_ANTI_DEADZONE = 0.00f
        const val MAX_ANTI_DEADZONE = 0.28f
        const val ANTI_DEADZONE_STEP = 0.02f
        const val ANTI_DEADZONE_STEPS = 13

        const val VERTICAL_SENSITIVITY_GAIN = 1.50f
        const val IDLE_HOLD_THRESHOLD_MS = 34L

        fun snapSensitivity(rawSensitivity: Float): Float {
            val stepped = Math.round(rawSensitivity / SENSITIVITY_STEP) * SENSITIVITY_STEP
            return ((stepped * 100f).let { Math.round(it) } / 100f)
                .coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        }

        fun snapAntiDeadzone(rawAntiDeadzone: Float): Float {
            val stepped = Math.round(rawAntiDeadzone / ANTI_DEADZONE_STEP) * ANTI_DEADZONE_STEP
            return ((stepped * 100f).let { Math.round(it) } / 100f)
                .coerceIn(MIN_ANTI_DEADZONE, MAX_ANTI_DEADZONE)
        }

        private const val BASE_FULL_STICK_DP = 21.3f
        private const val LEASH_HEADROOM = 1.45f
        private const val TAU_MS = 28f
        private const val ANTI_DEADZONE_RAMP_THRESHOLD = 0.05f
        private const val ZERO_SNAP_DP = 0.015f
    }
}
