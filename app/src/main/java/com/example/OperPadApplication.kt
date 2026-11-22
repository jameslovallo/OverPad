package com.example

import android.app.Application
import com.example.data.OperPadDatabase
import com.example.data.OperPadPreferences
import com.example.data.OperPadRepository
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
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

class OperPadApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: OperPadDatabase
        private set
    lateinit var preferences: OperPadPreferences
        private set
    lateinit var repository: OperPadRepository
        private set
    lateinit var foregroundAppMonitor: ForegroundAppMonitor
        private set
    lateinit var xInputEngine: XInputControllerEngine
        private set
    lateinit var shizukuBridge: ShizukuBridgeManager
        private set

    private val _overlayState = MutableStateFlow(OverlayRuntimeState())
    val overlayState: StateFlow<OverlayRuntimeState> = _overlayState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        database = OperPadDatabase.getInstance(this)
        preferences = OperPadPreferences(this)
        repository = OperPadRepository(database.operPadDao(), preferences)
        foregroundAppMonitor = ForegroundAppMonitor(this)
        xInputEngine = XInputControllerEngine()
        shizukuBridge = ShizukuBridgeManager(this).apply {
            initialize()
        }
        xInputEngine.onStateUpdated = { state ->
            shizukuBridge.pushXInputState(state)
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
        }
    }

    fun updateOverlayState(transform: (OverlayRuntimeState) -> OverlayRuntimeState) {
        _overlayState.value = transform(_overlayState.value)
    }
}
