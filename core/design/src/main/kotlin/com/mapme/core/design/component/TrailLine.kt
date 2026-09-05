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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeTheme

/**
 * The journey line. The most important thing MapMe draws.
 *
 * This is not a navigation route and not a GPS trace. Four decisions make the
 * difference:
 *
 * 1. **It is smoothed, not polygonal.** Real movement curves. A Catmull-Rom
 *    spline through the samples means the line reads as a path someone walked
 *    rather than a list of coordinates someone plotted.
 * 2. **It carries time in its colour.** The far end is [MapMeColors.trailFar],
 *    the near end [MapMeColors.trailNear] — so the gradient itself tells you
 *    which way the day ran, without a single arrowhead.
 * 3. **It glows.** A wide, faint pass under the sharp stroke lifts the line off
 *    a dark map, and keeps it visible when you zoom out far enough that the
 *    line is only a few pixels wide.
 * 4. **It can be drawn rather than shown.** [progress] reveals the line along
 *    its own length, which is what makes replay feel like rediscovery instead
 *    of playback.
 *
 * @param points the journey in normalised space — (0,0) top-left to (1,1)
 *   bottom-right — so the same trail can be drawn at any size.
 * @param progress how much of the line exists yet, 0..1.
 * @param head draws the live marker at the leading end.
 */
@Composable
fun TrailLine(
    points: List<Offset>,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    head: Boolean = true,
    strokeWidth: Dp = 5.dp,
    farColor: Color = MapMeTheme.colors.trailFar,
    nearColor: Color = MapMeTheme.colors.trailNear,
    headColor: Color = MapMeTheme.colors.trailHead,
) {
    val reduceMotion = LocalReduceMotion.current
    val path = remember(points) { smoothPathThrough(points) }

    // The head breathes. It is the only thing on screen that says "still
    // happening", so it is the only thing allowed to loop forever — and the
    // loop is not even started when motion is reduced.
    val pulse: Float = if (reduceMotion) {
        0.45f
    } else {
        val transition = rememberInfiniteTransition(label = "TrailHeadPulse")
        val breathing by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "TrailHeadPulseValue",
        )
        breathing
    }

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val scaled = Path()
        scaled.addPath(path)
        // Normalised path -> pixels. Scaling the path rather than the points
        // keeps the spline maths resolution-independent.
        val matrix = androidx.compose.ui.graphics.Matrix().apply {
            scale(size.width, size.height, 1f)
        }
        scaled.transform(matrix)

        val visible = if (progress >= 1f) {
            scaled
        } else {
            val measure = PathMeasure().apply { setPath(scaled, false) }
            Path().also { measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), it, true) }
        }

        val brush = Brush.linearGradient(
            colors = listOf(farColor, nearColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )

        val stroke = strokeWidth.toPx()

        // Bloom, then core. Two passes is enough to read as light; three
        // starts costing frames on mid-range hardware for no visible gain.
        drawPath(
            path = visible,
            brush = brush,
            alpha = 0.16f,
            style = Stroke(width = stroke * 3.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = visible,
            brush = brush,
            alpha = 0.30f,
            style = Stroke(width = stroke * 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = visible,
            brush = brush,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        if (head) {
            val measure = PathMeasure().apply { setPath(scaled, false) }
            val distance = measure.length * progress.coerceIn(0f, 1f)
            drawTrailHead(
                center = measure.getPosition(distance),
                radius = stroke,
                color = headColor,
                pulse = pulse,
            )
        }
    }
}

private fun DrawScope.drawTrailHead(
    center: Offset,
    radius: Float,
    color: Color,
    pulse: Float,
) {
    drawCircle(color = color, radius = radius * (2.6f + pulse * 1.9f), center = center, alpha = 0.10f)
    drawCircle(color = color, radius = radius * (1.7f + pulse * 0.7f), center = center, alpha = 0.22f)
    drawCircle(color = color, radius = radius * 1.05f, center = center)
    // A cool core stops the dot from turning into a blob of pink at small sizes.
    drawCircle(color = Color.White, radius = radius * 0.38f, center = center, alpha = 0.85f)
}

/**
 * A Catmull-Rom spline through every point, expressed as cubic Béziers.
 *
 * Catmull-Rom is the right curve here because it passes *through* the samples
 * rather than near them: a smoothed journey that no longer touches the places
 * you actually went would be a lie, however pretty.
 */
internal fun smoothPathThrough(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    for (i in 0 until points.size - 1) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.size - 1)]

        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)

        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}
