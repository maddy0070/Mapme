package com.mapme.core.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.haptics.LocalMapMeHaptics
import com.mapme.core.design.theme.LocalMapMeContentColor
import com.mapme.core.design.theme.MapMeTheme

/**
 * How loud a button is.
 *
 * There is exactly one [Primary] button on a screen. If a screen seems to need
 * two, one of them is really a [Secondary].
 */
enum class MapMeButtonStyle {
    /** The one thing this screen wants you to do. Accent fill. */
    Primary,

    /** A real alternative. Glass, with an edge. */
    Secondary,

    /** Available, but quiet. No container at all. */
    Ghost,
}

enum class MapMeButtonSize {
    /** Full-width commitments: start recording, grant permission. */
    Large,

    /** Everything else. */
    Medium,
}

/**
 * A MapMe button.
 *
 * MapMe buttons do not ripple. A ripple is a splash of ink spreading through
 * paper — the wrong metaphor for glass. Instead the button *compresses* under
 * a finger and springs back, which is what a physical control does, and it is
 * paired with a haptic so the feedback lands in two senses at once.
 *
 * Every button is a pill. There are no rectangular buttons in MapMe.
 */
@Composable
fun MapMeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MapMeButtonStyle = MapMeButtonStyle.Primary,
    size: MapMeButtonSize = MapMeButtonSize.Medium,
    enabled: Boolean = true,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MapMeTheme.colors
    val motion = MapMeTheme.motion
    val haptics = LocalMapMeHaptics.current

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.965f else 1f,
        animationSpec = motion.snappy(),
        label = "MapMeButtonPress",
    )

    val height: Dp = when (size) {
        MapMeButtonSize.Large -> 56.dp
        MapMeButtonSize.Medium -> 48.dp
    }
    val horizontalPadding: Dp = when (size) {
        MapMeButtonSize.Large -> 28.dp
        MapMeButtonSize.Medium -> 22.dp
    }

    val contentColor = when (style) {
        MapMeButtonStyle.Primary -> colors.textOnAccent
        MapMeButtonStyle.Secondary -> colors.textPrimary
        MapMeButtonStyle.Ghost -> colors.accent
    }

    val shape = MapMeTheme.radius.button

    var container = Modifier
        .height(height)
        // Never smaller than a comfortable target, whatever the label says.
        .defaultMinSize(minWidth = MapMeTheme.space.minTouchTarget)
        .scale(scale)

    container = when (style) {
        MapMeButtonStyle.Primary -> container
            .shadow(
                elevation = MapMeTheme.depth.raised.elevation,
                shape = shape,
                clip = false,
                ambientColor = MapMeTheme.depth.raised.ambient,
                spotColor = MapMeTheme.depth.raised.spot,
            )
            .clip(shape)
            // A flat fill reads as a sticker; the gradient gives the pill a
            // top-lit curvature that matches the glass around it.
            .background(
                Brush.verticalGradient(
                    listOf(colors.accent, colors.accentMuted),
                ),
            )

        MapMeButtonStyle.Secondary -> container
            .clip(shape)
            .background(colors.glassTint)
            .border(1.dp, colors.outlineStrong, shape)

        MapMeButtonStyle.Ghost -> container.clip(shape)
    }

    Row(
        modifier = modifier
            .then(container)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                when (style) {
                    MapMeButtonStyle.Primary -> haptics.confirm()
                    else -> haptics.select()
                }
                onClick()
            }
            .padding(
                PaddingValues(
                    horizontal = if (style == MapMeButtonStyle.Ghost) 12.dp else horizontalPadding,
                ),
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalMapMeContentColor provides contentColor) {
            leading?.invoke(this@Row)
            MapMeText(
                text = text,
                style = MapMeTheme.type.button,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
