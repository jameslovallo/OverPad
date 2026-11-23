package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.model.PresetCategory
import com.example.ui.gamepad.GamepadControlIcon
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.hasCustomIcon
import com.example.xinput.TouchMouseLookFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    presets: List<ControllerPreset>,
    editingPresetId: String,
    selectedControl: ControlType,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onUpdateAntiDeadzone: (Float) -> Unit = {},
    onSelectPresetToEdit: (String) -> Unit = {},
    onSelectControl: (ControlType) -> Unit,
    onUpdatePreset: (ControllerPreset) -> Unit,
    onDuplicatePreset: (ControllerPreset, String) -> Unit,
    onResetBuiltInPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val preset = presets.firstOrNull { it.id == editingPresetId } ?: presets.firstOrNull() ?: return
    val selectedPlacement = preset.placementFor(selectedControl)
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateName by remember { mutableStateOf("") }

    if (onNavigateBack != null) {
        BackHandler(onBack = onNavigateBack)
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Save Preset Copy") },
            text = {
                OutlinedTextField(
                    value = duplicateName,
                    onValueChange = { duplicateName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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

    Column(modifier = Modifier.fillMaxSize()) {
        if (onNavigateBack != null) {
            TopAppBar(
                title = { Text(preset.name, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            duplicateName = "${preset.name} Custom"
                            showDuplicateDialog = true
                        },
                        modifier = Modifier.testTag("duplicate_preset_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate preset")
                    }
                    if (preset.isBuiltIn) {
                        IconButton(
                            onClick = { onResetBuiltInPreset(preset.id) },
                            modifier = Modifier.testTag("reset_preset_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset preset")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                onDeleteCustomPreset(preset.id)
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("delete_preset_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete preset",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive Grid-Snapping Controller Canvas
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (preset.snapToGrid) "${preset.gridStepDp}dp grid" else "Free drag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${"%.2f".format(selectedPlacement.xFraction)}, ${"%.2f".format(selectedPlacement.yFraction)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        val density = LocalDensity.current
                        val gridColor = MaterialTheme.colorScheme.primary
                        val zoneColor = MaterialTheme.colorScheme.secondary
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9.5f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        ) {
                            val widthPx = with(density) { maxWidth.toPx() }
                            val heightPx = with(density) { maxHeight.toPx() }
                            val canvasScale = (maxWidth / 760.dp).coerceIn(0.45f, 0.95f)
                            val stepPx = with(density) { (preset.gridStepDp * canvasScale).dp.toPx() }.coerceAtLeast(10f)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (preset.fpsRightZoneEnabled) {
                                    drawRect(
                                        color = zoneColor.copy(alpha = 0.06f),
                                        topLeft = Offset(size.width / 2f, 0f),
                                        size = Size(size.width / 2f, size.height)
                                    )
                                }
                                var x = 0f
                                while (x <= size.width) {
                                    drawLine(
                                        color = gridColor.copy(alpha = if (preset.snapToGrid) 0.16f else 0.06f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    x += stepPx
                                }
                                var y = 0f
                                while (y <= size.height) {
                                    drawLine(
                                        color = gridColor.copy(alpha = if (preset.snapToGrid) 0.16f else 0.06f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    y += stepPx
                                }
                            }

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

            // Selected Control Inspector
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp),
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
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (selectedControl.hasCustomIcon()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        GamepadControlIcon(
                                            controlType = selectedControl,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            size = 24.dp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = selectedControl.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = selectedControl.xinputBinding,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
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

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Control Size", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(selectedPlacement.scale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
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
                                modifier = Modifier.testTag("control_size_slider")
                            )
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
                            ControlType.entries.forEach { type ->
                                val placement = preset.placementFor(type)
                                val isSelected = type == selectedControl
                                val contentTint = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    placement.visible -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            placement.visible -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectControl(type) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (type.hasCustomIcon()) {
                                            GamepadControlIcon(
                                                controlType = type,
                                                tint = contentTint,
                                                size = 16.dp
                                            )
                                        } else {
                                            Text(
                                                text = type.shortLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = contentTint
                                            )
                                        }
                                        if (type.canHide) {
                                            Icon(
                                                imageVector = if (placement.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle ${type.displayName}",
                                                tint = if (placement.visible) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
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
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Layout & Aim", style = MaterialTheme.typography.titleMedium)

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
                                valueRange = GridSnapper.MIN_OPACITY..GridSnapper.MAX_OPACITY,
                                modifier = Modifier.testTag("preset_opacity_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Snap to Grid", style = MaterialTheme.typography.titleSmall)
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
                            Text("Grid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf(16, 24, 32).forEach { step ->
                                FilterChip(
                                    selected = preset.gridStepDp == step,
                                    onClick = { onUpdatePreset(preset.copy(gridStepDp = step)) },
                                    label = { Text("${step}dp") }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Right-Half Screen Look", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = preset.fpsRightZoneEnabled,
                                onCheckedChange = { onUpdatePreset(preset.copy(fpsRightZoneEnabled = it)) }
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
                                onCheckedChange = { onUpdatePreset(preset.copy(triggerDragAimEnabled = it)) }
                            )
                        }

                        if (preset.fpsRightZoneEnabled || preset.triggerDragAimEnabled || preset.category == PresetCategory.FPS) {
                            val clampedSensitivity = preset.fpsLookSensitivity.coerceIn(
                                TouchMouseLookFilter.MIN_SENSITIVITY,
                                TouchMouseLookFilter.MAX_SENSITIVITY
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                }
                                Column {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Invert Right Y", style = MaterialTheme.typography.titleSmall)
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
}
