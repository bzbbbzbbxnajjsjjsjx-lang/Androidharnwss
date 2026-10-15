package com.vardiya.app.ui.components

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/** Material 3 Expressive shapes used for morphing: a circle and a 9-sided cookie. */
object VardiyaShapes {
    val Circle: RoundedPolygon = RoundedPolygon.circle(numVertices = 9)
    val Cookie: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.8f,
        rounding = CornerRounding(radius = 0.3f),
    )
    val Burst: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        innerRadius = 0.7f,
        rounding = CornerRounding(radius = 0.15f),
    )
}

/**
 * Draws [morph] at [progress] (0 = start shape, 1 = end shape), scaled to fill the box,
 * with [content] centered on top.
 */
@Composable
fun MorphingShape(
    morph: Morph,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    content: @Composable () -> Unit = {},
) {
    val androidPath = remember { android.graphics.Path() }
    val bounds = remember { RectF() }
    val matrix = remember { Matrix() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            androidPath.rewind()
            morph.toPath(progress, androidPath)
            androidPath.computeBounds(bounds, true)
            matrix.setRectToRect(bounds, RectF(0f, 0f, size.width, size.height), Matrix.ScaleToFit.CENTER)
            androidPath.transform(matrix)
            rotate(rotation) {
                drawPath(androidPath.asComposePath(), color)
            }
        }
        content()
    }
}
