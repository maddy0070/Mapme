package com.mapme.core.map

import com.mapme.core.design.theme.MapMeColors
import com.mapme.core.model.GeoPoint
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * The journey, drawn on the map.
 *
 * ## Why the engine draws this and not Compose
 *
 * The dot showing where you are is one coordinate, so projecting it in Compose
 * every frame costs nothing. A trail is not: an hour's walk is a couple of
 * thousand points, and projecting all of them in Kotlin on every frame of a
 * pan would turn the smoothest screen in the product into the worst. As a
 * source and a line layer it is the GPU's problem, and it stays smooth however
 * long the walk gets.
 *
 * The head — the live leading edge — stays in Compose, because it is one point
 * and it wants the design system's own drawing.
 *
 * ## Segments
 *
 * Each segment becomes its own `LineString`. That is the entire mechanism by
 * which a pause looks like a pause: MapLibre draws no line between two
 * features, so the gap where the person stopped recording is simply not drawn.
 * One journey, with an honest hole in it.
 *
 * ## Two layers, not one
 *
 * A wide, faint casing under a solid core. On night it reads as the trail
 * glowing very slightly into the dark; on paper it reads as a soft shadow
 * giving the line weight. It is the same trick [com.mapme.core.design.component.TrailLine]
 * uses in the introduction, so the thing the person sees on the map is
 * recognisably the object they were shown on the way in — and it is two draw
 * calls, not a blur.
 */
internal class MapTrail {

    /**
     * Rebuilds source and layers from nothing.
     *
     * Called on every style load, including the one a theme change causes.
     * Loading a style discards every source and layer that was added to it, so
     * a trail that is not reinstalled here is a trail that vanishes the moment
     * someone switches to dark — which §22 of the brief specifically forbids,
     * and which is invisible until you switch theme mid-walk.
     */
    fun install(style: Style, colors: MapMeColors) {
        if (style.getSource(SOURCE) == null) {
            style.addSource(GeoJsonSource(SOURCE, FeatureCollection.fromFeatures(emptyList())))
        }

        if (style.getLayer(CASING) == null) {
            style.addLayer(
                LineLayer(CASING, SOURCE).withProperties(
                    PropertyFactory.lineColor(colors.trailFar.hex()),
                    PropertyFactory.lineCap("round"),
                    PropertyFactory.lineJoin("round"),
                    PropertyFactory.lineOpacity(if (colors.trailGlows) 0.28f else 0.18f),
                    PropertyFactory.lineWidth(WIDE),
                ),
            )
        }

        if (style.getLayer(CORE) == null) {
            style.addLayer(
                LineLayer(CORE, SOURCE).withProperties(
                    PropertyFactory.lineColor(colors.trailNear.hex()),
                    PropertyFactory.lineCap("round"),
                    PropertyFactory.lineJoin("round"),
                    PropertyFactory.lineWidth(NARROW),
                ),
            )
        }
    }

    /**
     * Hands the engine the current shape of the walk.
     *
     * Rebuilding the whole collection each time looks wasteful and is not: a
     * reading arrives every couple of seconds, so this runs about thirty times
     * a minute over a list that reaches a few thousand at most. The alternative
     * — incremental geometry updates — buys nothing measurable and is a much
     * better place for a bug about a missing last point.
     */
    fun update(style: Style, segments: List<List<GeoPoint>>) {
        val source = style.getSource(SOURCE) as? GeoJsonSource ?: return
        val features = segments
            // A single point is a position, not a path; LineString needs two.
            .filter { it.size >= 2 }
            .map { segment ->
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        segment.map { Point.fromLngLat(it.longitude, it.latitude) },
                    ),
                )
            }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private companion object {
        const val SOURCE = "mapme-journey"
        const val CASING = "mapme-journey-casing"
        const val CORE = "mapme-journey-core"

        /**
         * Thick enough to be the hero, thin enough to be a route.
         *
         * Zoom-interpolated rather than fixed, or the trail is a hairline
         * across a city and a stripe across a street.
         */
        val NARROW: org.maplibre.android.style.expressions.Expression =
            org.maplibre.android.style.expressions.Expression.interpolate(
                org.maplibre.android.style.expressions.Expression.linear(),
                org.maplibre.android.style.expressions.Expression.zoom(),
                org.maplibre.android.style.expressions.Expression.stop(10, 2.5f),
                org.maplibre.android.style.expressions.Expression.stop(14, 5.0f),
                org.maplibre.android.style.expressions.Expression.stop(18, 9.0f),
            )

        val WIDE: org.maplibre.android.style.expressions.Expression =
            org.maplibre.android.style.expressions.Expression.interpolate(
                org.maplibre.android.style.expressions.Expression.linear(),
                org.maplibre.android.style.expressions.Expression.zoom(),
                org.maplibre.android.style.expressions.Expression.stop(10, 7.0f),
                org.maplibre.android.style.expressions.Expression.stop(14, 14.0f),
                org.maplibre.android.style.expressions.Expression.stop(18, 26.0f),
            )
    }
}

/** MapLibre's property factory wants `#rrggbb`, not a Compose colour. */
private fun androidx.compose.ui.graphics.Color.hex(): String {
    fun channel(value: Float) = (value * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(channel(red), channel(green), channel(blue))
}
