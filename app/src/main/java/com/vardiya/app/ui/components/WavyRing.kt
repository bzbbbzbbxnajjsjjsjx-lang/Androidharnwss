package com.vardiya.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Circular progress drawn as a wave that keeps flowing along the ring.
 * [progress] is read while drawing, so a fast-changing value does not recompose the screen.
 */
@Composable
fun WavyRing(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    trackWidth: Dp = 8.dp,
    amplitude: Dp = 5.dp,
    waves: Int = 16,
) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase",
    )
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val amp = amplitude.toPx()
        val radius = (size.minDimension - stroke) / 2f - amp
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(trackColor, radius, center, style = Stroke(width = trackWidth.toPx()))

        // Never fully empty: at 0 the ring shows a small dot at the top.
        val sweep = max(progress().coerceIn(0f, 1f) * 2f * PI.toFloat(), 0.02f)
        val steps = max(2, (sweep / (2f * PI.toFloat()) * 720f).toInt())
        val path = Path()
        for (i in 0..steps) {
            val t = sweep * i / steps
            val angle = -PI.toFloat() / 2f + t
            val r = radius + amp * sin(waves * angle - phase)
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
