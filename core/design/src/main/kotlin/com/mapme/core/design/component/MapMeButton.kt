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
 * How loud a button is. There is exactly one [Primary] per screen; if a screen
 * seems to need two, one of them is really a [Secondary].
 */
enum class MapMeButtonStyle { Primary, Secondary, Ghost }

enum class MapMeButtonSize { Large, Medium }

/**
 * A MapMe button.
 *
 * **It is a squircle, not a pill.** The fully rounded capsule is the single
 * most common button shape in modern apps, which is exactly why MapMe does not
 * use one. The corner here is the same superellipse as every card and the logo
 * itself, so a button looks like it was cut from the same material as
 * everything around it.
 *
 * **It does not ripple.** A ripple is ink spreading through paper — the wrong
 * physics for a pane of glass. Instead the button compresses under a finger
 * and springs back, paired with a haptic, so the feedback lands in two senses
 * at once.
 */
@Composable
fun MapMeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MapMeButtonStyle = MapMeButtonStyle.Primary,
    size: MapMeButtonSize = MapMeButtonSize.Medium,
    enabled: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MapMeTheme.colors
    val motion = MapMeTheme.motion
    val haptics = LocalMapMeHaptics.current

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.962f else 1f,
        animationSpec = motion.snappy(),
        label = "MapMeButtonPress",
    )

    val height: Dp = when (size) {
        MapMeButtonSize.Large -> 58.dp
        MapMeButtonSize.Medium -> 50.dp
    }
    val horizontal: Dp = when (size) {
        MapMeButtonSize.Large -> 30.dp
        MapMeButtonSize.Medium -> 24.dp
    }

    val contentColor = when (style) {
        MapMeButtonStyle.Primary -> colors.onAccent
        MapMeButtonStyle.Secondary -> colors.textPrimary
        MapMeButtonStyle.Ghost -> colors.accent
    }

    val shape = MapMeTheme.radius.button

    var container = Modifier
        .height(height)
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
            // A flat fill reads as a sticker; the gradient gives the surface a
            // top-lit curvature that matches the glass around it.
            .background(Brush.verticalGradient(listOf(colors.accent, colors.accentDeep)))

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
                    horizontal = if (style == MapMeButtonStyle.Ghost) 14.dp else horizontal,
                ),
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalMapMeContentColor provides contentColor) {
            MapMeText(
                text = text,
                style = MapMeTheme.type.button,
                color = contentColor,
                maxLines = 1,
            )
            trailing?.invoke(this@Row)
        }
    }
}
