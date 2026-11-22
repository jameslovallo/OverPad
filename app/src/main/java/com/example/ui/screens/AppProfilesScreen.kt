package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.OverlayRuntimeState
import com.example.model.AppPresetBinding
import com.example.model.ControllerPreset
import com.example.model.InstalledAppInfo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TriggerAmber

@Composable
fun AppProfilesScreen(
    installedApps: List<InstalledAppInfo>,
    isLoadingApps: Boolean,
    presets: List<ControllerPreset>,
    bindings: List<AppPresetBinding>,
    autoSwitchEnabled: Boolean,
    hasUsagePermission: Boolean,
    overlayState: OverlayRuntimeState,
    onToggleAutoSwitch: (Boolean) -> Unit,
    onBindApp: (packageName: String, label: String, presetId: String) -> Unit,
    onUnbindApp: (packageName: String) -> Unit,
    onRefreshApps: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var pickingForApp by remember { mutableStateOf<InstalledAppInfo?>(null) }

    val bindingsMap = remember(bindings) { bindings.associateBy { it.packageName } }
    val presetsMap = remember(presets) { presets.associateBy { it.id } }

    val filteredApps = remember(installedApps, searchQuery, bindingsMap) {
        val query = searchQuery.trim().lowercase()
        val matches = if (query.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query)
            }
        }
        // Pin bound apps first
        matches.sortedWith(
            compareByDescending<InstalledAppInfo> { bindingsMap.containsKey(it.packageName) }
                .thenBy { it.label.lowercase() }
        )
    }

    pickingForApp?.let { targetApp ->
        val currentBinding = bindingsMap[targetApp.packageName]
        AlertDialog(
            onDismissRequest = { pickingForApp = null },
            title = { Text("Preset for ${targetApp.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUnbindApp(targetApp.packageName)
                                pickingForApp = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentBinding == null,
                            onClick = {
                                onUnbindApp(targetApp.packageName)
                                pickingForApp = null
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Use Default Preset")
                    }
                    HorizontalDivider(color = HairlineBorder)
                    presets.forEach { preset ->
                        val selected = currentBinding?.presetId == preset.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBindApp(targetApp.packageName, targetApp.label, preset.id)
                                    pickingForApp = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    onBindApp(targetApp.packageName, targetApp.label, preset.id)
                                    pickingForApp = null
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(preset.name, style = MaterialTheme.typography.titleSmall)
                                Text(preset.category.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingForApp = null }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Usage Monitor & Auto-Switch Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QueryStats, contentDescription = null, tint = ElectricCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("PER-APP AUTO PROFILES", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
                        }
                        IconButton(onClick = onRefreshApps) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh installed apps", tint = TextPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Load Saved Preset by Running App", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Uses App Usage stats while overlay is running to switch layouts automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = autoSwitchEnabled,
                            onCheckedChange = onToggleAutoSwitch
                        )
                    }

                    if (!hasUsagePermission) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TriggerAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, TriggerAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Grant Usage Access so OperPad can detect which game or app is in the foreground.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                                    modifier = Modifier.testTag("profiles_grant_usage_button")
                                ) {
                                    Text("Enable")
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SlateSurfaceElevated,
                            border = BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LAST DETECTED FOREGROUND APP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = overlayState.detectedAppLabel ?: "Launch overlay and open any app to detect",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (overlayState.detectedPackageName != null) TacticalEmerald else TextSecondary
                                    )
                                    overlayState.detectedPackageName?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                                Icon(Icons.Default.Check, contentDescription = "Usage access enabled", tint = TacticalEmerald)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps or package names...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_input")
                    )
                }
            }
        }

        if (isLoadingApps) {
            item {
                Text(
                    text = "Scanning launchable apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else if (filteredApps.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No matching apps found",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Try clearing your search query or refreshing installed apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(filteredApps, key = { it.packageName }) { appInfo ->
                val binding = bindingsMap[appInfo.packageName]
                val boundPreset = binding?.let { presetsMap[it.presetId] }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)
                        .clickable { pickingForApp = appInfo },
                    shape = RoundedCornerShape(14.dp),
                    color = SlateSurface,
                    border = BorderStroke(
                        1.dp,
                        if (boundPreset != null) ElectricCyan.copy(alpha = 0.55f) else HairlineBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appInfo.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = appInfo.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (boundPreset != null) ElectricCyan.copy(alpha = 0.16f) else SlateSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (boundPreset != null) ElectricCyan else HairlineBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = if (boundPreset != null) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = boundPreset?.name ?: "Default",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (boundPreset != null) ElectricCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
