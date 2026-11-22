package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.model.TriggerDragMode
import com.example.ui.gamepad.GamepadControlNode
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
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayoutEditorScreen(
    presets: List<ControllerPreset>,
    editingPresetId: String,
    selectedControl: ControlType,
    onSelectPresetToEdit: (String) -> Unit,
    onSelectControl: (ControlType) -> Unit,
    onUpdatePreset: (ControllerPreset) -> Unit,
    onDuplicatePreset: (ControllerPreset, String) -> Unit,
    onResetBuiltInPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit
) {
    val preset = presets.firstOrNull { it.id == editingPresetId } ?: presets.firstOrNull() ?: return
    val selectedPlacement = preset.placementFor(selectedControl)
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateName by remember { mutableStateOf("") }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Save As Custom Preset") },
            text = {
                OutlinedTextField(
                    value = duplicateName,
                    onValueChange = { duplicateName = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onDuplicatePreset(preset, duplicateName.ifBlank { "${preset.name} Custom" })
                    showDuplicateDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset Selector & Actions
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LAYOUT STUDIO", style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    duplicateName = "${preset.name} Custom"
                                    showDuplicateDialog = true
                                },
                                modifier = Modifier.testTag("duplicate_preset_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clone")
                            }
                            if (preset.isBuiltIn) {
                                OutlinedButton(
                                    onClick = { onResetBuiltInPreset(preset.id) },
                                    modifier = Modifier.testTag("reset_preset_button")
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reset")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onDeleteCustomPreset(preset.id) },
                                    modifier = Modifier.testTag("delete_preset_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", color = CrimsonAlert)
                                }
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { item ->
                            FilterChip(
                                selected = item.id == preset.id,
                                onClick = { onSelectPresetToEdit(item.id) },
                                label = { Text(item.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        // Interactive Grid-Snapping Controller Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DRAG TO MOVE  •  SNAPS TO ${preset.gridStepDp}DP GRID",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Text(
                            text = "X: ${"%.2f".format(selectedPlacement.xFraction)}  Y: ${"%.2f".format(selectedPlacement.yFraction)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val density = LocalDensity.current
                    // Scale preview widgets slightly in portrait canvas so all controls fit cleanly without overlapping
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9.5f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B13))
                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    ) {
                        val widthPx = with(density) { maxWidth.toPx() }
                        val heightPx = with(density) { maxHeight.toPx() }
                        val canvasScale = (maxWidth / 760.dp).coerceIn(0.45f, 0.95f)
                        val stepPx = with(density) { (preset.gridStepDp * canvasScale).dp.toPx() }.coerceAtLeast(10f)

                        // Grid & optional FPS Right Zone preview
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (preset.fpsRightZoneEnabled) {
                                drawRect(
                                    color = ElectricCyan.copy(alpha = 0.05f),
                                    topLeft = Offset(size.width / 2f, 0f),
                                    size = androidx.compose.ui.geometry.Size(size.width / 2f, size.height)
                                )
                            }
                            var x = 0f
                            while (x <= size.width) {
                                drawLine(
                                    color = ElectricCyan.copy(alpha = if (preset.snapToGrid) 0.16f else 0.06f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                                x += stepPx
                            }
                            var y = 0f
                            while (y <= size.height) {
                                drawLine(
                                    color = ElectricCyan.copy(alpha = if (preset.snapToGrid) 0.16f else 0.06f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                y += stepPx
                            }
                        }

                        // Controls
                        preset.controls.forEach { placement ->
                            if (placement.visible || placement.type == selectedControl) {
                                val effectivePlacement = placement.copy(scale = placement.scale * canvasScale)
                                val wPx = with(density) { (effectivePlacement.type.baseWidthDp * effectivePlacement.scale).dp.toPx() }
                                val hPx = with(density) { (effectivePlacement.type.baseHeightDp * effectivePlacement.scale).dp.toPx() }
                                val offsetX = (placement.xFraction * widthPx - wPx / 2f).roundToInt()
                                val offsetY = (placement.yFraction * heightPx - hPx / 2f).roundToInt()

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX, offsetY) }
                                        .pointerInput(placement.type, preset.snapToGrid, preset.gridStepDp, widthPx, heightPx) {
                                            detectDragGestures(
                                                onDragStart = { onSelectControl(placement.type) },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val latest = preset.placementFor(placement.type)
                                                    val rawX = latest.xFraction + (dragAmount.x / widthPx)
                                                    val rawY = latest.yFraction + (dragAmount.y / heightPx)
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
                                                            latest.copy(xFraction = snappedX, yFraction = snappedY)
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                        .clickable { onSelectControl(placement.type) }
                                ) {
                                    GamepadControlNode(
                                        placement = effectivePlacement,
                                        preset = preset,
                                        engine = null,
                                        interactive = false,
                                        isSelectedInEditor = selectedControl == placement.type
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Control Inspector (Size, Visibility, Fixed Non-Remappable Binding)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SELECTED: ${selectedControl.displayName.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Fixed binding",
                                    tint = TriggerAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Fixed XInput: ${selectedControl.xinputBinding} (${selectedControl.androidCodeSummary})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TriggerAmber
                                )
                            }
                        }

                        if (selectedControl.canHide) {
                            FilterChip(
                                selected = selectedPlacement.visible,
                                onClick = {
                                    onUpdatePreset(
                                        preset.updatePlacement(selectedPlacement.copy(visible = !selectedPlacement.visible))
                                    )
                                },
                                label = { Text(if (selectedPlacement.visible) "Visible" else "Hidden") },
                                leadingIcon = {
                                    Icon(
                                        if (selectedPlacement.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Individual Control Size Scale Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Individual Control Size", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${(selectedPlacement.scale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = selectedPlacement.scale,
                            onValueChange = { raw ->
                                onUpdatePreset(
                                    preset.updatePlacement(selectedPlacement.copy(scale = GridSnapper.snapScale(raw)))
                                )
                            },
                            valueRange = GridSnapper.MIN_SCALE..GridSnapper.MAX_SCALE,
                            modifier = Modifier.testTag("control_size_slider"),
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )
                    }

                    HorizontalDivider(color = HairlineBorder)

                    // Show / Hide All Controls Tray
                    Text(
                        text = "TAP TO SELECT OR TOGGLE VISIBILITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ControlType.entries.forEach { type ->
                            val placement = preset.placementFor(type)
                            val isSelected = type == selectedControl
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ElectricCyan.copy(alpha = 0.18f) else SlateSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isSelected -> ElectricCyan
                                        placement.visible -> TacticalEmerald.copy(alpha = 0.45f)
                                        else -> HairlineBorder
                                    }
                                ),
                                modifier = Modifier.clickable { onSelectControl(type) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = type.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (placement.visible) TextPrimary else TextSecondary
                                    )
                                    if (type.canHide) {
                                        Icon(
                                            imageVector = if (placement.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle ${type.displayName}",
                                            tint = if (placement.visible) TacticalEmerald else TextSecondary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    onSelectControl(type)
                                                    onUpdatePreset(
                                                        preset.updatePlacement(placement.copy(visible = !placement.visible))
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Opacity, Grid Snap & FPS Mechanics
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("PRESET OPACITY, GRID & FPS AIM", style = MaterialTheme.typography.titleMedium)

                    // Opacity of ALL controls
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("All Controls Opacity", style = MaterialTheme.typography.titleSmall)
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
                            modifier = Modifier.testTag("preset_opacity_slider"),
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )
                    }

                    // Snap to grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Snap Controls to Grid", style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = preset.snapToGrid,
                            onCheckedChange = { onUpdatePreset(preset.copy(snapToGrid = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Size:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        listOf(16, 24, 32).forEach { step ->
                            FilterChip(
                                selected = preset.gridStepDp == step,
                                onClick = { onUpdatePreset(preset.copy(gridStepDp = step)) },
                                label = { Text("${step}dp") }
                            )
                        }
                    }

                    HorizontalDivider(color = HairlineBorder)

                    // FPS Right-Side Drag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FPS Right-Side Screen Look", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Drag anywhere on the right half of the screen to emulate Right Thumbstick input",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = preset.fpsRightZoneEnabled,
                            onCheckedChange = { onUpdatePreset(preset.copy(fpsRightZoneEnabled = it)) }
                        )
                    }

                    // Trigger Hold & Drag Aim
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trigger Hold & Drag Aim (LT / RT)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Hold a trigger button and drag in any direction to steer Right Stick while keeping trigger depressed",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = preset.triggerDragAimEnabled,
                            onCheckedChange = { onUpdatePreset(preset.copy(triggerDragAimEnabled = it)) }
                        )
                    }

                    if (preset.triggerDragAimEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trigger Drag Mode:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            TriggerDragMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preset.triggerDragMode == mode,
                                    onClick = { onUpdatePreset(preset.copy(triggerDragMode = mode)) },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                    }

                    if (preset.fpsRightZoneEnabled || preset.triggerDragAimEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Look Sensitivity", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${"%.2f".format(preset.fpsLookSensitivity)}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                            }
                            Slider(
                                value = preset.fpsLookSensitivity,
                                onValueChange = { onUpdatePreset(preset.copy(fpsLookSensitivity = it)) },
                                valueRange = 0.5f..3.0f
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Invert Right Stick Y-Axis", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = preset.invertRightY,
                                onCheckedChange = { onUpdatePreset(preset.copy(invertRightY = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
