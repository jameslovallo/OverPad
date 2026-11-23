package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.ui.gamepad.FpsRightLookZoneSurface
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.LeftMoveZoneSurface
import com.example.xinput.TouchMouseLookFilter
import com.example.xinput.XInputControllerEngine
import com.example.xinput.XInputState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SandboxScreen(
    presets: List<ControllerPreset>,
    activePreset: ControllerPreset,
    xInputState: XInputState,
    xInputEngine: XInputControllerEngine,
    hapticsEnabled: Boolean,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onSelectPreset: (ControllerPreset) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    var reticleX by remember { mutableFloatStateOf(0.5f) }
    var reticleY by remember { mutableFloatStateOf(0.5f) }

    if (onNavigateBack != null) {
        BackHandler(onBack = onNavigateBack)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val s = xInputEngine.state.value
            if (kotlin.math.abs(s.thumbRX) > 0.02f || kotlin.math.abs(s.thumbRY) > 0.02f) {
                reticleX = (reticleX + s.thumbRX * 0.022f).coerceIn(0.06f, 0.94f)
                reticleY = (reticleY + s.thumbRY * 0.022f).coerceIn(0.08f, 0.92f)
            }
            delay(16L)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onNavigateBack != null) {
            TopAppBar(
                title = { Text("Test Pad", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sandbox_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            reticleX = 0.5f
                            reticleY = 0.5f
                            xInputEngine.resetAll()
                        },
                        modifier = Modifier.testTag("reset_reticle_button")
                    ) {
                        Text("Center")
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
            // Preset Selector Strip
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { item ->
                        FilterChip(
                            selected = item.id == activePreset.id,
                            onClick = { onSelectPreset(item) },
                            label = { Text(item.name) }
                        )
                    }
                }
            }

            // Interactive Test Pad + Live Reticle Canvas
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    val density = LocalDensity.current
                    val crosshairColor = MaterialTheme.colorScheme.outlineVariant
                    val stickOrbColor = MaterialTheme.colorScheme.secondary
                    val aimDefaultColor = MaterialTheme.colorScheme.primary
                    val aimAdsColor = MaterialTheme.colorScheme.tertiary
                    val aimFireColor = MaterialTheme.colorScheme.error

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .aspectRatio(16f / 9.5f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    ) {
                        val widthPx = with(density) { maxWidth.toPx() }
                        val heightPx = with(density) { maxHeight.toPx() }
                        val canvasScale = (maxWidth / 760.dp).coerceIn(0.45f, 0.95f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawLine(
                                color = crosshairColor,
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = crosshairColor,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 1.dp.toPx()
                            )

                            val lsCenter = Offset(
                                x = size.width * 0.5f + xInputState.thumbLX * (size.width * 0.18f),
                                y = size.height * 0.5f + xInputState.thumbLY * (size.height * 0.18f)
                            )
                            drawCircle(
                                color = stickOrbColor.copy(alpha = 0.28f),
                                radius = 16.dp.toPx(),
                                center = lsCenter
                            )
                            drawCircle(
                                color = stickOrbColor,
                                radius = 6.dp.toPx(),
                                center = lsCenter
                            )

                            val aimPos = Offset(reticleX * size.width, reticleY * size.height)
                            val adsActive = xInputState.leftTrigger > 0.2f
                            val fireActive = xInputState.rightTrigger > 0.2f
                            val reticleRadius = if (adsActive) 15.dp.toPx() else 24.dp.toPx()
                            val reticleColor = when {
                                fireActive -> aimFireColor
                                adsActive -> aimAdsColor
                                else -> aimDefaultColor
                            }
                            drawCircle(
                                color = reticleColor,
                                radius = reticleRadius,
                                center = aimPos,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawLine(
                                color = reticleColor,
                                start = aimPos - Offset(reticleRadius + 7.dp.toPx(), 0f),
                                end = aimPos + Offset(reticleRadius + 7.dp.toPx(), 0f),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawLine(
                                color = reticleColor,
                                start = aimPos - Offset(0f, reticleRadius + 7.dp.toPx()),
                                end = aimPos + Offset(0f, reticleRadius + 7.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            if (fireActive) {
                                drawCircle(
                                    color = aimFireColor.copy(alpha = 0.28f),
                                    radius = reticleRadius * 1.45f,
                                    center = aimPos
                                )
                            }
                        }

                        LeftMoveZoneSurface(
                            preset = activePreset,
                            engine = xInputEngine,
                            showGuideOverlay = true,
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight()
                                .align(Alignment.CenterStart)
                        )

                        FpsRightLookZoneSurface(
                            preset = activePreset,
                            engine = xInputEngine,
                            showGuideOverlay = true,
                            antiDeadzone = antiDeadzone,
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd)
                        )

                        activePreset.controls.filter { it.visible && it.type != ControlType.SETTINGS }.forEach { placement ->
                            val scaled = placement.copy(scale = placement.scale * canvasScale)
                            val wPx = with(density) { (scaled.type.baseWidthDp * scaled.scale).dp.toPx() }
                            val hPx = with(density) { (scaled.type.baseHeightDp * scaled.scale).dp.toPx() }
                            val offsetX = (placement.xFraction * widthPx - wPx / 2f).roundToInt()
                            val offsetY = (placement.yFraction * heightPx - hPx / 2f).roundToInt()

                            Box(modifier = Modifier.offset { IntOffset(offsetX, offsetY) }) {
                                GamepadControlNode(
                                    placement = scaled,
                                    preset = activePreset,
                                    engine = xInputEngine,
                                    interactive = true,
                                    hapticsEnabled = hapticsEnabled,
                                    antiDeadzone = antiDeadzone
                                )
                            }
                        }
                    }
                }
            }

            // Live XInput Report Telemetry
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("XInput Telemetry", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "#${xInputState.packetCounter}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = xInputState.toHexReportSummary(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (xInputState.activeSources.isEmpty() && xInputState.activeKeyCodes.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "IDLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                xInputState.activeSources.forEach { src ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = src,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                xInputState.activeKeyCodes.forEach { code ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Text(
                                            text = XInputState.keyCodeDisplay(code),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AxisMeterRow("LX", xInputState.thumbLX, "${xInputState.shortLX}")
                                AxisMeterRow("LY", xInputState.thumbLY, "${xInputState.shortLY}")
                                AxisMeterRow("LT", xInputState.leftTrigger, "${xInputState.leftTriggerByte}/255", isTrigger = true)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AxisMeterRow("RX", xInputState.thumbRX, "${xInputState.shortRX}")
                                AxisMeterRow("RY", xInputState.thumbRY, "${xInputState.shortRY}")
                                AxisMeterRow("RT", xInputState.rightTrigger, "${xInputState.rightTriggerByte}/255", isTrigger = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisMeterRow(
    label: String,
    value: Float,
    rawDisplay: String,
    isTrigger: Boolean = false
) {
    val progress = if (isTrigger) value.coerceIn(0f, 1f) else ((value + 1f) / 2f).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(rawDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isTrigger && value > 0.1f) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
