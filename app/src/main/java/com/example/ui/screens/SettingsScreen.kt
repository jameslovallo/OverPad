package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.data.OperPadUserSettings
import com.example.model.AppPresetBinding
import com.example.model.ControllerPreset
import com.example.model.InstalledAppInfo
import com.example.shizuku.ShizukuConnectionStage
import com.example.shizuku.ShizukuStatus
import com.example.xinput.DriverCapabilityStatus

@Composable
fun SettingsScreen(
    presets: List<ControllerPreset>,
    bindings: List<AppPresetBinding>,
    installedApps: List<InstalledAppInfo>,
    isLoadingApps: Boolean,
    userSettings: OperPadUserSettings,
    hasOverlayPermission: Boolean,
    hasUsagePermission: Boolean,
    shizukuStatus: ShizukuStatus,
    driverCapability: DriverCapabilityStatus,
    onOpenOnboarding: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onReconnectShizuku: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onDownloadShizuku: () -> Unit,
    onToggleAutoSwitch: (Boolean) -> Unit,
    onBindApp: (packageName: String, label: String, presetId: String) -> Unit,
    onUnbindApp: (packageName: String) -> Unit,
    onRefreshApps: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetUdpBridge: (Boolean, String, Int) -> Unit,
    onOpenSandbox: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PermissionChecker.PERMISSION_GRANTED
            } else true
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    var udpHostText by remember(userSettings.udpHost) { mutableStateOf(userSettings.udpHost) }
    var udpPortText by remember(userSettings.udpPort) { mutableStateOf(userSettings.udpPort.toString()) }

    var showAddAppPresetDialog by remember { mutableStateOf(false) }
    var editingBinding by remember { mutableStateOf<AppPresetBinding?>(null) }

    val missingAnyPermission = !hasOverlayPermission || !hasUsagePermission || !hasNotificationPermission
    val presetsMap = remember(presets) { presets.associateBy { it.id } }

    fun openOverlayPermissionSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    }

    fun openUsageAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    if (showAddAppPresetDialog) {
        AddAppPresetDialog(
            installedApps = installedApps,
            isLoadingApps = isLoadingApps,
            presets = presets,
            existingBoundPackages = bindings.map { it.packageName }.toSet(),
            defaultPresetId = userSettings.defaultPresetId,
            onRefreshApps = onRefreshApps,
            onConfirm = { app, preset ->
                onBindApp(app.packageName, app.label, preset.id)
                showAddAppPresetDialog = false
            },
            onDismiss = { showAddAppPresetDialog = false }
        )
    }

    editingBinding?.let { binding ->
        AlertDialog(
            onDismissRequest = { editingBinding = null },
            title = { Text(binding.appLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onBindApp(binding.packageName, binding.appLabel, preset.id)
                                    editingBinding = null
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = binding.presetId == preset.id,
                                onClick = {
                                    onBindApp(binding.packageName, binding.appLabel, preset.id)
                                    editingBinding = null
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(preset.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingBinding = null }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. System Permissions (shown ONLY if not already set)
        if (missingAnyPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("System Permissions", style = MaterialTheme.typography.titleMedium)
                        }

                        if (!hasOverlayPermission) {
                            MissingPermissionRow(
                                icon = Icons.Default.Layers,
                                title = "Display Over Apps",
                                actionLabel = "Grant",
                                tag = "grant_overlay_button",
                                onClick = ::openOverlayPermissionSettings
                            )
                        }
                        if (!hasUsagePermission) {
                            MissingPermissionRow(
                                icon = Icons.Default.QueryStats,
                                title = "Usage Access",
                                actionLabel = "Grant",
                                tag = "grant_usage_button",
                                onClick = ::openUsageAccessSettings
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            MissingPermissionRow(
                                icon = Icons.Default.Notifications,
                                title = "Notifications",
                                actionLabel = "Allow",
                                tag = "grant_notifications_button",
                                onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Shizuku Gamepad Bridge
        item {
            SettingsShizukuBridgeCard(
                shizukuStatus = shizukuStatus,
                onOpenOnboarding = onOpenOnboarding,
                onRequestShizukuPermission = onRequestShizukuPermission,
                onReconnectShizuku = onReconnectShizuku,
                onOpenShizukuApp = onOpenShizukuApp,
                onDownloadShizuku = onDownloadShizuku
            )
        }

        // 3. App Presets (empty list by default, "+" button in corner across from title)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("App Presets", style = MaterialTheme.typography.titleMedium)
                        }
                        FilledTonalIconButton(
                            onClick = {
                                if (installedApps.isEmpty()) onRefreshApps()
                                showAddAppPresetDialog = true
                            },
                            modifier = Modifier.testTag("add_app_preset_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add app preset")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto-switch by app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = userSettings.autoSwitchByAppUsage,
                            onCheckedChange = onToggleAutoSwitch
                        )
                    }

                    if (bindings.isEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Text(
                                text = "No app presets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bindings.forEach { binding ->
                                val presetName = presetsMap[binding.presetId]?.name ?: "Default"
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { editingBinding = binding }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = binding.appLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = presetName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onUnbindApp(binding.packageName) }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove ${binding.appLabel}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. XInput Hardware Profile
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("XInput Hardware Profile", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = onOpenSandbox,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_sandbox_button")
                        ) {
                            Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Test", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (driverCapability == DriverCapabilityStatus.UINPUT_WRITABLE) {
                                        "/dev/uinput Writable"
                                    } else {
                                        "Xbox Wireless / 360 XInput"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "VID 0x045E • PID 0x028E / 0x0B20",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Haptic Feedback", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = userSettings.hapticFeedbackEnabled,
                            onCheckedChange = onSetHaptics
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UDP XInput Stream", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = userSettings.udpBridgeEnabled,
                            onCheckedChange = { enabled ->
                                onSetUdpBridge(
                                    enabled,
                                    udpHostText,
                                    udpPortText.toIntOrNull() ?: 26760
                                )
                            }
                        )
                    }

                    if (userSettings.udpBridgeEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = udpHostText,
                                onValueChange = {
                                    udpHostText = it
                                    onSetUdpBridge(true, it, udpPortText.toIntOrNull() ?: 26760)
                                },
                                label = { Text("Host") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = udpPortText,
                                onValueChange = {
                                    udpPortText = it
                                    onSetUdpBridge(true, udpHostText, it.toIntOrNull() ?: 26760)
                                },
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingPermissionRow(
    icon: ImageVector,
    title: String,
    actionLabel: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onClick,
                modifier = Modifier.testTag(tag),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SettingsShizukuBridgeCard(
    shizukuStatus: ShizukuStatus,
    onOpenOnboarding: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onReconnectShizuku: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onDownloadShizuku: () -> Unit
) {
    val isConnected = shizukuStatus.stage == ShizukuConnectionStage.CONNECTED
    val badgeColor = when {
        shizukuStatus.stage != ShizukuConnectionStage.CONNECTED -> when (shizukuStatus.stage) {
            ShizukuConnectionStage.CONNECTING, ShizukuConnectionStage.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.tertiary
        }
        shizukuStatus.isHardwareSpoofed -> MaterialTheme.colorScheme.secondary
        shizukuStatus.backendMode == "STANDBY" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shizuku Gamepad Bridge", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = when (shizukuStatus.stage) {
                            ShizukuConnectionStage.CONNECTED -> shizukuStatus.modeLabel
                            ShizukuConnectionStage.CONNECTING -> "Binding service..."
                            ShizukuConnectionStage.PERMISSION_REQUIRED -> "Authorization required"
                            ShizukuConnectionStage.SERVICE_STOPPED -> "Service stopped"
                            ShizukuConnectionStage.NOT_INSTALLED -> "Shizuku not installed"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.40f))
                ) {
                    Text(
                        text = when (shizukuStatus.stage) {
                            ShizukuConnectionStage.CONNECTED -> when {
                                shizukuStatus.isHardwareSpoofed -> "PAD LIVE"
                                shizukuStatus.backendMode == "STANDBY" -> "STANDBY"
                                else -> "ACTIVE"
                            }
                            ShizukuConnectionStage.CONNECTING -> "BINDING"
                            ShizukuConnectionStage.PERMISSION_REQUIRED -> "AUTH"
                            ShizukuConnectionStage.SERVICE_STOPPED -> "STOPPED"
                            ShizukuConnectionStage.NOT_INSTALLED -> "SETUP"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            shizukuStatus.lastError?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (shizukuStatus.stage) {
                    ShizukuConnectionStage.NOT_INSTALLED -> {
                        Button(
                            onClick = onDownloadShizuku,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hub_get_shizuku_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Get Shizuku")
                        }
                    }

                    ShizukuConnectionStage.SERVICE_STOPPED -> {
                        Button(
                            onClick = onOpenShizukuApp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hub_open_shizuku_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start Shizuku")
                        }
                    }

                    ShizukuConnectionStage.PERMISSION_REQUIRED -> {
                        Button(
                            onClick = onRequestShizukuPermission,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hub_authorize_shizuku_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Authorize")
                        }
                    }

                    ShizukuConnectionStage.CONNECTING, ShizukuConnectionStage.CONNECTED -> {
                        OutlinedButton(
                            onClick = onReconnectShizuku,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hub_reconnect_shizuku_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when {
                                    shizukuStatus.isHardwareSpoofed -> "Re-verify"
                                    shizukuStatus.backendMode == "STANDBY" -> "Refresh"
                                    isConnected -> "Probe Pad"
                                    else -> "Reconnect"
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenOnboarding,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_shizuku_onboarding_button")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Setup")
                }
            }
        }
    }
}

@Composable
private fun AddAppPresetDialog(
    installedApps: List<InstalledAppInfo>,
    isLoadingApps: Boolean,
    presets: List<ControllerPreset>,
    existingBoundPackages: Set<String>,
    defaultPresetId: String,
    onRefreshApps: () -> Unit,
    onConfirm: (InstalledAppInfo, ControllerPreset) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var selectedPreset by remember(presets, defaultPresetId) {
        mutableStateOf(presets.firstOrNull { it.id == defaultPresetId } ?: presets.firstOrNull())
    }

    val filteredApps = remember(installedApps, searchQuery, existingBoundPackages) {
        val q = searchQuery.trim().lowercase()
        installedApps
            .filter { it.packageName !in existingBoundPackages }
            .filter {
                q.isEmpty() || it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
            .sortedBy { it.label.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add App Preset")
                IconButton(onClick = onRefreshApps) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh apps")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (presets.isNotEmpty()) {
                    Text("Preset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(presets, key = { it.id }) { preset ->
                            FilterChip(
                                selected = selectedPreset?.id == preset.id,
                                onClick = { selectedPreset = preset },
                                label = { Text(preset.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text("App", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isLoadingApps) {
                    Text("Loading...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (filteredApps.isEmpty()) {
                    Text("No apps found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChosen = selectedApp?.packageName == app.packageName
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChosen) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedApp = app }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedApp != null && selectedPreset != null,
                onClick = {
                    val app = selectedApp
                    val preset = selectedPreset
                    if (app != null && preset != null) {
                        onConfirm(app, preset)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
