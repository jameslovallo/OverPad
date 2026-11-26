package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.OverPadApplication
import com.example.OverlayRuntimeState
import com.example.data.OverPadUserSettings
import com.example.model.AppPresetBinding
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.model.InstalledAppInfo
import com.example.overlay.OverPadOverlayService
import com.example.shizuku.PhysicalControllerState
import com.example.shizuku.ShizukuStatus
import com.example.xinput.DriverCapabilityStatus
import com.example.xinput.XInputState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

data class BackupStatusMessage(
    val text: String,
    val isError: Boolean = false
)

class OverPadViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OverPadApplication
    private val repository = app.repository
    private val monitor = app.foregroundAppMonitor
    private val shizukuBridge = app.shizukuBridge
    val xInputEngine = app.xInputEngine

    val presets: StateFlow<List<ControllerPreset>> = repository.presetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultPresets.all)

    val bindings: StateFlow<List<AppPresetBinding>> = repository.bindingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<OverPadUserSettings> = repository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverPadUserSettings())

    val loadedUserSettings: StateFlow<OverPadUserSettings?> = repository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val overlayState: StateFlow<OverlayRuntimeState> = app.overlayState
    val isAppInMultiWindow: StateFlow<Boolean> = app.isAppInMultiWindow
    val xInputState: StateFlow<XInputState> = app.xInputEngine.state
    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuBridge.status
    val physicalControllerState: StateFlow<PhysicalControllerState> = app.physicalControllerMonitor.state
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

    private val _backupStatus = MutableStateFlow<BackupStatusMessage?>(null)
    val backupStatus: StateFlow<BackupStatusMessage?> = _backupStatus.asStateFlow()

    init {
        refreshPermissions(application)
        loadInstalledApps()
    }

    fun refreshPermissions(context: Context) {
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
        _hasUsagePermission.value = monitor.hasUsageAccessPermission()
        shizukuBridge.refreshStatusAndAutoConnect()
        app.refreshPhysicalControllerState()
        app.syncQuickActionNotification()
    }

    fun requestShizukuPermission() {
        shizukuBridge.requestShizukuPermission()
    }

    fun reconnectShizuku() {
        shizukuBridge.reprobeVirtualGamepad()
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
            app.syncQuickActionNotification()
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

    fun createAndSelectPreset(source: ControllerPreset, newName: String) {
        viewModelScope.launch {
            val created = repository.createCustomPresetFrom(source, newName)
            _editingPresetId.value = created.id
            app.updateOverlayState { it.copy(activePreset = created, autoMatchedByApp = false) }
            repository.setDefaultPresetId(created.id)
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

    fun setFpsAntiDeadzone(boost: Float) {
        viewModelScope.launch {
            repository.setFpsAntiDeadzone(boost)
        }
    }

    fun setNotificationQuickActionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationQuickActionEnabled(enabled)
            app.syncQuickActionNotification()
        }
    }

    fun startOverlay(context: Context) {
        if (!app.overlayState.value.isRunning && app.refreshPhysicalControllerState().isConnected) {
            app.syncQuickActionNotification()
            return
        }
        if (Settings.canDrawOverlays(context) || app.isAppInMultiWindow.value) {
            shizukuBridge.refreshStatusAndAutoConnect()
            OverPadOverlayService.start(context)
        }
    }

    fun stopOverlay(context: Context) {
        shizukuBridge.setOverlayActive(false)
        OverPadOverlayService.stop(context)
    }

    fun openOverlaySettings() {
        xInputEngine.resetAll()
        app.updateOverlayState { it.copy(isOverlaySettingsOpen = true) }
    }

    fun closeOverlaySettings() {
        app.updateOverlayState { it.copy(isOverlaySettingsOpen = false) }
    }

    fun enterOverlayEditMode() {
        app.updateOverlayState { it.copy(isOverlaySettingsOpen = false, isOverlayEditMode = true) }
    }

    fun exitOverlayEditMode() {
        app.updateOverlayState { it.copy(isOverlayEditMode = false) }
    }

    fun bindOverlayAppToPreset(packageName: String, appLabel: String, presetId: String) {
        viewModelScope.launch {
            repository.bindAppToPreset(packageName, appLabel, presetId)
            app.updateOverlayState { it.copy(autoMatchedByApp = true) }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun exportProfilesToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val (json, presetCount) = withContext(Dispatchers.IO) {
                    val count = repository.getAllPresetsSnapshot().size
                    val encoded = repository.exportProfilesJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(encoded.toByteArray(StandardCharsets.UTF_8))
                        stream.flush()
                    } ?: throw IllegalStateException("Could not open file for writing.")
                    encoded to count
                }
                if (json.isNotEmpty()) {
                    _backupStatus.value = BackupStatusMessage("Exported $presetCount profiles")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backupStatus.value = BackupStatusMessage(
                    e.message ?: "Export failed",
                    isError = true
                )
            }
        }
    }

    fun exportSinglePresetToUri(context: Context, presetId: String, uri: Uri) {
        viewModelScope.launch {
            try {
                val presetName = withContext(Dispatchers.IO) {
                    val snapshot = repository.getAllPresetsSnapshot().firstOrNull { it.id == presetId }
                    val encoded = repository.exportSinglePresetJson(presetId)
                        ?: throw IllegalStateException("Profile not found.")
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(encoded.toByteArray(StandardCharsets.UTF_8))
                        stream.flush()
                    } ?: throw IllegalStateException("Could not open file for writing.")
                    snapshot?.name ?: "Profile"
                }
                _backupStatus.value = BackupStatusMessage("Exported $presetName")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backupStatus.value = BackupStatusMessage(
                    e.message ?: "Export failed",
                    isError = true
                )
            }
        }
    }

    fun importProfilesFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val (result, activePreset) = withContext(Dispatchers.IO) {
                    val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(StandardCharsets.UTF_8).readText()
                    } ?: throw IllegalStateException("Could not read selected file.")

                    val importResult = repository.importProfilesJson(json)
                    val allPresets = repository.getAllPresetsSnapshot()
                    val settings = repository.userSettingsFlow.first()
                    val resolvedActive = allPresets.firstOrNull { it.id == importResult.restoredDefaultPresetId }
                        ?: allPresets.firstOrNull { it.id == settings.defaultPresetId }
                        ?: allPresets.firstOrNull()
                    importResult to resolvedActive
                }

                if (activePreset != null) {
                    app.updateOverlayState { it.copy(activePreset = activePreset, autoMatchedByApp = false) }
                }
                app.syncQuickActionNotification()

                val summary = buildString {
                    append("Imported ${result.importedPresetsCount} ")
                    append(if (result.importedPresetsCount == 1) "profile" else "profiles")
                    if (result.importedBindingsCount > 0) {
                        append(", ${result.importedBindingsCount} app ")
                        append(if (result.importedBindingsCount == 1) "binding" else "bindings")
                    }
                }
                _backupStatus.value = BackupStatusMessage(summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backupStatus.value = BackupStatusMessage(
                    e.message ?: "Import failed",
                    isError = true
                )
            }
        }
    }
}
