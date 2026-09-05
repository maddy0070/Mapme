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
 * Two rules hold it together:
 *
 * 1. **Display shrinks its tracking as it grows.** Big type at default
 *    tracking looks loose and amateur; the negative tracking on the large
 *    sizes is what makes a headline look set rather than typed.
 * 2. **Numbers are a first-class size class.** A distance is the most
 *    emotional thing MapMe can show you, so [heroMetric] and [metric] are
 *    tabular, tight and heavy — they are meant to be looked at, not read.
 *
 * Labels are the only uppercase style. They are for eyebrows and units, never
 * for buttons or sentences.
 */
@Immutable
data class MapMeType(
    /** "142 km" on a year in review. The largest thing in the product. */
    val heroMetric: TextStyle,
    /** A statistic inside a card. */
    val metric: TextStyle,
    /** The unit or caption sitting under a metric. */
    val metricLabel: TextStyle,

    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,

    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,

    /** Uppercase eyebrow above a section. */
    val label: TextStyle,
    val labelSmall: TextStyle,

    val button: TextStyle,
    /** Place names drawn on the basemap. */
    val mapLabel: TextStyle,
)

private const val TABULAR = "tnum"

fun mapMeType(fonts: BrandFonts): MapMeType {
    val display = fonts.display
    val text = fonts.text

    return MapMeType(
        heroMetric = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.03).em,
            fontFeatureSettings = TABULAR,
        ),
        metric = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.015).em,
            fontFeatureSettings = TABULAR,
        ),
        metricLabel = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.06.em,
        ),

        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.025).em,
        ),
        displayMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.02).em,
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.015).em,
        ),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.01).em,
        ),
        titleSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.005).em,
        ),

        bodyLarge = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 26.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.005.em,
        ),

        label = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.07.em,
        ),
        labelSmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.09.em,
        ),

        button = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.01.em,
            textAlign = TextAlign.Center,
        ),
        mapLabel = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.02.em,
        ),
    )
}

internal val LocalMapMeType = staticCompositionLocalOf { mapMeType(BrandFonts.Fallback) }
