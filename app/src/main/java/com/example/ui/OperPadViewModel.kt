package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.OperPadApplication
import com.example.OverlayRuntimeState
import com.example.data.OperPadUserSettings
import com.example.model.AppPresetBinding
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.model.InstalledAppInfo
import com.example.overlay.OperPadOverlayService
import com.example.shizuku.ShizukuStatus
import com.example.xinput.DriverCapabilityStatus
import com.example.xinput.XInputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OperPadViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OperPadApplication
    private val repository = app.repository
    private val monitor = app.foregroundAppMonitor
    private val shizukuBridge = app.shizukuBridge
    val xInputEngine = app.xInputEngine

    val presets: StateFlow<List<ControllerPreset>> = repository.presetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultPresets.all)

    val bindings: StateFlow<List<AppPresetBinding>> = repository.bindingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<OperPadUserSettings> = repository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OperPadUserSettings())

    val loadedUserSettings: StateFlow<OperPadUserSettings?> = repository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val overlayState: StateFlow<OverlayRuntimeState> = app.overlayState
    val xInputState: StateFlow<XInputState> = app.xInputEngine.state
    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuBridge.status
    val driverCapability: DriverCapabilityStatus = app.xInputEngine.driverCapability

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _editingPresetId = MutableStateFlow(DefaultPresets.ID_FPS_PRO)
    val editingPresetId: StateFlow<String> = _editingPresetId.asStateFlow()

    private val _selectedControl = MutableStateFlow(ControlType.TRIGGER_RT)
    val selectedControl: StateFlow<ControlType> = _selectedControl.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init {
        refreshPermissions(application)
        loadInstalledApps()
    }

    fun refreshPermissions(context: Context) {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
        _hasUsagePermission.value = monitor.hasUsageAccessPermission()
        shizukuBridge.refreshStatusAndAutoConnect()
    }

    fun requestShizukuPermission() {
        shizukuBridge.requestShizukuPermission()
    }

    fun reconnectShizuku() {
        shizukuBridge.refreshStatusAndAutoConnect()
        shizukuBridge.bindInjectorService()
    }

    fun openShizukuApp(context: Context) {
        if (!shizukuBridge.openShizukuApp(context)) {
            shizukuBridge.openShizukuDownloadPage(context)
        }
    }

    fun openShizukuDownload(context: Context) {
        shizukuBridge.openShizukuDownloadPage(context)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            repository.setOnboardingCompleted(completed)
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = monitor.loadLaunchableApps()
            _isLoadingApps.value = false
        }
    }

    fun selectActivePreset(preset: ControllerPreset) {
        app.updateOverlayState { it.copy(activePreset = preset, autoMatchedByApp = false) }
        viewModelScope.launch {
            repository.setDefaultPresetId(preset.id)
        }
    }

    fun selectPresetForEditing(presetId: String) {
        _editingPresetId.value = presetId
    }

    fun selectControl(type: ControlType) {
        _selectedControl.value = type
    }

    fun updatePreset(updated: ControllerPreset) {
        if (app.overlayState.value.activePreset.id == updated.id) {
            app.updateOverlayState { it.copy(activePreset = updated) }
        }
        viewModelScope.launch {
            repository.savePreset(updated)
        }
    }

    fun duplicatePreset(source: ControllerPreset, newName: String) {
        viewModelScope.launch {
            val created = repository.createCustomPresetFrom(source, newName)
            _editingPresetId.value = created.id
        }
    }

    fun resetBuiltInPreset(presetId: String) {
        viewModelScope.launch {
            val reset = repository.resetBuiltInPreset(presetId) ?: return@launch
            if (app.overlayState.value.activePreset.id == reset.id) {
                app.updateOverlayState { it.copy(activePreset = reset) }
            }
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
            if (_editingPresetId.value == presetId) {
                _editingPresetId.value = DefaultPresets.ID_FPS_PRO
            }
            if (app.overlayState.value.activePreset.id == presetId) {
                val fallback = presets.value.firstOrNull { it.id != presetId } ?: DefaultPresets.fpsPro()
                app.updateOverlayState { it.copy(activePreset = fallback) }
            }
        }
    }

    fun bindAppToPreset(packageName: String, appLabel: String, presetId: String) {
        viewModelScope.launch {
            repository.bindAppToPreset(packageName, appLabel, presetId)
        }
    }

    fun unbindApp(packageName: String) {
        viewModelScope.launch {
            repository.unbindApp(packageName)
        }
    }

    fun setAutoSwitchByAppUsage(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSwitchByAppUsage(enabled)
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticFeedback(enabled)
        }
    }

    fun setUdpBridge(enabled: Boolean, host: String, port: Int) {
        xInputEngine.configureUdpRelay(enabled, host, port)
        viewModelScope.launch {
            repository.setUdpBridge(enabled, host, port)
        }
    }

    fun startOverlay(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            shizukuBridge.refreshStatusAndAutoConnect()
            OperPadOverlayService.start(context)
        }
    }

    fun stopOverlay(context: Context) {
        OperPadOverlayService.stop(context)
    }
}
