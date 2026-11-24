package com.example.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.OverlayRuntimeState
import com.example.model.ButtonShape
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.model.PresetCategory
import com.example.ui.gamepad.ControlDragHelper
import com.example.ui.gamepad.GamepadControlIcon
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.hasCustomIcon
import com.example.ui.theme.OverPadTheme
import com.example.xinput.TouchMouseLookFilter
import kotlin.math.abs
import kotlin.math.hypot
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
    OverPadTheme(darkTheme = true) {
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
                            valueRange = GridSnapper.MIN_OPACITY..GridSnapper.MAX_OPACITY,
                            steps = GridSnapper.OPACITY_STEPS
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
                                onValueChange = { raw ->
                                    onUpdatePreset(
                                        preset.copy(fpsLookSensitivity = TouchMouseLookFilter.snapSensitivity(raw))
                                    )
                                },
                                valueRange = TouchMouseLookFilter.MIN_SENSITIVITY..TouchMouseLookFilter.MAX_SENSITIVITY,
                                steps = TouchMouseLookFilter.SENSITIVITY_STEPS
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
                                value = antiDeadzone.coerceIn(
                                    TouchMouseLookFilter.MIN_ANTI_DEADZONE,
                                    TouchMouseLookFilter.MAX_ANTI_DEADZONE
                                ),
                                onValueChange = { raw ->
                                    onUpdateAntiDeadzone(TouchMouseLookFilter.snapAntiDeadzone(raw))
                                },
                                valueRange = TouchMouseLookFilter.MIN_ANTI_DEADZONE..TouchMouseLookFilter.MAX_ANTI_DEADZONE,
                                steps = TouchMouseLookFilter.ANTI_DEADZONE_STEPS
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverlayGridEditorScreen(
    preset: ControllerPreset,
    onUpdatePreset: (ControllerPreset) -> Unit,
    onFinishEdit: () -> Unit
) {
    OverPadTheme(darkTheme = true) {
        var selectedType by remember(preset.id) {
            mutableStateOf(preset.controls.firstOrNull { it.visible }?.type ?: ControlType.TRIGGER_RT)
        }
        var activeDragPos by remember { mutableStateOf<Pair<ControlType, Pair<Float, Float>>?>(null) }
        var toolbarDockedBottom by remember { mutableStateOf(false) }
        val selectedPlacement = preset.placementFor(selectedType)
        val density = LocalDensity.current
        val gridColor = MaterialTheme.colorScheme.primary
        val leftZoneColor = MaterialTheme.colorScheme.tertiary
        val rightZoneColor = MaterialTheme.colorScheme.secondary

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f))
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val gridCols = GridSnapper.computeGridColumns(preset.gridRows, widthPx, heightPx)

            val latestPreset by rememberUpdatedState(preset)
            val latestSelectedType by rememberUpdatedState(selectedType)
            val latestWidthPx by rememberUpdatedState(widthPx)
            val latestHeightPx by rememberUpdatedState(heightPx)
            val latestOnUpdatePreset by rememberUpdatedState(onUpdatePreset)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            val hit = ControlDragHelper.findHitControl(
                                touchX = down.position.x,
                                touchY = down.position.y,
                                controls = latestPreset.controls,
                                selectedControl = latestSelectedType,
                                canvasWidthPx = latestWidthPx,
                                canvasHeightPx = latestHeightPx,
                                effectiveScaleMultiplier = 1f,
                                densityPx = density.density
                            ) ?: return@awaitEachGesture

                            down.consume()
                            selectedType = hit.type

                            val startX = hit.xFraction
                            val startY = hit.yFraction
                            var hasCrossedSlop = false
                            var lastCommitted = startX to startY
                            activeDragPos = hit.type to lastCommitted
                            val touchSlopPx = viewConfiguration.touchSlop

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) {
                                        change?.consume()
                                        break
                                    }
                                    change.consume()
                                    val totalDelta = change.position - down.position
                                    if (!hasCrossedSlop && hypot(totalDelta.x, totalDelta.y) >= touchSlopPx) {
                                        hasCrossedSlop = true
                                    }
                                    if (hasCrossedSlop) {
                                        val snapped = ControlDragHelper.computeDraggedPlacement(
                                            startXFraction = startX,
                                            startYFraction = startY,
                                            totalDeltaXPx = totalDelta.x,
                                            totalDeltaYPx = totalDelta.y,
                                            canvasWidthPx = latestWidthPx,
                                            canvasHeightPx = latestHeightPx,
                                            gridRows = latestPreset.gridRows,
                                            snapToGrid = latestPreset.snapToGrid
                                        )
                                        activeDragPos = hit.type to snapped
                                        if (snapped != lastCommitted) {
                                            lastCommitted = snapped
                                            latestOnUpdatePreset(
                                                latestPreset.updatePlacement(
                                                    latestPreset.placementFor(hit.type).copy(
                                                        xFraction = snapped.first,
                                                        yFraction = snapped.second
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            } finally {
                                if (hasCrossedSlop) {
                                    val finalPos = activeDragPos?.second ?: lastCommitted
                                    latestOnUpdatePreset(
                                        latestPreset.updatePlacement(
                                            latestPreset.placementFor(hit.type).copy(
                                                xFraction = finalPos.first,
                                                yFraction = finalPos.second
                                            )
                                        )
                                    )
                                }
                                activeDragPos = null
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (preset.leftMoveZoneEnabled) {
                        drawRect(
                            color = leftZoneColor.copy(alpha = 0.06f),
                            topLeft = Offset.Zero,
                            size = Size(size.width / 2f, size.height)
                        )
                    }
                    if (preset.fpsRightZoneEnabled) {
                        drawRect(
                            color = rightZoneColor.copy(alpha = 0.06f),
                            topLeft = Offset(size.width / 2f, 0f),
                            size = Size(size.width / 2f, size.height)
                        )
                    }
                    val cols = GridSnapper.computeGridColumns(preset.gridRows, size.width, size.height)
                    GridSnapper.gridLineFractions(cols).forEach { xFrac ->
                        val xPx = xFrac * size.width
                        val isCenter = abs(xFrac - 0.5f) < 0.0005f
                        drawLine(
                            color = gridColor.copy(
                                alpha = when {
                                    isCenter -> if (preset.snapToGrid) 0.36f else 0.18f
                                    preset.snapToGrid -> 0.18f
                                    else -> 0.07f
                                }
                            ),
                            start = Offset(xPx, 0f),
                            end = Offset(xPx, size.height),
                            strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }
                    GridSnapper.gridLineFractions(preset.gridRows).forEach { yFrac ->
                        val yPx = yFrac * size.height
                        val isCenter = abs(yFrac - 0.5f) < 0.0005f
                        drawLine(
                            color = gridColor.copy(
                                alpha = when {
                                    isCenter -> if (preset.snapToGrid) 0.36f else 0.18f
                                    preset.snapToGrid -> 0.18f
                                    else -> 0.07f
                                }
                            ),
                            start = Offset(0f, yPx),
                            end = Offset(size.width, yPx),
                            strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }
                    activeDragPos?.second?.let { (dx, dy) ->
                        val cx = dx * size.width
                        val cy = dy * size.height
                        drawLine(
                            color = gridColor.copy(alpha = 0.44f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridColor.copy(alpha = 0.44f),
                            start = Offset(0f, cy),
                            end = Offset(size.width, cy),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawCircle(
                            color = gridColor.copy(alpha = 0.90f),
                            radius = 4.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                preset.controls.forEach { placement ->
                    if (placement.visible || placement.type == selectedType) {
                        val draggedCoords = if (activeDragPos?.first == placement.type) {
                            activeDragPos!!.second
                        } else null
                        val renderX = draggedCoords?.first ?: placement.xFraction
                        val renderY = draggedCoords?.second ?: placement.yFraction

                        val wPx = with(density) { placement.effectiveWidthDp().dp.toPx() }
                        val hPx = with(density) { placement.effectiveHeightDp().dp.toPx() }
                        val offsetX = (renderX * widthPx - wPx / 2f).roundToInt()
                        val offsetY = (renderY * heightPx - hPx / 2f).roundToInt()

                        Box(
                            modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
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
            }

            Surface(
                modifier = Modifier
                    .align(if (toolbarDockedBottom) Alignment.BottomCenter else Alignment.TopCenter)
                    .padding(
                        top = if (toolbarDockedBottom) 0.dp else 12.dp,
                        bottom = if (toolbarDockedBottom) 12.dp else 0.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                    .widthIn(max = 580.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = true).consume()
                        }
                    },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { toolbarDockedBottom = !toolbarDockedBottom }
                            ) {
                                Icon(
                                    imageVector = if (toolbarDockedBottom) {
                                        Icons.Default.VerticalAlignTop
                                    } else {
                                        Icons.Default.VerticalAlignBottom
                                    },
                                    contentDescription = if (toolbarDockedBottom) "Dock toolbar at top" else "Dock toolbar at bottom",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            "Snap",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = preset.snapToGrid,
                            onCheckedChange = { onUpdatePreset(preset.copy(snapToGrid = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grid Divisions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val nextRows = (preset.gridRows - 1).coerceIn(
                                        GridSnapper.MIN_GRID_ROWS,
                                        GridSnapper.MAX_GRID_ROWS
                                    )
                                    onUpdatePreset(preset.copy(gridRows = nextRows))
                                },
                                enabled = preset.gridRows > GridSnapper.MIN_GRID_ROWS
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Fewer grid rows", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${preset.gridRows} × $gridCols",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    val nextRows = (preset.gridRows + 1).coerceIn(
                                        GridSnapper.MIN_GRID_ROWS,
                                        GridSnapper.MAX_GRID_ROWS
                                    )
                                    onUpdatePreset(preset.copy(gridRows = nextRows))
                                },
                                enabled = preset.gridRows < GridSnapper.MAX_GRID_ROWS
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "More grid rows", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (selectedType.supportsShape || selectedType.supportsToggle) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (selectedType.supportsShape) {
                                val currentShape = selectedPlacement.resolvedShape()
                                ButtonShape.entries.forEach { shapeOption ->
                                    FilterChip(
                                        selected = currentShape == shapeOption,
                                        onClick = {
                                            onUpdatePreset(
                                                preset.updatePlacement(selectedPlacement.copy(shape = shapeOption))
                                            )
                                        },
                                        label = { Text(shapeOption.label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            if (selectedType.supportsToggle) {
                                FilterChip(
                                    selected = selectedPlacement.toggleable,
                                    onClick = {
                                        onUpdatePreset(
                                            preset.updatePlacement(selectedPlacement.copy(toggleable = !selectedPlacement.toggleable))
                                        )
                                    },
                                    label = { Text("Toggle", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
