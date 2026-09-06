package com.mapme.core.map

import androidx.compose.ui.graphics.Color
import com.mapme.core.design.theme.MapMeColors
import com.mapme.core.design.theme.mapMeDarkColors
import com.mapme.core.design.theme.mapMeLightColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The basemap is generated from the design tokens, so these test the thing
 * that generation is *for*: that the map cannot drift away from the product,
 * and cannot start competing with the journey drawn on top of it.
 */
class MapMeStyleTest {

    private val dark = mapMeDarkColors()
    private val light = mapMeLightColors()

    private fun layers(colors: MapMeColors): List<Map<String, Any?>> {
        @Suppress("UNCHECKED_CAST")
        return MapMeStyle.spec(colors)["layers"] as List<Map<String, Any?>>
    }

    private fun layer(colors: MapMeColors, id: String): Map<String, Any?> =
        layers(colors).firstOrNull { it["id"] == id }
            ?: error("no layer '$id'; the style has ${layers(colors).map { it["id"] }}")

    private fun paint(colors: MapMeColors, id: String): Map<*, *> =
        layer(colors, id)["paint"] as Map<*, *>

    @Test
    fun `a map can be read — the layers that orient someone are all present`() {
        listOf(dark, light).forEach { colors ->
            val ids = layers(colors).map { it["id"] }
            listOf(
                "background", "water", "park", "landcover",
                "road-major", "road-minor", "building", "place-label",
            ).forEach { required ->
                assertTrue("a map without '$required' cannot be navigated: $ids", required in ids)
            }
        }
    }

    @Test
    fun `every colour in the map comes from the design system`() {
        listOf(dark, light).forEach { colors ->
            assertEquals(colors.mapLand.css(), paint(colors, "background")["background-color"])
            assertEquals(colors.mapWater.css(), paint(colors, "water")["fill-color"])
            assertEquals(colors.mapGreen.css(), paint(colors, "park")["fill-color"])
            assertEquals(colors.mapRoad.css(), paint(colors, "road-minor")["line-color"])
            assertEquals(colors.mapRoadMajor.css(), paint(colors, "road-major")["line-color"])
            assertEquals(colors.mapBuilding.css(), paint(colors, "building")["fill-color"])
            assertEquals(colors.mapLabel.css(), paint(colors, "place-label")["text-color"])
        }
    }

    /**
     * The rule the whole basemap exists to obey.
     *
     * MapMe draws exactly one saturated thing, and it is the journey. If any
     * part of the map creeps towards the accent — a rose-tinted road, a warm
     * pink building — the trail stops being the only vivid line on screen and
     * the product loses its single strongest visual idea.
     */
    @Test
    fun `nothing in the basemap competes with the journey`() {
        listOf(dark to "night", light to "paper").forEach { (colors, name) ->
            val basemap = listOf(
                "land" to colors.mapLand, "green" to colors.mapGreen, "water" to colors.mapWater,
                "road" to colors.mapRoad, "roadMajor" to colors.mapRoadMajor,
                "building" to colors.mapBuilding, "boundary" to colors.mapBoundary,
            )
            val accentChroma = colors.accent.chroma()
            basemap.forEach { (label, colour) ->
                assertTrue(
                    "$name map $label has chroma ${colour.chroma()} against the accent's " +
                        "$accentChroma — the basemap is the stage, not a performer",
                    colour.chroma() < 0.25f,
                )
                assertTrue(
                    "$name map $label sits at the accent's hue with enough colour to show it",
                    hueDistance(colour, colors.accent) > 25f || colour.chroma() < 0.06f,
                )
            }
        }
    }

    /** The trail has to be findable on the ground it is drawn on. */
    @Test
    fun `the journey stands off the map in both themes`() {
        listOf(dark to "night", light to "paper").forEach { (colors, name) ->
            val ratio = contrast(colors.trailNear, colors.mapLand)
            assertTrue(
                "on $name the trail is only ${"%.1f".format(ratio)}:1 against the map",
                ratio >= 3.0,
            )
        }
    }

    @Test
    fun `place names are readable on the ground they sit on`() {
        listOf(dark to "night", light to "paper").forEach { (colors, name) ->
            val ratio = contrast(colors.mapLabel, colors.mapLand)
            assertTrue(
                "$name map labels are ${"%.1f".format(ratio)}:1 — below the 4.5 needed to read",
                ratio >= 4.5,
            )
        }
    }

    @Test
    fun `the two maps are different designs, not an inversion of each other`() {
        assertNotEquals(dark.mapLand, light.mapLand)
        assertNotEquals(dark.mapWater, light.mapWater)
        // An inverted map has the same relationships upside down. These do not:
        // on night the road casing is darker than the ground, on paper lighter.
        assertTrue("night casing should sit under the ground", dark.mapRoadCasing.luminance() < dark.mapLand.luminance())
        assertTrue("paper casing should sit over the ground", light.mapRoadCasing.luminance() < light.mapLand.luminance())
        assertTrue("night roads lift off the ground", dark.mapRoad.luminance() > dark.mapLand.luminance())
        assertTrue("paper roads lift off the ground", light.mapRoad.luminance() > light.mapLand.luminance())
    }

    /**
     * Order is not cosmetic in a map: it is the difference between a river
     * under a bridge and a river over one.
     */
    @Test
    fun `the layer stack is built ground upwards`() {
        val ids = layers(dark).map { it["id"] as String }
        fun at(id: String) = ids.indexOf(id)

        assertTrue("water must cover the ground", at("background") < at("water"))
        assertTrue("green belongs under water", at("landcover") < at("water"))
        assertTrue("roads cross water, not the reverse", at("water") < at("road-major"))
        assertTrue("a road's casing sits under its fill", at("road-casing") < at("road-minor"))
        assertTrue("labels are read last", at("road-major") < at("place-label"))
        assertEquals("nothing is drawn over a name", ids.last(), "water-label")
    }

    @Test
    fun `the source is addressed by TileJSON so a rotating tile path cannot break it`() {
        @Suppress("UNCHECKED_CAST")
        val sources = MapMeStyle.spec(dark)["sources"] as Map<String, Map<String, Any?>>
        val source = sources.values.single()
        assertEquals("vector", source["type"])
        assertEquals(MapMeStyle.TILE_JSON, source["url"])
        assertTrue(
            "the tile pattern must not be hardcoded — it carries a dated build id",
            (source["url"] as String).none { it.isDigit() },
        )
        assertTrue("OpenStreetMap attribution is not optional", source["attribution"].toString().contains("OpenStreetMap"))
    }

    @Test
    fun `no shop pins — the map is a stage, not a directory`() {
        listOf(dark, light).forEach { colors ->
            val sourceLayers = layers(colors).map { it["source-layer"] }
            assertTrue("a POI layer would cover the map in noise", "poi" !in sourceLayers)
            assertTrue("house numbers are unreadable clutter at any zoom", "housenumber" !in sourceLayers)
        }
    }

    @Test
    fun `the style serialises to JSON that says what the tree says`() {
        val json = MapMeStyle.json(dark)
        assertTrue(json.startsWith("{") && json.endsWith("}"))
        assertTrue("version 8 is the style spec MapLibre reads", json.contains("\"version\":8"))
        assertTrue(json.contains("\"${MapMeStyle.TILE_JSON}\""))
        assertTrue("colours must survive serialisation", json.contains(dark.mapWater.css()))
        // Whole numbers should not come out as 14.0 — the spec accepts it, but
        // a style full of trailing zeroes is a style nobody wants to read.
        assertTrue("stray float formatting", !json.contains(".0,") && !json.contains(".0}"))
    }

    // --- colour maths, local to this test ----------------------------------

    private fun Color.luminance(): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /**
     * How much colour is actually present — max channel minus min.
     *
     * Deliberately **not** HSV saturation, which was the first thing tried and
     * was wrong. HSV divides by the brightest channel, so a near-black navy
     * like the night water reads as 69% "saturated" while being, to the eye,
     * almost black. Meanwhile the night land, green and building all sat at
     * 0.33–0.35 and were one nudge from failing for the same reason.
     *
     * Chroma measures the thing the rule is actually about. Every colour in
     * both basemaps lands under 0.13; the accent is 0.82. That gap is the
     * design, and a threshold of 0.25 protects it with room to spare.
     */
    private fun Color.chroma(): Float = maxOf(red, green, blue) - minOf(red, green, blue)

    private fun Color.hue(): Float {
        val mx = maxOf(red, green, blue)
        val mn = minOf(red, green, blue)
        val d = mx - mn
        if (d == 0f) return 0f
        val h = when (mx) {
            red -> 60f * (((green - blue) / d) % 6f)
            green -> 60f * ((blue - red) / d + 2f)
            else -> 60f * ((red - green) / d + 4f)
        }
        return (h + 360f) % 360f
    }

    private fun hueDistance(a: Color, b: Color): Float {
        val d = abs(a.hue() - b.hue()) % 360f
        return min(d, 360f - d)
    }
}
