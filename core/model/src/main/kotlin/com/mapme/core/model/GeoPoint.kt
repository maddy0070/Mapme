package com.mapme.core.model

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A place on Earth.
 *
 * Nothing more. No accuracy, no timestamp, no source — those belong to
 * [TrackPoint], which is a *reading*. Keeping the two apart matters because
 * MapMe will spend a lot of its life deciding which readings to believe, and
 * that is much easier when a coordinate cannot lie about being one.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude $latitude is off the planet" }
        require(longitude in -180.0..180.0) { "Longitude $longitude is off the planet" }
    }

    /**
     * Great-circle distance to [other], in metres.
     *
     * Haversine on a spherical Earth. It is wrong by up to about 0.5% versus a
     * proper ellipsoidal calculation, which for "you walked 4.2 km today" is
     * an error of twenty metres — far below GPS noise, and worth the hundred
     * times faster arithmetic when smoothing thousands of points.
     */
    fun distanceTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(other.longitude - longitude)

        val a = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_METRES * asin(sqrt(a).coerceAtMost(1.0))
    }

    /** Initial compass bearing to [other], in degrees clockwise from north. */
    fun bearingTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    companion object {
        /** Mean Earth radius, IUGG. */
        const val EARTH_RADIUS_METRES: Double = 6_371_008.8
    }
}
