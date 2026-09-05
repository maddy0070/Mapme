package com.mapme.core.design.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mapme.core.design.icon.MapMeIcon
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeTheme

enum class PillTone {
    /** Context: a date, a place name, a count. */
    Neutral,

    /** Recording, right now. Comes with a heartbeat. */
    Live,

    /** Something good. */
    Accent,

    /** A record or a first. */
    Milestone,
}

/**
 * A small piece of state, worn on the interface.
 *
 * Pills say what *is*, never what to do — if tapping it does something, it is a
 * button, and it should look like one.
 */
@Composable
fun MapMePill(
    text: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Neutral,
    icon: ImageVector? = null,
) {
    val colors = MapMeTheme.colors
    val (foreground, background) = when (tone) {
        PillTone.Neutral -> colors.textSecondary to colors.glassTint
        PillTone.Live -> colors.live to colors.liveSubtle
        PillTone.Accent -> colors.accent to colors.accentSubtle
        PillTone.Milestone -> colors.milestone to colors.milestoneSubtle
    }

    Row(
        modifier = modifier
            .clip(MapMeTheme.radius.chip)
            .background(background)
            .border(1.dp, colors.outline, MapMeTheme.radius.chip)
            .padding(horizontal = MapMeTheme.space.x3, vertical = MapMeTheme.space.x2 - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2 - 2.dp),
    ) {
        when {
            tone == PillTone.Live -> LiveDot(color = foreground)
            icon != null -> MapMeIcon(icon = icon, contentDescription = null, size = 14.dp, tint = foreground)
        }
        MapMeText(
            text = text,
            style = MapMeTheme.type.labelSmall,
            color = foreground,
            maxLines = 1,
        )
    }
}

/**
 * The heartbeat.
 *
 * A solid dot with a halo that expands and fades, once every 1.4 seconds —
 * roughly a resting pulse, which is why it reads as alive rather than as a
 * blinking indicator light. It is the one loop MapMe runs indefinitely, and it
 * runs only while something is genuinely being recorded.
 */
@Composable
fun LiveDot(
    color: Color = MapMeTheme.colors.live,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val beat: Float = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "LiveDot")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Restart,
            ),
            label = "LiveDotBeat",
        )
        value
    }

    Box(
        modifier = modifier
            .size(14.dp)
            .drawBehind {
                val core = size.minDimension * 0.28f
                if (beat > 0f) {
                    drawCircle(
                        color = color,
                        radius = core + (size.minDimension * 0.5f - core) * beat,
                        alpha = 0.45f * (1f - beat),
                    )
                }
                drawCircle(color = color, radius = core)
            },
    )
}
