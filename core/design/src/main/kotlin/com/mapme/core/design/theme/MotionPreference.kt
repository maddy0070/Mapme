package com.mapme.core.design.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the person using MapMe has asked the system to stop animating.
 *
 * MapMe leans hard on motion, which makes honouring this setting a real
 * obligation rather than a checkbox. The contract is: **nothing disappears**
 * when motion is reduced. Transitions collapse to their end state, ambient
 * loops stop, and the trail is drawn complete instead of drawing itself. The
 * information is identical; only the theatre is gone.
 */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
internal fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        scale == 0f
    }
}

/**
 * Scales a duration token to the current motion preference.
 *
 * Returns 0 when motion is reduced, so animations resolve immediately instead
 * of being skipped — the end state is always reached.
 */
@Composable
@ReadOnlyComposable
fun motionDuration(base: Int): Int = if (LocalReduceMotion.current) 0 else base
