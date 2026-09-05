package com.mapme.core.design.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * MapMe's haptic vocabulary.
 *
 * Five feelings, each with a meaning. Anything that does not mean one of these
 * things does not vibrate — a phone that buzzes at everything is a phone you
 * put on silent, and then the milestone you actually wanted to feel is lost
 * with it.
 */
@Immutable
class MapMeHaptics internal constructor(
    private val view: View?,
    private val vibrator: Vibrator?,
) {

    /** A choice landed: a date picked, a journey selected, a toggle flipped. */
    fun select() {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Something the person asked for actually happened. */
    fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        view?.performHapticFeedback(constant)
    }

    /**
     * A light detent while dragging: scrubbing a replay, sweeping through
     * days. Called often, so it must stay almost subliminal.
     */
    fun tick() {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * The good one. A personal record, a new place, a completed year. This is
     * the only haptic MapMe is allowed to make memorable, which is exactly why
     * it is a shaped waveform rather than a system constant.
     */
    fun milestone() {
        val vibrator = vibrator
        if (vibrator == null || !vibrator.hasVibrator()) {
            confirm()
            return
        }
        val timings = longArrayOf(0, 18, 60, 26)
        val amplitudes = intArrayOf(0, 110, 0, 200)
        runCatching {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }.onFailure { confirm() }
    }

    /** Gently: that did not work. Never punitive. */
    fun warn() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view?.performHapticFeedback(constant)
    }

    companion object {
        /** A no-op instance, for previews and tests. */
        val None = MapMeHaptics(view = null, vibrator = null)
    }
}

val LocalMapMeHaptics = staticCompositionLocalOf { MapMeHaptics.None }

@Composable
internal fun rememberMapMeHaptics(): MapMeHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) {
        MapMeHaptics(view = view, vibrator = context.defaultVibrator())
    }
}

private fun Context.defaultVibrator(): Vibrator? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
}.getOrNull()
