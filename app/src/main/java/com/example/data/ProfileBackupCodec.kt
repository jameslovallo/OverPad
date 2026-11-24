package com.example.data

import com.example.model.AppPresetBinding
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.xinput.TouchMouseLookFilter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class AppPresetBackupBinding(
    val packageName: String,
    val appLabel: String,
    val presetId: String
)

@JsonClass(generateAdapter = true)
data class OverPadProfileBackup(
    val formatVersion: Int = ProfileBackupCodec.CURRENT_VERSION,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val presets: List<ControllerPreset>,
    val bindings: List<AppPresetBackupBinding> = emptyList(),
    val defaultPresetId: String? = null
)

data class ProfileImportResult(
    val importedPresetsCount: Int,
    val importedBindingsCount: Int,
    val restoredDefaultPresetId: String?
)

object ProfileBackupCodec {
    const val CURRENT_VERSION = 1

    private val moshi: Moshi = Moshi.Builder().build()
    private val backupAdapter = moshi.adapter(OverPadProfileBackup::class.java).indent("  ")
    private val singlePresetAdapter = moshi.adapter(ControllerPreset::class.java).indent("  ")

    fun encodeBackup(
        presets: List<ControllerPreset>,
        bindings: List<AppPresetBinding> = emptyList(),
        defaultPresetId: String? = null,
        exportedAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        val payload = OverPadProfileBackup(
            formatVersion = CURRENT_VERSION,
            exportedAtEpochMs = exportedAtEpochMs,
            presets = presets.map(::normalizePreset),
            bindings = bindings.map {
                AppPresetBackupBinding(
                    packageName = it.packageName,
                    appLabel = it.appLabel,
                    presetId = it.presetId
                )
            },
            defaultPresetId = defaultPresetId
        )
        return backupAdapter.toJson(payload)
    }

    fun encodeSinglePreset(
        preset: ControllerPreset,
        exportedAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        val normalized = normalizePreset(preset)
        return encodeBackup(
            presets = listOf(normalized),
            bindings = emptyList(),
            defaultPresetId = normalized.id,
            exportedAtEpochMs = exportedAtEpochMs
        )
    }

    fun decodeBackup(json: String): OverPadProfileBackup {
        val trimmed = json.trim()
        require(trimmed.isNotEmpty()) { "Profile backup file is empty." }

        val parsedBackup = runCatching { backupAdapter.fromJson(trimmed) }.getOrNull()
        if (parsedBackup != null && parsedBackup.presets.isNotEmpty()) {
            return parsedBackup.copy(
                presets = parsedBackup.presets.map(::normalizePreset)
            )
        }

        val singlePreset = runCatching { singlePresetAdapter.fromJson(trimmed) }.getOrNull()
        if (singlePreset != null && singlePreset.name.isNotBlank() && singlePreset.controls.isNotEmpty()) {
            return OverPadProfileBackup(
                formatVersion = CURRENT_VERSION,
                presets = listOf(normalizePreset(singlePreset)),
                bindings = emptyList(),
                defaultPresetId = singlePreset.id
            )
        }

        throw IllegalArgumentException("File is not a valid OverPad profile backup.")
    }

    fun normalizePreset(preset: ControllerPreset): ControllerPreset {
        return preset.copy(
            name = preset.name.trim().ifBlank { "Imported Profile" },
            opacity = preset.opacity.coerceIn(GridSnapper.MIN_OPACITY, GridSnapper.MAX_OPACITY),
            gridRows = preset.gridRows.coerceIn(GridSnapper.MIN_GRID_ROWS, GridSnapper.MAX_GRID_ROWS),
            fpsLookSensitivity = preset.fpsLookSensitivity.coerceIn(
                TouchMouseLookFilter.MIN_SENSITIVITY,
                TouchMouseLookFilter.MAX_SENSITIVITY
            ),
            controls = preset.controls.map { placement ->
                placement.copy(
                    xFraction = placement.xFraction.coerceIn(0.03f, 0.97f),
                    yFraction = placement.yFraction.coerceIn(0.04f, 0.96f),
                    scale = placement.scale.coerceIn(GridSnapper.MIN_SCALE, GridSnapper.MAX_SCALE)
                )
            }
        )
    }
}
