package com.mapme.app.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mapme.app.R
import com.mapme.core.design.component.GlassCard
import com.mapme.core.design.component.GlassTone
import com.mapme.core.design.component.MapMeBackground
import com.mapme.core.design.component.MapMeButton
import com.mapme.core.design.component.MapMeButtonStyle
import com.mapme.core.design.component.MapMeMark
import com.mapme.core.design.component.MapMePill
import com.mapme.core.design.component.MapMeText
import com.mapme.core.design.component.MapMeWordmark
import com.mapme.core.design.component.PillTone
import com.mapme.core.design.haptics.LocalMapMeHaptics
import com.mapme.core.design.icon.MapMeIcon
import com.mapme.core.design.icon.MapMeIcons
import com.mapme.core.design.theme.LocalAppearance
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeTheme

/**
 * Home, with nothing on it yet.
 *
 * This is the real first screen of the product, in its real first state. It is
 * not a placeholder and it is not a demo: MapMe has recorded nothing, so it
 * says so — warmly, and without a single fabricated statistic or invented
 * journey to make the screenshot look busier than the truth.
 *
 * The mark breathes rather than the trail drawing itself, and that is the
 * point. There is no journey to draw. When there is, this screen becomes the
 * map and the mark steps aside.
 */
@Composable
fun HomeScreen(
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MapMeTheme.colors
    val reduceMotion = LocalReduceMotion.current

    val breath: Float = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "MarkBreath")
        val value by transition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = MapMeTheme.motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "MarkBreathValue",
        )
        value
    }

    MapMeBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = MapMeTheme.space.screenEdge),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MapMeTheme.space.x2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapMeWordmark(markSize = 32.dp, textStyle = MapMeTheme.type.titleMedium)
                AppearanceToggle()
            }

            Spacer(Modifier.weight(1f))

            MapMeMark(
                size = 132.dp,
                modifier = Modifier.scale(breath),
            )

            Spacer(Modifier.height(MapMeTheme.space.x8))

            MapMeText(
                text = stringResource(R.string.home_empty_headline),
                style = MapMeTheme.type.displayLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(MapMeTheme.space.x4))
            MapMeText(
                text = stringResource(R.string.home_empty_body),
                style = MapMeTheme.type.bodyLarge,
                color = colors.textSecondary,
            )

            Spacer(Modifier.weight(1.2f))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MapMeTheme.space.x5),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x4)) {
                    MapMePill(
                        text = stringResource(R.string.home_coming_next),
                        tone = PillTone.Discovery,
                        icon = MapMeIcons.Sparkle,
                    )
                    MapMeText(
                        text = stringResource(R.string.home_coming_next_body),
                        style = MapMeTheme.type.bodyMedium,
                        color = colors.textTertiary,
                    )
                    MapMeButton(
                        text = stringResource(R.string.home_replay_intro),
                        onClick = onReplayIntro,
                        style = MapMeButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        trailing = {
                            MapMeIcon(MapMeIcons.Replay, contentDescription = null, size = 18.dp)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Auto → Light → Dark, in one tap.
 *
 * A word rather than an icon: "Auto" is a state no sun-or-moon glyph has ever
 * communicated on the first try, and MapMe would rather be understood than
 * clever. It lives on home because both modes are designed, so choosing
 * between them is a real preference and not a buried setting.
 */
@Composable
private fun AppearanceToggle(modifier: Modifier = Modifier) {
    val appearance = LocalAppearance.current ?: return
    val haptics = LocalMapMeHaptics.current
    val colors = MapMeTheme.colors
    val shape = MapMeTheme.radius.control
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.glassTint)
            .border(1.dp, colors.outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = stringResource(R.string.home_appearance_action),
            ) {
                haptics.select()
                appearance.cycle()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        MapMeText(
            text = appearance.mode.label,
            style = MapMeTheme.type.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}
