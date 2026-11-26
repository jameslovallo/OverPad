package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.OverlayRuntimeState
import com.example.model.ControllerPreset
import com.example.shizuku.PhysicalControllerState
import com.example.shizuku.ShizukuStatus
import kotlin.math.roundToInt

@Composable
fun ControlsScreen(
    overlayState: OverlayRuntimeState,
    presets: List<ControllerPreset>,
    shizukuStatus: ShizukuStatus,
    physicalControllerState: PhysicalControllerState = PhysicalControllerState(),
    hasOverlayPermission: Boolean,
    isAppInMultiWindow: Boolean = false,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onSelectPreset: (ControllerPreset) -> Unit,
    onCreatePreset: (basePreset: ControllerPreset, name: String) -> Unit,
    onEditCurrentPreset: (ControllerPreset) -> Unit,
    onImportProfiles: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    var showPresetPicker by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    val importProfilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportProfiles(uri)
        }
    }

    if (showPresetPicker) {
        AlertDialog(
            onDismissRequest = { showPresetPicker = false },
            title = { Text("Presets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presets, key = { it.id }) { item ->
                            val selected = item.id == overlayState.activePreset.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSelectPreset(item)
                                        showPresetPicker = false
                                    }
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
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = item.category.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                showPresetPicker = false
                                newPresetName = "${overlayState.activePreset.name} Custom"
                                showAddPresetDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_preset_from_picker_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                        OutlinedButton(
                            onClick = {
                                showPresetPicker = false
                                importProfilesLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_preset_from_picker_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddPresetDialog) {
        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("New Preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePreset(
                            overlayState.activePreset,
                            newPresetName.trim().ifBlank { "${overlayState.activePreset.name} Custom" }
                        )
                        showAddPresetDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val canActivateOverlay = hasOverlayPermission || isAppInMultiWindow
    val isBlockedByPhysicalController = !overlayState.isRunning && physicalControllerState.isConnected

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        val isCompactLandscape = maxWidth >= 520.dp && maxHeight < 460.dp
        val isCompactHeight = maxHeight < 520.dp
        val heroMaxWidth = when {
            isCompactLandscape -> (maxHeight * 1.15f).coerceIn(190.dp, 310.dp)
            isCompactHeight -> (maxHeight * 0.68f).coerceIn(190.dp, 320.dp)
            else -> 360.dp
        }

        val handleToggleOverlay = {
            if (isBlockedByPhysicalController) {
                // Physical controller connected: main screen trigger cannot activate overlay
            } else if (!canActivateOverlay) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } else if (overlayState.isRunning) {
                onStopOverlay()
            } else {
                onStartOverlay()
            }
        }

        val statusColor = when {
            isBlockedByPhysicalController -> MaterialTheme.colorScheme.tertiary
            !canActivateOverlay -> MaterialTheme.colorScheme.tertiary
            overlayState.isRunning && shizukuStatus.isHardwareSpoofed -> MaterialTheme.colorScheme.secondary
            overlayState.isRunning -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val statusLabel = when {
            isBlockedByPhysicalController -> "Controller Connected"
            !canActivateOverlay -> "Grant Overlay"
            overlayState.isRunning && shizukuStatus.isHardwareSpoofed -> "Connected"
            overlayState.isRunning -> "Active"
            else -> "Disconnected"
        }

        if (isCompactLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                XboxControllerOutlineHero(
                    isOverlayRunning = overlayState.isRunning,
                    isHardwareSpoofed = shizukuStatus.isHardwareSpoofed,
                    hasOverlayPermission = canActivateOverlay,
                    isActivationBlocked = isBlockedByPhysicalController,
                    maxHeroWidth = heroMaxWidth,
                    onToggleOverlay = handleToggleOverlay
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = if (overlayState.isRunning) 0.18f else 0.10f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = if (overlayState.isRunning) 0.55f else 0.25f))
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    if (isBlockedByPhysicalController) {
                        val controllerName = physicalControllerState.primaryName ?: "Physical controller"
                        Text(
                            text = "$controllerName • Overlay disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .testTag("physical_controller_blocked_note")
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showPresetPicker = true },
                            shape = RoundedCornerShape(
                                topStart = 26.dp,
                                bottomStart = 26.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .widthIn(min = 160.dp, max = 240.dp)
                                .testTag("preset_picker_button")
                        ) {
                            Text(
                                text = overlayState.activePreset.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Choose preset",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { onEditCurrentPreset(overlayState.activePreset) },
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp,
                                topEnd = 26.dp,
                                bottomEnd = 26.dp
                            ),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("edit_active_preset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit current layout"
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = if (isCompactHeight) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 16.dp else 28.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                XboxControllerOutlineHero(
                    isOverlayRunning = overlayState.isRunning,
                    isHardwareSpoofed = shizukuStatus.isHardwareSpoofed,
                    hasOverlayPermission = canActivateOverlay,
                    isActivationBlocked = isBlockedByPhysicalController,
                    maxHeroWidth = heroMaxWidth,
                    onToggleOverlay = handleToggleOverlay
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = if (overlayState.isRunning) 0.18f else 0.10f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = if (overlayState.isRunning) 0.55f else 0.25f))
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    if (isBlockedByPhysicalController) {
                        val controllerName = physicalControllerState.primaryName ?: "Physical controller"
                        Text(
                            text = "$controllerName • Overlay disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .testTag("physical_controller_blocked_note")
                        )
                    }
                }

                // Centered Expressive Connected Button Group: [ Preset Picker ▾ ] [ ✎ ]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { showPresetPicker = true },
                        shape = RoundedCornerShape(
                            topStart = 26.dp,
                            bottomStart = 26.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .widthIn(min = 180.dp, max = 260.dp)
                            .testTag("preset_picker_button")
                    ) {
                        Text(
                            text = overlayState.activePreset.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Choose preset",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { onEditCurrentPreset(overlayState.activePreset) },
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 26.dp,
                            bottomEnd = 26.dp
                        ),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("edit_active_preset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit current layout"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XboxControllerOutlineHero(
    isOverlayRunning: Boolean,
    isHardwareSpoofed: Boolean,
    hasOverlayPermission: Boolean,
    isActivationBlocked: Boolean = false,
    maxHeroWidth: androidx.compose.ui.unit.Dp = 360.dp,
    onToggleOverlay: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val activeAccent = if (isHardwareSpoofed) colorScheme.secondary else colorScheme.primary
    val activeContainer = if (isHardwareSpoofed) colorScheme.secondaryContainer else colorScheme.primaryContainer
    val activeOnAccent = if (isHardwareSpoofed) colorScheme.onSecondary else colorScheme.onPrimary

    val bodyFillColor by animateColorAsState(
        targetValue = if (isOverlayRunning) activeContainer else colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "controller_body_fill"
    )
    val bodyStrokeColor by animateColorAsState(
        targetValue = when {
            isOverlayRunning -> activeAccent
            isActivationBlocked -> colorScheme.outlineVariant.copy(alpha = 0.65f)
            else -> colorScheme.outlineVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "controller_body_stroke"
    )
    val bumperFillColor by animateColorAsState(
        targetValue = if (isOverlayRunning) {
            activeAccent.copy(alpha = 0.28f)
        } else {
            colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "controller_bumper_fill"
    )
    val bezelFillColor by animateColorAsState(
        targetValue = if (isOverlayRunning) {
            activeAccent.copy(alpha = 0.20f)
        } else {
            colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "controller_bezel_fill"
    )
    val stickFillColor by animateColorAsState(
        targetValue = if (isOverlayRunning) activeAccent else colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "controller_stick_fill"
    )
    val buttonContainerColor by animateColorAsState(
        targetValue = when {
            isActivationBlocked -> colorScheme.surfaceContainerLow
            !hasOverlayPermission -> colorScheme.tertiaryContainer
            isOverlayRunning -> activeAccent
            else -> colorScheme.surfaceContainerLow
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "center_btn_fill"
    )
    val buttonContentColor by animateColorAsState(
        targetValue = when {
            isActivationBlocked -> colorScheme.onSurface.copy(alpha = 0.38f)
            !hasOverlayPermission -> colorScheme.onTertiaryContainer
            isOverlayRunning -> activeOnAccent
            else -> colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "center_btn_content"
    )
    val buttonBorderColor by animateColorAsState(
        targetValue = when {
            isActivationBlocked -> colorScheme.outlineVariant.copy(alpha = 0.50f)
            !hasOverlayPermission -> colorScheme.tertiary
            isOverlayRunning -> activeOnAccent.copy(alpha = 0.35f)
            else -> colorScheme.outline
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "center_btn_border"
    )
    val buttonElevation by animateDpAsState(
        targetValue = when {
            isActivationBlocked -> 0.dp
            isOverlayRunning -> 10.dp
            else -> 1.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "center_btn_elevation"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isOverlayRunning) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "controller_glow"
    )

    val wellColor = colorScheme.surfaceContainerLowest
    val wellRimColor = if (isOverlayRunning) activeAccent.copy(alpha = 0.45f) else colorScheme.outlineVariant

    val svgPaths = remember { XboxControllerSvgPaths.create() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = maxHeroWidth)
            .aspectRatio(SVG_VIEW_WIDTH / SVG_VIEW_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val scale = minOf(widthPx / SVG_VIEW_WIDTH, heightPx / SVG_VIEW_HEIGHT)
        val drawOriginX = (widthPx - SVG_VIEW_WIDTH * scale) / 2f
        val drawOriginY = (heightPx - SVG_VIEW_HEIGHT * scale) / 2f

        val btnCenterX = drawOriginX + (CENTER_BTN_SVG_X - SVG_MIN_X) * scale
        val btnCenterY = drawOriginY + (CENTER_BTN_SVG_Y - SVG_MIN_Y) * scale
        val btnSize = (maxWidth * 0.21f).coerceIn(58.dp, 74.dp)
        val btnSizePx = with(density) { btnSize.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (glowAlpha > 0.02f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeAccent.copy(alpha = 0.24f * glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(btnCenterX, btnCenterY),
                        radius = size.width * 0.46f
                    ),
                    radius = size.width * 0.46f,
                    center = Offset(btnCenterX, btnCenterY)
                )
            }

            val tx = drawOriginX - SVG_MIN_X * scale
            val ty = drawOriginY - SVG_MIN_Y * scale
            val strokeInSvg = (1.5.dp.toPx() / scale).coerceAtLeast(0.25f)

            withTransform({
                translate(left = tx, top = ty)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // Soft recessed wells behind cutouts (sticks, D-pad, ABXY)
                drawCircle(color = wellColor, radius = 6.9f, center = Offset(26.107f, 38.931f))
                drawCircle(color = wellColor, radius = 6.9f, center = Offset(60.891f, 52.819f))
                drawCircle(color = wellColor, radius = 7.4f, center = Offset(37.44f, 53.67f))

                // Subtle ABXY Monet-harmonized pads inside face button cutouts
                val abxyColors = if (isOverlayRunning) {
                    listOf(
                        Offset(72.48f, 33.09f) to colorScheme.tertiary,
                        Offset(66.146f, 39.09f) to colorScheme.primary,
                        Offset(72.48f, 45.25f) to colorScheme.secondary,
                        Offset(78.64f, 39.09f) to colorScheme.error
                    )
                } else {
                    val mutedWell = colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                    listOf(
                        Offset(72.48f, 33.09f) to mutedWell,
                        Offset(66.146f, 39.09f) to mutedWell,
                        Offset(72.48f, 45.25f) to mutedWell,
                        Offset(78.64f, 39.09f) to mutedWell
                    )
                }
                abxyColors.forEach { (center, tint) ->
                    drawCircle(color = wellColor, radius = 3.25f, center = center)
                    drawCircle(color = tint.copy(alpha = if (isOverlayRunning) 0.85f else 1f), radius = 2.65f, center = center)
                }

                // Shoulder bumpers
                drawPath(path = svgPaths.leftBumper, color = bumperFillColor)
                drawPath(
                    path = svgPaths.leftBumper,
                    color = bodyStrokeColor,
                    style = Stroke(width = strokeInSvg * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(path = svgPaths.rightBumper, color = bumperFillColor)
                drawPath(
                    path = svgPaths.rightBumper,
                    color = bodyStrokeColor,
                    style = Stroke(width = strokeInSvg * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Top center bezel
                drawPath(path = svgPaths.centerBezel, color = bezelFillColor)
                drawPath(
                    path = svgPaths.centerBezel,
                    color = bodyStrokeColor,
                    style = Stroke(width = strokeInSvg * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Main controller body silhouette (EvenOdd punches out stick/D-pad/ABXY wells)
                drawPath(path = svgPaths.body, color = bodyFillColor)
                drawPath(
                    path = svgPaths.body,
                    color = bodyStrokeColor,
                    style = Stroke(width = strokeInSvg, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Thumbstick domes
                drawCircle(color = stickFillColor, radius = 4.779f, center = Offset(26.107f, 38.931f))
                drawCircle(
                    color = wellRimColor,
                    radius = 4.779f,
                    center = Offset(26.107f, 38.931f),
                    style = Stroke(width = strokeInSvg * 0.65f)
                )
                drawCircle(color = stickFillColor, radius = 4.779f, center = Offset(60.891f, 52.819f))
                drawCircle(
                    color = wellRimColor,
                    radius = 4.779f,
                    center = Offset(60.891f, 52.819f),
                    style = Stroke(width = strokeInSvg * 0.65f)
                )

                // Subtle recessed seat ring behind the center toggle button
                drawCircle(color = wellColor, radius = 10.6f, center = Offset(CENTER_BTN_SVG_X, CENTER_BTN_SVG_Y))
            }
        }

        // Large Center Material 3 Circular Toggle Button
        val toggleDescription = when {
            isActivationBlocked -> "Physical controller connected; overlay activation disabled"
            !hasOverlayPermission -> "Grant overlay permission"
            isOverlayRunning -> "Deactivate OverPad overlay"
            else -> "Activate OverPad overlay"
        }

        Surface(
            onClick = onToggleOverlay,
            enabled = !isActivationBlocked,
            shape = CircleShape,
            color = buttonContainerColor,
            contentColor = buttonContentColor,
            shadowElevation = buttonElevation,
            tonalElevation = if (isOverlayRunning) 6.dp else 0.dp,
            border = BorderStroke(
                width = if (isOverlayRunning) 2.dp else 1.75.dp,
                color = buttonBorderColor
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (btnCenterX - btnSizePx / 2f).roundToInt(),
                        y = (btnCenterY - btnSizePx / 2f).roundToInt()
                    )
                }
                .size(btnSize)
                .testTag("toggle_overlay_button")
                .semantics {
                    contentDescription = toggleDescription
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = Triple(hasOverlayPermission, isOverlayRunning, isActivationBlocked),
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.80f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            .togetherWith(
                                fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleOut(targetScale = 0.80f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                            )
                    },
                    label = "center_toggle_icon"
                ) { (hasPerm, running, blocked) ->
                    val icon = when {
                        blocked -> Icons.Rounded.PowerSettingsNew
                        !hasPerm -> Icons.Outlined.Layers
                        running -> Icons.Filled.SportsEsports
                        else -> Icons.Rounded.PowerSettingsNew
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(btnSize * 0.46f)
                    )
                }
            }
        }
    }
}

private const val SVG_MIN_X = 3.5f
private const val SVG_MIN_Y = 19.0f
private const val SVG_VIEW_WIDTH = 92.0f
private const val SVG_VIEW_HEIGHT = 64.0f
private const val CENTER_BTN_SVG_X = 49.2f
private const val CENTER_BTN_SVG_Y = 35.0f

private class XboxControllerSvgPaths(
    val leftBumper: Path,
    val rightBumper: Path,
    val centerBezel: Path,
    val body: Path
) {
    companion object {
        private fun parse(data: String): Path =
            PathParser()
                .parsePathString(data)
                .toPath()

        fun create(): XboxControllerSvgPaths {
            val leftBumper = parse(
                "M37.181,22.164c-0.385-0.093-0.757-0.239-1.103-0.438l-2.909-1.674c0,0-5.168-0.795-13.773,3.767" +
                    "c-0.556,0.295-0.999,0.767-1.249,1.345c-0.604,1.393-1.122,2.429-1.122,2.429l0.023,0.075" +
                    "c5.347-2.937,10.397-3.932,12.969-4.265c1.079-0.14,2.316,0.267,3.04,1.079l1.707,1.91L37.181,22.164z"
            )
            val rightBumper = parse(
                "M81.333,27.594c0,0-0.518-1.036-1.122-2.429c-0.25-0.577-0.693-1.05-1.249-1.345" +
                    "c-8.605-4.562-13.773-3.767-13.773-3.767l-2.909,1.674c-0.339,0.195-0.703,0.339-1.079,0.432" +
                    "l2.41,4.214l1.689-1.89c0.725-0.811,1.962-1.218,3.04-1.079c2.586,0.335,7.677,1.339,13.055,4.312L81.333,27.594z"
            )
            val centerBezel = parse(
                "M60.593,22.262c-0.131,0.013-0.263,0.021-0.396,0.021H49.418h-0.159H38.162" +
                    "c-0.13,0-0.258-0.008-0.387-0.02l-2.613,4.574l4.415,4.942c0.67,0.75,1.627,1.178,2.633,1.178" +
                    "h6.81h0.159h6.969c1.005,0,1.963-0.429,2.633-1.178l4.423-4.95L60.593,22.262z"
            )
            val body = parse(
                "M92.776,56.773c-0.558-2.629-7.169-24.055-10.036-27.48c-5.86-3.584-11.598-4.73-14.399-5.093" +
                    "c-1.079-0.14-2.156,0.267-2.881,1.079l-6.519,7.296c-0.67,0.75-1.627,1.178-2.633,1.178h-7.129H49.02h-6.969" +
                    "c-1.005,0-1.963-0.429-2.633-1.178l-6.519-7.296c-0.725-0.811-1.802-1.218-2.881-1.079" +
                    "c-2.801,0.362-8.539,1.508-14.399,5.093c-2.868,3.425-9.479,24.852-10.036,27.48" +
                    "s-6.133,22.064,7.647,24.692c8.204-5.655,11.55-12.107,17.763-15.134" +
                    "c3.809-0.693,8.257-0.478,18.187-0.478h0.159c9.93,0,14.219-0.215,18.028,0.478" +
                    "c6.213,3.027,9.558,9.479,17.763,15.134C98.91,78.837,93.334,59.402,92.776,56.773z" +
                    "M26.107,45.701c-3.739,0-6.771-3.031-6.771-6.771s3.031-6.771,6.771-6.771s6.771,3.031,6.771,6.771S29.846,45.701,26.107,45.701z" +
                    "M43.975,55.113c0,0.418-0.339,0.758-0.758,0.758H40.03c-0.211,0-0.383,0.171-0.383,0.383v3.188" +
                    "c0,0.418-0.339,0.758-0.758,0.758h-2.892c-0.418,0-0.758-0.339-0.758-0.758v-3.188c0-0.211-0.171-0.383-0.383-0.383H31.67" +
                    "c-0.418,0-0.758-0.339-0.758-0.758v-2.892c0-0.418,0.339-0.758,0.758-0.758h3.188c0.211,0,0.383-0.171,0.383-0.383v-3.188" +
                    "c0-0.418,0.339-0.758,0.758-0.758h2.892c0.418,0,0.758,0.339,0.758,0.758v3.188c0,0.211,0.171,0.383,0.383,0.383h3.188" +
                    "c0.418,0,0.758,0.339,0.758,0.758V55.113z" +
                    "M72.48,30.01c1.701,0,3.08,1.379,3.08,3.08c0,1.701-1.379,3.08-3.08,3.08c-1.701,0-3.08-1.379-3.08-3.08C69.4,31.389,70.779,30.01,72.48,30.01z" +
                    "M60.891,59.59c-3.739,0-6.771-3.031-6.771-6.771c0-3.739,3.031-6.771,6.771-6.771c3.739,0,6.771,3.031,6.771,6.771C67.662,56.559,64.631,59.59,60.891,59.59z" +
                    "M66.146,42.17c-1.701,0-3.08-1.379-3.08-3.08s1.379-3.08,3.08-3.08c1.701,0,3.08,1.379,3.08,3.08S67.847,42.17,66.146,42.17z" +
                    "M72.48,48.33c-1.701,0-3.08-1.379-3.08-3.08c0-1.701,1.379-3.08,3.08-3.08c1.701,0,3.08,1.379,3.08,3.08C75.56,46.951,74.181,48.33,72.48,48.33z" +
                    "M78.64,42.17c-1.701,0-3.08-1.379-3.08-3.08s1.379-3.08,3.08-3.08c1.701,0,3.08,1.379,3.08,3.08S80.341,42.17,78.64,42.17z"
            ).apply {
                fillType = PathFillType.EvenOdd
            }
            return XboxControllerSvgPaths(
                leftBumper = leftBumper,
                rightBumper = rightBumper,
                centerBezel = centerBezel,
                body = body
            )
        }
    }
}
