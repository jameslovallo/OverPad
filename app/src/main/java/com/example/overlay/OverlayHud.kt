package com.example.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.OverlayRuntimeState
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.model.PresetCategory
import com.example.ui.gamepad.GamepadControlIcon
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.hasCustomIcon
import com.example.ui.theme.OperPadTheme
import com.example.xinput.TouchMouseLookFilter
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
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OverPad",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            runtimeState.detectedAppLabel?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close HUD")
                        }
                    }

                    if (!runtimeState.detectedPackageName.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = runtimeState.detectedAppLabel ?: runtimeState.detectedPackageName,
                                        style = MaterialTheme.typography.bodySmall
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
                                    Text("Bind", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Preset",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                } else null
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacity", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${(preset.opacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = preset.opacity,
                            onValueChange = { raw ->
                                onUpdatePreset(preset.copy(opacity = GridSnapper.snapOpacity(raw)))
                            },
                            valueRange = GridSnapper.MIN_OPACITY..GridSnapper.MAX_OPACITY
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Left-Half Screen Move", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = preset.leftMoveZoneEnabled,
                            onCheckedChange = { enabled ->
                                val updatedControls = preset.controls.map {
                                    if (it.type == ControlType.LEFT_STICK) it.copy(visible = !enabled) else it
                                }
                                onUpdatePreset(
                                    preset.copy(
                                        leftMoveZoneEnabled = enabled,
                                        controls = updatedControls
                                    )
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Right-Half Screen Look", style = MaterialTheme.typography.titleSmall)
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
                        Text("Trigger Swipe Look (LT / RT)", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = preset.triggerDragAimEnabled,
                            onCheckedChange = {
                                onUpdatePreset(preset.copy(triggerDragAimEnabled = it))
                            }
                        )
                    }

                    if (preset.fpsRightZoneEnabled || preset.triggerDragAimEnabled || preset.category == PresetCategory.FPS) {
                        val clampedSensitivity = preset.fpsLookSensitivity.coerceIn(
                            TouchMouseLookFilter.MIN_SENSITIVITY,
                            TouchMouseLookFilter.MAX_SENSITIVITY
                        )
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Look Sensitivity", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${"%.2f".format(clampedSensitivity)}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = clampedSensitivity,
                                onValueChange = { onUpdatePreset(preset.copy(fpsLookSensitivity = it)) },
                                valueRange = TouchMouseLookFilter.MIN_SENSITIVITY..TouchMouseLookFilter.MAX_SENSITIVITY
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Deadzone Boost", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(antiDeadzone * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = antiDeadzone,
                                onValueChange = onUpdateAntiDeadzone,
                                valueRange = TouchMouseLookFilter.MIN_ANTI_DEADZONE..TouchMouseLookFilter.MAX_ANTI_DEADZONE
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Controls",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ControlType.entries.filter { it.canHide }.forEach { controlType ->
                            val placement = preset.placementFor(controlType)
                            val chipTint = if (placement.visible) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            FilterChip(
                                selected = placement.visible,
                                onClick = {
                                    onUpdatePreset(preset.updatePlacement(placement.copy(visible = !placement.visible)))
                                },
                                label = {
                                    if (controlType.hasCustomIcon()) {
                                        GamepadControlIcon(
                                            controlType = controlType,
                                            tint = chipTint,
                                            size = 16.dp
                                        )
                                    } else {
                                        Text(controlType.shortLabel)
                                    }
                                },
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
                            Text("Edit Layout")
                        }
                        OutlinedButton(
                            onClick = onOpenMainApp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("App")
                        }
                    }

                    Button(
                        onClick = onStopOverlay,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Disconnect Overlay")
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
        val gridColor = MaterialTheme.colorScheme.primary

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f))
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = preset.gridStepDp.dp.toPx().coerceAtLeast(12f)
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = gridColor.copy(alpha = if (preset.snapToGrid) 0.18f else 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += cell
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = gridColor.copy(alpha = if (preset.snapToGrid) 0.18f else 0.08f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += cell
                }
            }

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

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 580.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
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
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${selectedType.displayName} • ${(selectedPlacement.scale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge
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
                        Text(
                            "Size",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        Text(
                            "Grid",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
