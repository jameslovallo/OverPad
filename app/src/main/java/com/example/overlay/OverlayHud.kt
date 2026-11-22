package com.example.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.OverlayRuntimeState
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.model.PresetCategory
import com.example.xinput.TouchMouseLookFilter
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OperPadTheme
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverlaySettingsHud(
    runtimeState: OverlayRuntimeState,
    allPresets: List<ControllerPreset>,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onUpdateAntiDeadzone: (Float) -> Unit = {},
    onSelectPreset: (ControllerPreset) -> Unit,
    onBindForegroundApp: (String, String, String) -> Unit,
    onUpdatePreset: (ControllerPreset) -> Unit,
    onEnterEditMode: () -> Unit,
    onOpenMainApp: () -> Unit,
    onStopOverlay: () -> Unit,
    onDismiss: () -> Unit
) {
    OperPadTheme(darkTheme = true) {
        val preset = runtimeState.activePreset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(16.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                color = SlateSurface.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OVERPAD QUICK HUD",
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan
                            )
                            val fgText = runtimeState.detectedAppLabel?.let {
                                "Foreground: $it" + if (runtimeState.autoMatchedByApp) " (Auto-Loaded)" else ""
                            } ?: "No foreground app tracked yet"
                            Text(
                                text = fgText,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close HUD", tint = TextPrimary)
                        }
                    }

                    // Bind current foreground app button
                    if (!runtimeState.detectedPackageName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SlateSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = TacticalEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Bind ${runtimeState.detectedAppLabel ?: runtimeState.detectedPackageName} to '${preset.name}'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onBindForegroundApp(
                                            runtimeState.detectedPackageName,
                                            runtimeState.detectedAppLabel ?: runtimeState.detectedPackageName,
                                            preset.id
                                        )
                                    }
                                ) {
                                    Text("Bind App", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Preset Switcher
                    Text(
                        text = "ACTIVE PRESET",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allPresets.forEach { item ->
                            val selected = item.id == preset.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectPreset(item) },
                                label = { Text(item.name) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.20f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = HairlineBorder)

                    // Global Opacity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("All Controls Opacity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "${(preset.opacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = preset.opacity,
                            onValueChange = { raw ->
                                onUpdatePreset(preset.copy(opacity = GridSnapper.snapOpacity(raw)))
                            },
                            valueRange = GridSnapper.MIN_OPACITY..GridSnapper.MAX_OPACITY,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan
                            )
                        )
                    }

                    // FPS Mechanics Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Right-Side Screen Look Drag", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Drag right half of screen to emulate Right Stick",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = preset.fpsRightZoneEnabled,
                            onCheckedChange = {
                                onUpdatePreset(preset.copy(fpsRightZoneEnabled = it))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trigger Hold + Drag Aim (LT/RT)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Hold LT/RT and swipe to steer Right Stick mouse-style",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = preset.triggerDragAimEnabled,
                            onCheckedChange = {
                                onUpdatePreset(preset.copy(triggerDragAimEnabled = it))
                            }
                        )
                    }

                    if (preset.fpsRightZoneEnabled || preset.triggerDragAimEnabled || preset.category == PresetCategory.FPS) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mouse-Look Sensitivity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    text = "${"%.2f".format(preset.fpsLookSensitivity)}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                            }
                            Slider(
                                value = preset.fpsLookSensitivity,
                                onValueChange = { onUpdatePreset(preset.copy(fpsLookSensitivity = it)) },
                                valueRange = 0.5f..3.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Game Deadzone Boost", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    text = "${(antiDeadzone * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                            }
                            Slider(
                                value = antiDeadzone,
                                onValueChange = onUpdateAntiDeadzone,
                                valueRange = TouchMouseLookFilter.MIN_ANTI_DEADZONE..TouchMouseLookFilter.MAX_ANTI_DEADZONE,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = HairlineBorder)

                    // Show / Hide Individual Controls
                    Text(
                        text = "SHOW / HIDE INDIVIDUAL CONTROLS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ControlType.entries.filter { it.canHide }.forEach { controlType ->
                            val placement = preset.placementFor(controlType)
                            FilterChip(
                                selected = placement.visible,
                                onClick = {
                                    onUpdatePreset(preset.updatePlacement(placement.copy(visible = !placement.visible)))
                                },
                                label = { Text(controlType.shortLabel) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (placement.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEnterEditMode,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit & Resize")
                        }
                        OutlinedButton(
                            onClick = onOpenMainApp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Full Studio")
                        }
                    }

                    Button(
                        onClick = onStopOverlay,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonAlert.copy(alpha = 0.22f),
                            contentColor = CrimsonAlert
                        )
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Stop OverPad Overlay")
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayGridEditorScreen(
    preset: ControllerPreset,
    onUpdatePreset: (ControllerPreset) -> Unit,
    onFinishEdit: () -> Unit
) {
    OperPadTheme(darkTheme = true) {
        var selectedType by remember { mutableStateOf(ControlType.LEFT_STICK) }
        val selectedPlacement = preset.placementFor(selectedType)
        val density = LocalDensity.current
        val stepPx = with(density) { preset.gridStepDp.dp.toPx() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg.copy(alpha = 0.62f))
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            // Grid Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = preset.gridStepDp.dp.toPx().coerceAtLeast(12f)
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = ElectricCyan.copy(alpha = if (preset.snapToGrid) 0.18f else 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += cell
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = ElectricCyan.copy(alpha = if (preset.snapToGrid) 0.18f else 0.08f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += cell
                }
            }

            // Draggable Controls
            preset.controls.forEach { placement ->
                if (placement.visible || placement.type == selectedType) {
                    val wPx = with(density) { (placement.type.baseWidthDp * placement.scale).dp.toPx() }
                    val hPx = with(density) { (placement.type.baseHeightDp * placement.scale).dp.toPx() }
                    val offsetX = (placement.xFraction * widthPx - wPx / 2f).roundToInt()
                    val offsetY = (placement.yFraction * heightPx - hPx / 2f).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .pointerInput(placement.type, preset.snapToGrid, preset.gridStepDp, widthPx, heightPx) {
                                detectDragGestures(
                                    onDragStart = { selectedType = placement.type },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val current = preset.placementFor(placement.type)
                                        val rawX = current.xFraction + (dragAmount.x / widthPx)
                                        val rawY = current.yFraction + (dragAmount.y / heightPx)
                                        val (snappedX, snappedY) = GridSnapper.snapPosition(
                                            rawXFraction = rawX,
                                            rawYFraction = rawY,
                                            widthPx = widthPx,
                                            heightPx = heightPx,
                                            stepPx = stepPx,
                                            enabled = preset.snapToGrid
                                        )
                                        onUpdatePreset(
                                            preset.updatePlacement(
                                                current.copy(xFraction = snappedX, yFraction = snappedY)
                                            )
                                        )
                                    }
                                )
                            }
                            .clickable { selectedType = placement.type }
                    ) {
                        GamepadControlNode(
                            placement = placement,
                            preset = preset,
                            engine = null,
                            interactive = false,
                            isSelectedInEditor = selectedType == placement.type
                        )
                    }
                }
            }

            // Top Floating Inspector Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 580.dp),
                shape = RoundedCornerShape(16.dp),
                color = SlateSurface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${selectedType.displayName}  •  Scale ${(selectedPlacement.scale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedType.canHide) {
                                OutlinedButton(
                                    onClick = {
                                        onUpdatePreset(
                                            preset.updatePlacement(selectedPlacement.copy(visible = !selectedPlacement.visible))
                                        )
                                    }
                                ) {
                                    Text(if (selectedPlacement.visible) "Hide" else "Show")
                                }
                            }
                            Button(onClick = onFinishEdit) {
                                Text("Done")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Size", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Slider(
                            value = selectedPlacement.scale,
                            onValueChange = { raw ->
                                onUpdatePreset(
                                    preset.updatePlacement(
                                        selectedPlacement.copy(scale = GridSnapper.snapScale(raw))
                                    )
                                )
                            },
                            valueRange = GridSnapper.MIN_SCALE..GridSnapper.MAX_SCALE,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Grid", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Switch(
                            checked = preset.snapToGrid,
                            onCheckedChange = { onUpdatePreset(preset.copy(snapToGrid = it)) }
                        )
                    }
                }
            }
        }
    }
}
