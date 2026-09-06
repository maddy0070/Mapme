package com.mapme.core.map

import com.mapme.core.design.theme.MapMeColors

/**
 * The MapMe basemap, built from the design system rather than beside it.
 *
 * ## Why this is generated
 *
 * Every colour here comes from [MapMeColors]. A hand-authored style JSON is a
 * second, unversioned palette that drifts the first time someone adjusts the
 * product's colour and forgets the map — and MapMe has two themes, so it would
 * be two files drifting independently. Generating from the tokens makes that
 * impossible: change `mapWater` and both maps change.
 *
 * ## What the map is for
 *
 * Enough structure to know where you are — water, parks, roads that separate
 * into a hierarchy, neighbourhood and place names — and no more. The journey
 * line is the only saturated thing MapMe ever draws, so the basemap is built
 * to lose to it. Concretely: no rose anywhere, buildings barely above the
 * ground, POI icons absent entirely, and labels one step quieter than you
 * would set them if the map were the product.
 *
 * ## Schema
 *
 * OpenMapTiles, which is what the tile source serves: `water`, `waterway`,
 * `landcover`, `park`, `building`, `transportation`, `transportation_name`,
 * `boundary`, `place`, `water_name`. Layer and field names below are that
 * schema's, confirmed against the source's own TileJSON rather than assumed.
 */
object MapMeStyle {

    /**
     * The tile source, addressed by its TileJSON rather than its tile pattern.
     *
     * The actual tile URL carries a dated build id — `.../planet/20260830_.../
     * {z}/{x}/{y}.pbf` — which rotates. Pointing at the TileJSON means the
     * client resolves the current build at runtime; hardcoding the pattern
     * would work until the day it silently didn't.
     */
    const val TILE_JSON = "https://tiles.openfreemap.org/planet"

    /** Label glyphs. Served by the same host; verified reachable from CI. */
    const val GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

    /** Required by ODbL. Rendered by the app; never remove it. */
    const val ATTRIBUTION = "© OpenStreetMap contributors"

    private const val SRC = "mapme"

    /**
     * The only font stack the tile host actually serves.
     *
     * Checked, not assumed: `Noto Sans Regular` returns glyphs, `Noto Sans
     * Medium` and `Open Sans Regular` both 404. A style that names a missing
     * stack does not fail — it renders a map with no labels at all, which is
     * the kind of bug that survives review because everything still "works".
     * Adding a weight here means checking the host first.
     */
    private val TEXT_FONT = listOf("Noto Sans Regular")

    /** The style as a Kotlin tree. Tests read this; [json] serialises it. */
    fun spec(colors: MapMeColors): Map<String, Any?> = mapOf(
        "version" to 8,
        "name" to if (colors.isDark) "MapMe Night" else "MapMe Paper",
        "glyphs" to GLYPHS,
        "sources" to mapOf(
            SRC to mapOf(
                "type" to "vector",
                "url" to TILE_JSON,
                "attribution" to ATTRIBUTION,
            ),
        ),
        "layers" to layers(colors),
    )

    fun json(colors: MapMeColors): String = jsonOf(spec(colors))

    // --- the layer stack, ground up ----------------------------------------

    private fun layers(c: MapMeColors): List<Map<String, Any?>> = buildList {
        add(background(c))
        addAll(green(c))
        addAll(water(c))
        add(building(c))
        addAll(roads(c))
        add(boundary(c))
        addAll(labels(c))
    }

    private fun background(c: MapMeColors) = mapOf(
        "id" to "background",
        "type" to "background",
        "paint" to mapOf("background-color" to c.mapLand.css()),
    )

    private fun green(c: MapMeColors) = listOf(
        fill(
            id = "landcover",
            source = "landcover",
            color = c.mapGreen.css(),
            filter = listOf("in", "class", "wood", "grass", "farmland", "scrub"),
            // Woodland fades in rather than switching on, so zooming out of a
            // city does not make the surroundings blink.
            opacity = zoom(4 to 0.0, 7 to 1.0),
        ),
        fill(
            id = "park",
            source = "park",
            color = c.mapGreen.css(),
            opacity = zoom(6 to 0.0, 9 to 1.0),
        ),
    )

    private fun water(c: MapMeColors) = listOf(
        fill(id = "water", source = "water", color = c.mapWater.css()),
        mapOf(
            "id" to "waterway",
            "type" to "line",
            "source" to SRC,
            "source-layer" to "waterway",
            "paint" to mapOf(
                "line-color" to c.mapWater.css(),
                "line-width" to zoom(8 to 0.6, 14 to 2.4, 18 to 5.0),
            ),
        ),
    )

    /**
     * Buildings, and only just.
     *
     * They arrive late (z15) and sit a hair off the ground colour. A city
     * rendered building-by-building is a beautiful map and a terrible stage:
     * the trail would run through visual noise for its entire length.
     */
    private fun building(c: MapMeColors) = mapOf(
        "id" to "building",
        "type" to "fill",
        "source" to SRC,
        "source-layer" to "building",
        "minzoom" to 15.0,
        "paint" to mapOf(
            "fill-color" to c.mapBuilding.css(),
            "fill-opacity" to zoom(15 to 0.0, 16.5 to 1.0),
        ),
    )

    /**
     * Roads, in two weights and a casing.
     *
     * The hierarchy is deliberately shallow — arterial and everything else.
     * A five-tier road classification is how a basemap starts shouting; two
     * tiers is enough to recognise a place. Casings are drawn as a wider line
     * *under* the fill, which is what gives a road an edge without an outline.
     */
    private fun roads(c: MapMeColors) = listOf(
        line(
            id = "road-casing",
            layer = "transportation",
            color = c.mapRoadCasing.css(),
            width = zoom(11 to 1.4, 14 to 4.0, 18 to 18.0),
            filter = listOf("!in", "class", "ferry", "path", "track"),
            minzoom = 11.0,
        ),
        line(
            id = "road-minor",
            layer = "transportation",
            color = c.mapRoad.css(),
            width = zoom(11 to 0.5, 14 to 2.4, 18 to 14.0),
            filter = listOf("in", "class", "minor", "service", "residential", "street"),
            minzoom = 12.0,
        ),
        line(
            id = "road-major",
            layer = "transportation",
            color = c.mapRoadMajor.css(),
            width = zoom(6 to 0.5, 11 to 1.8, 14 to 4.2, 18 to 20.0),
            filter = listOf("in", "class", "motorway", "trunk", "primary", "secondary", "tertiary"),
        ),
    )

    private fun boundary(c: MapMeColors) = mapOf(
        "id" to "boundary",
        "type" to "line",
        "source" to SRC,
        "source-layer" to "boundary",
        "filter" to listOf("<=", "admin_level", 4),
        "paint" to mapOf(
            "line-color" to c.mapBoundary.css(),
            "line-width" to zoom(3 to 0.6, 10 to 1.4),
            "line-dasharray" to listOf(3.0, 2.0),
        ),
    )

    /**
     * Names, haloed so they stay legible over anything beneath them.
     *
     * Place names only, plus water. No POI layer: a map covered in shop pins
     * is the opposite of somewhere you would want to see your own life drawn.
     */
    private fun labels(c: MapMeColors) = listOf(
        symbol(
            id = "place-label",
            layer = "place",
            colors = c,
            filter = listOf("in", "class", "city", "town", "village", "suburb", "neighbourhood"),
            size = zoom(4 to 11.0, 10 to 14.0, 16 to 17.0),
        ),
        symbol(
            id = "water-label",
            layer = "water_name",
            colors = c,
            size = zoom(6 to 10.0, 14 to 13.0),
            italic = true,
        ),
    )

    // --- small builders ----------------------------------------------------

    private fun fill(
        id: String,
        source: String,
        color: String,
        filter: List<Any?>? = null,
        opacity: Any? = null,
    ) = buildMap<String, Any?> {
        put("id", id)
        put("type", "fill")
        put("source", SRC)
        put("source-layer", source)
        filter?.let { put("filter", it) }
        put("paint", buildMap {
            put("fill-color", color)
            opacity?.let { put("fill-opacity", it) }
        })
    }

    private fun line(
        id: String,
        layer: String,
        color: String,
        width: Any?,
        filter: List<Any?>? = null,
        minzoom: Double? = null,
    ) = buildMap<String, Any?> {
        put("id", id)
        put("type", "line")
        put("source", SRC)
        put("source-layer", layer)
        minzoom?.let { put("minzoom", it) }
        filter?.let { put("filter", it) }
        put("layout", mapOf("line-cap" to "round", "line-join" to "round"))
        put("paint", mapOf("line-color" to color, "line-width" to width))
    }

    private fun symbol(
        id: String,
        layer: String,
        colors: MapMeColors,
        size: Any?,
        filter: List<Any?>? = null,
        italic: Boolean = false,
    ) = buildMap<String, Any?> {
        put("id", id)
        put("type", "symbol")
        put("source", SRC)
        put("source-layer", layer)
        filter?.let { put("filter", it) }
        put("layout", buildMap {
            put("text-field", listOf("coalesce", listOf("get", "name:latin"), listOf("get", "name")))
            put("text-font", TEXT_FONT)
            put("text-size", size)
            put("text-max-width", 7.0)
            put("text-letter-spacing", if (italic) 0.08 else 0.02)
            put("text-transform", "none")
        })
        put("paint", mapOf(
            "text-color" to colors.mapLabel.css(),
            "text-halo-color" to colors.mapLabelHalo.css(),
            // A wide halo is what lets a quiet label stay readable over a
            // road or a park without being set louder.
            "text-halo-width" to 1.6,
            "text-halo-blur" to 0.6,
        ))
    }

    /** `["interpolate", ["linear"], ["zoom"], z, v, ...]` — the style spec's ramp. */
    private fun zoom(vararg stops: Pair<Number, Number>): List<Any?> =
        buildList {
            add("interpolate")
            add(listOf("linear"))
            add(listOf("zoom"))
            stops.forEach { (z, v) -> add(z.toDouble()); add(v.toDouble()) }
        }
}
