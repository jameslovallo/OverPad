package com.example

import android.app.Application
import com.example.data.OverPadDatabase
import com.example.data.OverPadPreferences
import com.example.data.OverPadRepository
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.overlay.OverlayNotificationHelper
import com.example.shizuku.ShizukuBridgeManager
import com.example.usage.ForegroundAppMonitor
import com.example.xinput.XInputControllerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class OverlayRuntimeState(
    val isRunning: Boolean = false,
    val activePreset: ControllerPreset = DefaultPresets.fpsPro(),
    val detectedPackageName: String? = null,
    val detectedAppLabel: String? = null,
    val autoMatchedByApp: Boolean = false,
    val isOverlaySettingsOpen: Boolean = false,
    val isOverlayEditMode: Boolean = false
)

class OverPadApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: OverPadDatabase
        private set
    lateinit var preferences: OverPadPreferences
        private set
    lateinit var repository: OverPadRepository
        private set
    lateinit var foregroundAppMonitor: ForegroundAppMonitor
        private set
    lateinit var xInputEngine: XInputControllerEngine
        private set
    lateinit var shizukuBridge: ShizukuBridgeManager
        private set

    private val _overlayState = MutableStateFlow(OverlayRuntimeState())
    val overlayState: StateFlow<OverlayRuntimeState> = _overlayState.asStateFlow()

    private val _isAppInMultiWindow = MutableStateFlow(false)
    val isAppInMultiWindow: StateFlow<Boolean> = _isAppInMultiWindow.asStateFlow()

    fun setAppInMultiWindow(inMultiWindow: Boolean) {
        _isAppInMultiWindow.value = inMultiWindow
    }

    override fun onCreate() {
        super.onCreate()
        database = OverPadDatabase.getInstance(this)
        preferences = OverPadPreferences(this)
        repository = OverPadRepository(database.overPadDao(), preferences)
        foregroundAppMonitor = ForegroundAppMonitor(this)
        xInputEngine = XInputControllerEngine()
        shizukuBridge = ShizukuBridgeManager(this).apply {
            initialize()
        }
        xInputEngine.onStateUpdated = { state ->
            if (_overlayState.value.isRunning) {
                shizukuBridge.pushXInputState(state)
            }
        }

        applicationScope.launch {
            repository.ensureDefaultPresetsSeeded()
            val settings = repository.userSettingsFlow.first()
            xInputEngine.configureUdpRelay(
                enabled = settings.udpBridgeEnabled,
                host = settings.udpHost,
                port = settings.udpPort
            )
            val presets = repository.presetsFlow.first()
            val initial = presets.firstOrNull { it.id == settings.defaultPresetId }
                ?: presets.firstOrNull()
                ?: DefaultPresets.fpsPro()
            updateOverlayState { it.copy(activePreset = initial) }
            OverlayNotificationHelper.syncStandbyNotification(
                context = this@OverPadApplication,
                isOverlayRunning = _overlayState.value.isRunning,
                presetName = initial.name,
                quickActionEnabled = settings.notificationQuickActionEnabled
            )
            OverlayNotificationHelper.requestTileUpdate(this@OverPadApplication)
        }
    }

    fun syncQuickActionNotification() {
        applicationScope.launch {
            val settings = repository.userSettingsFlow.first()
            val runtime = _overlayState.value
            if (runtime.isRunning) {
                OverlayNotificationHelper.updateActiveNotificationIfPossible(
                    context = this@OverPadApplication,
                    presetName = runtime.activePreset.name
                )
            } else {
                OverlayNotificationHelper.syncStandbyNotification(
                    context = this@OverPadApplication,
                    isOverlayRunning = false,
                    presetName = runtime.activePreset.name,
                    quickActionEnabled = settings.notificationQuickActionEnabled
                )
            }
            OverlayNotificationHelper.requestTileUpdate(this@OverPadApplication)
        }
    }

    fun updateOverlayState(transform: (OverlayRuntimeState) -> OverlayRuntimeState) {
        _overlayState.value = transform(_overlayState.value)
    }
}
