package com.mapme.core.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.MAPME_EXPONENT
import com.mapme.core.design.theme.MapMeTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.withSign

/**
 * The MapMe mark.
 *
 * **The idea.** A journey that loops almost all the way back, and a dot that
 * has not closed it yet. The loop is a life's worth of movement; the dot is
 * you, still going. The mark says *map* (the enclosing form), *me* (the dot)
 * and *journey* (the taper) at once, without drawing a pin, a letter, or an
 * arrow — the three things every other location product already owns.
 *
 * **Why it looks like it does.** The loop is a superellipse at the same
 * [MAPME_EXPONENT] as every card, button and icon frame in the product, so
 * the logo is not a separate object bolted onto the design — it is the house
 * geometry at its purest. The stroke tapers from a whisper to a confident
 * head using the identical function that tapers a real journey in [TrailLine],
 * which means the brand mark and the product's core visual are the same thing
 * drawn at different lengths.
 *
 * **Why the gap.** A closed ring is a full stop. The gap, and the detached
 * dot sitting just past the head, make the mark read as unfinished on purpose:
 * your map is still being drawn. It also gives the silhouette an asymmetry
 * that survives being shrunk to a 16px favicon, where a symmetrical ring would
 * turn into an anonymous blob.
 */
private const val LOOP_RADIUS = 34f
private const val LOOP_START_DEG = 82f
private const val LOOP_END_DEG = 384f
private const val LOOP_START_HALF = 1.6f
private const val LOOP_END_HALF = 5.4f
private const val DOT_DEG = 66.8f
private const val DOT_RADIUS = 6.2f
private const val VIEWPORT = 100f

@Composable
fun MapMeMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    tint: Color = Color.Unspecified,
    /** Sits the mark inside a squircle plate, the way a launcher icon does. */
    framed: Boolean = false,
) {
    val colors = MapMeTheme.colors
    val loop = remember { Path() }
    val far = tint.takeOrElse { colors.trailFar }
    val near = tint.takeOrElse { colors.trailNear }
    val dotColor = tint.takeOrElse { colors.accent }

    val plate = if (framed) {
        Modifier
            .background(colors.surfaceRaised, MapMeTheme.radius.iconFrame)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(plate)
            .clearAndSetSemantics { contentDescription = "MapMe" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(if (framed) size * 0.68f else size)) {
            val unit = this.size.minDimension / VIEWPORT
            val centre = MarkGeometry.centreline.map { Offset(it.x * unit, it.y * unit) }
            buildTaperedOutline(loop, centre, LOOP_START_HALF * unit, LOOP_END_HALF * unit)
            drawPath(
                loop,
                Brush.linearGradient(
                    listOf(far, near),
                    start = centre.first(),
                    end = centre.last(),
                ),
            )
            val dot = dotCentre()
            drawCircle(
                color = dotColor,
                radius = DOT_RADIUS * unit,
                center = Offset(dot.x * unit, dot.y * unit),
            )
        }
    }
}

/**
 * The mark's centreline, in its own 100×100 space, computed once per process.
 *
 * The resampling matters. A superellipse races along its flat sections and
 * crawls around its corners, so stepping the angle uniformly leaves long
 * straight facets exactly where the curve is most visible. These points are
 * spaced evenly *along the curve* instead, which is why the mark stays smooth
 * blown up to 132dp on the home screen.
 */
private object MarkGeometry {
    val centreline: List<Offset> by lazy { resampleEvenly(samples = 140) }

    private fun squirclePoint(deg: Float, radius: Float): Offset {
        val rad = Math.toRadians(deg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val power = 2f / MAPME_EXPONENT
        return Offset(
            VIEWPORT / 2f + (radius * abs(c).pow(power)).withSign(c),
            VIEWPORT / 2f - (radius * abs(s).pow(power)).withSign(s),
        )
    }

    private fun resampleEvenly(samples: Int): List<Offset> {
        val dense = ArrayList<Offset>(DENSE + 1)
        for (i in 0..DENSE) {
            val deg = LOOP_START_DEG + (LOOP_END_DEG - LOOP_START_DEG) * (i.toFloat() / DENSE)
            dense += squirclePoint(deg, LOOP_RADIUS)
        }
        val cumulative = FloatArray(dense.size)
        for (i in 1 until dense.size) {
            cumulative[i] = cumulative[i - 1] + (dense[i] - dense[i - 1]).getDistance()
        }
        val total = cumulative.last()
        val out = ArrayList<Offset>(samples + 1)
        var j = 0
        for (k in 0..samples) {
            val target = total * k / samples
            while (j < cumulative.size - 2 && cumulative[j + 1] < target) j++
            val span = (cumulative[j + 1] - cumulative[j]).takeIf { it > 1e-6f } ?: 1e-6f
            val f = (target - cumulative[j]) / span
            out += dense[j] + (dense[j + 1] - dense[j]) * f
        }
        return out
    }

    private const val DENSE = 2000
}

/** The dot's centre, in the mark's own 100×100 space. */
private fun dotCentre(): Offset {
    val rad = Math.toRadians(DOT_DEG.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    val power = 2f / MAPME_EXPONENT
    return Offset(
        VIEWPORT / 2f + (LOOP_RADIUS * abs(c).pow(power)).withSign(c),
        VIEWPORT / 2f - (LOOP_RADIUS * abs(s).pow(power)).withSign(s),
    )
}


/**
 * Mark plus wordmark.
 *
 * "Map" is the world; "Me" is you, and it is the only word in the product set
 * in the accent colour. The name means *map me* — the emphasis is the point.
 */
@Composable
fun MapMeWordmark(
    modifier: Modifier = Modifier,
    markSize: Dp = 40.dp,
    textStyle: TextStyle = MapMeTheme.type.titleLarge,
    showMark: Boolean = true,
) {
    val colors = MapMeTheme.colors
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "MapMe" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3),
    ) {
        if (showMark) MapMeMark(size = markSize)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MapMeText("Map", style = textStyle, color = colors.textPrimary, maxLines = 1)
            MapMeText("Me", style = textStyle, color = colors.accent, maxLines = 1)
        }
    }
}
