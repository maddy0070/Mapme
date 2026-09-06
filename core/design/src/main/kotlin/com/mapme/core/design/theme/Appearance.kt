package com.mapme.core.design.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
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

    /** What this mode actually resolves to right now. */
    fun resolve(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    /** The order a single tappable control walks through. */
    fun next(): ThemeMode = when (this) {
        System -> Light
        Light -> Dark
        Dark -> System
    }
}

/**
 * A theme change that has been asked for but not yet applied.
 *
 * It carries [origin] because the whole point is that the new appearance
 * spreads from the control you touched — see [ThemeTransition]. [id] makes
 * every request distinct, so asking for the same mode twice still restarts the
 * animation instead of being swallowed as "no change".
 */
@Immutable
internal data class AppearanceRequest(
    val mode: ThemeMode,
    val origin: Offset,
    val id: Long,
)

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

    internal var request: AppearanceRequest? by mutableStateOf(null)
        private set

    private var nextRequestId = 0L

    /** Asks for [next], spreading from [origin] in window coordinates. */
    fun select(next: ThemeMode, origin: Offset) {
        request = AppearanceRequest(next, origin, nextRequestId++)
    }

    /** Auto → Light → Dark → Auto. What a single tappable control does. */
    fun cycle(origin: Offset) = select(mode.next(), origin)

    internal fun commit(next: ThemeMode) {
        if (next == mode) return
        mode = next
        prefs.edit().putString(KEY_MODE, next.name).apply()
    }

    internal fun clearRequest() {
        request = null
    }

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
