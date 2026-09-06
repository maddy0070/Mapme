package com.mapme.core.design.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeTheme
import kotlin.math.hypot

/**
 * The journey line. The most important thing MapMe draws.
 *
 * This is not a navigation route and not a GPS trace. Four decisions make the
 * difference:
 *
 * 1. **It tapers.** The line starts as a whisper and arrives thick. Age is
 *    carried by weight, which survives being zoomed out to nothing — a
 *    uniform stroke cannot tell you which end is now.
 * 2. **It is smoothed, not polygonal.** A Catmull-Rom spline passes *through*
 *    every sample, so the line reads as movement without ever inventing a
 *    place nobody went.
 * 3. **It behaves differently in each mode.** On night it is light, and blooms.
 *    On paper it is pigment: no glow — a bloom on white just looks like a
 *    printing error — so it gains density and a soft tinted shadow instead.
 * 4. **It can be drawn rather than shown.** [progress] reveals the line along
 *    its own length, which is what will one day make replay feel like
 *    rediscovery instead of playback.
 *
 * @param points the journey in thread space — (0,0) top-left to (1,1).
 * @param camera where to stand relative to the drawing.
 * @param markers indices of [points] worth marking as places.
 */
@Composable
fun TrailLine(
    points: List<Offset>,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    camera: ThreadCamera = FullView,
    strokeWidth: Dp = 6.dp,
    /** Width at the oldest end, as a fraction of the head. */
    taper: Float = 0.22f,
    head: Boolean = true,
    markers: List<Int> = emptyList(),
    markerAlpha: Float = 1f,
) {
    val colors = MapMeTheme.colors
    val reduceMotion = LocalReduceMotion.current

    val pulse: Float = if (reduceMotion) {
        0.45f
    } else {
        val transition = rememberInfiniteTransition(label = "TrailHead")
        val breathing by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1900, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "TrailHeadPulse",
        )
        breathing
    }

    // Reused across frames so revealing the line does not allocate a new Path
    // sixty times a second.
    val corePath = remember { Path() }
    val glowPath = remember { Path() }

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val visible = visibleSpline(points, progress, camera, size)
        if (visible.size < 2) return@Canvas

        val width = strokeWidth.toPx()
        glowPath.reset()
        glowPath.moveTo(visible[0].x, visible[0].y)
        for (i in 1 until visible.size) glowPath.lineTo(visible[i].x, visible[i].y)

        val brush = Brush.linearGradient(
            colors = listOf(colors.trailFar, colors.trailNear),
            start = visible.first(),
            end = visible.last(),
        )

        if (colors.trailGlows) {
            // Light in a dark room. Two diffuse passes read as bloom; three
            // costs frames for nothing the eye can find.
            drawPath(
                glowPath,
                brush,
                alpha = 0.13f,
                style = Stroke(width * 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                glowPath,
                brush,
                alpha = 0.22f,
                style = Stroke(width * 2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            // Pigment on paper. A soft offset shadow gives the line weight
            // without pretending it emits light.
            translate(0f, width * 0.42f) {
                drawPath(
                    glowPath,
                    color = colors.shadowTint,
                    alpha = 0.10f,
                    style = Stroke(width * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        buildTaperedOutline(corePath, visible, width * taper * 0.5f, width * 0.5f)
        drawPath(corePath, brush)

        markers.forEach { index ->
            val at = markerPosition(points, index, progress, camera, size) ?: return@forEach
            drawPlace(at, width, colors.trailNear, colors.canvas, markerAlpha)
        }

        if (head) {
            drawHead(visible.last(), width, colors.trailHead, colors.trailNear, pulse, colors.trailGlows)
        }
    }
}

/** Standing far enough back to see the whole thread. */
val FullView: ThreadCamera = ThreadCamera(scale = 1f, focus = Offset(0.5f, 0.5f))

// --- drawing ---------------------------------------------------------------

private fun DrawScope.drawPlace(
    centre: Offset,
    width: Float,
    color: Color,
    ground: Color,
    alpha: Float,
) {
    if (alpha <= 0.01f) return
    drawCircle(color, radius = width * 1.35f, center = centre, alpha = 0.22f * alpha)
    drawCircle(ground, radius = width * 0.72f, center = centre, alpha = alpha)
    drawCircle(color, radius = width * 0.44f, center = centre, alpha = alpha)
}

private fun DrawScope.drawHead(
    centre: Offset,
    width: Float,
    headColor: Color,
    haloColor: Color,
    pulse: Float,
    glows: Boolean,
) {
    val r = width * 0.5f
    if (glows) {
        drawCircle(haloColor, radius = r * (3.4f + pulse * 2.6f), center = centre, alpha = 0.10f)
        drawCircle(haloColor, radius = r * (2.1f + pulse * 1.1f), center = centre, alpha = 0.20f)
    } else {
        drawCircle(haloColor, radius = r * (2.4f + pulse * 1.0f), center = centre, alpha = 0.16f)
    }
    drawCircle(headColor, radius = r * 1.5f, center = centre)
}

/**
 * Builds a filled outline whose half-width runs [startHalf] → [endHalf].
 *
 * A tapering stroke is not something Canvas can express, so the taper is drawn
 * as a shape: walk the centreline offsetting to one side, then walk back
 * offsetting to the other.
 */
internal fun buildTaperedOutline(
    into: Path,
    centre: List<Offset>,
    startHalf: Float,
    endHalf: Float,
) {
    into.reset()
    val n = centre.size
    if (n < 2) return

    fun halfWidthAt(i: Int): Float {
        val t = i.toFloat() / (n - 1)
        // Eased so the thickening is felt near the head, where it means "now".
        return startHalf + (endHalf - startHalf) * (t * t * (3f - 2f * t))
    }

    fun normalAt(i: Int): Offset {
        val a = centre[if (i == 0) 0 else i - 1]
        val b = centre[if (i == n - 1) n - 1 else i + 1]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy).takeIf { it > 1e-4f } ?: 1f
        return Offset(-dy / len, dx / len)
    }

    val first = centre[0]
    val n0 = normalAt(0)
    val w0 = halfWidthAt(0)
    into.moveTo(first.x + n0.x * w0, first.y + n0.y * w0)
    for (i in 1 until n) {
        val p = centre[i]
        val nrm = normalAt(i)
        val w = halfWidthAt(i)
        into.lineTo(p.x + nrm.x * w, p.y + nrm.y * w)
    }
    for (i in n - 1 downTo 0) {
        val p = centre[i]
        val nrm = normalAt(i)
        val w = halfWidthAt(i)
        into.lineTo(p.x - nrm.x * w, p.y - nrm.y * w)
    }
    into.close()
}

// --- geometry --------------------------------------------------------------

private fun project(p: Offset, camera: ThreadCamera, size: Size): Offset {
    val base = minOf(size.width, size.height)
    return Offset(
        size.width / 2f + (p.x - camera.focus.x) * camera.scale * base,
        size.height / 2f + (p.y - camera.focus.y) * camera.scale * base,
    )
}

private fun markerPosition(
    points: List<Offset>,
    index: Int,
    progress: Float,
    camera: ThreadCamera,
    size: Size,
): Offset? {
    val reachedUpTo = (points.size - 1) * progress.coerceIn(0f, 1f)
    if (index > reachedUpTo) return null
    return project(points[index], camera, size)
}

/**
 * The visible span of the thread, projected to pixels and smoothed.
 *
 * Subdivision scales with the camera: pulled back, the raw samples are already
 * closer together than a pixel and subdividing would burn frames for nothing.
 */
private fun visibleSpline(
    points: List<Offset>,
    progress: Float,
    camera: ThreadCamera,
    size: Size,
): List<Offset> {
    val clamped = progress.coerceIn(0f, 1f)
    val exact = (points.size - 1) * clamped
    val lastIndex = exact.toInt()
    val tail = exact - lastIndex

    val raw = ArrayList<Offset>(lastIndex + 2)
    for (i in 0..lastIndex) raw += project(points[i], camera, size)
    if (tail > 0f && lastIndex + 1 < points.size) {
        val a = project(points[lastIndex], camera, size)
        val b = project(points[lastIndex + 1], camera, size)
        raw += Offset(a.x + (b.x - a.x) * tail, a.y + (b.y - a.y) * tail)
    }
    if (raw.size < 3) return raw

    val subdivisions = camera.scale.coerceIn(1f, 4f).toInt()
    if (subdivisions <= 1) return raw
    return catmullRom(raw, subdivisions)
}

/**
 * Catmull-Rom through every point.
 *
 * It passes through the samples rather than near them, which matters: a
 * smoothed journey that no longer touches the places you actually went would
 * be a prettier lie.
 */
internal fun catmullRom(points: List<Offset>, subdivisions: Int): List<Offset> {
    val n = points.size
    if (n < 3 || subdivisions < 2) return points
    val out = ArrayList<Offset>(n * subdivisions)
    for (i in 0 until n - 1) {
        val p0 = points[if (i == 0) 0 else i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 >= n) n - 1 else i + 2]
        for (s in 0 until subdivisions) {
            val t = s.toFloat() / subdivisions
            val t2 = t * t
            val t3 = t2 * t
            out += Offset(
                0.5f * ((2f * p1.x) + (-p0.x + p2.x) * t +
                    (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                    (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3),
                0.5f * ((2f * p1.y) + (-p0.y + p2.y) * t +
                    (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                    (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3),
            )
        }
    }
    out += points.last()
    return out
}
