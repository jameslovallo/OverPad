package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.ui.gamepad.FpsRightLookZoneSurface
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
import com.example.xinput.TouchMouseLookFilter
import com.example.xinput.XInputControllerEngine
import com.example.xinput.XInputState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SandboxScreen(
    presets: List<ControllerPreset>,
    activePreset: ControllerPreset,
    xInputState: XInputState,
    xInputEngine: XInputControllerEngine,
    hapticsEnabled: Boolean,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onSelectPreset: (ControllerPreset) -> Unit
) {
    // Simulated camera yaw/pitch reticle driven directly by Right Stick (including FPS drag + Trigger Hold+Drag!)
    var reticleX by remember { mutableFloatStateOf(0.5f) }
    var reticleY by remember { mutableFloatStateOf(0.5f) }

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset Selector Strip
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("XINPUT SANDBOX & FPS TEST BENCH", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                        TextButton(
                            onClick = {
                                reticleX = 0.5f
                                reticleY = 0.5f
                                xInputEngine.resetAll()
                            },
                            modifier = Modifier.testTag("reset_reticle_button")
                        ) {
                            Text("Center Aim")
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { item ->
                            FilterChip(
                                selected = item.id == activePreset.id,
                                onClick = { onSelectPreset(item) },
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

        // Interactive Test Pad + Live Reticle Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (activePreset.fpsRightZoneEnabled && activePreset.triggerDragAimEnabled) {
                            "Try: Drag right side to aim • Hold RT/LT and drag in any direction to fire + aim simultaneously!"
                        } else {
                            "Multi-touch test arena: test thumbsticks, D-Pad, triggers, and buttons simultaneously."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    val density = LocalDensity.current
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9.5f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF060A12))
                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    ) {
                        val widthPx = with(density) { maxWidth.toPx() }
                        val heightPx = with(density) { maxHeight.toPx() }
                        val canvasScale = (maxWidth / 760.dp).coerceIn(0.45f, 0.95f)

                        // Background Reticle & Movement Target Field
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Subtle tactical horizon crosshair
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Left Stick Position Orb
                            val lsCenter = Offset(
                                x = size.width * 0.5f + xInputState.thumbLX * (size.width * 0.18f),
                                y = size.height * 0.5f + xInputState.thumbLY * (size.height * 0.18f)
                            )
                            drawCircle(
                                color = TacticalEmerald.copy(alpha = 0.30f),
                                radius = 16.dp.toPx(),
                                center = lsCenter
                            )
                            drawCircle(
                                color = TacticalEmerald,
                                radius = 6.dp.toPx(),
                                center = lsCenter
                            )

                            // Right Stick Driven Aim Reticle
                            val aimPos = Offset(reticleX * size.width, reticleY * size.height)
                            val adsActive = xInputState.leftTrigger > 0.2f
                            val fireActive = xInputState.rightTrigger > 0.2f
                            val reticleRadius = if (adsActive) 15.dp.toPx() else 24.dp.toPx()
                            val reticleColor = when {
                                fireActive -> CrimsonAlert
                                adsActive -> TriggerAmber
                                else -> ElectricCyan
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
                                    color = CrimsonAlert.copy(alpha = 0.28f),
                                    radius = reticleRadius * 1.45f,
                                    center = aimPos
                                )
                            }
                        }

                        // Right-Side FPS Look Zone behind controls
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

                        // Interactive Controls (excluding SETTINGS or keeping SETTINGS harmless)
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
        }

        // Live XInput 1.4 Telemetry Inspector
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LIVE XINPUT REPORT TELEMETRY", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "PKT #${xInputState.packetCounter}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }

                    // 20-byte XInput Report Hex
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianBg,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Text(
                            text = xInputState.toHexReportSummary(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TacticalEmerald,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    // Active Sources & KeyCodes
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (xInputState.activeSources.isEmpty() && xInputState.activeKeyCodes.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SlateSurfaceElevated
                            ) {
                                Text(
                                    text = "IDLE (0x0000)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            xInputState.activeSources.forEach { src ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ElectricCyan)
                                ) {
                                    Text(
                                        text = src,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            xInputState.activeKeyCodes.forEach { code ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TriggerAmber.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, TriggerAmber)
                                ) {
                                    Text(
                                        text = XInputState.keyCodeDisplay(code),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TriggerAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Axes Readouts
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            Text(rawDisplay, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isTrigger && value > 0.1f) CrimsonAlert else ElectricCyan,
            trackColor = SlateSurfaceElevated
        )
    }
}
