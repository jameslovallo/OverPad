package com.example.ui.gamepad

import com.example.xinput.DPadDirectionState
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Stateful gesture solver for the virtual D-Pad supporting both:
 * 1) Flick / swipe from anywhere inside the D-Pad bounds (relative to touch-down anchor, with
 *    a soft leash for rapid direction reversals without lifting), and
 * 2) Direct tap or hold on outer directional arms when the thumb doesn't swipe.
 */
class DPadFlickController {

    data class MoveResult(
        val direction: DPadDirectionState,
        val directionChanged: Boolean,
        val isFlickActive: Boolean,
        val visualOffsetXFraction: Float,
        val visualOffsetYFraction: Float
    )

    data class UpResult(
        val shouldDeferClearMs: Long,
        val tapPulseDirection: DPadDirectionState?
    )

    var anchorX: Float = 0f
        private set
    var anchorY: Float = 0f
        private set
    var downX: Float = 0f
        private set
    var downY: Float = 0f
        private set
    var isFlickMode: Boolean = false
        private set
    var engagedByHold: Boolean = false
        private set
    var currentDirection: DPadDirectionState = DPadDirectionState()
        private set
    private var lastActivatedUptimeMs: Long = 0L

    fun onDown(x: Float, y: Float) {
        downX = x
        downY = y
        anchorX = x
        anchorY = y
        isFlickMode = false
        engagedByHold = false
        currentDirection = DPadDirectionState()
        lastActivatedUptimeMs = 0L
    }

    fun onHoldDwellElapsed(width: Int, height: Int, uptimeMs: Long): DPadDirectionState? {
        if (isFlickMode || engagedByHold) return null
        val dir = outerArmDirectionAt(downX, downY, width, height)
        if (dir == DPadDirectionState()) return null
        engagedByHold = true
        currentDirection = dir
        lastActivatedUptimeMs = uptimeMs
        return dir
    }

    fun onMove(
        x: Float,
        y: Float,
        densityPx: Float,
        uptimeMs: Long
    ): MoveResult {
        val safeDensity = densityPx.coerceAtLeast(1f)
        val flickThresholdPx = FLICK_THRESHOLD_DP * safeDensity
        val releaseDeadzonePx = RELEASE_DEADZONE_DP * safeDensity
        val leashRadiusPx = LEASH_RADIUS_DP * safeDensity

        if (!isFlickMode) {
            val fromDownDist = hypot(x - downX, y - downY)
            if (fromDownDist < flickThresholdPx) {
                return MoveResult(
                    direction = currentDirection,
                    directionChanged = false,
                    isFlickActive = false,
                    visualOffsetXFraction = 0f,
                    visualOffsetYFraction = 0f
                )
            }
            isFlickMode = true
            engagedByHold = false
        }

        var dx = x - anchorX
        var dy = y - anchorY
        var dist = hypot(dx, dy)

        if (dist > leashRadiusPx && dist > 0f) {
            val scale = leashRadiusPx / dist
            anchorX = x - dx * scale
            anchorY = y - dy * scale
            dx = x - anchorX
            dy = y - anchorY
            dist = leashRadiusPx
        }

        val next = if (dist >= releaseDeadzonePx) {
            directionFromVector(dx, dy)
        } else {
            DPadDirectionState()
        }

        val changed = next != currentDirection
        if (changed) {
            currentDirection = next
            if (next != DPadDirectionState()) {
                lastActivatedUptimeMs = uptimeMs
            }
        }

        val visualScale = if (leashRadiusPx > 0f) (dist / leashRadiusPx).coerceIn(0f, 1f) else 0f
        val normX = if (dist > 0.001f) (dx / dist) * visualScale else 0f
        val normY = if (dist > 0.001f) (dy / dist) * visualScale else 0f

        return MoveResult(
            direction = next,
            directionChanged = changed,
            isFlickActive = true,
            visualOffsetXFraction = normX,
            visualOffsetYFraction = normY
        )
    }

    fun onUp(width: Int, height: Int, uptimeMs: Long): UpResult {
        if (isFlickMode) {
            val active = currentDirection != DPadDirectionState()
            val elapsed = if (active && lastActivatedUptimeMs > 0L) {
                (uptimeMs - lastActivatedUptimeMs).coerceAtLeast(0L)
            } else {
                MIN_FLICK_PULSE_MS
            }
            val deferMs = if (active && elapsed < MIN_FLICK_PULSE_MS) {
                MIN_FLICK_PULSE_MS - elapsed
            } else {
                0L
            }
            return UpResult(shouldDeferClearMs = deferMs, tapPulseDirection = null)
        }

        if (engagedByHold) {
            return UpResult(shouldDeferClearMs = 0L, tapPulseDirection = null)
        }

        val tapDir = outerArmDirectionAt(downX, downY, width, height)
        return if (tapDir != DPadDirectionState()) {
            UpResult(shouldDeferClearMs = 0L, tapPulseDirection = tapDir)
        } else {
            UpResult(shouldDeferClearMs = 0L, tapPulseDirection = null)
        }
    }

    companion object {
        const val FLICK_THRESHOLD_DP = 8f
        const val RELEASE_DEADZONE_DP = 5.5f
        const val LEASH_RADIUS_DP = 26f
        const val HOLD_DWELL_MS = 85L
        const val MIN_FLICK_PULSE_MS = 45L
        const val OUTER_TAP_INNER_FRACTION = 0.28f

        fun outerArmDirectionAt(x: Float, y: Float, width: Int, height: Int): DPadDirectionState {
            if (width <= 0 || height <= 0) return DPadDirectionState()
            val halfW = (width / 2f).coerceAtLeast(1f)
            val halfH = (height / 2f).coerceAtLeast(1f)
            val normDx = (x - halfW) / halfW
            val normDy = (y - halfH) / halfH
            if (hypot(normDx, normDy) < OUTER_TAP_INNER_FRACTION) {
                return DPadDirectionState()
            }
            return directionFromVector(normDx, normDy)
        }

        fun directionFromVector(dx: Float, dy: Float): DPadDirectionState {
            if (dx == 0f && dy == 0f) return DPadDirectionState()
            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
            // 64 deg pure cardinal sectors + 26 deg diagonal wedges so normal thumb flicks hit clean cardinals
            val right = angleDeg in -58.0..58.0
            val down = angleDeg in 32.0..148.0
            val up = angleDeg in -148.0..-32.0
            val left = angleDeg >= 122.0 || angleDeg <= -122.0
            return DPadDirectionState(up = up, down = down, left = left, right = right)
        }
    }
}
