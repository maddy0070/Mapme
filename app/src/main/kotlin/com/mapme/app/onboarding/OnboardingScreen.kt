package com.mapme.app.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.app.R
import com.mapme.core.design.component.GlassCard
import com.mapme.core.design.component.MapMeBackground
import com.mapme.core.design.component.MapMeButton
import com.mapme.core.design.component.MapMeButtonSize
import com.mapme.core.design.component.MapMeButtonStyle
import com.mapme.core.design.component.MapMeText
import com.mapme.core.design.component.ThreadCamera
import com.mapme.core.design.component.TrailLine
import com.mapme.core.design.component.parallax
import com.mapme.core.design.component.rememberJourneyThread
import com.mapme.core.design.component.JourneyThread
import com.mapme.core.design.icon.MapMeIcon
import com.mapme.core.design.icon.MapMeIcons
import com.mapme.core.design.theme.LocalReduceMotion
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.design.theme.motionDuration
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * A thread lying further back in the composition.
 *
 * These are not other people's journeys and they are not data — they are the
 * same generated artwork at different seeds, drawn faint and small. Their only
 * job is depth: something for the hero line to be *in front of*. Without them
 * the introduction is a drawing on a blank page; with them it is a view into
 * somewhere.
 */
private data class Backdrop(
    val seed: Int,
    /** How much of the camera move this layer feels. Less means further away. */
    val parallax: Float,
    val intensity: Float,
    val width: Dp,
    val count: Int,
)

private val BACKDROPS = listOf(
    Backdrop(seed = JourneyThread.SIGNATURE_SEED + 977, parallax = 0.46f, intensity = 0.20f, width = 4.dp, count = 150),
    Backdrop(seed = JourneyThread.SIGNATURE_SEED + 411, parallax = 0.74f, intensity = 0.33f, width = 5.dp, count = 150),
)

/**
 * One beat of the introduction.
 *
 * The camera and reveal are part of the *content*, not decoration: each beat
 * is a distance to stand at, and the words only make sense from that distance.
 */
private data class Beat(
    val headline: Int,
    val body: Int,
    val camera: ThreadCamera,
    val reveal: Float,
)

/**
 * MapMe's introduction.
 *
 * ## The idea
 *
 * There is exactly one drawing in this whole flow, and the person never leaves
 * it. Swiping does not change the picture — it changes *where you are standing
 * relative to it*. Beat one is nose-to-the-paper on a few minutes of a line.
 * Beat two pulls back and the line you were looking at turns out to be a small
 * piece of something much longer. Beat three pulls back again and it is a life.
 *
 * That is the entire product argument made without a single word of
 * explanation, and it is why this is a camera move rather than three slides.
 *
 * ## How the continuity works
 *
 * The artwork reads the pager's *fractional* position, not its page index, so
 * the zoom tracks the finger — drag halfway and you are halfway between two
 * distances. Scale is interpolated in log space, because linear interpolation
 * of a zoom factor accelerates horribly at the wide end and reads as a lurch.
 *
 * The glass panel does not move. It is the fixed thing you are holding while
 * the world moves behind it, which is what makes the movement feel like yours.
 *
 * ## On honesty
 *
 * The line is generated artwork from a fixed seed — see [JourneyThread]. It is
 * not a recording, not sample data, and nothing on screen claims it is.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thread = rememberJourneyThread()
    val markers = remember(thread) { JourneyThread.placeMarkers(count = 9, threadSize = thread.size) }

    val beats = remember(thread) {
        listOf(
            Beat(
                headline = R.string.onboarding_1_headline,
                body = R.string.onboarding_1_body,
                camera = ThreadCamera(scale = 3.9f, focus = thread[16]),
                reveal = 0.09f,
            ),
            Beat(
                headline = R.string.onboarding_2_headline,
                body = R.string.onboarding_2_body,
                camera = ThreadCamera(scale = 1.9f, focus = thread[112]),
                reveal = 0.50f,
            ),
            Beat(
                headline = R.string.onboarding_3_headline,
                body = R.string.onboarding_3_body,
                camera = ThreadCamera(scale = 1f, focus = Offset(0.5f, 0.5f)),
                reveal = 1f,
            ),
        )
    }

    val pager = rememberPagerState(pageCount = { beats.size })
    val scope = rememberCoroutineScope()
    val motion = MapMeTheme.motion
    val reduceMotion = LocalReduceMotion.current
    val drawDuration = motionDuration(motion.epic)

    val backdrops = remember { BACKDROPS.map { it to JourneyThread.generate(it.count, it.seed) } }

    // The world fades up before the hero line starts drawing on it, so the
    // first thing you see is a place rather than a stroke on nothing.
    val depth = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) depth.animateTo(1f, tween(motionDuration(motion.travel), easing = motion.gentle))
    }

    // A very slow wander, a few thousandths of the canvas wide. Individually
    // invisible; collectively the difference between an illustration and a
    // screenshot of one.
    val drift: Float = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "ThreadDrift")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(38_000, easing = motion.linear),
                repeatMode = RepeatMode.Restart,
            ),
            label = "ThreadDriftPhase",
        )
        value
    }

    // The line draws itself once, on arrival. After that the pager owns it.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        intro.animateTo(1f, tween(drawDuration, easing = motion.cinematic))
    }

    val position = pager.currentPage + pager.currentPageOffsetFraction
    val aimed = interpolateCamera(beats, position)
    val camera = aimed.copy(
        focus = Offset(
            aimed.focus.x + sin(drift) * 0.009f,
            aimed.focus.y + cos(drift * 0.7f) * 0.007f,
        ),
    )
    val reveal = interpolateReveal(beats, position) * intro.value
    // Places only start appearing once there is enough line for them to sit on.
    val markerAlpha = ((position - 0.55f) / 0.6f).coerceIn(0f, 1f)

    MapMeBackground(modifier = modifier.fillMaxSize()) {
        // Back to front. Each layer feels less of the camera move than the one
        // in front of it, which is the whole of the depth: swipe, and the
        // distance between them opens up.
        backdrops.forEach { (layer, points) ->
            TrailLine(
                points = points,
                modifier = Modifier.fillMaxSize(),
                progress = 1f,
                camera = camera.parallax(layer.parallax),
                strokeWidth = layer.width,
                head = false,
                intensity = layer.intensity * depth.value,
            )
        }

        TrailLine(
            points = thread,
            modifier = Modifier.fillMaxSize(),
            progress = reveal,
            camera = camera,
            strokeWidth = 7.dp,
            markers = markers,
            markerAlpha = markerAlpha,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MapMeTheme.space.x3, vertical = MapMeTheme.space.x2),
                horizontalArrangement = Arrangement.End,
            ) {
                MapMeButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = onFinish,
                    style = MapMeButtonStyle.Ghost,
                    modifier = Modifier.alpha(1f - (position / (beats.size - 1)).coerceIn(0f, 1f)),
                )
            }

            Spacer(Modifier.weight(1f))

            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 0.dp,
            ) { page ->
                val beat = beats[page]
                // Words drift with the page and fade at the edges, so they read
                // as attached to the view rather than pasted over it.
                val distance = abs(page - position)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MapMeTheme.space.screenEdge)
                        .alpha((1f - distance * 1.6f).coerceIn(0f, 1f)),
                    verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x4),
                ) {
                    MapMeText(
                        text = stringResource(beat.headline),
                        style = MapMeTheme.type.displayHero,
                        color = MapMeTheme.colors.textPrimary,
                    )
                    MapMeText(
                        text = stringResource(beat.body),
                        style = MapMeTheme.type.bodyLarge,
                        color = MapMeTheme.colors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(MapMeTheme.space.x8))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MapMeTheme.space.screenEdge)
                    .padding(bottom = MapMeTheme.space.x5),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Progress(count = beats.size, position = position)
                    val last = pager.currentPage == beats.lastIndex
                    MapMeButton(
                        text = stringResource(
                            if (last) R.string.onboarding_start else R.string.onboarding_next,
                        ),
                        onClick = {
                            if (last) {
                                onFinish()
                            } else {
                                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                            }
                        },
                        size = MapMeButtonSize.Large,
                        trailing = if (last) {
                            null
                        } else {
                            { MapMeIcon(MapMeIcons.ArrowRight, contentDescription = null, size = 18.dp) }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Three bars. The one you are on stretches; the others stay short.
 *
 * It tracks the fractional position too, so the indicator grows under your
 * finger rather than snapping once the page settles.
 */
@Composable
private fun Progress(count: Int, position: Float) {
    val colors = MapMeTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val nearness = (1f - abs(index - position)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .height(6.dp)
                    .width(6.dp + 22.dp * nearness)
                    .clip(MapMeTheme.radius.chip)
                    .background(
                        if (nearness > 0.02f) colors.accent else colors.outlineStrong,
                    )
                    .alpha(0.35f + 0.65f * nearness),
            )
        }
    }
}

// --- camera interpolation --------------------------------------------------

private fun interpolateCamera(beats: List<Beat>, position: Float): ThreadCamera {
    val clamped = position.coerceIn(0f, (beats.size - 1).toFloat())
    val low = clamped.toInt().coerceAtMost(beats.size - 2)
    val t = (clamped - low).coerceIn(0f, 1f)
    val a = beats[low].camera
    val b = beats[low + 1].camera
    // Zoom is multiplicative, so interpolate its logarithm — a linear lerp
    // between 3.9 and 1.0 spends most of the gesture nearly stationary and
    // then lurches.
    val scale = exp(ln(a.scale) + (ln(b.scale) - ln(a.scale)) * t)
    return ThreadCamera(
        scale = scale,
        focus = Offset(
            a.focus.x + (b.focus.x - a.focus.x) * t,
            a.focus.y + (b.focus.y - a.focus.y) * t,
        ),
    )
}

private fun interpolateReveal(beats: List<Beat>, position: Float): Float {
    val clamped = position.coerceIn(0f, (beats.size - 1).toFloat())
    val low = clamped.toInt().coerceAtMost(beats.size - 2)
    val t = (clamped - low).coerceIn(0f, 1f)
    return beats[low].reveal + (beats[low + 1].reveal - beats[low].reveal) * t
}
