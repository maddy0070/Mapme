package com.mapme.core.design.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeColors
import com.mapme.core.design.theme.MapMeTheme

/**
 * The ground MapMe stands on before there is a map to stand on.
 *
 * Two enormous, very faint colour fields drift across it on a half-minute
 * cycle — rose high, indigo low. Individually they are almost invisible;
 * together they stop the screen from reading as dead, and they cost one draw
 * call each because they are gradients rather than particles or shaders.
 *
 * The two modes are lit differently. Night is a room with two distant lights
 * in it. Paper is a sheet with the faintest warm bloom, because a bright wash
 * on white reads as a printing fault rather than atmosphere — so on light the
 * fields are weaker and the vignette is gone entirely.
 *
 * It is also a [GlassBackdropHost], so panes laid over it genuinely refract
 * what is behind them. This is deliberately *not* wallpaper for screens that
 * have a map: once the map is the ground, the map is the ground.
 */
@Composable
fun MapMeBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = MapMeTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val drifting = animated && !reduceMotion

    val phase: Float = if (drifting) {
        val transition = rememberInfiniteTransition(label = "AtmosphereDrift")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(34_000, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "AtmospherePhase",
        )
        value
    } else {
        0.5f
    }

    GlassBackdropHost(
        modifier = modifier.fillMaxSize(),
        backdrop = {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawAtmosphere(colors, phase) },
            )
        },
        content = content,
    )
}

private fun DrawScope.drawAtmosphere(colors: MapMeColors, phase: Float) {
    drawRect(colors.canvas)

    val w = size.width
    val h = size.height
    val reach = maxOf(w, h)
    val dark = colors.isDark

    // Rose, high and warm, drifting right.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.accent.copy(alpha = if (dark) 0.15f else 0.10f),
                Color.Transparent,
            ),
            center = Offset(w * (0.16f + 0.26f * phase), h * (0.12f + 0.07f * phase)),
            radius = reach * 0.88f,
        ),
    )

    // Indigo, low and cool, drifting the other way.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.focus.copy(alpha = if (dark) 0.13f else 0.07f),
                Color.Transparent,
            ),
            center = Offset(w * (0.90f - 0.28f * phase), h * (0.82f - 0.10f * phase)),
            radius = reach * 0.78f,
        ),
    )

    if (dark) {
        // Settle back into the ink low down, so content over the bottom half
        // always has a quiet ground beneath it.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to colors.canvas.copy(alpha = 0.5f),
                1f to colors.canvas,
            ),
        )
    }
}
