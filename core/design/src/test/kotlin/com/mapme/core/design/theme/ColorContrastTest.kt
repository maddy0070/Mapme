package com.mapme.core.design.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Accessibility is a property of the palette, not something checked once by
 * eye and then quietly broken by a later tweak.
 *
 * These tests are why the MapMe palette can be adjusted with confidence:
 * change a token to something unreadable and the build fails before anyone has
 * to notice it on a phone in the sun. They found five real failures while this
 * palette was being designed.
 *
 * Thresholds follow WCAG 2.1: 4.5:1 for text, 3:1 for interface elements that
 * carry meaning on their own.
 */
class ColorContrastTest {

    private val dark = mapMeDarkColors()
    private val light = mapMeLightColors()

    private fun MapMeColors.surfaces() = listOf(canvas, surface, surfaceRaised, surfaceOverlay, surfaceSunken)

    @Test
    fun `every text tone is readable on every surface it can land on`() {
        listOf("night" to dark, "paper" to light).forEach { (mode, colors) ->
            colors.surfaces().forEach { ground ->
                assertContrast(colors.textPrimary, ground, 4.5, "$mode textPrimary")
                assertContrast(colors.textSecondary, ground, 4.5, "$mode textSecondary")
                // Tertiary carries metadata — units, captions, timestamps — which
                // is the text people most often squint at. It gets the full bar,
                // not the softer large-text one.
                assertContrast(colors.textTertiary, ground, 4.5, "$mode textTertiary")
            }
        }
    }

    @Test
    fun `signal colours stand out against their own surface`() {
        listOf("night" to dark, "paper" to light).forEach { (mode, colors) ->
            mapOf(
                "accent" to colors.accent,
                "focus" to colors.focus,
                "discovery" to colors.discovery,
                "critical" to colors.critical,
            ).forEach { (name, color) ->
                assertContrast(color, colors.surface, 3.0, "$mode $name")
            }
        }
    }

    @Test
    fun `text on a filled accent is readable in both modes`() {
        // Night puts ink on hot rose; paper puts white on deep rose. Neither is
        // the other inverted — white on hot rose reaches only 3.6:1.
        assertContrast(dark.onAccent, dark.accent, 4.5, "night onAccent")
        assertContrast(light.onAccent, light.accent, 4.5, "paper onAccent")
        assertContrast(dark.onDiscovery, dark.discovery, 4.5, "night onDiscovery")
        assertContrast(light.onDiscovery, light.discovery, 4.5, "paper onDiscovery")
    }

    @Test
    fun `the trail reads as one colour getting brighter, or darker, but never both`() {
        // The ramp carries time through luminance rather than hue, which is
        // what keeps it from ever looking like a rainbow — and what stops a
        // blue-to-pink gradient passing through purple at its midpoint.
        val night = listOf(dark.trailPast, dark.trailFar, dark.trailNear, dark.trailHead)
            .map { relativeLuminance(it) }
        assertTrue(
            "on night the trail must brighten towards now, got $night",
            night.zipWithNext().all { (a, b) -> b > a },
        )

        val paper = listOf(light.trailPast, light.trailFar, light.trailNear, light.trailHead)
            .map { relativeLuminance(it) }
        assertTrue(
            "on paper the trail must darken towards now, got $paper",
            paper.zipWithNext().all { (a, b) -> b < a },
        )
    }

    @Test
    fun `the head is distinguishable from the line it sits on`() {
        // Night has room to make the head near-white. Paper does not — both
        // ends are deep pigment there — so the head also earns its separation
        // from being a larger filled dot with a halo. 1.4 is the floor at which
        // the colour difference is still doing part of the work.
        assertTrue(
            "night head is lost in the line",
            contrastRatio(dark.trailHead, dark.trailNear) >= 2.5,
        )
        assertTrue(
            "paper head is lost in the line",
            contrastRatio(light.trailHead, light.trailNear) >= 1.4,
        )
    }

    @Test
    fun `the two modes are genuinely different designs`() {
        // A guard against the easiest regression there is: someone "simplifies"
        // light mode into an inversion of dark. If these ever match, the design
        // decision documented in mapMeLightColors has been lost.
        assertTrue("light and dark accents must not be identical", light.accent != dark.accent)
        assertTrue("light and dark onAccent must not be identical", light.onAccent != dark.onAccent)
        assertTrue("only night lets the trail bloom", dark.trailGlows && !light.trailGlows)
    }

    private fun assertContrast(fg: Color, bg: Color, minimum: Double, what: String) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "$what has a contrast ratio of ${"%.2f".format(ratio)}, below the required $minimum:1",
            ratio >= minimum,
        )
    }
}

internal fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

internal fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}
