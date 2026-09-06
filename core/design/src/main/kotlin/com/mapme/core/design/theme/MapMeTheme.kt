package com.mapme.core.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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
    val dark = when (appearance.mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val context = LocalContext.current
    val colors = remember(dark) { if (dark) mapMeDarkColors() else mapMeLightColors() }
    val depth = remember(colors) { mapMeDepth(colors) }
    // Font resolution touches the asset manager, so it happens once per process
    // rather than once per recomposition.
    val fonts = remember(context.applicationContext) { BrandFonts.resolve(context) }
    val type = remember(fonts) { mapMeType(fonts) }
    val haptics = rememberMapMeHaptics()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalMapMeColors provides colors,
        LocalMapMeType provides type,
        LocalMapMeSpacing provides MapMeSpacing(),
        LocalMapMeRadius provides MapMeRadius(),
        LocalMapMeDepth provides depth,
        LocalMapMeBlur provides MapMeBlur(),
        LocalMapMeMotion provides MapMeMotion(),
        LocalMapMeHaptics provides haptics,
        LocalMapMeBrandFonts provides fonts,
        LocalAppearance provides appearance,
        LocalReduceMotion provides reduceMotion,
        LocalMapMeContentColor provides colors.textPrimary,
        content = content,
    )
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
