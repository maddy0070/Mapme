package com.mapme.core.design.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.withFrameNanos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The theme changing from where you touched it.
 *
 * ## Why not simply swap the colours
 *
 * Swapping every token at once is the cheapest thing to build and the least
 * convincing thing to watch: the interface blinks and you learn nothing about
 * what caused it. Cross-fading is barely better — mid-fade both themes are
 * present at half strength everywhere, which is a state the product never
 * otherwise occupies, and text goes muddy while it passes through.
 *
 * ## How this works instead
 *
 * At the moment of the tap the entire interface is recorded into a single
 * `GraphicsLayer` — one texture, the old theme frozen exactly as it was. The
 * theme then flips underneath, so the live tree is already fully the *new*
 * appearance. The frozen old one is drawn back on top and progressively
 * **erased** outward from the point you touched.
 *
 * That gives two things nothing else does:
 *
 * 1. **Coherence for free.** Every pixel is either wholly old or wholly new.
 *    Background, text, borders, shadows, glass, the trail and the icons cannot
 *    disagree with each other or arrive at different times, because they are
 *    not being animated at all — a boundary is moving across a photograph.
 * 2. **It is cheap.** The old theme costs one texture blit per frame, and the
 *    boundary costs three gradient circles. Nothing recomposes, nothing
 *    re-lays-out, and the animation runs entirely in the draw phase.
 *
 * ## Why it is not a ripple
 *
 * A Material ripple is a hard-edged circle, and a hard circle sliding across
 * an interface reads as a wipe. The erase here is a soft radial gradient —
 * a fifth or so of the radius is a graded edge that widens as the front
 * travels, so the boundary is a diffusion rather than a line. It is also not
 * one circle but three, offset from each other by a fraction of the radius and
 * breathing a few percent out of step, so the front reshapes itself on the way
 * across instead of inflating rigidly. Riding just ahead of the edge is a very
 * faint warm rim in the incoming accent, which is the only "light" in the
 * effect — there are no particles and nothing sparkles.
 *
 * ## Timing
 *
 * [MapMeMotion.theme] is 700ms. The first version ran in 380 and was legible
 * on a desk but not on a phone: the boundary had crossed the screen before the
 * eye caught up with it, so the change registered as having happened rather
 * than as having been watched.
 *
 * The extra time is spent travelling, not waiting. That is a property of
 * [MapMeMotion.reveal] rather than of the duration — behind a hard ease-out,
 * 700ms would put three quarters of the movement in the first 150ms and then
 * crawl, which is slower to sit through and worse to look at. The boundary is
 * roughly a fifth of the way out at 150ms, half way at 250ms, and still moving
 * at 600ms.
 */
@Stable
class ThemeTransition internal constructor() {

    internal enum class Phase { Idle, Capturing, Revealing }

    internal var phase by mutableStateOf(Phase.Idle)
        private set

    /** Where the finger landed, in window coordinates. */
    internal var originInWindow by mutableStateOf(Offset.Unspecified)
        private set

    internal var hostOrigin by mutableStateOf(Offset.Zero)

    internal val progress = Animatable(1f)

    /**
     * Which transition is currently the real one.
     *
     * [LaunchedEffect] cancels the outgoing reveal without waiting for it, so a
     * superseded coroutine can run its cleanup *after* its replacement has
     * already started capturing. Without this guard that late cleanup drops the
     * phase back to Idle, the snapshot is never recorded, and the reveal draws a
     * stale layer over a live interface. Only the newest transition is allowed
     * to touch the phase on its way out.
     */
    private var generation = 0

    internal fun originIn(size: Size): Offset {
        val o = originInWindow
        if (o == Offset.Unspecified) return Offset(size.width / 2f, size.height / 2f)
        return o - hostOrigin
    }

    internal fun aimAt(origin: Offset) {
        originInWindow = origin
    }

    internal fun enter(next: Phase) {
        phase = next
    }

    /**
     * Records the old interface, flips the theme, then erases outward.
     *
     * Cancellation is the normal case, not an error: tapping again mid-reveal
     * supersedes this one. Two things make that safe. The incoming transition
     * first runs the outgoing boundary out to the edge over a few frames, so
     * the interface never jumps to a finished state it was still travelling
     * towards; and [generation] stops the superseded coroutine's cleanup from
     * switching off the transition that replaced it.
     */
    internal suspend fun play(
        reduceMotion: Boolean,
        durationMillis: Int,
        easing: Easing,
        // Ahead of [commit] so that stays the trailing lambda at every call site.
        onBoundaryPassed: () -> Unit = {},
        commit: () -> Unit,
    ) {
        if (reduceMotion) {
            // The contract is that nothing disappears when motion is reduced.
            // A theme change has no information in its movement, so it simply
            // arrives.
            commit()
            onBoundaryPassed()
            return
        }
        val mine = ++generation
        try {
            // An interrupted reveal is not thrown away. Its boundary races to
            // the edge in the time it has left, so the outgoing theme finishes
            // arriving instead of popping into place — then the new one starts
            // from the control you just touched. Snapping here is what makes
            // rapid switching look broken.
            if (phase == Phase.Revealing && progress.value < 1f) {
                val remaining = 1f - progress.value
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = (remaining * durationMillis * CATCH_UP_SHARE)
                            .toInt()
                            .coerceIn(70, 190),
                        easing = easing,
                    ),
                )
            }
            enter(Phase.Capturing)
            // Two frames, deliberately. The first lets the recording draw pass
            // happen; the second guarantees it finished before the colours
            // change underneath it. Capturing a frame too early would freeze
            // the new theme and the reveal would show nothing.
            withFrameNanos { }
            withFrameNanos { }
            commit()
            progress.snapTo(0f)
            enter(Phase.Revealing)
            var passed = false
            progress.animateTo(1f, tween(durationMillis, easing = easing)) {
                if (!passed && value >= BOUNDARY_PASSED) {
                    passed = true
                    onBoundaryPassed()
                }
            }
            if (!passed) onBoundaryPassed()
        } finally {
            // Only if nothing has superseded us in the meantime.
            if (generation == mine) enter(Phase.Idle)
        }
    }

    private companion object {
        /**
         * How much of the transition's own duration an interrupted boundary
         * gets to finish in.
         *
         * Expressed as a share rather than a fixed number of milliseconds so
         * the hurry stays in proportion if the transition is ever retimed —
         * at 700ms a boundary caught half way out has about 130ms to clear the
         * screen, which reads as hurrying rather than as being cut off. The
         * clamp keeps a fast series of taps from either snapping or queueing.
         */
        const val CATCH_UP_SHARE = 0.36f

        /**
         * When anything that cannot be recorded should change over.
         *
         * The system bars are drawn by the platform, not by us, so they are not
         * in the snapshot and cannot travel with the boundary — they can only
         * be switched at some instant. The control sits at the top of the
         * screen, so the top strip is one of the first places the boundary
         * covers; by the time this much of the reveal has run, the whole strip
         * has changed over and the bar icons are switching onto a background
         * that already agrees with them.
         */
        const val BOUNDARY_PASSED = 0.45f
    }
}

@Composable
internal fun rememberThemeTransition(): ThemeTransition = remember { ThemeTransition() }

/**
 * Wraps the themed interface so a change can spread through it.
 *
 * [rimColor] is the *incoming* accent — this sits inside the theme, so by the
 * time the reveal is running the colour it reads is already the new one.
 */
@Composable
internal fun ThemeRevealHost(
    transition: ThemeTransition,
    rimColor: Color,
    content: @Composable () -> Unit,
) {
    val snapshot = rememberGraphicsLayer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { transition.hostOrigin = it.positionInWindow() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (transition.phase == ThemeTransition.Phase.Capturing) {
                        snapshot.record { this@drawWithContent.drawContent() }
                        drawLayer(snapshot)
                    } else {
                        drawContent()
                    }
                },
        ) {
            content()
        }

        if (transition.phase == ThemeTransition.Phase.Revealing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // The erase is a DstOut blend, which only makes sense
                    // against this overlay's own pixels — without an offscreen
                    // layer it would punch a hole through the whole window.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        val fraction = transition.progress.value
                        val origin = transition.originIn(size)
                        val radius = revealRadius(fraction, origin, size)
                        val feather = radius * featherFraction(fraction)

                        drawLayer(snapshot)
                        eraseOutward(origin, radius, feather, fraction)
                        drawEdgeLight(origin, radius, feather, rimColor, fraction)
                    },
            )
        }
    }
}

/**
 * How much of the leading radius is graded rather than hard.
 *
 * It widens as the boundary travels. A wave front is tight where it starts and
 * softer once it has spread, and the practical effect is that the edge is
 * crisp enough to read as an edge under the control and diffuse enough by the
 * far side of the screen that its departure is not an event.
 */
private const val FEATHER_START = 0.18f
private const val FEATHER_END = 0.26f

internal fun featherFraction(fraction: Float): Float =
    FEATHER_START + (FEATHER_END - FEATHER_START) * fraction.coerceIn(0f, 1f)

/**
 * The three lobes that make the boundary organic.
 *
 * Offsets are a fraction of the current radius, so the shape starts almost
 * round under the fingertip and grows steadily more lopsided — the way a
 * spill spreads, rather than the way a circle inflates.
 *
 * Each lobe also breathes, very slightly, on its own phase. Three fixed
 * offsets scaled by a growing radius is a rigid shape being enlarged, and over
 * two thirds of a second the eye reads that as a blob inflating. Letting their
 * relative sizes drift a few percent out of step makes the front reshape
 * itself as it travels, which is what the extra time is for. The amplitude is
 * deliberately below the threshold of "something is wobbling".
 */
private val LOBES = listOf(
    Lobe(0f, 0f, 1.00f, phase = 0f),
    Lobe(0.11f, -0.07f, 0.93f, phase = 1.9f),
    Lobe(-0.08f, 0.10f, 0.88f, phase = 3.7f),
)

private class Lobe(val dx: Float, val dy: Float, val scale: Float, val phase: Float)

/** How far a lobe strays from its nominal size, at most. */
private const val BREATH = 0.045f

private fun DrawScope.eraseOutward(
    origin: Offset,
    radius: Float,
    feather: Float,
    fraction: Float,
) {
    if (radius <= 0f) return
    LOBES.forEach { lobe ->
        val breath = 1f + BREATH * sin(fraction * Math.PI.toFloat() * 1.35f + lobe.phase)
        val r = radius * lobe.scale * breath
        if (r <= 1f) return@forEach
        val centre = Offset(
            origin.x + radius * lobe.dx * breath,
            origin.y + radius * lobe.dy * breath,
        )
        val solid = ((r - feather) / r).coerceIn(0f, 0.999f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Black,
                    solid to Color.Black,
                    1f to Color.Transparent,
                ),
                center = centre,
                radius = r,
            ),
            radius = r,
            center = centre,
            blendMode = BlendMode.DstOut,
        )
    }
}

/**
 * A very faint warm rim riding the boundary.
 *
 * This is the whole of the "magic": light where the two themes meet, present
 * only while the edge is travelling. It fades in and out on a sine so it is
 * never visible at rest and never announces its own arrival.
 */
private fun DrawScope.drawEdgeLight(
    origin: Offset,
    radius: Float,
    feather: Float,
    color: Color,
    fraction: Float,
) {
    if (radius <= 1f) return
    val presence = sin(fraction.coerceIn(0f, 1f) * Math.PI).toFloat()
    val alpha = presence * 0.16f
    if (alpha <= 0.005f) return

    val inner = ((radius - feather * 1.9f) / radius).coerceIn(0f, 0.98f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                inner to Color.Transparent,
                (inner + (1f - inner) * 0.5f) to color.copy(alpha = alpha),
                1f to Color.Transparent,
            ),
            center = origin,
            radius = radius,
        ),
        radius = radius,
        center = origin,
    )
}

// --- geometry, kept pure so it can be tested ------------------------------

/**
 * The distance from [origin] to the furthest corner — how far the boundary has
 * to travel before every pixel has changed.
 */
internal fun maxCornerDistance(origin: Offset, size: Size): Float {
    val corners = listOf(
        Offset(0f, 0f),
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height),
    )
    return corners.maxOf { hypot(it.x - origin.x, it.y - origin.y) }
}

/**
 * Radius at [fraction] of the way through — the outer edge of the graded band.
 *
 * The overshoot is what stops the transition ending in a step, and the amount
 * of it is not a matter of taste. Only the part inside [solidRadius] is
 * *completely* erased; beyond that the old theme is still partly there. So the
 * boundary has to travel far enough that the **solid** front — not this outer
 * edge — has passed the furthest corner by the time the animation ends.
 *
 * It used to overshoot by `1 + feather`, which is not enough: at the end the
 * far corner was still 18% covered by the old theme, and that last 18%
 * disappeared in one frame when the overlay was removed. Dividing by
 * `1 - feather` instead puts the solid front exactly on the corner at
 * [fraction] 1, so there is nothing left to pop.
 */
internal fun revealRadius(fraction: Float, origin: Offset, size: Size): Float {
    val target = maxCornerDistance(origin, size)
    val full = target / (1f - FEATHER_END) + 1f
    return max(0f, full * fraction.coerceIn(0f, 1f))
}

/**
 * The radius inside which the old theme is completely gone.
 *
 * Everything between this and [revealRadius] is the graded band, where the two
 * themes are both partly present. This is the number that decides whether the
 * reveal actually finished.
 */
internal fun solidRadius(fraction: Float, origin: Offset, size: Size): Float =
    revealRadius(fraction, origin, size) * (1f - featherFraction(fraction))
