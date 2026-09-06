package com.mapme.core.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mapme.core.design.haptics.LocalMapMeHaptics
import com.mapme.core.design.haptics.rememberMapMeHaptics

/**
 * The colour text inherits from its surroundings. Set it once when the ground
 * under a block of content changes — on an accent fill, inside a sheet —
 * instead of colouring every string at the call site.
 */
val LocalMapMeContentColor = compositionLocalOf { Color.Unspecified }

/**
 * Wraps MapMe in its design system.
 *
 * Notice what is not here: no MaterialTheme. MapMe is built on Compose
 * foundation and draws its own components, because a product whose promise is
 * "this looks like nothing else" cannot inherit its buttons from a spec that
 * ships on a billion devices. The cost is that MapMe owns its primitives; the
 * benefit is that nothing can quietly drift back to default.
 */
@Composable
fun MapMeTheme(
    appearance: AppearanceState = rememberAppearanceState(),
    reduceMotion: Boolean = rememberSystemReduceMotion(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = appearance.mode.resolve(systemDark)

    val context = LocalContext.current
    val colors = remember(dark) { if (dark) mapMeDarkColors() else mapMeLightColors() }
    val depth = remember(colors) { mapMeDepth(colors) }
    // Font resolution touches the asset manager, so it happens once per process
    // rather than once per recomposition.
    val fonts = remember(context.applicationContext) { BrandFonts.resolve(context) }
    val type = remember(fonts) { mapMeType(fonts) }
    val haptics = rememberMapMeHaptics()

    // A change the person asked for spreads out from where they touched it,
    // rather than the whole interface blinking. The transition is driven from
    // here because this is the one place that knows both the request and the
    // colours it is heading towards.
    val transition = rememberThemeTransition()
    val motion = remember { MapMeMotion() }

    // The status and navigation bar icons are drawn by the platform, so they
    // are not in the snapshot the reveal erases and cannot travel with the
    // boundary. Flipping them the instant the theme commits — which is what
    // this used to do — puts dark icons on a still-dark screen for the whole
    // length of the reveal. They change over once the boundary is past instead.
    var systemBarsDark by remember { mutableStateOf(dark) }
    LaunchedEffect(dark) {
        // A change with no request behind it: the phone's own theme moved while
        // Auto was selected. Nothing is revealing, so nothing has to wait.
        if (appearance.request == null) systemBarsDark = dark
    }

    LaunchedEffect(appearance.request) {
        val pending = appearance.request ?: return@LaunchedEffect
        transition.aimAt(pending.origin)
        transition.play(
            reduceMotion = reduceMotion,
            durationMillis = motion.theme,
            easing = motion.reveal,
            commit = { appearance.commit(pending.mode) },
            onBoundaryPassed = { systemBarsDark = pending.mode.resolve(systemDark) },
        )
        appearance.clearRequest()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Read during composition on purpose: that is what makes this
        // recompose when the bars are due to change. It happens once per
        // transition, not once per frame.
        val barsDark = systemBarsDark
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !barsDark
                isAppearanceLightNavigationBars = !barsDark
            }
        }
    }

    ThemeRevealHost(transition = transition, rimColor = colors.accent) {
    CompositionLocalProvider(
        LocalMapMeColors provides colors,
        LocalMapMeType provides type,
        LocalMapMeSpacing provides MapMeSpacing(),
        LocalMapMeRadius provides MapMeRadius(),
        LocalMapMeDepth provides depth,
        LocalMapMeBlur provides MapMeBlur(),
        LocalMapMeMotion provides motion,
        LocalMapMeHaptics provides haptics,
        LocalMapMeBrandFonts provides fonts,
        LocalAppearance provides appearance,
        LocalReduceMotion provides reduceMotion,
        LocalMapMeContentColor provides colors.textPrimary,
        content = content,
    )
    }
}

internal val LocalMapMeBrandFonts = compositionLocalOf { BrandFonts.Fallback }

/**
 * The only way design tokens should be read.
 *
 * `MapMeTheme.colors.accent`, `MapMeTheme.space.cardPadding`,
 * `MapMeTheme.type.displayHero` — if a value is not reachable through here, it
 * does not belong in a screen.
 */
object MapMeTheme {
    val colors: MapMeColors
        @Composable @ReadOnlyComposable get() = LocalMapMeColors.current

    val type: MapMeType
        @Composable @ReadOnlyComposable get() = LocalMapMeType.current

    val space: MapMeSpacing
        @Composable @ReadOnlyComposable get() = LocalMapMeSpacing.current

    val radius: MapMeRadius
        @Composable @ReadOnlyComposable get() = LocalMapMeRadius.current

    val depth: MapMeDepth
        @Composable @ReadOnlyComposable get() = LocalMapMeDepth.current

    val blur: MapMeBlur
        @Composable @ReadOnlyComposable get() = LocalMapMeBlur.current

    val motion: MapMeMotion
        @Composable @ReadOnlyComposable get() = LocalMapMeMotion.current

    val fonts: BrandFonts
        @Composable @ReadOnlyComposable get() = LocalMapMeBrandFonts.current
}
