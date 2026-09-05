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
 * The ground MapMe sits on when there is no map yet.
 *
 * Two enormous, very faint aurora fields drift across the ink on a half-minute
 * cycle. Individually they are almost invisible; together they stop a dark
 * screen from reading as a dead one, and they cost one draw call each because
 * they are gradients rather than particles or shaders.
 *
 * It is also a [GlassBackdropHost], so panes laid on top of it genuinely
 * refract the aurora.
 *
 * This is deliberately *not* wallpaper for screens that have a map. Once the
 * map is the ground, the map is the ground.
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
        val transition = rememberInfiniteTransition(label = "AuroraDrift")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(32_000, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "AuroraDriftPhase",
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
                    .drawBehind { drawAurora(colors, phase) },
            )
        },
        content = content,
    )
}

private fun DrawScope.drawAurora(colors: MapMeColors, phase: Float) {
    drawRect(colors.canvas)

    val w = size.width
    val h = size.height
    val reach = maxOf(w, h)

    // High and cool, drifting right.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(colors.accent.copy(alpha = 0.13f), Color.Transparent),
            center = Offset(w * (0.18f + 0.24f * phase), h * (0.14f + 0.06f * phase)),
            radius = reach * 0.85f,
        ),
    )

    // Low and deep, drifting the other way.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(colors.focus.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(w * (0.92f - 0.30f * phase), h * (0.78f - 0.10f * phase)),
            radius = reach * 0.75f,
        ),
    )

    // Settle back into the ink towards the bottom, so anything sitting over
    // the lower half always has a quiet ground underneath it.
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to colors.canvas.copy(alpha = 0.55f),
            1f to colors.canvas,
        ),
    )
}
