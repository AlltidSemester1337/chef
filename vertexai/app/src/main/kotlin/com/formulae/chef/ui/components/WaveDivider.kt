package com.formulae.chef.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.formulae.chef.ui.theme.GenerativeAISample
import com.formulae.chef.ui.theme.Terracotta100

// Matches Figma's "Decorative element" wave motif (node 28:4540): a background-colored wave
// cutout, followed by three cascading colored wave strokes fading from coral to pale pink.
private val WaveAccentPrimary = Color(0xFFE07A5F)
private val WaveAccentSecondary = Color(0xFFF3BCAE)

private val WaveLength = 94.dp
private val LayerAmplitude = 6.dp
private val StrokeWidth = 3.dp
private val FrameHeight = 20.dp

// How far the frame's top sits above the image's true bottom edge — the wave cuts into that
// overlap, and the cascade trails (FrameHeight - OverlapIntoImage) further below the image.
private val OverlapIntoImage = 11.dp

private data class WaveLayer(val baseY: Dp, val color: Color)

private val waveLayers = listOf(
    WaveLayer(7.5.dp, WaveAccentPrimary),
    WaveLayer(9.5.dp, WaveAccentSecondary),
    WaveLayer(12.5.dp, Terracotta100)
)

private fun buildWavePath(width: Float, baseY: Float, amplitudePx: Float, wavelengthPx: Float): Path =
    Path().apply {
        moveTo(0f, baseY)
        var x = 0f
        while (x < width + wavelengthPx) {
            quadraticTo(x + wavelengthPx / 4f, baseY - amplitudePx, x + wavelengthPx / 2f, baseY)
            quadraticTo(x + wavelengthPx * 3f / 4f, baseY + amplitudePx, x + wavelengthPx, baseY)
            x += wavelengthPx
        }
    }

private fun DrawScope.drawWaveLayers() {
    val amplitudePx = LayerAmplitude.toPx()
    val wavelengthPx = WaveLength.toPx()
    val strokePx = StrokeWidth.toPx()
    waveLayers.forEach { layer ->
        val path = buildWavePath(size.width, layer.baseY.toPx(), amplitudePx, wavelengthPx)
        drawPath(path, color = layer.color, style = Stroke(width = strokePx))
    }
}

/** A standalone decorative wave divider matching the Figma motif, for use between content sections. */
@Composable
fun WaveDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(FrameHeight)
    ) {
        drawWaveLayers()
    }
}

/**
 * The same wave motif, meant to overlay the bottom [OverlapIntoImage] of a [WavyBottomShape]-clipped
 * image and trail below it. Align to [androidx.compose.ui.Alignment.BottomStart] and offset down by
 * `FrameHeight - OverlapIntoImage` (9.dp) so the cascade lines up with the clipped edge.
 */
@Composable
fun BottomWaveAccent(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(FrameHeight)
    ) {
        drawWaveLayers()
    }
}

val BottomWaveAccentOffset: Dp = FrameHeight - OverlapIntoImage

/** Clips content (e.g. a hero image) with a wavy bottom edge matching the background-colored mask
 * behind [BottomWaveAccent] (Figma node 28:4540's "Vector 5"). */
class WavyBottomShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val amplitudePx = with(density) { LayerAmplitude.toPx() }
        val baseY = size.height - with(density) { (OverlapIntoImage - 7.dp).toPx() }
        val path = buildWavePath(size.width, baseY, amplitudePx, with(density) { WaveLength.toPx() }).apply {
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFDFC)
@Composable
private fun WaveDividerPreview() {
    GenerativeAISample {
        WaveDivider()
    }
}
