package com.vardiya.app.ui.components

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * Circular wavy progress set up like F-Droid's first-start indicator
 * (fdroidclient `ui/discover/FirstStart.kt`): a 128dp ring with a 6dp round stroke for both the
 * indicator and the track, a 24dp wavelength, and Material's defaults for everything else.
 * Those proportions are kept and scaled up to [size].
 *
 * [progress] is read while drawing, so a value that changes every frame does not recompose.
 */
@Composable
fun WavyRing(progress: () -> Float, size: Dp, modifier: Modifier = Modifier) {
    val scale = size / FDROID_SIZE
    val stroke = Stroke(width = with(LocalDensity.current) { (FDROID_STROKE * scale).toPx() }, cap = StrokeCap.Round)
    CircularWavyProgressIndicator(
        progress = progress,
        wavelength = FDROID_WAVELENGTH * scale,
        stroke = stroke,
        trackStroke = stroke,
        modifier = modifier.then(Modifier.size(size)),
    )
}

private val FDROID_SIZE = 128.dp
private val FDROID_STROKE = 6.dp
private val FDROID_WAVELENGTH = 24.dp
