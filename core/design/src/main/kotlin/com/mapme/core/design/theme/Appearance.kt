package com.mapme.core.design.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Which face of MapMe to wear.
 *
 * Both modes are designed, neither is a fallback — see [mapMeLightColors] for
 * why Paper is not an inversion of Night.
 */
enum class ThemeMode {
    /** Follow the phone. The default, because the phone usually knows. */
    System,
    Light,
    Dark,
    ;

    val label: String
        get() = when (this) {
            System -> "Auto"
            Light -> "Light"
            Dark -> "Dark"
        }
}

/**
 * Holds the chosen [ThemeMode] and remembers it across launches.
 *
 * Deliberately backed by plain `SharedPreferences` rather than a new
 * dependency: this is one enum, and MapMe does not add a persistence library
 * until there is a journey to persist.
 */
@Stable
class AppearanceState internal constructor(
    private val prefs: SharedPreferences,
    initial: ThemeMode,
) {
    var mode: ThemeMode by mutableStateOf(initial)
        private set

    fun set(next: ThemeMode) {
        if (next == mode) return
        mode = next
        prefs.edit().putString(KEY_MODE, next.name).apply()
    }

    /** Auto → Light → Dark → Auto. What a single tappable control does. */
    fun cycle() = set(
        when (mode) {
            ThemeMode.System -> ThemeMode.Light
            ThemeMode.Light -> ThemeMode.Dark
            ThemeMode.Dark -> ThemeMode.System
        },
    )

    internal companion object {
        const val PREFS = "mapme.appearance"
        const val KEY_MODE = "theme_mode"
    }
}

@Composable
fun rememberAppearanceState(): AppearanceState {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        val prefs = context.applicationContext
            .getSharedPreferences(AppearanceState.PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(AppearanceState.KEY_MODE, null)
        val initial = ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.System
        AppearanceState(prefs, initial)
    }
}

val LocalAppearance = staticCompositionLocalOf<AppearanceState?> { null }
