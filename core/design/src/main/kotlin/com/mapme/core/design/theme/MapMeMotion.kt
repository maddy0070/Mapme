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
 * The durations are named after intent, not milliseconds, because the right
 * question at a call site is "is this a state flip or a journey unfolding?"
 * — not "is this 200 or 300".
 *
 * - [instant] / [quick] — a control acknowledging a finger. Must feel like a
 *   physical response, so it has to beat the eye.
 * - [brisk] — a state change on something already on screen.
 * - [smooth] — something arriving or leaving.
 * - [flowing] — a panel expanding, a sheet, a map camera nudge.
 * - [cinematic] — the map travelling somewhere, a day transitioning into
 *   another day. Long enough to follow with your eyes.
 * - [epic] — reserved for the trail drawing itself. This is the moment the
 *   product is selling; it is allowed to take its time.
 */
@Immutable
data class MapMeMotion(
    val instant: Int = 90,
    val quick: Int = 140,
    val brisk: Int = 200,
    val smooth: Int = 300,
    val flowing: Int = 450,
    val cinematic: Int = 700,
    val epic: Int = 1400,

    /**
     * The default. Leaves quickly, arrives gently — the shape of something
     * with mass.
     */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** For things entering the screen. */
    val entering: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    /** For things leaving. Gets out of the way fast. */
    val exiting: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f),
    /** Symmetric and calm. Ambient loops, breathing, glows. */
    val gentle: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
    /** Constant speed. Only for progress and trail replay. */
    val linear: Easing = Easing { fraction -> fraction },
) {
    /** A control answering a press. Tight, no overshoot you can see. */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 900f)

    /** Something with weight settling into place. The MapMe default spring. */
    fun <T> physical(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = 380f)

    /** Large surfaces: sheets, expanding cards. Never bounces. */
    fun <T> settling(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 220f)
}

internal val LocalMapMeMotion = staticCompositionLocalOf { MapMeMotion() }
