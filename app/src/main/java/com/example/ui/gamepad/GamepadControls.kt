package com.example.ui.gamepad

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import com.example.model.ButtonShape
import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.PresetCategory
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun ButtonShape.toComposeShape(): Shape = when (this) {
    ButtonShape.CIRCLE -> CircleShape
    ButtonShape.ROUNDED -> RoundedCornerShape(percent = 24)
    ButtonShape.PILL -> RoundedCornerShape(50)
    ButtonShape.SQUARE -> RoundedCornerShape(percent = 6)
}

internal const val MAX_BUTTON_ICON_HEIGHT_FRACTION = 0.48f

internal fun buttonIconSizeDp(
    buttonWidthDp: Float,
    buttonHeightDp: Float
): Float = minOf(buttonWidthDp, buttonHeightDp).coerceAtLeast(0f) * MAX_BUTTON_ICON_HEIGHT_FRACTION

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
    val widthDp = placement.effectiveWidthDp().dp
    val heightDp = placement.effectiveHeightDp().dp
    val resolvedShape = placement.resolvedShape()
    val composeShape = resolvedShape.toComposeShape()
    val isToggleable = placement.toggleable && type.supportsToggle
    val view = LocalView.current

    val keyCode = type.keyCode
    val engineActive: Boolean? = if (engine != null && interactive && isToggleable && keyCode != null) {
        remember(engine, keyCode) {
            engine.state.map { it.isPressed(keyCode) }.distinctUntilChanged()
        }.collectAsState(initial = engine.state.value.isPressed(keyCode)).value
    } else {
        null
    }

    fun performHaptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    val selectionShape = if (type.supportsShape) composeShape else RoundedCornerShape(14.dp)
    val selectionModifier = if (isSelectedInEditor) {
        Modifier.border(2.dp, ElectricCyan, selectionShape)
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
                    shape = composeShape,
                    interactive = interactive,
                    onDirectionChanged = { dir -> engine?.updateDPad(dir) },
                    onTouchDown = { performHaptic() }
                )
            }

            ControlType.TRIGGER_LT, ControlType.TRIGGER_RT -> {
                TriggerDragAimWidget(
                    controlType = type,
                    shape = composeShape,
                    toggleable = isToggleable,
                    engineActive = engineActive,
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
                    shape = composeShape,
                    toggleable = isToggleable,
                    engineActive = engineActive,
                    interactive = interactive,
                    onPressChanged = { pressed ->
                        if (pressed) performHaptic()
                        engine?.setButtonPressed(type, pressed)
                    }
                )
            }

            ControlType.SETTINGS -> {
                SettingsOverlayButton(
                    shape = composeShape,
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
                    shape = composeShape,
                    toggleable = isToggleable,
                    engineActive = engineActive,
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
    sensitivity: Float = TouchMouseLookFilter.DEFAULT_SENSITIVITY,
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
                var vsyncJob: Job? = null

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
                                vsyncJob?.cancel()
                                vsyncJob = scope.launch {
                                    var lastFrameNanos = 0L
                                    while (isActive && activePointerId != null) {
                                        withFrameNanos { frameNanos ->
                                            val dtMs = if (lastFrameNanos == 0L) {
                                                16.67f
                                            } else {
                                                ((frameNanos - lastFrameNanos) / 1_000_000f).coerceIn(1f, 50f)
                                            }
                                            lastFrameNanos = frameNanos
                                            val output = mouseFilter.stepFrame(
                                                dtMs = dtMs,
                                                sensitivity = currentSensitivity,
                                                antiDeadzone = currentAntiDeadzone
                                            )
                                            if (output != null) {
                                                normX = output.first
                                                normY = output.second
                                                currentOnDeflect(
                                                    output.first,
                                                    if (currentInvertY) -output.second else output.second
                                                )
                                            }
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
                            vsyncJob?.cancel()
                            vsyncJob = null
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
                                mouseFilter.accumulateDelta(
                                    dxDp = deltaPx.x / density.density,
                                    dyDp = deltaPx.y / density.density,
                                    sensitivity = currentSensitivity
                                )
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
    shape: Shape,
    interactive: Boolean,
    onDirectionChanged: (DPadDirectionState) -> Unit,
    onTouchDown: () -> Unit
) {
    var dpadState by remember { mutableStateOf(DPadDirectionState()) }
    var touching by remember { mutableStateOf(false) }
    var flickOffset by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val flickController = remember { DPadFlickController() }
    val currentOnDirectionChanged by rememberUpdatedState(onDirectionChanged)
    val currentOnTouchDown by rememberUpdatedState(onTouchDown)

    DisposableEffect(Unit) {
        onDispose {
            dpadState = DPadDirectionState()
            currentOnDirectionChanged(DPadDirectionState())
        }
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                var holdJob: Job? = null
                var clearPulseJob: Job? = null

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            clearPulseJob?.cancel()
                            clearPulseJob = null
                            activePointerId = down.id
                            touching = true
                            flickOffset = Offset.Zero
                            flickController.onDown(down.position.x, down.position.y)
                            if (dpadState != DPadDirectionState()) {
                                val cleared = DPadDirectionState()
                                dpadState = cleared
                                currentOnDirectionChanged(cleared)
                            }
                            val pointerId = down.id
                            val boxWidth = size.width
                            val boxHeight = size.height
                            holdJob?.cancel()
                            holdJob = scope.launch {
                                delay(DPadFlickController.HOLD_DWELL_MS)
                                if (activePointerId == pointerId) {
                                    val holdDir = flickController.onHoldDwellElapsed(
                                        width = boxWidth,
                                        height = boxHeight,
                                        uptimeMs = SystemClock.uptimeMillis()
                                    )
                                    if (holdDir != null) {
                                        dpadState = holdDir
                                        currentOnDirectionChanged(holdDir)
                                        currentOnTouchDown()
                                    }
                                }
                            }
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            holdJob?.cancel()
                            holdJob = null
                            touching = false
                            flickOffset = Offset.Zero

                            val outcome = flickController.onUp(
                                width = size.width,
                                height = size.height,
                                uptimeMs = change?.uptimeMillis ?: SystemClock.uptimeMillis()
                            )
                            when {
                                outcome.tapPulseDirection != null -> {
                                    val pulse = outcome.tapPulseDirection
                                    dpadState = pulse
                                    currentOnDirectionChanged(pulse)
                                    currentOnTouchDown()
                                    clearPulseJob?.cancel()
                                    clearPulseJob = scope.launch {
                                        delay(DPadFlickController.MIN_FLICK_PULSE_MS)
                                        val cleared = DPadDirectionState()
                                        dpadState = cleared
                                        currentOnDirectionChanged(cleared)
                                    }
                                }
                                outcome.shouldDeferClearMs > 0L -> {
                                    clearPulseJob?.cancel()
                                    clearPulseJob = scope.launch {
                                        delay(outcome.shouldDeferClearMs)
                                        val cleared = DPadDirectionState()
                                        dpadState = cleared
                                        currentOnDirectionChanged(cleared)
                                    }
                                }
                                else -> {
                                    val cleared = DPadDirectionState()
                                    dpadState = cleared
                                    currentOnDirectionChanged(cleared)
                                }
                            }
                            change?.consume()
                        } else {
                            val move = flickController.onMove(
                                x = change.position.x,
                                y = change.position.y,
                                densityPx = density.density,
                                uptimeMs = change.uptimeMillis
                            )
                            if (move.isFlickActive) {
                                holdJob?.cancel()
                                holdJob = null
                                flickOffset = Offset(move.visualOffsetXFraction, move.visualOffsetYFraction)
                            }
                            if (move.directionChanged) {
                                dpadState = move.direction
                                currentOnDirectionChanged(move.direction)
                                if (move.direction != DPadDirectionState()) {
                                    currentOnTouchDown()
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    val isLit = touching || dpadState != DPadDirectionState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (isLit) Color(0xFF142138).copy(alpha = 0.90f)
                else Color(0xFF0E1525).copy(alpha = 0.85f)
            )
            .border(
                width = (if (isLit) 2.0f else 1.5f).dp,
                color = if (isLit) ElectricCyan.copy(alpha = 0.78f) else Color(0xFF3A4A66),
                shape = shape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val minDim = size.minDimension
            val stemStartX = minDim * 0.11f
            val stemStartY = minDim * 0.11f
            val tipDistX = w * 0.36f
            val tipDistY = h * 0.36f
            val headBack = minDim * 0.085f
            val headWing = minDim * 0.082f
            val strokeWidth = (minDim * 0.022f).coerceAtLeast(1.8.dp.toPx())
            val haloRadius = minDim * 0.13f

            fun drawStemmedArrow(
                stemStart: Offset,
                tip: Offset,
                wing1: Offset,
                wing2: Offset,
                lit: Boolean
            ) {
                val arrowColor = if (lit) ElectricCyan else Color(0xFFCBD5E1)
                if (lit) {
                    val haloCenter = Offset(
                        stemStart.x + (tip.x - stemStart.x) * 0.80f,
                        stemStart.y + (tip.y - stemStart.y) * 0.80f
                    )
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.24f),
                        radius = haloRadius,
                        center = haloCenter
                    )
                }

                // Thin stem with rounded caps, fading smoothly to transparent toward the center
                drawLine(
                    brush = Brush.linearGradient(
                        0.0f to arrowColor.copy(alpha = 0f),
                        0.78f to arrowColor,
                        1.0f to arrowColor,
                        start = stemStart,
                        end = tip
                    ),
                    start = stemStart,
                    end = tip,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Thin arrowhead chevron with rounded caps and rounded tip join
                val headPath = Path().apply {
                    moveTo(wing1.x, wing1.y)
                    lineTo(tip.x, tip.y)
                    lineTo(wing2.x, wing2.y)
                }
                drawPath(
                    path = headPath,
                    color = arrowColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Up arrow
            val upTip = Offset(cx, cy - tipDistY)
            drawStemmedArrow(
                stemStart = Offset(cx, cy - stemStartY),
                tip = upTip,
                wing1 = Offset(upTip.x - headWing, upTip.y + headBack),
                wing2 = Offset(upTip.x + headWing, upTip.y + headBack),
                lit = dpadState.up
            )

            // Down arrow
            val downTip = Offset(cx, cy + tipDistY)
            drawStemmedArrow(
                stemStart = Offset(cx, cy + stemStartY),
                tip = downTip,
                wing1 = Offset(downTip.x - headWing, downTip.y - headBack),
                wing2 = Offset(downTip.x + headWing, downTip.y - headBack),
                lit = dpadState.down
            )

            // Left arrow
            val leftTip = Offset(cx - tipDistX, cy)
            drawStemmedArrow(
                stemStart = Offset(cx - stemStartX, cy),
                tip = leftTip,
                wing1 = Offset(leftTip.x + headBack, leftTip.y - headWing),
                wing2 = Offset(leftTip.x + headBack, leftTip.y + headWing),
                lit = dpadState.left
            )

            // Right arrow
            val rightTip = Offset(cx + tipDistX, cy)
            drawStemmedArrow(
                stemStart = Offset(cx + stemStartX, cy),
                tip = rightTip,
                wing1 = Offset(rightTip.x - headBack, rightTip.y - headWing),
                wing2 = Offset(rightTip.x - headBack, rightTip.y + headWing),
                lit = dpadState.right
            )

            // Live flick puck feedback while touching
            if (touching) {
                val puckRadius = minDim * 0.055f
                val puckCenter = Offset(
                    cx + flickOffset.x * w * 0.24f,
                    cy + flickOffset.y * h * 0.24f
                )
                if (hypot(flickOffset.x, flickOffset.y) > 0.05f) {
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.6f),
                        start = Offset(cx, cy),
                        end = puckCenter,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    color = ElectricCyan.copy(alpha = if (dpadState != DPadDirectionState()) 0.78f else 0.40f),
                    radius = puckRadius,
                    center = puckCenter
                )
            }
        }
    }
}

@Composable
private fun TriggerDragAimWidget(
    controlType: ControlType,
    shape: Shape,
    toggleable: Boolean,
    engineActive: Boolean?,
    preset: ControllerPreset,
    interactive: Boolean,
    antiDeadzone: Float = TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
    onUpdate: (pressed: Boolean, dragX: Float, dragY: Float) -> Unit,
    onTouchDown: () -> Unit
) {
    var pressed by remember(controlType, toggleable) { mutableStateOf(false) }
    var dragVecX by remember { mutableFloatStateOf(0f) }
    var dragVecY by remember { mutableFloatStateOf(0f) }
    var isHoldingPointer by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeFilter = remember { TouchMouseLookFilter() }
    val isRt = controlType == ControlType.TRIGGER_RT
    val activeColor = if (isRt) XboxRed else TriggerAmber

    val currentSensitivity by rememberUpdatedState(preset.fpsLookSensitivity)
    val currentInvertY by rememberUpdatedState(preset.invertRightY)
    val currentAntiDeadzone by rememberUpdatedState(antiDeadzone)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    val currentToggleable by rememberUpdatedState(toggleable)

    LaunchedEffect(engineActive, toggleable) {
        if (toggleable && engineActive != null) {
            pressed = engineActive
        } else if (!toggleable && !isHoldingPointer && pressed) {
            pressed = false
            currentOnUpdate(false, 0f, 0f)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            swipeFilter.reset()
        }
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(controlType, preset.triggerDragAimEnabled, toggleable) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                var downPos = Offset.Zero
                var prevPos = Offset.Zero
                var hasStartedDrag = false
                var vsyncJob: Job? = null
                var sessionLatchedState = false

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            isHoldingPointer = true
                            downPos = down.position
                            prevPos = down.position
                            hasStartedDrag = false
                            sessionLatchedState = if (currentToggleable) !pressed else true
                            pressed = sessionLatchedState
                            dragVecX = 0f
                            dragVecY = 0f
                            onTouchDown()
                            currentOnUpdate(sessionLatchedState, 0f, 0f)
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            isHoldingPointer = false
                            hasStartedDrag = false
                            vsyncJob?.cancel()
                            vsyncJob = null
                            swipeFilter.reset()
                            val keepActive = if (currentToggleable) sessionLatchedState else false
                            pressed = keepActive
                            dragVecX = 0f
                            dragVecY = 0f
                            currentOnUpdate(keepActive, 0f, 0f)
                            change?.consume()
                        } else {
                            if (preset.triggerDragAimEnabled && (!currentToggleable || sessionLatchedState)) {
                                if (!hasStartedDrag) {
                                    val fromDown = change.position - downPos
                                    val fromDownDp = hypot(fromDown.x, fromDown.y) / density.density
                                    if (fromDownDp > TRIGGER_DRAG_SQUISH_GUARD_DP) {
                                        hasStartedDrag = true
                                        prevPos = change.position
                                        swipeFilter.onPointerDown(change.uptimeMillis)
                                        swipeFilter.accumulateDelta(
                                            dxDp = fromDown.x / density.density,
                                            dyDp = fromDown.y / density.density,
                                            sensitivity = currentSensitivity
                                        )

                                        vsyncJob?.cancel()
                                        vsyncJob = scope.launch {
                                            var lastFrameNanos = 0L
                                            while (isActive && activePointerId != null) {
                                                withFrameNanos { frameNanos ->
                                                    val dtMs = if (lastFrameNanos == 0L) {
                                                        16.67f
                                                    } else {
                                                        ((frameNanos - lastFrameNanos) / 1_000_000f).coerceIn(1f, 50f)
                                                    }
                                                    lastFrameNanos = frameNanos
                                                    val output = swipeFilter.stepFrame(
                                                        dtMs = dtMs,
                                                        sensitivity = currentSensitivity,
                                                        antiDeadzone = currentAntiDeadzone
                                                    )
                                                    if (output != null) {
                                                        dragVecX = output.first
                                                        dragVecY = output.second
                                                        currentOnUpdate(
                                                            true,
                                                            output.first,
                                                            if (currentInvertY) -output.second else output.second
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val step = change.position - prevPos
                                    prevPos = change.position
                                    swipeFilter.accumulateDelta(
                                        dxDp = step.x / density.density,
                                        dyDp = step.y / density.density,
                                        sensitivity = currentSensitivity
                                    )
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
        }
    } else Modifier

    val effectivePressed = if (toggleable && engineActive != null) engineActive else pressed

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (effectivePressed) activeColor.copy(alpha = 0.34f)
                else SlateSurfaceElevated.copy(alpha = 0.90f)
            )
            .border(
                width = if (effectivePressed) 2.dp else 1.5.dp,
                color = if (effectivePressed) activeColor else Color(0xFF475569),
                shape = shape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        if (effectivePressed && preset.triggerDragAimEnabled && isHoldingPointer) {
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
        GamepadControlIcon(
            controlType = controlType,
            tint = if (effectivePressed) Color.White else TextPrimary,
            size = buttonIconSizeDp(maxWidth.value, maxHeight.value).dp
        )
        if (toggleable) {
            ToggleLatchIndicator(
                latched = effectivePressed,
                activeColor = activeColor,
                dotSize = (minDim * 0.13f).coerceIn(4.dp, 7.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (minDim * 0.08f).coerceAtLeast(2.dp))
            )
        }
    }
}

@Composable
private fun FaceButtonWidget(
    controlType: ControlType,
    shape: Shape,
    toggleable: Boolean,
    engineActive: Boolean?,
    interactive: Boolean,
    onPressChanged: (Boolean) -> Unit
) {
    var pressed by remember(controlType, toggleable) { mutableStateOf(false) }

    val accentColor = when (controlType) {
        ControlType.BUTTON_A -> XboxGreen
        ControlType.BUTTON_B -> XboxRed
        ControlType.BUTTON_X -> XboxBlue
        ControlType.BUTTON_Y -> XboxAmber
        else -> ElectricCyan
    }

    val currentToggleable by rememberUpdatedState(toggleable)
    val currentOnPressChanged by rememberUpdatedState(onPressChanged)

    LaunchedEffect(engineActive, toggleable) {
        if (toggleable && engineActive != null) {
            pressed = engineActive
        } else if (!toggleable && pressed) {
            pressed = false
            currentOnPressChanged(false)
        }
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(controlType, toggleable) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            if (currentToggleable) {
                                val next = !pressed
                                pressed = next
                                currentOnPressChanged(next)
                            } else {
                                pressed = true
                                currentOnPressChanged(true)
                            }
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            if (!currentToggleable) {
                                pressed = false
                                currentOnPressChanged(false)
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

    val effectivePressed = if (toggleable && engineActive != null) engineActive else pressed

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (effectivePressed) accentColor.copy(alpha = 0.45f)
                else Color(0xFF141C2B).copy(alpha = 0.92f)
            )
            .border(
                width = (if (effectivePressed) 2.5f else 1.8f).dp,
                color = if (effectivePressed) accentColor else accentColor.copy(alpha = 0.78f),
                shape = shape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        Text(
            text = controlType.shortLabel,
            fontFamily = ChakraPetchFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (minDim.value * 0.34f).sp,
            color = if (effectivePressed) Color.White else accentColor
        )
        if (toggleable) {
            ToggleLatchIndicator(
                latched = effectivePressed,
                activeColor = accentColor,
                dotSize = (minDim * 0.12f).coerceIn(4.dp, 7.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (minDim * 0.08f).coerceAtLeast(2.dp))
            )
        }
    }
}

@Composable
private fun CapsuleGamepadButton(
    controlType: ControlType,
    shape: Shape,
    toggleable: Boolean,
    engineActive: Boolean?,
    interactive: Boolean,
    onPressChanged: (Boolean) -> Unit
) {
    var pressed by remember(controlType, toggleable) { mutableStateOf(false) }

    val currentToggleable by rememberUpdatedState(toggleable)
    val currentOnPressChanged by rememberUpdatedState(onPressChanged)

    LaunchedEffect(engineActive, toggleable) {
        if (toggleable && engineActive != null) {
            pressed = engineActive
        } else if (!toggleable && pressed) {
            pressed = false
            currentOnPressChanged(false)
        }
    }

    val touchMod = if (interactive) {
        Modifier.pointerInput(controlType, toggleable) {
            awaitPointerEventScope {
                var activePointerId: PointerId? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (activePointerId == null) {
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null) {
                            activePointerId = down.id
                            if (currentToggleable) {
                                val next = !pressed
                                pressed = next
                                currentOnPressChanged(next)
                            } else {
                                pressed = true
                                currentOnPressChanged(true)
                            }
                            down.consume()
                        }
                    } else {
                        val change = event.changes.firstOrNull { it.id == activePointerId }
                        if (change == null || !change.pressed) {
                            activePointerId = null
                            if (!currentToggleable) {
                                pressed = false
                                currentOnPressChanged(false)
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

    val effectivePressed = if (toggleable && engineActive != null) engineActive else pressed
    val contentTint = if (effectivePressed) Color.White else TextPrimary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (effectivePressed) ElectricCyan.copy(alpha = 0.35f)
                else SlateSurfaceElevated.copy(alpha = 0.92f)
            )
            .border(
                width = (if (effectivePressed) 2f else 1.4f).dp,
                color = if (effectivePressed) ElectricCyan else Color(0xFF475569),
                shape = shape
            )
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        if (controlType.hasCustomIcon()) {
            GamepadControlIcon(
                controlType = controlType,
                tint = contentTint,
                size = buttonIconSizeDp(maxWidth.value, maxHeight.value).dp
            )
        } else {
            Text(
                text = controlType.shortLabel,
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = (minDim.value * 0.28f).sp,
                color = contentTint
            )
        }
        if (toggleable) {
            ToggleLatchIndicator(
                latched = effectivePressed,
                activeColor = ElectricCyan,
                dotSize = (minDim * 0.12f).coerceIn(4.dp, 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (minDim * 0.08f).coerceAtLeast(2.dp))
            )
        }
    }
}

@Composable
private fun ToggleLatchIndicator(
    latched: Boolean,
    activeColor: Color,
    dotSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(if (latched) activeColor else Color.White.copy(alpha = 0.25f))
            .border(0.75.dp, if (latched) Color.White else activeColor.copy(alpha = 0.7f), CircleShape)
    )
}

@Composable
private fun SettingsOverlayButton(
    shape: Shape,
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (pressed) ElectricCyan.copy(alpha = 0.45f)
                else ObsidianBg.copy(alpha = 0.90f)
            )
            .border(1.8.dp, ElectricCyan, shape)
            .then(touchMod),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "OverPad Overlay Settings",
            tint = ElectricCyan,
            modifier = Modifier.size(buttonIconSizeDp(maxWidth.value, maxHeight.value).dp)
        )
    }
}

private const val TRIGGER_DRAG_SQUISH_GUARD_DP = 1.5f

/**
 * Left-side tap-anywhere floating thumbstick movement surface.
 * Touch-down anchors a virtual stick origin beneath the finger; dragging deflects
 * Left Thumbstick (LX, LY) smoothly with a soft leash for quick direction reversals.
 */
@Composable
fun LeftMoveZoneSurface(
    preset: ControllerPreset,
    engine: XInputControllerEngine?,
    showGuideOverlay: Boolean,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!preset.leftMoveZoneEnabled) return

    val density = LocalDensity.current
    val view = LocalView.current
    var activeMove by remember { mutableStateOf(false) }
    var anchorPos by remember { mutableStateOf(Offset.Zero) }
    var thumbPos by remember { mutableStateOf(Offset.Zero) }

    val currentEngine by rememberUpdatedState(engine)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)

    DisposableEffect(Unit) {
        onDispose {
            currentEngine?.updateLeftMoveZoneStick(0f, 0f)
        }
    }

    Box(
        modifier = modifier
            .pointerInput(preset.leftMoveZoneEnabled) {
                val maxRadiusPx = with(density) { 44.dp.toPx() }.coerceAtLeast(1f)
                val leashRadiusPx = maxRadiusPx * 1.5f
                awaitPointerEventScope {
                    var activePointerId: PointerId? = null
                    var currentAnchor = Offset.Zero

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (activePointerId == null) {
                            val down = event.changes.firstOrNull { it.pressed }
                            if (down != null) {
                                activePointerId = down.id
                                currentAnchor = down.position
                                anchorPos = currentAnchor
                                thumbPos = currentAnchor
                                activeMove = true
                                if (currentHaptics) {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                                currentEngine?.updateLeftMoveZoneStick(0f, 0f)
                                down.consume()
                            }
                        } else {
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                            if (change == null || !change.pressed) {
                                activePointerId = null
                                activeMove = false
                                currentEngine?.updateLeftMoveZoneStick(0f, 0f)
                                change?.consume()
                            } else {
                                var delta = change.position - currentAnchor
                                val dist = hypot(delta.x, delta.y)
                                if (dist > leashRadiusPx) {
                                    currentAnchor = change.position - delta * (leashRadiusPx / dist)
                                    anchorPos = currentAnchor
                                    delta = change.position - currentAnchor
                                }
                                val clampedDist = hypot(delta.x, delta.y)
                                val clamped = if (clampedDist > maxRadiusPx) {
                                    delta * (maxRadiusPx / clampedDist)
                                } else {
                                    delta
                                }
                                thumbPos = currentAnchor + clamped
                                val lx = (clamped.x / maxRadiusPx).coerceIn(-1f, 1f)
                                val ly = (clamped.y / maxRadiusPx).coerceIn(-1f, 1f)
                                currentEngine?.updateLeftMoveZoneStick(lx, ly)
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
                    .background(TacticalEmerald.copy(alpha = if (activeMove) 0.09f else 0.04f))
                    .border(
                        width = 1.dp,
                        color = TacticalEmerald.copy(alpha = if (activeMove) 0.45f else 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = if (activeMove) "TOUCH-MOVE ACTIVE" else "LEFT MOVE ZONE (TAP & DRAG)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalEmerald.copy(alpha = 0.78f)
                )
            }
        }
        if (activeMove) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringRadius = 44.dp.toPx()
                val puckRadius = 18.dp.toPx()
                drawCircle(
                    color = TacticalEmerald.copy(alpha = 0.14f),
                    radius = ringRadius,
                    center = anchorPos
                )
                drawCircle(
                    color = TacticalEmerald.copy(alpha = 0.55f),
                    radius = ringRadius,
                    center = anchorPos,
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawLine(
                    color = TacticalEmerald.copy(alpha = 0.45f),
                    start = anchorPos,
                    end = thumbPos,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = TacticalEmerald.copy(alpha = 0.48f),
                    radius = puckRadius,
                    center = thumbPos
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = puckRadius,
                    center = thumbPos,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
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
                    var vsyncJob: Job? = null

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
                                vsyncJob?.cancel()
                                vsyncJob = scope.launch {
                                    var lastFrameNanos = 0L
                                    while (isActive && activePointerId != null) {
                                        withFrameNanos { frameNanos ->
                                            val dtMs = if (lastFrameNanos == 0L) {
                                                16.67f
                                            } else {
                                                ((frameNanos - lastFrameNanos) / 1_000_000f).coerceIn(1f, 50f)
                                            }
                                            lastFrameNanos = frameNanos
                                            val output = mouseLookFilter.stepFrame(
                                                dtMs = dtMs,
                                                sensitivity = currentSensitivity,
                                                antiDeadzone = currentAntiDeadzone
                                            )
                                            if (output != null) {
                                                val ry = if (currentInvertY) -output.second else output.second
                                                currentEngine?.updateFpsLookZoneStick(output.first, ry)
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
                                vsyncJob?.cancel()
                                vsyncJob = null
                                mouseLookFilter.reset()
                                activeLook = false
                                currentEngine?.updateFpsLookZoneStick(0f, 0f)
                                change?.consume()
                            } else {
                                val deltaPx = change.position - prevPos
                                prevPos = change.position
                                mouseLookFilter.accumulateDelta(
                                    dxDp = deltaPx.x / density.density,
                                    dyDp = deltaPx.y / density.density,
                                    sensitivity = currentSensitivity
                                )
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
