package com.example.ui.gamepad

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ControlType

/**
 * Custom Xbox button SVG icons (LT, RT, LB, RB, L-click, R-click, Back/View, Start/Menu)
 * colorized dynamically for both the floating overlay and system-wide editor/HUD chips.
 */
@Composable
fun GamepadControlIcon(
    controlType: ControlType,
    tint: Color,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    when (controlType) {
        ControlType.TRIGGER_LT -> TriggerSvgIcon(isLeft = true, tint = tint, modifier = modifier.size(size))
        ControlType.TRIGGER_RT -> TriggerSvgIcon(isLeft = false, tint = tint, modifier = modifier.size(size))
        ControlType.BUTTON_LB -> BumperSvgIcon(isLeft = true, tint = tint, modifier = modifier.size(size))
        ControlType.BUTTON_RB -> BumperSvgIcon(isLeft = false, tint = tint, modifier = modifier.size(size))
        ControlType.BUTTON_L3 -> StickClickSvgIcon(isLeft = true, tint = tint, modifier = modifier.size(size))
        ControlType.BUTTON_R3 -> StickClickSvgIcon(isLeft = false, tint = tint, modifier = modifier.size(size))
        ControlType.BUTTON_BACK -> BackViewIcon(tint = tint, modifier = modifier.size(size))
        ControlType.BUTTON_START -> MenuTextLinesIcon(tint = tint, modifier = modifier.size(size))
        else -> Unit
    }
}

fun ControlType.hasCustomIcon(): Boolean = when (this) {
    ControlType.TRIGGER_LT,
    ControlType.TRIGGER_RT,
    ControlType.BUTTON_LB,
    ControlType.BUTTON_RB,
    ControlType.BUTTON_L3,
    ControlType.BUTTON_R3,
    ControlType.BUTTON_BACK,
    ControlType.BUTTON_START -> true
    else -> false
}

@Composable
private fun TriggerSvgIcon(
    isLeft: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val path = remember(isLeft) {
        PathParser().parsePathString(
            if (isLeft) {
                "M13.328,2.5 L58.21,2.5 C58.21,2.5 60.2605,2.5 61.4655,3.7051 C62.6705,4.9102 63.1518,6.7394 63.1518,6.7394 " +
                    "L65.1865,19.9734 C65.1865,19.9734 66.3176,27.322 64.078,34.0704 C61.8384,40.8188 56.6638,45.8344 56.6638,45.8344 " +
                    "L43.7858,58.7124 C43.7858,58.7124 39.5662,62.932 34.0531,65.2156 C28.54,67.4992 22.5721,67.4992 22.5721,67.4992 " +
                    "H9.2681 C9.2681,67.4992 6.7452,67.4992 5.2462,65.4698 C3.7472,63.4404 4.48911,61.029 4.48911,61.029 " +
                    "L10.47421,41.58 C10.47421,41.58 11.47341,38.332 11.72081,34.9429 C11.96821,31.5538 11.45127,28.1951 11.45127,28.1951 " +
                    "L8.38527,8.2601 C8.38527,8.2601 8.03563,5.9884 9.53217,4.244 C11.02867,2.4995 13.32707,2.4995 13.32707,2.4995 Z"
            } else {
                "M56.672,2.5 L11.79,2.5 C11.79,2.5 9.7395,2.5 8.5345,3.7051 C7.3295,4.9102 6.8482,6.7394 6.8482,6.7394 " +
                    "L4.8135,19.9734 C4.8135,19.9734 3.6824,27.322 5.922,34.0704 C8.1616,40.8188 13.3362,45.8344 13.3362,45.8344 " +
                    "L26.2142,58.7124 C26.2142,58.7124 30.4338,62.932 35.9469,65.2156 C41.46,67.4992 47.4279,67.4992 47.4279,67.4992 " +
                    "H60.7319 C60.7319,67.4992 63.2548,67.4992 64.7538,65.4698 C66.2527,63.4404 65.51089,61.029 65.51089,61.029 " +
                    "L59.52579,41.58 C59.52579,41.58 58.52659,38.332 58.27919,34.9429 C58.03179,31.5538 58.54873,28.1951 58.54873,28.1951 " +
                    "L61.61473,8.2601 C61.61473,8.2601 61.96437,5.9884 60.46783,4.244 C58.97133,2.4995 56.67293,2.4995 56.67293,2.4995 Z"
            }
        ).toPath()
    }
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 30f
            letterSpacing = -0.08f
        }
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = if (isLeft) "Left Trigger" else "Right Trigger"
        }
    ) {
        val scale = minOf(size.width / 70f, size.height / 70f)
        val ox = (size.width - 70f * scale) / 2f
        val oy = (size.height - 70f * scale) / 2f

        withTransform({
            translate(left = ox, top = oy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawPath(path = path, color = tint.copy(alpha = 0.14f))
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 4.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            textPaint.color = tint.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                if (isLeft) "LT" else "RT",
                if (isLeft) 35.5f else 34.5f,
                42f,
                textPaint
            )
        }
    }
}

@Composable
private fun BumperSvgIcon(
    isLeft: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val path = remember(isLeft) {
        PathParser().parsePathString(
            if (isLeft) {
                "m8.4021 50s-2.3172 1e-6 -3.815-1.768-1.117-4.0537-1.117-4.0537l2.9524-17.711" +
                    "s0.49076-2.9453 2.516-5.1393 4.922-2.9184 4.922-2.9184l37.625-9.0888" +
                    "s2.733-0.74932 5.6239 0.22637c2.8908 0.97568 4.6108 3.2279 4.6108 3.2279" +
                    "l2.9909 3.7386s1.3081 1.6351 1.8684 3.7263c0.56033 2.0912 0.09896 4.6922 0.09896 4.6922" +
                    "l-3.482 20.89s-0.3013 1.8083-1.7 2.9932c-1.3988 1.185-3.232 1.185-3.232 1.185z"
            } else {
                "m61.598 50s2.3172 1e-6 3.815-1.768 1.117-4.0537 1.117-4.0537l-2.9524-17.711" +
                    "s-0.49076-2.9453-2.516-5.1393-4.922-2.9184-4.922-2.9184l-37.625-9.0888" +
                    "s-2.733-0.74932-5.6239 0.22637c-2.8908 0.97568-4.6108 3.2279-4.6108 3.2279" +
                    "l-2.9909 3.7386s-1.3081 1.6351-1.8684 3.7263-0.098956 4.6922-0.098956 4.6922" +
                    "l3.482 20.89s0.3013 1.8083 1.7001 2.9932c1.3987 1.185 3.232 1.185 3.232 1.185z"
            }
        ).toPath()
    }
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 27f
            letterSpacing = 0f
        }
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = if (isLeft) "Left Bumper" else "Right Bumper"
        }
    ) {
        val scale = minOf(size.width / 70f, size.height / 70f)
        val ox = (size.width - 70f * scale) / 2f
        val oy = (size.height - 70f * scale) / 2f

        withTransform({
            translate(left = ox, top = oy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawPath(path = path, color = tint.copy(alpha = 0.14f))
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 4.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            textPaint.color = tint.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                if (isLeft) "LB" else "RB",
                35f,
                42f,
                textPaint
            )
        }
    }
}

@Composable
private fun StickClickSvgIcon(
    isLeft: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val arrowPath = remember {
        PathParser().parsePathString("m25 15h20l-10 7.5z").toPath()
    }
    val stickPath = remember {
        PathParser().parsePathString("m52.5 70v-27.5h15v-15h-65v15h15v27.5").toPath()
    }
    val stickFillPath = remember {
        PathParser().parsePathString("m52.5 68v-25.5h15v-15h-65v15h15v25.5z").toPath()
    }
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 26f
        }
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = if (isLeft) "Left Stick Click" else "Right Stick Click"
        }
    ) {
        val scale = minOf(size.width / 70f, size.height / 70f)
        val ox = (size.width - 70f * scale) / 2f
        val oy = (size.height - 70f * scale) / 2f

        withTransform({
            translate(left = ox, top = oy)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            drawPath(path = arrowPath, color = tint)
            drawPath(path = stickFillPath, color = tint.copy(alpha = 0.12f))
            drawPath(
                path = stickPath,
                color = tint,
                style = Stroke(width = 4.6f, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
            )
            textPaint.color = tint.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                if (isLeft) "L" else "R",
                35f,
                65.5f,
                textPaint
            )
        }
    }
}

@Composable
private fun BackViewIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Back / View"
        }
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (minOf(w, h) * 0.11f).coerceAtLeast(1.dp.toPx())
        val radius = CornerRadius(w * 0.12f, h * 0.12f)

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.16f, h * 0.18f),
            size = Size(w * 0.46f, h * 0.46f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.22f),
            topLeft = Offset(w * 0.38f, h * 0.36f),
            size = Size(w * 0.46f, h * 0.46f),
            cornerRadius = radius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.38f, h * 0.36f),
            size = Size(w * 0.46f, h * 0.46f),
            cornerRadius = radius,
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun MenuTextLinesIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Start / Menu"
        }
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (minOf(w, h) * 0.13f).coerceAtLeast(1.dp.toPx())

        listOf(0.28f, 0.50f, 0.72f).forEach { ny ->
            drawLine(
                color = tint,
                start = Offset(w * 0.18f, h * ny),
                end = Offset(w * 0.82f, h * ny),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
