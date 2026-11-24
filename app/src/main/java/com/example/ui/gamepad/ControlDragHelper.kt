package com.example.ui.gamepad

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.GridSnapper
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pure hit-testing, device aspect ratio, and fixed-canvas drag math shared by the in-app
 * Layout Editor and the floating Overlay Grid Editor.
 */
object ControlDragHelper {

    fun deviceLandscapeAspectRatio(context: Context): Float {
        val windowManager = context.getSystemService(WindowManager::class.java)
        val (w, h) = if (windowManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            bounds.width().toFloat() to bounds.height().toFloat()
        } else if (windowManager != null) {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels.toFloat() to metrics.heightPixels.toFloat()
        } else {
            val metrics = context.resources.displayMetrics
            metrics.widthPixels.toFloat() to metrics.heightPixels.toFloat()
        }
        return if (w > 0f && h > 0f) {
            (maxOf(w, h) / minOf(w, h)).coerceIn(1.25f, 2.60f)
        } else {
            16f / 9f
        }
    }

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
            val visualHalfW = (placement.effectiveWidthDp() * effectiveScaleMultiplier * densityPx) / 2f
            val visualHalfH = (placement.effectiveHeightDp() * effectiveScaleMultiplier * densityPx) / 2f
            val hitHalfW = maxOf(visualHalfW, minHalfHitPx)
            val hitHalfH = maxOf(visualHalfH, minHalfHitPx)

            val dx = abs(touchX - cx)
            val dy = abs(touchY - cy)
            if (dx <= hitHalfW && dy <= hitHalfH) {
                val normalizedDist = hypot(dx / hitHalfW, dy / hitHalfH)
                val areaFactor = (placement.effectiveWidthDp() * placement.effectiveHeightDp()) / 20_000f
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
        gridRows: Int,
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
            gridRows = gridRows,
            enabled = snapToGrid
        )
    }
}
