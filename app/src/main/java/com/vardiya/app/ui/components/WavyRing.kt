package com.vardiya.app.ui.components

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hero-size Material 3 Expressive circular wavy progress indicator.
 *
 * The library defaults (4dp stroke, 15dp wavelength, 4dp gap) are meant for a ~48dp indicator; at
 * hero size they turn into a tight zigzag, so stroke, gap and wavelength keep the token proportions
 * (gap = stroke, wavelength = 3.75 × stroke) scaled to [strokeWidth].
 *
 * [progress] and [amplitude] (0 = flat, 1 = full wave) are read while drawing, so fast-changing
 * values do not recompose the screen.
 */
@Composable
fun WavyRing(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    amplitude: () -> Float = { 1f },
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    strokeWidth: Dp = 10.dp,
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val stroke = remember(strokePx) { Stroke(width = strokePx, cap = StrokeCap.Round) }
    CircularWavyProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = strokeWidth,
        amplitude = { amplitude() },
        wavelength = strokeWidth * 3.75f,
    )
}
