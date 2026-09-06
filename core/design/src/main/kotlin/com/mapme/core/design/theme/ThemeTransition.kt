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
 * [FEATHER_FRACTION] of the radius is a graded edge, so the boundary is a
 * diffusion rather than a line. It is also not one circle but three, offset
 * from each other by a fraction of the radius, so the shape is gently
 * irregular and grows lopsided the way spilled light does. Riding just ahead
 * of the edge is a very faint warm rim in the incoming accent, which is the
 * only "light" in the effect — there are no particles and nothing sparkles.
 *
 * ## Timing
 *
 * [MapMeMotion.theme] is 380ms on an easing that leaves immediately and glides
 * to a stop. Faster and the boundary is not legible as a movement; slower and
 * you are waiting for the interface to finish agreeing with you.
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
        commit: () -> Unit,
    ) {
        if (reduceMotion) {
            // The contract is that nothing disappears when motion is reduced.
            // A theme change has no information in its movement, so it simply
            // arrives.
            commit()
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
                        durationMillis = (remaining * CATCH_UP_MILLIS).toInt().coerceIn(40, 120),
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
            progress.animateTo(1f, tween(durationMillis, easing = easing))
        } finally {
            // Only if nothing has superseded us in the meantime.
            if (generation == mine) enter(Phase.Idle)
        }
    }

    private companion object {
        /** Full-length catch-up for a reveal interrupted at the very start. */
        const val CATCH_UP_MILLIS = 150f
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
                        val feather = radius * FEATHER_FRACTION

                        drawLayer(snapshot)
                        eraseOutward(origin, radius, feather)
                        drawEdgeLight(origin, radius, feather, rimColor, fraction)
                    },
            )
        }
    }
}

/** How much of the leading radius is graded rather than hard. */
private const val FEATHER_FRACTION = 0.22f

/**
 * The three lobes that make the boundary organic.
 *
 * Offsets are a fraction of the current radius, so the shape starts almost
 * round under the fingertip and grows steadily more lopsided — the way a
 * spill spreads, rather than the way a circle inflates.
 */
private val LOBES = listOf(
    Triple(0f, 0f, 1.00f),
    Triple(0.11f, -0.07f, 0.93f),
    Triple(-0.08f, 0.10f, 0.88f),
)

private fun DrawScope.eraseOutward(origin: Offset, radius: Float, feather: Float) {
    if (radius <= 0f) return
    LOBES.forEach { (dx, dy, scale) ->
        val r = radius * scale
        if (r <= 1f) return@forEach
        val centre = Offset(origin.x + radius * dx, origin.y + radius * dy)
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
 * Radius at [fraction] of the way through.
 *
 * Overshoots the far corner by the feather width, otherwise the graded edge
 * would still be visible in that corner when the animation ends and the last
 * sliver of the old theme would vanish in a step.
 */
internal fun revealRadius(fraction: Float, origin: Offset, size: Size): Float {
    val target = maxCornerDistance(origin, size)
    val full = target * (1f + FEATHER_FRACTION) + 1f
    return max(0f, full * fraction.coerceIn(0f, 1f))
}
