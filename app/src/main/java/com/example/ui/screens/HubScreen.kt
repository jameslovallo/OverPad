package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.OverlayRuntimeState
import com.example.R
import com.example.data.OperPadUserSettings
import com.example.model.ControllerPreset
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TriggerAmber
import com.example.xinput.DriverCapabilityStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HubScreen(
    overlayState: OverlayRuntimeState,
    presets: List<ControllerPreset>,
    userSettings: OperPadUserSettings,
    hasOverlayPermission: Boolean,
    hasUsagePermission: Boolean,
    driverCapability: DriverCapabilityStatus,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onSelectPreset: (ControllerPreset) -> Unit,
    onEditPreset: (ControllerPreset) -> Unit,
    onOpenSandbox: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetUdpBridge: (Boolean, String, Int) -> Unit
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

    fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    fun openUsageAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_operpad_hero_1795371252898),
                        contentDescription = "OperPad tactical controller backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.35f),
                                        ObsidianBg.copy(alpha = 0.88f),
                                        SlateSurface
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OPERPAD",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Floating XInput Touch Controller",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (overlayState.isRunning) {
                                    TacticalEmerald.copy(alpha = 0.20f)
                                } else {
                                    SlateSurfaceElevated
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (overlayState.isRunning) TacticalEmerald else HairlineBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (overlayState.isRunning) TacticalEmerald else TextSecondary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (overlayState.isRunning) "OVERLAY LIVE" else "STANDBY",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (overlayState.isRunning) TacticalEmerald else TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!hasOverlayPermission) {
                                        openOverlayPermissionSettings()
                                    } else if (overlayState.isRunning) {
                                        onStopOverlay()
                                    } else {
                                        onStartOverlay()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("toggle_overlay_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (overlayState.isRunning) CrimsonAlert else ElectricCyan,
                                    contentColor = if (overlayState.isRunning) Color.White else ObsidianBg
                                )
                            ) {
                                Icon(
                                    imageVector = if (overlayState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !hasOverlayPermission -> "Enable Overlay"
                                        overlayState.isRunning -> "Stop Overlay"
                                        else -> "Launch Overlay"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenSandbox,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("open_sandbox_button")
                            ) {
                                Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Test Pad")
                            }
                        }
                    }
                }
            }
        }

        // Permission Readiness Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ElectricCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("SYSTEM PERMISSIONS", style = MaterialTheme.typography.titleMedium)
                    }

                    PermissionStatusRow(
                        icon = Icons.Default.Layers,
                        title = "Display Over Other Apps",
                        subtitle = "Required to render the floating controller on top of games and apps.",
                        granted = hasOverlayPermission,
                        actionLabel = "Grant Overlay",
                        tag = "grant_overlay_button",
                        onClick = ::openOverlayPermissionSettings
                    )

                    PermissionStatusRow(
                        icon = Icons.Default.QueryStats,
                        title = "App Usage Access",
                        subtitle = "Detects the active foreground app to auto-load your saved preset.",
                        granted = hasUsagePermission,
                        actionLabel = "Grant Usage",
                        tag = "grant_usage_button",
                        onClick = ::openUsageAccessSettings
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionStatusRow(
                            icon = Icons.Default.Notifications,
                            title = "Overlay Service Notification",
                            subtitle = "Keeps the foreground overlay service visible with quick Stop action.",
                            granted = hasNotificationPermission,
                            actionLabel = "Allow",
                            tag = "grant_notifications_button",
                            onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }
                }
            }
        }

        // Active Preset Quick Switcher
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
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
                        Column {
                            Text("CONTROL PRESETS", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Active: ${overlayState.activePreset.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }
                        OutlinedButton(
                            onClick = { onEditPreset(overlayState.activePreset) },
                            modifier = Modifier.testTag("edit_active_preset_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Customize")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = preset.id == overlayState.activePreset.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.20f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurfaceElevated,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = overlayState.activePreset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = buildString {
                                    append("FPS Right Look: ")
                                    append(if (overlayState.activePreset.fpsRightZoneEnabled) "ON" else "OFF")
                                    append("  •  Trigger Hold+Drag Aim: ")
                                    append(if (overlayState.activePreset.triggerDragAimEnabled) "ON" else "OFF")
                                    append("  •  Opacity: ${(overlayState.activePreset.opacity * 100).toInt()}%")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // XInput Engine & Optional UDP Relay Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Usb, contentDescription = null, tint = TacticalEmerald)
                        Spacer(Modifier.width(8.dp))
                        Text("XINPUT HARDWARE PROFILE", style = MaterialTheme.typography.titleMedium)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurfaceElevated,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = driverCapability.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (driverCapability == DriverCapabilityStatus.UINPUT_WRITABLE) TacticalEmerald else ElectricCyan
                            )
                            Text(
                                text = driverCapability.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Fixed Hardware Map: Xbox 360 XInput (VID 0x045E / PID 0x028E) — Buttons are immutable.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TriggerAmber
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Tactile Feedback", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Vibrate lightly when pressing virtual sticks and buttons",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local UDP XInput Stream", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Broadcast live 20-byte XInput reports over UDP to a local ViGEm / ADB bridge",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
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
                                label = { Text("Host IP") },
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
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlateSurfaceElevated,
        border = BorderStroke(1.dp, if (granted) TacticalEmerald.copy(alpha = 0.4f) else HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) TacticalEmerald else TriggerAmber,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "$title granted",
                    tint = TacticalEmerald,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.testTag(tag),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
