package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.model.ControlPlacement
import com.example.model.ControllerPreset
import com.example.model.PresetCategory
import com.example.xinput.TouchMouseLookFilter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class ControlPlacementListWrapper(
    val items: List<ControlPlacement>
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isBuiltIn: Boolean,
    val opacity: Float,
    val gridStepDp: Int,
    val snapToGrid: Boolean,
    val leftMoveZoneEnabled: Boolean = false,
    val fpsRightZoneEnabled: Boolean,
    val fpsLookSensitivity: Float,
    val triggerDragAimEnabled: Boolean,
    val triggerDragMode: String,
    val triggerDragRadiusDp: Float,
    val invertRightY: Boolean,
    val controlsJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_bindings")
data class AppBindingEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val presetId: String,
    val updatedAt: Long = System.currentTimeMillis()
)

object PresetJsonMapper {
    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(ControlPlacementListWrapper::class.java)

    fun toJson(controls: List<ControlPlacement>): String {
        return adapter.toJson(ControlPlacementListWrapper(controls))
    }

    fun fromJson(json: String): List<ControlPlacement> {
        return runCatching {
            adapter.fromJson(json)?.items ?: emptyList()
        }.getOrDefault(emptyList())
    }
}

fun PresetEntity.toDomain(): ControllerPreset {
    val parsedCategory = runCatching { PresetCategory.valueOf(category) }
        .getOrDefault(PresetCategory.CUSTOM)
    return ControllerPreset(
        id = id,
        name = name,
        description = description,
        category = parsedCategory,
        isBuiltIn = isBuiltIn,
        opacity = opacity,
        gridStepDp = gridStepDp,
        snapToGrid = snapToGrid,
        leftMoveZoneEnabled = leftMoveZoneEnabled,
        fpsRightZoneEnabled = fpsRightZoneEnabled,
        fpsLookSensitivity = fpsLookSensitivity.coerceIn(
            TouchMouseLookFilter.MIN_SENSITIVITY,
            TouchMouseLookFilter.MAX_SENSITIVITY
        ),
        triggerDragAimEnabled = triggerDragAimEnabled,
        invertRightY = invertRightY,
        controls = PresetJsonMapper.fromJson(controlsJson)
    )
}

fun ControllerPreset.toEntity(): PresetEntity = PresetEntity(
    id = id,
    name = name,
    description = description,
    category = category.name,
    isBuiltIn = isBuiltIn,
    opacity = opacity,
    gridStepDp = gridStepDp,
    snapToGrid = snapToGrid,
    leftMoveZoneEnabled = leftMoveZoneEnabled,
    fpsRightZoneEnabled = fpsRightZoneEnabled,
    fpsLookSensitivity = fpsLookSensitivity.coerceIn(
        TouchMouseLookFilter.MIN_SENSITIVITY,
        TouchMouseLookFilter.MAX_SENSITIVITY
    ),
    triggerDragAimEnabled = triggerDragAimEnabled,
    triggerDragMode = "SWIPE_LOOK",
    triggerDragRadiusDp = 56f,
    invertRightY = invertRightY,
    controlsJson = PresetJsonMapper.toJson(controls),
    updatedAt = System.currentTimeMillis()
)

class OperPadTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<ControlPlacement>): String = PresetJsonMapper.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<ControlPlacement> = PresetJsonMapper.fromJson(value)
}
