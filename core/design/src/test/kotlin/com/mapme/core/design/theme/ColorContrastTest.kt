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
 * These tests are the reason the MapMe palette can be adjusted with
 * confidence: change a token to something unreadable and the build fails
 * before anyone has to notice it on a phone in the sun.
 *
 * Thresholds follow WCAG 2.1: 4.5:1 for body text, 3:1 for large text and for
 * interface elements that carry meaning on their own.
 */
class ColorContrastTest {

    private val dark = mapMeDarkColors()
    private val light = mapMeLightColors()

    @Test
    fun `dark mode body text is readable on every surface`() {
        listOf(dark.canvas, dark.surface, dark.surfaceRaised, dark.surfaceOverlay).forEach { bg ->
            assertContrast(dark.textPrimary, bg, minimum = 4.5, what = "textPrimary")
            assertContrast(dark.textSecondary, bg, minimum = 4.5, what = "textSecondary")
        }
    }

    @Test
    fun `dark mode tertiary text is readable on every surface too`() {
        // textTertiary carries metadata — units, timestamps, captions — which
        // is exactly the text people most often need to squint at. It gets the
        // full 4.5:1 bar on every surface, not the softer large-text one.
        listOf(dark.canvas, dark.surface, dark.surfaceRaised, dark.surfaceOverlay).forEach { bg ->
            assertContrast(dark.textTertiary, bg, minimum = 4.5, what = "textTertiary")
        }
        listOf(light.canvas, light.surface, light.surfaceSunken).forEach { bg ->
            assertContrast(light.textTertiary, bg, minimum = 4.5, what = "light textTertiary")
        }
    }

    @Test
    fun `dark mode signal colours are distinguishable against the ground`() {
        mapOf(
            "accent" to dark.accent,
            "focus" to dark.focus,
            "live" to dark.live,
            "milestone" to dark.milestone,
            "critical" to dark.critical,
        ).forEach { (name, color) ->
            assertContrast(color, dark.surface, minimum = 3.0, what = name)
        }
    }

    @Test
    fun `text on an accent fill is readable`() {
        assertContrast(dark.textOnAccent, dark.accent, minimum = 4.5, what = "textOnAccent")
        assertContrast(light.textOnAccent, light.accent, minimum = 4.5, what = "light textOnAccent")
    }

    @Test
    fun `light mode body text is readable`() {
        listOf(light.canvas, light.surface, light.surfaceSunken).forEach { bg ->
            assertContrast(light.textPrimary, bg, minimum = 4.5, what = "light textPrimary")
            assertContrast(light.textSecondary, bg, minimum = 4.5, what = "light textSecondary")
        }
    }

    @Test
    fun `light mode signal colours are distinguishable`() {
        mapOf(
            "accent" to light.accent,
            "focus" to light.focus,
            "live" to light.live,
            "milestone" to light.milestone,
            "critical" to light.critical,
        ).forEach { (name, color) ->
            assertContrast(color, light.surface, minimum = 3.0, what = "light $name")
        }
    }

    @Test
    fun `the live head stays distinct from the line it sits on`() {
        // The head and the trail are separated by hue, not by lightness —
        // magenta and azure sit at almost identical luminance, which is why
        // measuring this with a contrast ratio would pass a design that is
        // actually invisible. 60° is the floor for "obviously another colour".
        listOf("trailNear" to dark.trailNear, "trailFar" to dark.trailFar).forEach { (name, color) ->
            val separation = hueDistance(dark.trailHead, color)
            assertTrue(
                "trailHead is only ${separation.toInt()}° from $name; the live " +
                    "marker would disappear into the line",
                separation >= 60.0,
            )
        }
    }

    @Test
    fun `the trail gradient itself stays a short, elegant sweep`() {
        // Far-to-near is meant to read as one colour shifting, not as two
        // colours meeting. Anything past a quarter turn starts looking like a
        // rainbow, and the line stops being beautiful when zoomed out.
        val sweep = hueDistance(dark.trailFar, dark.trailNear)
        assertTrue("trail gradient spans ${sweep.toInt()}°, which is too wide", sweep <= 90.0)
    }

    private fun assertContrast(foreground: Color, background: Color, minimum: Double, what: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$what has a contrast ratio of ${"%.2f".format(ratio)} against its background, " +
                "which is below the required $minimum:1",
            ratio >= minimum,
        )
    }
}

internal fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/** Shortest distance between two hues on the colour wheel, in degrees. */
internal fun hueDistance(a: Color, b: Color): Double {
    val delta = Math.abs(hueOf(a) - hueOf(b))
    return minOf(delta, 360.0 - delta)
}

private fun hueOf(color: Color): Double {
    val r = color.red.toDouble()
    val g = color.green.toDouble()
    val b = color.blue.toDouble()
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val chroma = max - min
    if (chroma == 0.0) return 0.0
    val hue = when (max) {
        r -> ((g - b) / chroma) % 6.0
        g -> (b - r) / chroma + 2.0
        else -> (r - g) / chroma + 4.0
    } * 60.0
    return if (hue < 0) hue + 360.0 else hue
}

private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}
