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
import androidx.compose.ui.geometry.Size
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
 * Circular progress drawn as a wave that keeps flowing along the ring, following the Material 3
 * Expressive wavy indicator: round caps and a gap between the active wave and the track.
 * Material's own component has a fixed wave height meant for ~48dp, which disappears at hero
 * size, so the wave is scaled here.
 *
 * [progress] and [amplitude] (0 = flat, 1 = full wave) are read while drawing, so fast-changing
 * values do not recompose the screen.
 */
@Composable
fun WavyRing(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    amplitude: () -> Float = { 1f },
    strokeWidth: Dp = 16.dp,
    maxAmplitude: Dp = 6.dp,
    gap: Dp = 12.dp,
    waves: Int = 16,
) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase",
    )
    Canvas(modifier) {
        val twoPi = 2f * PI.toFloat()
        val stroke = strokeWidth.toPx()
        val amp = maxAmplitude.toPx() * amplitude().coerceIn(0f, 1f)
        val radius = (size.minDimension - stroke) / 2f - maxAmplitude.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val top = -PI.toFloat() / 2f

        // Never fully empty: at 0 the ring shows a small dot at the top.
        val sweep = max(progress().coerceIn(0f, 1f) * twoPi, 0.001f)
        val steps = max(2, (sweep / twoPi * 720f).toInt())
        val path = Path()
        for (i in 0..steps) {
            val angle = top + sweep * i / steps
            val r = radius + amp * sin(waves * angle - phase)
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // The track only covers what is left, with a gap on both sides of the active wave.
        val gapAngle = gap.toPx() / radius
        val trackStart = top + sweep + gapAngle
        val trackEnd = top + twoPi - gapAngle
        if (trackEnd > trackStart) {
            drawArc(
                color = trackColor,
                startAngle = Math.toDegrees(trackStart.toDouble()).toFloat(),
                sweepAngle = Math.toDegrees((trackEnd - trackStart).toDouble()).toFloat(),
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke * 0.5f, cap = StrokeCap.Round),
            )
        }
    }
}
