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
     * Faster than this and the boundary is not legible as a movement — it
     * reads as the same blink it replaced. Slower and you are waiting for the
     * interface to finish agreeing with you.
     */
    val theme: Int = 380,

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
     * Leaves under the finger, then glides. A change you caused should start
     * before you have finished touching it.
     */
    val reveal: Easing = CubicBezierEasing(0.16f, 0.84f, 0.24f, 1f),
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
