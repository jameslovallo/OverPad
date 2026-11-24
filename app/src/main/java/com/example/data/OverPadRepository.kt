package com.example.data

import com.example.model.AppPresetBinding
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.model.PresetCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class OverPadRepository(
    private val dao: OverPadDao,
    private val preferences: OverPadPreferences
) {
    val presetsFlow: Flow<List<ControllerPreset>> = dao.observeAllPresets().map { entities ->
        entities.map { it.toDomain() }
    }

    val bindingsFlow: Flow<List<AppPresetBinding>> = dao.observeAllBindings().map { list ->
        list.map { AppPresetBinding(it.packageName, it.appLabel, it.presetId) }
    }

    val userSettingsFlow: Flow<OverPadUserSettings> = preferences.settingsFlow

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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.setOnboardingCompleted(completed)
    }

    suspend fun setFpsAntiDeadzone(boost: Float) {
        preferences.setFpsAntiDeadzone(boost)
    }

    suspend fun setNotificationQuickActionEnabled(enabled: Boolean) {
        preferences.setNotificationQuickActionEnabled(enabled)
    }

    suspend fun getAllPresetsSnapshot(): List<ControllerPreset> {
        return dao.getAllPresetsOnce().map { it.toDomain() }
    }

    suspend fun exportProfilesJson(): String {
        val presets = dao.getAllPresetsOnce().map { it.toDomain() }
        val bindings = dao.getAllBindingsOnce().map {
            AppPresetBinding(it.packageName, it.appLabel, it.presetId)
        }
        val settings = preferences.settingsFlow.first()
        return ProfileBackupCodec.encodeBackup(
            presets = presets,
            bindings = bindings,
            defaultPresetId = settings.defaultPresetId
        )
    }

    suspend fun exportSinglePresetJson(presetId: String): String? {
        val preset = dao.getPresetById(presetId)?.toDomain() ?: return null
        return ProfileBackupCodec.encodeSinglePreset(preset)
    }

    suspend fun importProfilesJson(json: String): ProfileImportResult {
        val backup = ProfileBackupCodec.decodeBackup(json)
        val sanitizedPresets = backup.presets.map { preset ->
            val safeId = preset.id.trim().ifBlank { "preset_custom_${UUID.randomUUID().toString().take(8)}" }
            val isCanonicalBuiltIn = DefaultPresets.byId(safeId) != null
            preset.copy(
                id = safeId,
                isBuiltIn = isCanonicalBuiltIn
            )
        }
        dao.upsertPresets(sanitizedPresets.map { it.toEntity() })

        val validPresetIds = (dao.getAllPresetsOnce().map { it.id } + sanitizedPresets.map { it.id }).toSet()
        val validBindings = backup.bindings.filter {
            it.packageName.isNotBlank() && it.presetId in validPresetIds
        }
        validBindings.forEach { binding ->
            dao.upsertBinding(
                AppBindingEntity(
                    packageName = binding.packageName,
                    appLabel = binding.appLabel.ifBlank { binding.packageName },
                    presetId = binding.presetId
                )
            )
        }

        val restoredDefaultId = backup.defaultPresetId?.takeIf { it in validPresetIds }
            ?: sanitizedPresets.firstOrNull()?.id
        if (restoredDefaultId != null) {
            preferences.setDefaultPresetId(restoredDefaultId)
        }

        return ProfileImportResult(
            importedPresetsCount = sanitizedPresets.size,
            importedBindingsCount = validBindings.size,
            restoredDefaultPresetId = restoredDefaultId
        )
    }
}
