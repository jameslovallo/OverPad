package com.example.data

import com.example.model.AppPresetBinding
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.model.PresetCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OperPadRepository(
    private val dao: OperPadDao,
    private val preferences: OperPadPreferences
) {
    val presetsFlow: Flow<List<ControllerPreset>> = dao.observeAllPresets().map { entities ->
        entities.map { it.toDomain() }
    }

    val bindingsFlow: Flow<List<AppPresetBinding>> = dao.observeAllBindings().map { list ->
        list.map { AppPresetBinding(it.packageName, it.appLabel, it.presetId) }
    }

    val userSettingsFlow: Flow<OperPadUserSettings> = preferences.settingsFlow

    suspend fun ensureDefaultPresetsSeeded() {
        val existing = dao.getAllPresetsOnce()
        val existingIds = existing.map { it.id }.toSet()
        val missingDefaults = DefaultPresets.all.filter { it.id !in existingIds }
        if (missingDefaults.isNotEmpty()) {
            dao.upsertPresets(missingDefaults.map { it.toEntity() })
        }
    }

    suspend fun savePreset(preset: ControllerPreset) {
        dao.upsertPreset(preset.toEntity())
    }

    suspend fun createCustomPresetFrom(source: ControllerPreset, newName: String): ControllerPreset {
        val custom = source.copy(
            id = "preset_custom_${UUID.randomUUID().toString().take(8)}",
            name = newName.trim().ifBlank { "${source.name} Copy" },
            category = PresetCategory.CUSTOM,
            isBuiltIn = false
        )
        dao.upsertPreset(custom.toEntity())
        return custom
    }

    suspend fun resetBuiltInPreset(presetId: String): ControllerPreset? {
        val canonical = DefaultPresets.byId(presetId) ?: return null
        dao.upsertPreset(canonical.toEntity())
        return canonical
    }

    suspend fun deleteCustomPreset(presetId: String) {
        dao.deleteBindingsForPreset(presetId)
        dao.deleteCustomPreset(presetId)
    }

    suspend fun bindAppToPreset(packageName: String, appLabel: String, presetId: String) {
        dao.upsertBinding(
            AppBindingEntity(
                packageName = packageName,
                appLabel = appLabel,
                presetId = presetId
            )
        )
    }

    suspend fun unbindApp(packageName: String) {
        dao.deleteBinding(packageName)
    }

    suspend fun setDefaultPresetId(presetId: String) {
        preferences.setDefaultPresetId(presetId)
    }

    suspend fun setAutoSwitchByAppUsage(enabled: Boolean) {
        preferences.setAutoSwitchByAppUsage(enabled)
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        preferences.setHapticFeedback(enabled)
    }

    suspend fun setUdpBridge(enabled: Boolean, host: String, port: Int) {
        preferences.setUdpBridge(enabled, host, port)
    }
}
