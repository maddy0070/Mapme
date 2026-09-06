package com.mapme.app

import android.content.Context

/**
 * Whether the introduction has been seen.
 *
 * One boolean, so it lives in `SharedPreferences` rather than pulling in a
 * persistence library. MapMe adds one of those when there is a journey to
 * persist, not before.
 */
internal class OnboardingPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("mapme.onboarding", Context.MODE_PRIVATE)

    var seen: Boolean
        get() = prefs.getBoolean(KEY_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEN, value).apply()

    private companion object {
        const val KEY_SEEN = "seen"
    }
}
