package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.withSign

/**
 * MapMe's corner.
 *
 * Android's `RoundedCornerShape` sweeps a **circular** arc, which meets the
 * straight edge at an abrupt change in curvature — the eye reads it as a
 * rectangle with its corners cut off. MapMe instead uses a **superellipse**:
 *
 * ```
 * |x/r|^n + |y/r|^n = 1
 * ```
 *
 * At `n = 2` that is a circle, which is exactly the shape we are avoiding. At
 * [MAPME_EXPONENT] the curvature eases into the edge instead of arriving at
 * it, which is what makes the form read as *squarical* rather than spherical,
 * and what lets every surface in the product — buttons, cards, icon frames,
 * the logo itself — share one family resemblance.
 *
 * This is the single most repeated shape in MapMe, so it is worth the maths.
 */
@Immutable
class SquircleShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val exponent: Float = MAPME_EXPONENT,
) : Shape {

    constructor(all: Dp, exponent: Float = MAPME_EXPONENT) :
        this(all, all, all, all, exponent)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val limit = minOf(size.width, size.height) / 2f
        fun px(dp: Dp) = with(density) { dp.toPx() }.coerceIn(0f, limit)
        return Outline.Generic(
            squirclePath(
                size = size,
                topStart = px(topStart),
                topEnd = px(topEnd),
                bottomEnd = px(bottomEnd),
                bottomStart = px(bottomStart),
                exponent = exponent,
            ),
        )
    }

    override fun equals(other: Any?): Boolean =
        other is SquircleShape &&
            topStart == other.topStart && topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd && bottomStart == other.bottomStart &&
            exponent == other.exponent

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + exponent.hashCode()
        return result
    }
}

/** The MapMe corner constant. Higher is squarer; 2.0 would be a plain circle. */
const val MAPME_EXPONENT: Float = 4.2f

/** Samples per corner. Sixteen is past the point where an eye can see facets. */
private const val CORNER_SAMPLES = 16

internal fun squirclePath(
    size: Size,
    topStart: Float,
    topEnd: Float,
    bottomEnd: Float,
    bottomStart: Float,
    exponent: Float,
): Path {
    val w = size.width
    val h = size.height
    val p = Path()

    p.moveTo(topStart, 0f)
    p.lineTo(w - topEnd, 0f)
    p.corner(w - topEnd, topEnd, topEnd, 270f, 360f, exponent)
    p.lineTo(w, h - bottomEnd)
    p.corner(w - bottomEnd, h - bottomEnd, bottomEnd, 0f, 90f, exponent)
    p.lineTo(bottomStart, h)
    p.corner(bottomStart, h - bottomStart, bottomStart, 90f, 180f, exponent)
    p.lineTo(0f, topStart)
    p.corner(topStart, topStart, topStart, 180f, 270f, exponent)
    p.close()
    return p
}

/**
 * Sweeps one superellipse quarter around ([cx], [cy]).
 *
 * The `2/n` exponent is what turns the unit circle's cosine/sine into the
 * superellipse: at n = 2 it collapses to 1 and you get an ordinary arc back.
 */
private fun Path.corner(
    cx: Float,
    cy: Float,
    r: Float,
    fromDeg: Float,
    toDeg: Float,
    exponent: Float,
) {
    if (r <= 0f) return
    val power = 2f / exponent
    for (i in 0..CORNER_SAMPLES) {
        val t = i.toFloat() / CORNER_SAMPLES
        val rad = Math.toRadians((fromDeg + (toDeg - fromDeg) * t).toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        lineTo(
            cx + (r * abs(c).pow(power)).withSign(c),
            cy + (r * abs(s).pow(power)).withSign(s),
        )
    }
}

/**
 * MapMe's corner radii.
 *
 * The rule: the larger the surface, the larger the radius, so nothing ever
 * looks like a scaled-up version of something smaller.
 *
 * Note what is missing — there are **no pills**. A fully rounded capsule is
 * the single most common button shape in modern apps, which is precisely why
 * MapMe does not use one. Every interactive surface is a squircle.
 */
@Immutable
data class MapMeRadius(
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 18.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 30.dp,
    val xxl: Dp = 40.dp,
) {
    val chip: Shape get() = SquircleShape(sm)
    val control: Shape get() = SquircleShape(md)
    val button: Shape get() = SquircleShape(lg)
    val card: Shape get() = SquircleShape(xl)
    val panel: Shape get() = SquircleShape(xxl)
    val thumbnail: Shape get() = SquircleShape(md)

    /** Sheets curve at the top and meet the screen edge square. */
    val sheet: Shape get() = SquircleShape(xxl, xxl, 0.dp, 0.dp)

    /** The frame an icon sits inside. Matches the launcher-icon mask family. */
    val iconFrame: Shape get() = SquircleShape(md)
}
