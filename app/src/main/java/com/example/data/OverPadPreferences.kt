package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DefaultPresets
import com.example.xinput.TouchMouseLookFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.overPadDataStore: DataStore<Preferences> by preferencesDataStore(name = "operpad_prefs")

data class OverPadUserSettings(
    val defaultPresetId: String = DefaultPresets.ID_FPS_PRO,
    val autoSwitchByAppUsage: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val udpBridgeEnabled: Boolean = false,
    val udpHost: String = "127.0.0.1",
    val udpPort: Int = 26760,
    val hasCompletedOnboarding: Boolean = false,
    val fpsAntiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    val notificationQuickActionEnabled: Boolean = true
)

class OverPadPreferences(private val context: Context) {
    private object Keys {
        val DEFAULT_PRESET_ID = stringPreferencesKey("default_preset_id")
        val AUTO_SWITCH_USAGE = booleanPreferencesKey("auto_switch_usage")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val UDP_BRIDGE_ENABLED = booleanPreferencesKey("udp_bridge_enabled")
        val UDP_HOST = stringPreferencesKey("udp_host")
        val UDP_PORT = intPreferencesKey("udp_port")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FPS_ANTI_DEADZONE = floatPreferencesKey("fps_anti_deadzone")
        val NOTIFICATION_QUICK_ACTION = booleanPreferencesKey("notification_quick_action")
    }

    val settingsFlow: Flow<OverPadUserSettings> = context.overPadDataStore.data.map { prefs ->
        OverPadUserSettings(
            defaultPresetId = prefs[Keys.DEFAULT_PRESET_ID] ?: DefaultPresets.ID_FPS_PRO,
            autoSwitchByAppUsage = prefs[Keys.AUTO_SWITCH_USAGE] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            udpBridgeEnabled = prefs[Keys.UDP_BRIDGE_ENABLED] ?: false,
            udpHost = prefs[Keys.UDP_HOST] ?: "127.0.0.1",
            udpPort = prefs[Keys.UDP_PORT] ?: 26760,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            fpsAntiDeadzone = (prefs[Keys.FPS_ANTI_DEADZONE] ?: TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE)
                .coerceIn(TouchMouseLookFilter.MIN_ANTI_DEADZONE, TouchMouseLookFilter.MAX_ANTI_DEADZONE),
            notificationQuickActionEnabled = prefs[Keys.NOTIFICATION_QUICK_ACTION] ?: true
        )
    }

    suspend fun setDefaultPresetId(id: String) {
        context.overPadDataStore.edit { it[Keys.DEFAULT_PRESET_ID] = id }
    }

    suspend fun setAutoSwitchByAppUsage(enabled: Boolean) {
        context.overPadDataStore.edit { it[Keys.AUTO_SWITCH_USAGE] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.overPadDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setUdpBridge(enabled: Boolean, host: String, port: Int) {
        context.overPadDataStore.edit {
            it[Keys.UDP_BRIDGE_ENABLED] = enabled
            it[Keys.UDP_HOST] = host.ifBlank { "127.0.0.1" }
            it[Keys.UDP_PORT] = port.coerceIn(1024, 65535)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.overPadDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setFpsAntiDeadzone(boost: Float) {
        context.overPadDataStore.edit {
            it[Keys.FPS_ANTI_DEADZONE] = boost.coerceIn(
                TouchMouseLookFilter.MIN_ANTI_DEADZONE,
                TouchMouseLookFilter.MAX_ANTI_DEADZONE
            )
        }
    }

    suspend fun setNotificationQuickActionEnabled(enabled: Boolean) {
        context.overPadDataStore.edit { it[Keys.NOTIFICATION_QUICK_ACTION] = enabled }
    }
}
