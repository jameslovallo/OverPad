package com.example.ui.gamepad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ControlType

/**
 * Custom Xbox button vector icons (LT, RT, LB, RB, Back/View, Start/Menu)
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
        ControlType.TRIGGER_LT -> Icon(
            painter = painterResource(id = R.drawable.ic_trigger_left),
            contentDescription = "Left Trigger",
            tint = tint,
            modifier = modifier.size(size)
        )
        ControlType.TRIGGER_RT -> Icon(
            painter = painterResource(id = R.drawable.ic_trigger_right),
            contentDescription = "Right Trigger",
            tint = tint,
            modifier = modifier.size(size)
        )
        ControlType.BUTTON_LB -> Icon(
            painter = painterResource(id = R.drawable.ic_bumper_left),
            contentDescription = "Left Bumper",
            tint = tint,
            modifier = modifier.size(size)
        )
        ControlType.BUTTON_RB -> Icon(
            painter = painterResource(id = R.drawable.ic_bumper_right),
            contentDescription = "Right Bumper",
            tint = tint,
            modifier = modifier.size(size)
        )
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
    ControlType.BUTTON_BACK,
    ControlType.BUTTON_START -> true
    else -> false
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
