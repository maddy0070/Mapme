package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The MapMe type ramp.
 *
 * ## The pairing
 *
 * **Bricolage Grotesque** carries the headlines. It is a grotesque with
 * deliberate irregularities — slightly flared stems, a warm bowl on the 'a',
 * an 'g' with character — so a large title has a voice instead of merely being
 * large. It is neither sci-fi nor cartoon nor coldly geometric, which is the
 * narrow gap this product had to land in.
 *
 * **Plus Jakarta Sans** carries everything read at length. Smooth, modern,
 * friendly at small sizes, and quiet enough to disappear.
 *
 * ## The rules
 *
 * 1. **Tracking tightens as size grows.** Large type at default tracking looks
 *    typed rather than set; [displayHero] runs at −0.035em.
 * 2. **Numbers are their own class.** [heroMetric] and [metric] are tabular and
 *    tight — a distance is meant to be looked at, not read.
 * 3. **Uppercase is rationed.** Only [label] and [labelSmall], only for
 *    eyebrows and units. Never a button, never a sentence.
 *
 * The voice this ramp is married to is short, warm and confident — so the
 * headline sizes are large and the line heights are generous, because that is
 * how a short friendly sentence is supposed to be set.
 */
@Immutable
data class MapMeType(
    /** Onboarding headlines. The largest voice in the product. */
    val displayHero: TextStyle,
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,

    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,

    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,

    val label: TextStyle,
    val labelSmall: TextStyle,

    val button: TextStyle,

    /** A statistic a whole screen is built around. */
    val heroMetric: TextStyle,
    val metric: TextStyle,
    val metricLabel: TextStyle,
)

private const val TABULAR = "tnum"

fun mapMeType(fonts: BrandFonts): MapMeType {
    val display = fonts.display
    val text = fonts.text

    return MapMeType(
        displayHero = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.035).em,
        ),
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.03).em,
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.025).em,
        ),

        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.02).em,
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.015).em,
        ),
        titleSmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.005).em,
        ),

        bodyLarge = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 27.sp,
            letterSpacing = (-0.002).em,
        ),
        bodyMedium = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.004.em,
        ),

        label = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.09.em,
        ),
        labelSmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.1.em,
        ),

        button = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.005).em,
            textAlign = TextAlign.Center,
        ),

        heroMetric = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.04).em,
            fontFeatureSettings = TABULAR,
        ),
        metric = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.02).em,
            fontFeatureSettings = TABULAR,
        ),
        metricLabel = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.07.em,
        ),
    )
}

internal val LocalMapMeType = staticCompositionLocalOf { mapMeType(BrandFonts.Fallback) }
