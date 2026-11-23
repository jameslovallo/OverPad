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
    val gridStepDp: Int = 24,
    val snapToGrid: Boolean = true,
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
    const val MIN_SCALE = 0.60f
    const val MAX_SCALE = 1.80f
    const val SCALE_STEP = 0.05f
    const val MIN_OPACITY = 0.15f
    const val MAX_OPACITY = 1.0f

    fun snapPosition(
        rawXFraction: Float,
        rawYFraction: Float,
        widthPx: Float,
        heightPx: Float,
        stepPx: Float,
        enabled: Boolean
    ): Pair<Float, Float> {
        val clampedX = rawXFraction.coerceIn(0.04f, 0.96f)
        val clampedY = rawYFraction.coerceIn(0.06f, 0.94f)
        if (!enabled || widthPx <= 0f || heightPx <= 0f || stepPx <= 0f) {
            return clampedX to clampedY
        }
        val px = clampedX * widthPx
        val py = clampedY * heightPx
        val snappedPx = (px / stepPx).roundToInt() * stepPx
        val snappedPy = (py / stepPx).roundToInt() * stepPx
        return (snappedPx / widthPx).coerceIn(0.04f, 0.96f) to
            (snappedPy / heightPx).coerceIn(0.06f, 0.94f)
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
