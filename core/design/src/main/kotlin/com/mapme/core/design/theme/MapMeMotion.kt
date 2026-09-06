package com.mapme.core.design.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * MapMe's motion vocabulary.
 *
 * Motion here has one job: to explain where something came from and where it
 * went. Nothing animates to look nice.
 *
 * Durations are named after intent, because the right question at a call site
 * is "is this a state flip or a journey unfolding?", not "is this 200 or 300".
 *
 * The onboarding transition is the reason [travel] exists. Moving between
 * pages there is not a page change — it is a camera pulling back from one
 * continuous drawing — and a camera move that lands in 300ms reads as a cut.
 */
@Immutable
data class MapMeMotion(
    val instant: Int = 90,
    val quick: Int = 150,
    val brisk: Int = 220,
    val smooth: Int = 320,
    val flowing: Int = 480,
    /** A camera move across the same physical space. */
    val travel: Int = 780,
    /** The trail drawing itself. The moment the product is selling. */
    val epic: Int = 1400,
    /**
     * A change spreading out from where it was touched.
     *
     * 380 was legible on a desk and gone on a phone: the boundary crossed a
     * real screen before the eye had found it, so the effect registered as
     * having happened rather than as having been watched. The whole point of
     * spreading from the touch is that you can follow it.
     *
     * The extra time buys travel, not lingering. [reveal] is shaped so the
     * screen goes on changing over at a steady rate right through this window;
     * a duration this long behind a hard ease-out would lunge and then crawl,
     * which is slower *and* worse.
     */
    val theme: Int = 700,

    /** The default. Leaves quickly, arrives gently — the shape of mass. */
    val standard: Easing = CubicBezierEasing(0.22f, 0f, 0f, 1f),
    /** Entering the screen. */
    val entering: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    /** Leaving. Gets out of the way. */
    val exiting: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f),
    /** Symmetric and calm. Ambient loops, breathing, glows. */
    val gentle: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    /** Camera moves. Slow to start, long glide, soft stop. */
    val cinematic: Easing = CubicBezierEasing(0.16f, 0.8f, 0.12f, 1f),
    /**
     * The theme boundary spreading across the screen. Very nearly a straight
     * line, and that is the whole point.
     *
     * Every other easing here front-loads its movement, because most of them
     * move something small a short way and the eye only needs to be told it
     * happened. This one carries a boundary across the entire display and has
     * to stay watchable for the whole of [theme].
     *
     * **The eye tracks area, not radius.** A front spreading from a corner
     * covers area roughly as the square of its radius, so any easing that also
     * front-loads the radius converts most of the screen almost immediately
     * and then spends the rest of its time creeping through one corner. A
     * constant radius speed is what comes out even — which is also, not by
     * coincidence, how a real ripple travels: a wave front moves at the speed
     * of the medium, and the spreading look comes from the geometry rather
     * than from the front speeding up or slowing down.
     *
     * The small deviation from straight is a soft landing. Pure linear stops
     * at full speed; this arrives at about a third of it, which costs almost
     * nothing in evenness and means nothing halts.
     *
     * Measured across a 1080x2400 screen from the control: about a fifth of
     * the way out at 150ms, half way at 350ms, still moving at 600ms, settled
     * by 700ms — and the proportion of the screen that has changed over grows
     * at a near-constant rate throughout. The ease-out this replaced put 74%
     * of the travel inside the first 150ms.
     */
    val reveal: Easing = CubicBezierEasing(0.5f, 0.4f, 0.8f, 1f),
    val linear: Easing = Easing { it },
) {
    /** A control answering a press. No overshoot the eye can catch. */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 900f)

    /** Something with weight settling into place. The MapMe default. */
    fun <T> physical(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 400f)

    /** Large surfaces: sheets, expanding cards. Never bounces. */
    fun <T> settling(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 240f)
}

internal val LocalMapMeMotion = staticCompositionLocalOf { MapMeMotion() }
