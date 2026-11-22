package com.example.ui.gamepad

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.PresetCategory
import com.example.model.TriggerDragMode
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TriggerAmber
import com.example.ui.theme.XboxAmber
import com.example.ui.theme.XboxBlue
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxRed
import com.example.xinput.DPadDirectionState
import com.example.xinput.TouchMouseLookFilter
import com.example.xinput.XInputControllerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GamepadControlNode(
    placement: ControlPlacement,
    preset: ControllerPreset,
    engine: XInputControllerEngine?,
    interactive: Boolean,
    isSelectedInEditor: Boolean = false,
    hapticsEnabled: Boolean = true,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val type = placement.type
    val widthDp = (type.baseWidthDp * placement.scale).dp
    val heightDp = (type.baseHeightDp * placement.scale).dp
    val view = LocalView.current

    fun performHaptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    val selectionModifier = if (isSelectedInEditor) {
        Modifier.border(2.dp, ElectricCyan, RoundedCornerShape(14.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .alpha(if (placement.visible) preset.opacity else 0.25f)
            .then(selectionModifier)
            .semantics { contentDescription = type.displayName },
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            ControlType.LEFT_STICK, ControlType.RIGHT_STICK -> {
                val isFpsRightMouse = type == ControlType.RIGHT_STICK &&
                    (preset.category == PresetCategory.FPS || preset.fpsRightZoneEnabled)
                ThumbstickWidget(
                    isLeft = type == ControlType.LEFT_STICK,
                    scale = placement.scale,
                    interactive = interactive,
                    invertY = (type == ControlType.RIGHT_STICK && preset.invertRightY),
                    mouseLookMode = isFpsRightMouse,
                    sensitivity = preset.fpsLookSensitivity,
                    antiDeadzone = antiDeadzone,
                    onDeflect = { x, y ->
                        if (type == ControlType.LEFT_STICK) {
                            engine?.updateLeftStick(x, y)
                        } else {
                            engine?.updateRightStickDirect(x, y)
                        }
                    },
                    onTouchDown = { performHaptic() }
                )
            }

            ControlType.DPAD -> {
                DPadWidget(
                    interactive = interactive,
                    onDirectionChanged = { dir -> engine?.updateDPad(dir) },
                    onTouchDown = { performHaptic() }
                )
            }

            ControlType.TRIGGER_LT, ControlType.TRIGGER_RT -> {
                TriggerDragAimWidget(
                    controlType = type,
                    preset = preset,
                    interactive = interactive,
                    antiDeadzone = antiDeadzone,
                    onUpdate = { pressed, dragX, dragY ->
                        engine?.updateTriggerWithDragAim(type, pressed, dragX, dragY)
                    },
                    onTouchDown = { performHaptic() }
                )
            }

            ControlType.BUTTON_A, ControlType.BUTTON_B, ControlType.BUTTON_X, ControlType.BUTTON_Y -> {
                FaceButtonWidget(
                    controlType = type,
                    scale = placement.scale,
                    interactive = interactive,
                    onPressChanged = { pressed ->
                        if (pressed) performHaptic()
                        engine?.setButtonPressed(type, pressed)
                    }
                )
            }

            ControlType.SETTINGS -> {
                SettingsOverlayButton(
                    scale = placement.scale,
                    interactive = interactive,
                    onClick = {
                        performHaptic()
                        onSettingsClick?.invoke()
                    }
                )
            }

            else -> {
                CapsuleGamepadButton(
                    controlType = type,
                    scale = placement.scale,
                    interactive = interactive,
                    onPressChanged = { pressed ->
                        if (pressed) performHaptic()
                        engine?.setButtonPressed(type, pressed)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThumbstickWidget(
    isLeft: Boolean,
    scale: Float,
    interactive: Boolean,
    invertY: Boolean,
    mouseLookMode: Boolean = false,
    sensitivity: Float = 1.45f,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onDeflect: (Float, Float) -> Unit,
    onTouchDown: () -> Unit
) {
    var normX by remember { mutableFloatStateOf(0f) }
    var normY by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val mouseFilter = remember { TouchMouseLookFilter() }
    val currentInvertY by rememberUpdatedState(invertY)
    val currentSensitivity by rememberUpdatedState(sensitivity)
    val currentAntiDeadzone by rememberUpdatedState(antiDeadzone)
    val currentOnDeflect by rememberUpdatedState(onDeflect)

    DisposableEffect(Unit) {
        onDispose {
            mouseFilter.reset()
        }
    }

    val accent = when {
        isLeft -> ElectricCyan
        mouseLookMode -> ElectricCyan
        else -> TacticalEmerald
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(isLeft, mouseLookMode) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                var prevPos = Offset.Zero
                var decayJob: Job? = null

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            active = true
                            onTouchDown()
                            if (mouseLookMode) {
                                prevPos = down.position
                                mouseFilter.onPointerDown(down.uptimeMillis)
                                normX = 0f
                                normY = 0f
                                currentOnDeflect(0f, 0f)
                                decayJob?.cancel()
                                decayJob = scope.launch {
                                    while (isActive && activePointerId != null) {
                                        delay(12L)
                                        val decayed = mouseFilter.decayIfIdle(
                                            nowUptimeMs = SystemClock.uptimeMillis(),
                                            sensitivity = currentSensitivity,
                                            antiDeadzone = currentAntiDeadzone
                                        )
                                        if (decayed != null) {
                                            normX = decayed.first
                                            normY = decayed.second
                                            currentOnDeflect(
                                                decayed.first,
                                                if (currentInvertY) -decayed.second else decayed.second
                                            )
                                        }
                                    }
                                }
                            } else {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxRadius = (minOf(size.width, size.height) * 0.36f).coerceAtLeast(1f)
                                val delta = down.position - center
                                val dist = hypot(delta.x, delta.y)
                                val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                                normX = (clamped.x / maxRadius).coerceIn(-1f, 1f)
                                normY = (clamped.y / maxRadius).coerceIn(-1f, 1f)
                                currentOnDeflect(normX, if (currentInvertY) -normY else normY)
                            }
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            decayJob?.cancel()
                            decayJob = null
                            mouseFilter.reset()
                            active = false
                            normX = 0f
                            normY = 0f
                            currentOnDeflect(0f, 0f)
                            change?.consume()
                        } else {
                            if (mouseLookMode) {
                                val deltaPx = change.position - prevPos
                                prevPos = change.position
                                val (rx, ryRaw) = mouseFilter.onPointerMove(
                                    dxDp = deltaPx.x / density.density,
                                    dyDp = deltaPx.y / density.density,
                                    uptimeMs = change.uptimeMillis,
                                    sensitivity = currentSensitivity,
                                    antiDeadzone = currentAntiDeadzone
                                )
                                normX = rx
                                normY = ryRaw
                                currentOnDeflect(rx, if (currentInvertY) -ryRaw else ryRaw)
                            } else {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val maxRadius = (minOf(size.width, size.height) * 0.36f).coerceAtLeast(1f)
                                val delta = change.position - center
                                val dist = hypot(delta.x, delta.y)
                                val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                                normX = (clamped.x / maxRadius).coerceIn(-1f, 1f)
                                normY = (clamped.y / maxRadius).coerceIn(-1f, 1f)
                                currentOnDeflect(normX, if (currentInvertY) -normY else normY)
                            }
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    Box(modifier = Modifier.fillMaxSize().then(touchMod), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val outerRadius = minDim * 0.46f
            val travelRadius = minDim * 0.28f
            val capRadius = minDim * 0.23f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Recessed thumbstick gate
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0A0F1A), Color(0xFF1A2438)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = if (active) accent else Color(0xFF3E4F6E),
                radius = outerRadius,
                center = center,
                style = Stroke(width = (2.2f * scale).dp.toPx())
            )

            // Subtle 8-way cardinal ticks inside gate (or crosshair ticks in mouse-look mode)
            for (i in 0 until 8) {
                val angle = (i * Math.PI / 4.0).toFloat()
                val inner = outerRadius * 0.80f
                val outer = outerRadius * 0.94f
                drawLine(
                    color = if (mouseLookMode) accent.copy(alpha = 0.45f) else Color(0xFF475569).copy(alpha = 0.65f),
                    start = center + Offset(cos(angle) * inner, sin(angle) * inner),
                    end = center + Offset(cos(angle) * outer, sin(angle) * outer),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Cap position
            val capCenter = center + Offset(normX * travelRadius, normY * travelRadius)

            // Active stick stalk / vector glow
            if (active && hypot(normX, normY) > 0.05f) {
                drawLine(
                    color = accent.copy(alpha = 0.7f),
                    start = center,
                    end = capCenter,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Physical thumbstick cap with concave grip rings
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF151E2E)),
                    center = capCenter,
                    radius = capRadius
                ),
                radius = capRadius,
                center = capCenter
            )
            drawCircle(
                color = if (active) accent else Color(0xFF64748B),
                radius = capRadius,
                center = capCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF0F172A).copy(alpha = 0.7f),
                radius = capRadius * 0.66f,
                center = capCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = accent.copy(alpha = if (active) 0.9f else 0.35f),
                radius = capRadius * 0.24f,
                center = capCenter
            )
        }
    }
}

@Composable
private fun DPadWidget(
    interactive: Boolean,
    onDirectionChanged: (DPadDirectionState) -> Unit,
    onTouchDown: () -> Unit
) {
    var dpadState by remember { mutableStateOf(DPadDirectionState()) }

    fun computeDirection(pos: Offset, width: Int, height: Int): DPadDirectionState {
        val cx = width / 2f
        val cy = height / 2f
        val dx = pos.x - cx
        val dy = pos.y - cy
        val radius = (minOf(width, height) / 2f).coerceAtLeast(1f)
        if (hypot(dx, dy) < radius * 0.18f) {
            return DPadDirectionState()
        }
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        // 8-way directional sectors (each cardinal spans +/- 67.5 deg so diagonals overlap naturally)
        val right = angleDeg in -67.5..67.5
        val down = angleDeg in 22.5..157.5
        val up = angleDeg in -157.5..-22.5
        val left = angleDeg >= 112.5 || angleDeg <= -112.5
        return DPadDirectionState(up = up, down = down, left = left, right = right)
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            onTouchDown()
                            val next = computeDirection(down.position, size.width, size.height)
                            dpadState = next
                            onDirectionChanged(next)
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            val cleared = DPadDirectionState()
                            dpadState = cleared
                            onDirectionChanged(cleared)
                            change?.consume()
                        } else {
                            val next = computeDirection(change.position, size.width, size.height)
                            if (next != dpadState) {
                                dpadState = next
                                onDirectionChanged(next)
                            }
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    Box(modifier = Modifier.fillMaxSize().then(touchMod), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val armHalf = w * 0.16f
            val outerMargin = w * 0.08f
            val corner = w * 0.06f

            // Circular backing dish
            drawCircle(
                color = Color(0xFF0E1525).copy(alpha = 0.85f),
                radius = w * 0.46f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF2B3A55),
                radius = w * 0.46f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Sculpted physical Cross ("+") silhouette
            val crossPath = Path().apply {
                moveTo(cx - armHalf, outerMargin + corner)
                lineTo(cx - armHalf, cy - armHalf)
                lineTo(outerMargin + corner, cy - armHalf)
                lineTo(outerMargin, cy - armHalf + corner)
                lineTo(outerMargin, cy + armHalf - corner)
                lineTo(outerMargin + corner, cy + armHalf)
                lineTo(cx - armHalf, cy + armHalf)
                lineTo(cx - armHalf, h - outerMargin - corner)
                lineTo(cx - armHalf + corner, h - outerMargin)
                lineTo(cx + armHalf - corner, h - outerMargin)
                lineTo(cx + armHalf, h - outerMargin - corner)
                lineTo(cx + armHalf, cy + armHalf)
                lineTo(w - outerMargin - corner, cy + armHalf)
                lineTo(w - outerMargin, cy + armHalf - corner)
                lineTo(w - outerMargin, cy - armHalf + corner)
                lineTo(w - outerMargin - corner, cy - armHalf)
                lineTo(cx + armHalf, cy - armHalf)
                lineTo(cx + armHalf, outerMargin + corner)
                lineTo(cx + armHalf - corner, outerMargin)
                lineTo(cx - armHalf + corner, outerMargin)
                close()
            }

            drawPath(
                path = crossPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF293548), Color(0xFF172033))
                )
            )
            drawPath(
                path = crossPath,
                color = Color(0xFF64748B),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Active arm highlight pads
            fun drawArmHighlight(active: Boolean, topLeft: Offset, boxSize: Size) {
                if (!active) return
                drawRoundRect(
                    color = ElectricCyan.copy(alpha = 0.42f),
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            val armLen = (cy - armHalf) - outerMargin
            drawArmHighlight(dpadState.up, Offset(cx - armHalf * 0.85f, outerMargin + 2f), Size(armHalf * 1.7f, armLen))
            drawArmHighlight(dpadState.down, Offset(cx - armHalf * 0.85f, cy + armHalf), Size(armHalf * 1.7f, armLen))
            drawArmHighlight(dpadState.left, Offset(outerMargin + 2f, cy - armHalf * 0.85f), Size(armLen, armHalf * 1.7f))
            drawArmHighlight(dpadState.right, Offset(cx + armHalf, cy - armHalf * 0.85f), Size(armLen, armHalf * 1.7f))

            // Directional chevrons
            val chevDist = w * 0.29f
            val chevSize = w * 0.055f
            fun drawTriangle(tip: Offset, base1: Offset, base2: Offset, lit: Boolean) {
                val p = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(base1.x, base1.y)
                    lineTo(base2.x, base2.y)
                    close()
                }
                drawPath(p, color = if (lit) ElectricCyan else Color(0xFF94A3B8))
            }
            drawTriangle(Offset(cx, cy - chevDist - chevSize), Offset(cx - chevSize, cy - chevDist + chevSize * 0.5f), Offset(cx + chevSize, cy - chevDist + chevSize * 0.5f), dpadState.up)
            drawTriangle(Offset(cx, cy + chevDist + chevSize), Offset(cx - chevSize, cy + chevDist - chevSize * 0.5f), Offset(cx + chevSize, cy + chevDist - chevSize * 0.5f), dpadState.down)
            drawTriangle(Offset(cx - chevDist - chevSize, cy), Offset(cx - chevDist + chevSize * 0.5f, cy - chevSize), Offset(cx - chevDist + chevSize * 0.5f, cy + chevSize), dpadState.left)
            drawTriangle(Offset(cx + chevDist + chevSize, cy), Offset(cx + chevDist - chevSize * 0.5f, cy - chevSize), Offset(cx + chevDist - chevSize * 0.5f, cy + chevSize), dpadState.right)

            // Center pivot dimple
            drawCircle(
                color = Color(0xFF0F172A),
                radius = armHalf * 0.45f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun TriggerDragAimWidget(
    controlType: ControlType,
    preset: ControllerPreset,
    interactive: Boolean,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onUpdate: (pressed: Boolean, dragX: Float, dragY: Float) -> Unit,
    onTouchDown: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var dragVecX by remember { mutableFloatStateOf(0f) }
    var dragVecY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeFilter = remember { TouchMouseLookFilter() }
    val dragRadiusPx = with(density) { preset.triggerDragRadiusDp.coerceAtLeast(32f).dp.toPx() }
    val isRt = controlType == ControlType.TRIGGER_RT
    val activeColor = if (isRt) XboxRed else TriggerAmber

    val currentSensitivity by rememberUpdatedState(preset.fpsLookSensitivity)
    val currentInvertY by rememberUpdatedState(preset.invertRightY)
    val currentAntiDeadzone by rememberUpdatedState(antiDeadzone)
    val currentOnUpdate by rememberUpdatedState(onUpdate)

    DisposableEffect(Unit) {
        onDispose {
            swipeFilter.reset()
        }
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(controlType, preset.triggerDragAimEnabled, preset.triggerDragMode) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                var anchor = Offset.Zero
                var prevPos = Offset.Zero
                var decayJob: Job? = null

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            anchor = down.position
                            prevPos = down.position
                            pressed = true
                            dragVecX = 0f
                            dragVecY = 0f
                            onTouchDown()
                            currentOnUpdate(true, 0f, 0f)

                            if (preset.triggerDragAimEnabled && preset.triggerDragMode == TriggerDragMode.SWIPE_VELOCITY) {
                                swipeFilter.onPointerDown(down.uptimeMillis)
                                decayJob?.cancel()
                                decayJob = scope.launch {
                                    while (isActive && activePointerId != null) {
                                        delay(12L)
                                        val decayed = swipeFilter.decayIfIdle(
                                            nowUptimeMs = SystemClock.uptimeMillis(),
                                            sensitivity = currentSensitivity,
                                            antiDeadzone = currentAntiDeadzone
                                        )
                                        if (decayed != null) {
                                            dragVecX = decayed.first
                                            dragVecY = decayed.second
                                            currentOnUpdate(
                                                true,
                                                decayed.first,
                                                if (currentInvertY) -decayed.second else decayed.second
                                            )
                                        }
                                    }
                                }
                            }
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            decayJob?.cancel()
                            decayJob = null
                            swipeFilter.reset()
                            pressed = false
                            dragVecX = 0f
                            dragVecY = 0f
                            currentOnUpdate(false, 0f, 0f)
                            change?.consume()
                        } else {
                            if (preset.triggerDragAimEnabled) {
                                if (preset.triggerDragMode == TriggerDragMode.STICK_DISPLACEMENT) {
                                    val delta = change.position - anchor
                                    val dist = hypot(delta.x, delta.y)
                                    val clamped = if (dist > dragRadiusPx) delta * (dragRadiusPx / dist) else delta
                                    val rawX = (clamped.x / dragRadiusPx).coerceIn(-1f, 1f)
                                    val rawY = (clamped.y / dragRadiusPx).coerceIn(-1f, 1f)
                                    dragVecX = rawX
                                    dragVecY = rawY
                                    currentOnUpdate(true, rawX, if (currentInvertY) -rawY else rawY)
                                } else {
                                    val step = change.position - prevPos
                                    prevPos = change.position
                                    val (rx, ryRaw) = swipeFilter.onPointerMove(
                                        dxDp = step.x / density.density,
                                        dyDp = step.y / density.density,
                                        uptimeMs = change.uptimeMillis,
                                        sensitivity = currentSensitivity,
                                        antiDeadzone = currentAntiDeadzone
                                    )
                                    dragVecX = rx
                                    dragVecY = ryRaw
                                    currentOnUpdate(true, rx, if (currentInvertY) -ryRaw else ryRaw)
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (pressed) activeColor.copy(alpha = 0.34f)
                else SlateSurfaceElevated.copy(alpha = 0.90f)
            )
            .border(
                width = if (pressed) 2.dp else 1.5.dp,
                color = if (pressed) activeColor else Color(0xFF475569),
                shape = shape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        if (pressed && preset.triggerDragAimEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val end = c + Offset(dragVecX * (size.width * 0.36f), dragVecY * (size.height * 0.36f))
                drawLine(
                    color = ElectricCyan,
                    start = c,
                    end = end,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = ElectricCyan, radius = 4.dp.toPx(), center = end)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = controlType.shortLabel,
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (15f * presetScaleText(controlType)).sp,
                color = if (pressed) Color.White else TextPrimary
            )
            if (preset.triggerDragAimEnabled) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Drag to aim enabled",
                    tint = if (pressed) ElectricCyan else ElectricCyan.copy(alpha = 0.75f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun FaceButtonWidget(
    controlType: ControlType,
    scale: Float,
    interactive: Boolean,
    onPressChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val accentColor = when (controlType) {
        ControlType.BUTTON_A -> XboxGreen
        ControlType.BUTTON_B -> XboxRed
        ControlType.BUTTON_X -> XboxBlue
        ControlType.BUTTON_Y -> XboxAmber
        else -> ElectricCyan
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(controlType) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            pressed = true
                            onPressChanged(true)
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            pressed = false
                            onPressChanged(false)
                            change?.consume()
                        } else {
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                if (pressed) accentColor.copy(alpha = 0.45f)
                else Color(0xFF141C2B).copy(alpha = 0.92f)
            )
            .border(
                width = (if (pressed) 2.5f else 1.8f).dp,
                color = if (pressed) accentColor else accentColor.copy(alpha = 0.78f),
                shape = CircleShape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = controlType.shortLabel,
            fontFamily = ChakraPetchFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * scale.coerceIn(0.75f, 1.45f)).sp,
            color = if (pressed) Color.White else accentColor
        )
    }
}

@Composable
private fun CapsuleGamepadButton(
    controlType: ControlType,
    scale: Float,
    interactive: Boolean,
    onPressChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val touchMod = if (interactive) {
        Modifier.pointerInput(controlType) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            pressed = true
                            onPressChanged(true)
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            pressed = false
                            onPressChanged(false)
                            change?.consume()
                        } else {
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (pressed) ElectricCyan.copy(alpha = 0.35f)
                else SlateSurfaceElevated.copy(alpha = 0.92f)
            )
            .border(
                width = (if (pressed) 2f else 1.4f).dp,
                color = if (pressed) ElectricCyan else Color(0xFF475569),
                shape = shape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = controlType.shortLabel,
            fontFamily = ChakraPetchFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12f * scale.coerceIn(0.75f, 1.35f)).sp,
            color = if (pressed) Color.White else TextPrimary
        )
    }
}

@Composable
private fun SettingsOverlayButton(
    scale: Float,
    interactive: Boolean,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val touchMod = if (interactive) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            pressed = true
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            if (pressed) {
                                pressed = false
                                onClick()
                            }
                            change?.consume()
                        } else {
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                if (pressed) ElectricCyan.copy(alpha = 0.45f)
                else ObsidianBg.copy(alpha = 0.90f)
            )
            .border(1.8.dp, ElectricCyan, CircleShape)
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "OverPad Overlay Settings",
            tint = ElectricCyan,
            modifier = Modifier.size((22f * scale.coerceIn(0.75f, 1.3f)).dp)
        )
    }
}

private fun presetScaleText(type: ControlType): Float = when (type) {
    ControlType.TRIGGER_LT, ControlType.TRIGGER_RT -> 1.0f
    else -> 1.0f
}

/**
 * Right-side screen FPS mouse-like touch-look surface.
 * Converts 1:1 finger swipe velocity (dp/sec) with EMA smoothing and game deadzone compensation
 * into Right Thumbstick (RX, RY) output, gliding cleanly back to (0, 0) when the thumb pauses or lifts.
 */
@Composable
fun FpsRightLookZoneSurface(
    preset: ControllerPreset,
    engine: XInputControllerEngine?,
    showGuideOverlay: Boolean,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    modifier: Modifier = Modifier
) {
    if (!preset.fpsRightZoneEnabled) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val mouseLookFilter = remember { TouchMouseLookFilter() }
    var activeLook by remember { mutableStateOf(false) }

    val currentSensitivity by rememberUpdatedState(preset.fpsLookSensitivity)
    val currentInvertY by rememberUpdatedState(preset.invertRightY)
    val currentAntiDeadzone by rememberUpdatedState(antiDeadzone)
    val currentEngine by rememberUpdatedState(engine)

    DisposableEffect(Unit) {
        onDispose {
            mouseLookFilter.reset()
            currentEngine?.updateFpsLookZoneStick(0f, 0f)
        }
    }

    Box(
        modifier = modifier
            .pointerInput(preset.fpsRightZoneEnabled) {
                awaitPointerEventScope {
                    var activePointerId: PointerId? = null
                    var prevPos = Offset.Zero
                    var decayJob: Job? = null

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (activePointerId == null) {
                            val down = event.changes.firstOrNull { it.pressed }
                            if (down != null) {
                                activePointerId = down.id
                                prevPos = down.position
                                activeLook = true
                                mouseLookFilter.onPointerDown(down.uptimeMillis)
                                currentEngine?.updateFpsLookZoneStick(0f, 0f)
                                decayJob?.cancel()
                                decayJob = scope.launch {
                                    while (isActive && activePointerId != null) {
                                        delay(12L)
                                        val decayed = mouseLookFilter.decayIfIdle(
                                            nowUptimeMs = SystemClock.uptimeMillis(),
                                            sensitivity = currentSensitivity,
                                            antiDeadzone = currentAntiDeadzone
                                        )
                                        if (decayed != null) {
                                            currentEngine?.updateFpsLookZoneStick(
                                                decayed.first,
                                                if (currentInvertY) -decayed.second else decayed.second
                                            )
                                        }
                                    }
                                }
                                down.consume()
                            }
                        } else {
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                            if (change == null || !change.pressed) {
                                activePointerId = null
                                decayJob?.cancel()
                                decayJob = null
                                mouseLookFilter.reset()
                                activeLook = false
                                currentEngine?.updateFpsLookZoneStick(0f, 0f)
                                change?.consume()
                            } else {
                                val deltaPx = change.position - prevPos
                                prevPos = change.position
                                val (rx, ryRaw) = mouseLookFilter.onPointerMove(
                                    dxDp = deltaPx.x / density.density,
                                    dyDp = deltaPx.y / density.density,
                                    uptimeMs = change.uptimeMillis,
                                    sensitivity = currentSensitivity,
                                    antiDeadzone = currentAntiDeadzone
                                )
                                val ry = if (currentInvertY) -ryRaw else ryRaw
                                currentEngine?.updateFpsLookZoneStick(rx, ry)
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        if (showGuideOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElectricCyan.copy(alpha = if (activeLook) 0.09f else 0.04f))
                    .border(
                        width = 1.dp,
                        color = ElectricCyan.copy(alpha = if (activeLook) 0.45f else 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = if (activeLook) "MOUSE-LOOK ACTIVE" else "FPS MOUSE-LOOK ZONE (SWIPE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan.copy(alpha = 0.78f)
                )
            }
        }
    }
}
