package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.ButtonShape
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.GridSnapper
import com.example.model.PresetCategory
import com.example.ui.gamepad.ControlDragHelper
import com.example.ui.gamepad.GamepadControlIcon
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.hasCustomIcon
import com.example.xinput.TouchMouseLookFilter
import kotlin.math.abs
import kotlin.math.hypot
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
    onExportPreset: ((presetId: String, uri: Uri) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val preset = presets.firstOrNull { it.id == editingPresetId } ?: presets.firstOrNull() ?: return
    val selectedPlacement = preset.placementFor(selectedControl)
    val context = LocalContext.current
    val landscapeAspectRatio = remember(context) { ControlDragHelper.deviceLandscapeAspectRatio(context) }
    val previewCols = GridSnapper.computeGridColumns(preset.gridRows, landscapeAspectRatio)
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateName by remember { mutableStateOf("") }
    var activeDragPos by remember { mutableStateOf<Pair<ControlType, Pair<Float, Float>>?>(null) }
    val currentPresetId by rememberUpdatedState(preset.id)
    val exportPresetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportPreset?.invoke(currentPresetId, uri)
        }
    }

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
                windowInsets = WindowInsets(0),
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
                    if (onExportPreset != null) {
                        IconButton(
                            onClick = {
                                val slug = preset.name.lowercase()
                                    .replace(Regex("[^a-z0-9]+"), "-")
                                    .trim('-')
                                    .ifBlank { "profile" }
                                exportPresetLauncher.launch("overpad-$slug.json")
                            },
                            modifier = Modifier.testTag("export_preset_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Export profile")
                        }
                    }
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
            userScrollEnabled = activeDragPos == null,
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
                                text = if (preset.snapToGrid) "${preset.gridRows} × $previewCols grid" else "Free drag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val displayCoords = if (activeDragPos?.first == selectedControl) {
                                activeDragPos!!.second
                            } else {
                                selectedPlacement.xFraction to selectedPlacement.yFraction
                            }
                            Text(
                                text = "${"%.2f".format(displayCoords.first)}, ${"%.2f".format(displayCoords.second)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        val density = LocalDensity.current
                        val gridColor = MaterialTheme.colorScheme.primary
                        val leftZoneColor = MaterialTheme.colorScheme.tertiary
                        val zoneColor = MaterialTheme.colorScheme.secondary
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(landscapeAspectRatio)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        ) {
                            val widthPx = with(density) { maxWidth.toPx() }
                            val heightPx = with(density) { maxHeight.toPx() }
                            val canvasScale = (maxWidth / 760.dp).coerceIn(0.45f, 0.95f)

                            val latestPreset by rememberUpdatedState(preset)
                            val latestSelectedControl by rememberUpdatedState(selectedControl)
                            val latestWidthPx by rememberUpdatedState(widthPx)
                            val latestHeightPx by rememberUpdatedState(heightPx)
                            val latestCanvasScale by rememberUpdatedState(canvasScale)
                            val latestOnSelectControl by rememberUpdatedState(onSelectControl)
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
                                                selectedControl = latestSelectedControl,
                                                canvasWidthPx = latestWidthPx,
                                                canvasHeightPx = latestHeightPx,
                                                effectiveScaleMultiplier = latestCanvasScale,
                                                densityPx = density.density
                                            ) ?: return@awaitEachGesture

                                            down.consume()
                                            latestOnSelectControl(hit.type)

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
                                            color = zoneColor.copy(alpha = 0.06f),
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
                                                    isCenter -> if (preset.snapToGrid) 0.34f else 0.18f
                                                    preset.snapToGrid -> 0.16f
                                                    else -> 0.06f
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
                                                    isCenter -> if (preset.snapToGrid) 0.34f else 0.18f
                                                    preset.snapToGrid -> 0.16f
                                                    else -> 0.06f
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
                                            color = gridColor.copy(alpha = 0.42f),
                                            start = Offset(cx, 0f),
                                            end = Offset(cx, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        drawLine(
                                            color = gridColor.copy(alpha = 0.42f),
                                            start = Offset(0f, cy),
                                            end = Offset(size.width, cy),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        drawCircle(
                                            color = gridColor.copy(alpha = 0.85f),
                                            radius = 3.5.dp.toPx(),
                                            center = Offset(cx, cy)
                                        )
                                    }
                                }

                                preset.controls.forEach { placement ->
                                    if (placement.visible || placement.type == selectedControl) {
                                        val draggedCoords = if (activeDragPos?.first == placement.type) {
                                            activeDragPos!!.second
                                        } else null
                                        val renderX = draggedCoords?.first ?: placement.xFraction
                                        val renderY = draggedCoords?.second ?: placement.yFraction

                                        val effectivePlacement = placement.copy(scale = placement.scale * canvasScale)
                                        val wPx = with(density) { effectivePlacement.effectiveWidthDp().dp.toPx() }
                                        val hPx = with(density) { effectivePlacement.effectiveHeightDp().dp.toPx() }
                                        val offsetX = (renderX * widthPx - wPx / 2f).roundToInt()
                                        val offsetY = (renderY * heightPx - hPx / 2f).roundToInt()

                                        Box(
                                            modifier = Modifier.offset { IntOffset(offsetX, offsetY) }
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

                        if (selectedControl.supportsShape) {
                            val currentShape = selectedPlacement.resolvedShape()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Button Shape", style = MaterialTheme.typography.titleSmall)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ButtonShape.entries.forEach { shapeOption ->
                                        FilterChip(
                                            selected = currentShape == shapeOption,
                                            onClick = {
                                                onUpdatePreset(
                                                    preset.updatePlacement(selectedPlacement.copy(shape = shapeOption))
                                                )
                                            },
                                            label = { Text(shapeOption.label) },
                                            modifier = Modifier.testTag("shape_chip_${shapeOption.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedControl.supportsToggle) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Toggle Mode", style = MaterialTheme.typography.titleSmall)
                                Switch(
                                    checked = selectedPlacement.toggleable,
                                    onCheckedChange = { enabled ->
                                        onUpdatePreset(
                                            preset.updatePlacement(selectedPlacement.copy(toggleable = enabled))
                                        )
                                    },
                                    modifier = Modifier.testTag("control_toggle_switch")
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
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
                                steps = GridSnapper.OPACITY_STEPS,
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

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grid Divisions", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${preset.gridRows} rows × $previewCols cols",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val nextRows = (preset.gridRows - 1).coerceIn(
                                            GridSnapper.MIN_GRID_ROWS,
                                            GridSnapper.MAX_GRID_ROWS
                                        )
                                        onUpdatePreset(preset.copy(gridRows = nextRows))
                                    },
                                    enabled = preset.gridRows > GridSnapper.MIN_GRID_ROWS,
                                    modifier = Modifier.testTag("grid_rows_decrease_button")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease vertical grid divisions")
                                }
                                Slider(
                                    value = preset.gridRows.toFloat(),
                                    onValueChange = { raw ->
                                        val nextRows = raw.roundToInt().coerceIn(
                                            GridSnapper.MIN_GRID_ROWS,
                                            GridSnapper.MAX_GRID_ROWS
                                        )
                                        onUpdatePreset(preset.copy(gridRows = nextRows))
                                    },
                                    valueRange = GridSnapper.MIN_GRID_ROWS.toFloat()..GridSnapper.MAX_GRID_ROWS.toFloat(),
                                    steps = GridSnapper.MAX_GRID_ROWS - GridSnapper.MIN_GRID_ROWS - 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grid_rows_slider")
                                )
                                IconButton(
                                    onClick = {
                                        val nextRows = (preset.gridRows + 1).coerceIn(
                                            GridSnapper.MIN_GRID_ROWS,
                                            GridSnapper.MAX_GRID_ROWS
                                        )
                                        onUpdatePreset(preset.copy(gridRows = nextRows))
                                    },
                                    enabled = preset.gridRows < GridSnapper.MAX_GRID_ROWS,
                                    modifier = Modifier.testTag("grid_rows_increase_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase vertical grid divisions")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                                        onValueChange = { raw ->
                                            onUpdatePreset(
                                                preset.copy(fpsLookSensitivity = TouchMouseLookFilter.snapSensitivity(raw))
                                            )
                                        },
                                        valueRange = TouchMouseLookFilter.MIN_SENSITIVITY..TouchMouseLookFilter.MAX_SENSITIVITY,
                                        steps = TouchMouseLookFilter.SENSITIVITY_STEPS,
                                        modifier = Modifier.testTag("look_sensitivity_slider")
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
                                        value = antiDeadzone.coerceIn(
                                            TouchMouseLookFilter.MIN_ANTI_DEADZONE,
                                            TouchMouseLookFilter.MAX_ANTI_DEADZONE
                                        ),
                                        onValueChange = { raw ->
                                            onUpdateAntiDeadzone(TouchMouseLookFilter.snapAntiDeadzone(raw))
                                        },
                                        valueRange = TouchMouseLookFilter.MIN_ANTI_DEADZONE..TouchMouseLookFilter.MAX_ANTI_DEADZONE,
                                        steps = TouchMouseLookFilter.ANTI_DEADZONE_STEPS,
                                        modifier = Modifier.testTag("deadzone_boost_slider")
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
