package com.mapme.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.MapMeTheme

/**
 * The MapMe mark: an M drawn as a journey.
 *
 * Five points — bottom, peak, valley, peak, bottom — smoothed by the same
 * spline that draws real trails, in the same gradient, ending in the same live
 * head. It is not a logo that happens to sit next to the product; it is one
 * frame of what the product does, which is why it can also be *drawn* on
 * launch instead of merely appearing.
 */
private val MarkTrail = listOf(
    Offset(0.12f, 0.84f),
    Offset(0.28f, 0.18f),
    Offset(0.50f, 0.62f),
    Offset(0.72f, 0.18f),
    Offset(0.88f, 0.84f),
)

@Composable
fun MapMeMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    progress: Float = 1f,
    container: Boolean = true,
    head: Boolean = true,
) {
    val colors = MapMeTheme.colors
    val containerModifier = if (container) {
        Modifier
            // A squircle-ish 32% radius: soft enough to feel modern, square
            // enough to survive Android's adaptive-icon mask.
            .clip(RoundedCornerShape(percent = 30))
            .background(
                Brush.linearGradient(
                    listOf(colors.surfaceRaised, colors.surface),
                ),
            )
            .border(1.dp, colors.outline, RoundedCornerShape(percent = 30))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(containerModifier)
            .clearAndSetSemantics { contentDescription = "MapMe" },
        contentAlignment = Alignment.Center,
    ) {
        TrailLine(
            points = MarkTrail,
            modifier = Modifier
                .size(size)
                .padding(size * if (container) 0.20f else 0.06f),
            progress = progress,
            head = head,
            strokeWidth = (size.value * 0.075f).dp.coerceAtLeast(2.dp),
        )
    }
}

/**
 * Mark plus wordmark.
 *
 * "Map" is the world; "Me" is you, and it is the only word in the product set
 * in the accent colour. The name means *map me*, so the emphasis is the whole
 * point of the logotype.
 */
@Composable
fun MapMeWordmark(
    modifier: Modifier = Modifier,
    markSize: Dp = 44.dp,
    textStyle: TextStyle = MapMeTheme.type.titleLarge,
    progress: Float = 1f,
    showMark: Boolean = true,
) {
    val colors = MapMeTheme.colors
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "MapMe" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3),
    ) {
        if (showMark) {
            MapMeMark(size = markSize, progress = progress, container = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            MapMeText(text = "Map", style = textStyle, color = colors.textPrimary, maxLines = 1)
            MapMeText(text = "Me", style = textStyle, color = colors.accent, maxLines = 1)
        }
    }
}
