package com.example.model

import com.example.xinput.TouchMouseLookFilter
import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

enum class PresetCategory(val label: String) {
    FPS("FPS Touch-Aim"),
    CONSOLE("Console Full Pad"),
    ACTION("Action / Arena"),
    RETRO("Retro 2D"),
    CUSTOM("Custom")
}

@JsonClass(generateAdapter = true)
data class ControlPlacement(
    val type: ControlType,
    val xFraction: Float,
    val yFraction: Float,
    val scale: Float = 1.0f,
    val visible: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ControllerPreset(
    val id: String,
    val name: String,
    val description: String,
    val category: PresetCategory,
    val isBuiltIn: Boolean,
    val opacity: Float = 0.78f,
    val gridRows: Int = GridSnapper.DEFAULT_GRID_ROWS,
    val snapToGrid: Boolean = true,
    val leftMoveZoneEnabled: Boolean = false,
    val fpsRightZoneEnabled: Boolean = false,
    val fpsLookSensitivity: Float = TouchMouseLookFilter.DEFAULT_SENSITIVITY,
    val triggerDragAimEnabled: Boolean = true,
    val invertRightY: Boolean = false,
    val controls: List<ControlPlacement>
) {
    fun placementFor(type: ControlType): ControlPlacement {
        return controls.firstOrNull { it.type == type }
            ?: ControlPlacement(type = type, xFraction = 0.5f, yFraction = 0.5f, visible = false)
    }

    fun updatePlacement(updated: ControlPlacement): ControllerPreset {
        val exists = controls.any { it.type == updated.type }
        val next = if (exists) {
            controls.map { if (it.type == updated.type) updated else it }
        } else {
            controls + updated
        }
        return copy(controls = next)
    }
}

data class AppPresetBinding(
    val packageName: String,
    val appLabel: String,
    val presetId: String
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String
)

object GridSnapper {
    const val DEFAULT_GRID_ROWS = 20
    const val MIN_GRID_ROWS = 6
    const val MAX_GRID_ROWS = 40
    const val MIN_SCALE = 0.60f
    const val MAX_SCALE = 1.80f
    const val SCALE_STEP = 0.05f
    const val MIN_OPACITY = 0.15f
    const val MAX_OPACITY = 1.0f

    fun computeGridColumns(rows: Int, widthPx: Float, heightPx: Float): Int {
        val safeRows = rows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        if (widthPx <= 0f || heightPx <= 0f) return safeRows
        return computeGridColumns(safeRows, widthPx / heightPx)
    }

    fun computeGridColumns(rows: Int, aspectRatio: Float): Int {
        val safeRows = rows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        val safeAspect = if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else (16f / 9f)
        return (safeRows * safeAspect).roundToInt().coerceAtLeast(2)
    }

    /**
     * Normalized [0f..1f] coordinates for grid lines centered on 0.5f so the middle grid line
     * always passes directly through the center of the screen regardless of division parity.
     */
    fun gridLineFractions(divisions: Int): List<Float> {
        val safeDiv = divisions.coerceAtLeast(2)
        val step = 1f / safeDiv
        val maxIndex = kotlin.math.floor(0.5f * safeDiv + 1e-4f).toInt()
        return (-maxIndex..maxIndex).map { index ->
            (0.5f + index * step).coerceIn(0f, 1f)
        }
    }

    fun snapPosition(
        rawXFraction: Float,
        rawYFraction: Float,
        widthPx: Float,
        heightPx: Float,
        gridRows: Int,
        enabled: Boolean
    ): Pair<Float, Float> {
        val clampedX = rawXFraction.coerceIn(0.03f, 0.97f)
        val clampedY = rawYFraction.coerceIn(0.04f, 0.96f)
        if (!enabled || widthPx <= 0f || heightPx <= 0f) {
            return clampedX to clampedY
        }

        val rows = gridRows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        val cols = computeGridColumns(rows, widthPx, heightPx)
        val stepX = 1f / cols
        val stepY = 1f / rows

        val maxM = kotlin.math.floor((0.5f - 0.03f) * cols + 1e-4f).toInt().coerceAtLeast(1)
        val maxK = kotlin.math.floor((0.5f - 0.04f) * rows + 1e-4f).toInt().coerceAtLeast(1)

        val m = ((clampedX - 0.5f) * cols).roundToInt().coerceIn(-maxM, maxM)
        val k = ((clampedY - 0.5f) * rows).roundToInt().coerceIn(-maxK, maxK)

        return (0.5f + m * stepX) to (0.5f + k * stepY)
    }

    fun snapScale(rawScale: Float): Float {
        val stepped = (rawScale / SCALE_STEP).roundToInt() * SCALE_STEP
        return stepped.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun snapOpacity(rawOpacity: Float): Float {
        val stepped = (rawOpacity / 0.05f).roundToInt() * 0.05f
        return stepped.coerceIn(MIN_OPACITY, MAX_OPACITY)
    }
}
