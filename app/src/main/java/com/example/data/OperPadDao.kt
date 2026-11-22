package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OperPadDao {
    @Query("SELECT * FROM presets ORDER BY isBuiltIn DESC, updatedAt DESC")
    fun observeAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets ORDER BY isBuiltIn DESC, updatedAt DESC")
    suspend fun getAllPresetsOnce(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPresets(presets: List<PresetEntity>)

    @Query("DELETE FROM presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomPreset(id: String)

    @Query("SELECT * FROM app_bindings ORDER BY appLabel ASC")
    fun observeAllBindings(): Flow<List<AppBindingEntity>>

    @Query("SELECT * FROM app_bindings WHERE packageName = :packageName LIMIT 1")
    suspend fun getBindingForPackage(packageName: String): AppBindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBinding(binding: AppBindingEntity)

    @Query("DELETE FROM app_bindings WHERE packageName = :packageName")
    suspend fun deleteBinding(packageName: String)

    @Query("DELETE FROM app_bindings WHERE presetId = :presetId")
    suspend fun deleteBindingsForPreset(presetId: String)
}
