package com.example.ui.gamepad

import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.GridSnapper
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pure hit-testing and fixed-canvas drag math shared by the in-app Layout Editor
 * and the floating Overlay Grid Editor.
 */
object ControlDragHelper {

    fun findHitControl(
        touchX: Float,
        touchY: Float,
        controls: List<ControlPlacement>,
        selectedControl: ControlType,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        effectiveScaleMultiplier: Float,
        densityPx: Float
    ): ControlPlacement? {
        if (canvasWidthPx <= 0f || canvasHeightPx <= 0f || densityPx <= 0f) return null

        val minHalfHitPx = 24f * densityPx
        var bestCandidate: ControlPlacement? = null
        var bestScore = Float.MAX_VALUE

        controls.forEach { placement ->
            if (!placement.visible && placement.type != selectedControl) return@forEach

            val cx = placement.xFraction * canvasWidthPx
            val cy = placement.yFraction * canvasHeightPx
            val visualHalfW = (placement.type.baseWidthDp * placement.scale * effectiveScaleMultiplier * densityPx) / 2f
            val visualHalfH = (placement.type.baseHeightDp * placement.scale * effectiveScaleMultiplier * densityPx) / 2f
            val hitHalfW = maxOf(visualHalfW, minHalfHitPx)
            val hitHalfH = maxOf(visualHalfH, minHalfHitPx)

            val dx = abs(touchX - cx)
            val dy = abs(touchY - cy)
            if (dx <= hitHalfW && dy <= hitHalfH) {
                // Normalized distance to control center + slight tie-breaker favoring smaller/selected controls
                val normalizedDist = hypot(dx / hitHalfW, dy / hitHalfH)
                val areaFactor = (placement.type.baseWidthDp * placement.type.baseHeightDp) / 20_000f
                val selectionBonus = if (placement.type == selectedControl) -0.04f else 0f
                val score = normalizedDist + areaFactor + selectionBonus
                if (score < bestScore) {
                    bestScore = score
                    bestCandidate = placement
                }
            }
        }

        return bestCandidate
    }

    fun computeDraggedPlacement(
        startXFraction: Float,
        startYFraction: Float,
        totalDeltaXPx: Float,
        totalDeltaYPx: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        stepPx: Float,
        snapToGrid: Boolean
    ): Pair<Float, Float> {
        if (canvasWidthPx <= 0f || canvasHeightPx <= 0f) {
            return startXFraction to startYFraction
        }
        val rawX = startXFraction + (totalDeltaXPx / canvasWidthPx)
        val rawY = startYFraction + (totalDeltaYPx / canvasHeightPx)
        return GridSnapper.snapPosition(
            rawXFraction = rawX,
            rawYFraction = rawY,
            widthPx = canvasWidthPx,
            heightPx = canvasHeightPx,
            stepPx = stepPx,
            enabled = snapToGrid
        )
    }
}
