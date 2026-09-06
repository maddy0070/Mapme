package com.mapme.app.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mapme.app.R
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
import com.mapme.core.design.theme.ThemeMode
import com.mapme.core.design.theme.motionDuration

/**
 * Home, before there is anything on it.
 *
 * This is the product's real first screen in its real first state. It is not a
 * placeholder: MapMe has recorded nothing, so it says so — and says it as an
 * opening rather than an absence. No invented journeys, no fabricated
 * statistics, nothing pretending to be data.
 *
 * The composition is arranged as an announcement rather than an explanation:
 * an eyebrow that tells you what is coming, a short headline that looks
 * forward, one line of substance, and a single quiet action. The mark draws
 * itself once on arrival and then simply breathes, which is the whole of the
 * movement here — the screen has nothing to report yet, so it should not
 * behave as though it does.
 */
@Composable
fun HomeScreen(
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MapMeTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val motion = MapMeTheme.motion
    val drawDuration = motionDuration(motion.epic)

    val markDraw = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) markDraw.animateTo(1f, tween(drawDuration, easing = motion.cinematic))
    }

    val breath: Float = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "MarkBreath")
        val value by transition.animateFloat(
            initialValue = 0.975f,
            targetValue = 1.025f,
            animationSpec = infiniteRepeatable(
                animation = tween(5200, easing = motion.gentle),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "MarkBreathValue",
        )
        value
    }

    MapMeBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // A short screen has to lose something, and it should be the
            // theatre rather than the words. The mark shrinks and the headline
            // steps down one size; the copy and the action are untouched.
            val compact = maxHeight < 680.dp

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
                    MapMeWordmark(markSize = 30.dp, textStyle = MapMeTheme.type.titleMedium)
                    AppearanceToggle()
                }

                Spacer(Modifier.weight(1f))

                MapMeMark(
                    size = if (compact) 88.dp else 124.dp,
                    progress = markDraw.value,
                    modifier = Modifier.scale(breath),
                )

                Spacer(Modifier.height(if (compact) MapMeTheme.space.x8 else MapMeTheme.space.x10))

                MapMePill(
                    text = stringResource(R.string.home_next_up),
                    tone = PillTone.Discovery,
                    icon = MapMeIcons.Sparkle,
                )

                Spacer(Modifier.height(MapMeTheme.space.x4))

                MapMeText(
                    text = stringResource(R.string.home_empty_headline),
                    style = if (compact) MapMeTheme.type.displayMedium else MapMeTheme.type.displayLarge,
                    color = colors.textPrimary,
                )

                Spacer(Modifier.height(MapMeTheme.space.x3))

                MapMeText(
                    text = stringResource(R.string.home_empty_body),
                    style = if (compact) MapMeTheme.type.bodyMedium else MapMeTheme.type.bodyLarge,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.weight(1.1f))

                MapMeButton(
                    text = stringResource(R.string.home_replay_intro),
                    onClick = onReplayIntro,
                    style = MapMeButtonStyle.Secondary,
                    modifier = Modifier.padding(bottom = MapMeTheme.space.x6),
                    trailing = {
                        MapMeIcon(MapMeIcons.Replay, contentDescription = null, size = 18.dp)
                    },
                )
            }
        }
    }
}

/**
 * Auto → Light → Dark, in one tap.
 *
 * A word rather than an icon: "Auto" is a state no sun-or-moon glyph has ever
 * communicated on the first try, and MapMe would rather be understood than
 * clever. A small accent dot marks the two modes that are an explicit choice,
 * so the difference between "I picked Light" and "I am following the phone" is
 * visible at a glance rather than inferred from the word alone.
 *
 * It reports the centre of itself in window coordinates when tapped, because
 * the new appearance spreads from exactly this point — see `ThemeTransition`.
 */
@Composable
private fun AppearanceToggle(modifier: Modifier = Modifier) {
    val appearance = LocalAppearance.current ?: return
    val haptics = LocalMapMeHaptics.current
    val colors = MapMeTheme.colors
    val shape = MapMeTheme.radius.control
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = MapMeTheme.motion.snappy(),
        label = "AppearanceTogglePress",
    )

    var centre by remember { mutableStateOf(Offset.Unspecified) }
    val explicit = appearance.mode != ThemeMode.System

    Row(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(colors.glassTint)
            .border(1.dp, if (explicit) colors.outlineStrong else colors.outline, shape)
            .onGloballyPositioned {
                val topLeft = it.positionInWindow()
                centre = Offset(
                    topLeft.x + it.size.width / 2f,
                    topLeft.y + it.size.height / 2f,
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = stringResource(R.string.home_appearance_action),
            ) {
                haptics.select()
                appearance.cycle(centre)
            }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (explicit) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(MapMeTheme.radius.chip)
                    .background(colors.accent),
            )
        }
        MapMeText(
            text = appearance.mode.label,
            style = MapMeTheme.type.labelSmall,
            color = if (explicit) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
    }
}
