package com.example.xinput

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow

/**
 * Translates raw touch displacement deltas (px) into frame-rate-independent,
 * mouse-like FPS Right Thumbstick (RX, RY) output suitable for cloud/console games.
 *
 * Key properties matching native mobile FPS touch-look (COD Mobile / PUBG Mobile):
 * 1. Velocity-normalized (`dp / dt`) so 60Hz, 90Hz, and 120Hz+ touch rates produce identical turn speed.
 * 2. Asymmetric EMA smoothing (fast 10ms attack for snappy flicks, 20ms glide to suppress digitizer jitter).
 * 3. Sub-linear response curve + configurable anti-deadzone boost so micro-aim crosses console inner deadzones.
 * 4. Smooth exponential decay to exact zero when the thumb pauses, and instant zero on lift.
 */
class TouchMouseLookFilter {

    private var velX = 0f
    private var velY = 0f
    private var lastSampleTimeMs = 0L
    private var lastEmittedZero = true

    @Synchronized
    fun onPointerDown(uptimeMs: Long): Pair<Float, Float> {
        velX = 0f
        velY = 0f
        lastSampleTimeMs = uptimeMs
        lastEmittedZero = true
        return 0f to 0f
    }

    /**
     * Processes a pointer movement delta in density-independent pixels (`dp`).
     * Returns raw screen-oriented stick output `(rx, ry)` where +Y is downward before any Y-inversion.
     */
    @Synchronized
    fun onPointerMove(
        dxDp: Float,
        dyDp: Float,
        uptimeMs: Long,
        sensitivity: Float,
        antiDeadzone: Float = DEFAULT_ANTI_DEADZONE
    ): Pair<Float, Float> {
        val rawDt = uptimeMs - lastSampleTimeMs
        val dtMs = when {
            lastSampleTimeMs == 0L || rawDt > PAUSE_RESET_MS -> {
                // Fresh stroke after pause: reset residual velocity so there is no lag spike
                velX = 0f
                velY = 0f
                16L
            }
            rawDt <= 0L -> 8L
            else -> rawDt.coerceIn(4L, 64L)
        }
        lastSampleTimeMs = uptimeMs

        val stepMag = hypot(dxDp, dyDp)
        if (stepMag < JITTER_FLOOR_DP && hypot(velX, velY) < STOP_SPEED_DP_PER_SEC) {
            lastEmittedZero = true
            return 0f to 0f
        }

        val dtSec = dtMs / 1000f
        val instVx = dxDp / dtSec
        val instVy = dyDp / dtSec

        val instSpeed = hypot(instVx, instVy)
        val curSpeed = hypot(velX, velY)
        val tauMs = if (instSpeed > curSpeed) TAU_ATTACK_MS else TAU_GLIDE_MS
        val alpha = (1f - exp(-dtMs.toFloat() / tauMs)).coerceIn(0.08f, 1f)

        velX += alpha * (instVx - velX)
        velY += alpha * (instVy - velY)

        return mapVelocityToStick(velX, velY, sensitivity, antiDeadzone)
    }

    /**
     * Smoothly decays velocity toward zero when the thumb rests stationary on glass.
     * Returns `null` if the finger moved very recently or is already settled at zero.
     */
    @Synchronized
    fun decayIfIdle(
        nowUptimeMs: Long,
        sensitivity: Float,
        antiDeadzone: Float = DEFAULT_ANTI_DEADZONE
    ): Pair<Float, Float>? {
        val elapsed = nowUptimeMs - lastSampleTimeMs
        if (elapsed < IDLE_HOLD_THRESHOLD_MS) return null
        if (lastEmittedZero && velX == 0f && velY == 0f) return null

        val decayFactor = exp(-DECAY_STEP_MS / TAU_DECAY_MS)
        velX *= decayFactor
        velY *= decayFactor

        if (hypot(velX, velY) < STOP_SPEED_DP_PER_SEC) {
            velX = 0f
            velY = 0f
            lastEmittedZero = true
            return 0f to 0f
        }

        return mapVelocityToStick(velX, velY, sensitivity, antiDeadzone)
    }

    @Synchronized
    fun reset(): Pair<Float, Float> {
        velX = 0f
        velY = 0f
        lastSampleTimeMs = 0L
        lastEmittedZero = true
        return 0f to 0f
    }

    private fun mapVelocityToStick(
        vx: Float,
        vy: Float,
        sensitivity: Float,
        antiDeadzone: Float
    ): Pair<Float, Float> {
        val speed = hypot(vx, vy)
        if (speed < STOP_SPEED_DP_PER_SEC) {
            lastEmittedZero = true
            return 0f to 0f
        }

        val clampedSens = sensitivity.coerceIn(0.4f, 3.5f)
        val clampedBoost = antiDeadzone.coerceIn(0f, MAX_ANTI_DEADZONE)

        val scaled = ((speed * clampedSens) / REFERENCE_MAX_SPEED_DP_PER_SEC).coerceIn(0f, 1f)
        val curved = scaled.pow(RESPONSE_EXPONENT)
        val magnitude = (clampedBoost + (1f - clampedBoost) * curved).coerceIn(0f, 1f)

        val outX = ((vx / speed) * magnitude).coerceIn(-1f, 1f)
        val outY = ((vy / speed) * magnitude).coerceIn(-1f, 1f)
        lastEmittedZero = false
        return outX to outY
    }

    companion object {
        const val DEFAULT_ANTI_DEADZONE = 0.14f
        const val MIN_ANTI_DEADZONE = 0.00f
        const val MAX_ANTI_DEADZONE = 0.28f

        private const val REFERENCE_MAX_SPEED_DP_PER_SEC = 560f
        private const val RESPONSE_EXPONENT = 0.72f
        private const val JITTER_FLOOR_DP = 0.25f
        private const val STOP_SPEED_DP_PER_SEC = 7.5f

        private const val TAU_ATTACK_MS = 10f
        private const val TAU_GLIDE_MS = 20f
        private const val TAU_DECAY_MS = 16f
        private const val DECAY_STEP_MS = 12f
        private const val IDLE_HOLD_THRESHOLD_MS = 14L
        private const val PAUSE_RESET_MS = 120L
    }
}
