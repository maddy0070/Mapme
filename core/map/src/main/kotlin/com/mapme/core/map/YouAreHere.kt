package com.mapme.core.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeTheme

/**
 * You, on the map.
 *
 * ## Why this is drawn here and not by the engine
 *
 * Every map SDK ships a location component, and every one of them looks like
 * a map SDK. This is three circles in Compose, drawn from the same tokens as
 * the rest of the product, which means it inherits the theme — including the
 * theme *transition* — for free: it is part of the recorded snapshot like
 * everything else, so it crosses from night to paper with the boundary rather
 * than blinking on its own schedule.
 *
 * ## What the three circles mean
 *
 * The halo is not decoration: its radius is the accuracy the system actually
 * reported, converted through the map's own scale. A vague fix draws a wide
 * soft disc and a good one draws a tight dot, so the picture is honest about
 * how well MapMe knows where you are instead of asserting a confident point
 * over a 200-metre guess.
 *
 * The ring exists so the dot survives being over water, a park, or a bright
 * road, without a drop shadow.
 *
 * @param accuracyRadiusPx the reported accuracy already converted to pixels
 *   at this latitude and zoom, so this composable does no geography.
 */
@Composable
internal fun YouAreHere(
    centre: Offset,
    accuracyRadiusPx: Float,
    modifier: Modifier = Modifier,
    label: String,
) {
    val colors = MapMeTheme.colors
    val reduceMotion = LocalReduceMotion.current

    // One slow breath. Enough to say "live", not enough to notice twice.
    val pulse = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "YouAreHere")
        val value by transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "YouAreHerePulse",
        )
        value
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = label },
    ) {
        if (centre.x.isNaN() || centre.y.isNaN()) return@Canvas

        // Clamped at both ends: a halo smaller than the dot is invisible, and
        // one the size of the screen is just a wash over the map.
        val halo = accuracyRadiusPx.coerceIn(DOT_RADIUS_PX * 2f, size.minDimension * 0.75f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.accent.copy(alpha = 0.20f),
                    colors.accent.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = centre,
                radius = halo,
            ),
            radius = halo,
            center = centre,
        )

        drawCircle(
            color = colors.accent.copy(alpha = 0.28f),
            radius = DOT_RADIUS_PX * 2.1f * pulse,
            center = centre,
        )
        drawCircle(color = colors.onAccent, radius = DOT_RADIUS_PX * 1.5f, center = centre)
        drawCircle(color = colors.accent, radius = DOT_RADIUS_PX, center = centre)
    }
}

/** Roughly 7dp at 3x. The dot is small on purpose: it is a position, not a pin. */
private const val DOT_RADIUS_PX = 11f
