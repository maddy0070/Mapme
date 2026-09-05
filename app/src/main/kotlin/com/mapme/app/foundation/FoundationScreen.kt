package com.mapme.app.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapme.app.R
import com.mapme.core.design.component.GlassCard
import com.mapme.core.design.component.MapMeBackground
import com.mapme.core.design.component.MapMeButton
import com.mapme.core.design.component.MapMeButtonStyle
import com.mapme.core.design.component.MapMePill
import com.mapme.core.design.component.MapMeText
import com.mapme.core.design.component.MapMeWordmark
import com.mapme.core.design.component.PillTone
import com.mapme.core.design.component.TrailLine
import com.mapme.core.design.icon.MapMeIcons
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.design.theme.motionDuration

/**
 * A wandering afternoon. Seven points, no straight lines, and it ends higher
 * than it began.
 */
private val OpeningJourney = listOf(
    Offset(0.06f, 0.78f),
    Offset(0.21f, 0.58f),
    Offset(0.31f, 0.64f),
    Offset(0.45f, 0.34f),
    Offset(0.59f, 0.43f),
    Offset(0.72f, 0.20f),
    Offset(0.93f, 0.31f),
)

/**
 * What MapMe looks like today.
 *
 * This screen exists to be *seen on a phone* — it is how the ink, the trail,
 * the glass and the type get judged on real hardware in real light, which is
 * the only judgement that counts. It is honest about what the build is, in
 * MapMe's own voice, and it will be replaced wholesale by the home experience
 * the moment there is a journey to show.
 */
@Composable
fun FoundationScreen(
    onOpenKit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MapMeTheme.motion
    val trailDuration = motionDuration(motion.epic)
    val revealDuration = motionDuration(motion.flowing)

    val trailProgress = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // The trail draws itself first and the interface follows it in. That
        // order is the point: the journey is the product, the card is commentary.
        trailProgress.animateTo(1f, tween(trailDuration, easing = motion.entering))
    }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(revealDuration, delayMillis = revealDuration / 2, easing = motion.entering))
    }

    MapMeBackground(modifier = modifier.fillMaxSize()) {
        TrailLine(
            points = OpeningJourney,
            modifier = Modifier.fillMaxSize(),
            progress = trailProgress.value,
            strokeWidth = 6.dp,
        )

        MapMePill(
            text = stringResource(R.string.foundation_badge),
            tone = PillTone.Accent,
            icon = MapMeIcons.Sparkle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(MapMeTheme.space.screenEdge)
                .alpha(reveal.value),
        )

        GlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(MapMeTheme.space.screenEdge)
                .fillMaxWidth()
                .alpha(reveal.value)
                .risingBy(reveal.value),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x4)) {
                MapMeWordmark(markSize = 40.dp)

                MapMeText(
                    text = stringResource(R.string.brand_tagline),
                    style = MapMeTheme.type.titleMedium,
                    color = MapMeTheme.colors.textSecondary,
                )

                MapMeText(
                    text = stringResource(R.string.foundation_body),
                    style = MapMeTheme.type.bodyMedium,
                    color = MapMeTheme.colors.textTertiary,
                )

                MapMeButton(
                    text = stringResource(R.string.foundation_open_kit),
                    onClick = onOpenKit,
                    style = MapMeButtonStyle.Secondary,
                    modifier = Modifier.padding(top = MapMeTheme.space.x1),
                )
            }
        }
    }
}

/**
 * Lifts a composable into place as [progress] runs 0..1.
 *
 * A layout offset rather than a translation, so the card genuinely occupies
 * its final position for touch and accessibility while it is still settling.
 */
private fun Modifier.risingBy(progress: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val rise = ((1f - progress) * 28.dp.toPx()).toInt()
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, rise)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF05080B, widthDp = 400, heightDp = 880)
@Composable
private fun FoundationScreenPreview() {
    MapMeTheme {
        Box(Modifier.fillMaxSize()) {
            FoundationScreen(onOpenKit = {})
        }
    }
}
