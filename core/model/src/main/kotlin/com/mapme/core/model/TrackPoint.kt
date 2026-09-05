package com.mapme.core.model

/**
 * One reading from the device: where it thought it was, when, and how sure it
 * was about it.
 *
 * [accuracyMetres] is the field that earns its place. A phone in a city street
 * regularly reports positions it is only confident about to within eighty
 * metres, and drawing those as movement is exactly how a journal of someone's
 * life turns into a scribble. Every reading MapMe keeps carries its own
 * uncertainty so the cleaning stage can make that judgement honestly rather
 * than guessing later.
 */
data class TrackPoint(
    val position: GeoPoint,
    /** Milliseconds since the Unix epoch, from the location fix — not the clock. */
    val timestampMillis: Long,
    /** Radius of 68% confidence, in metres, as reported by the platform. */
    val accuracyMetres: Float,
    /** Metres above the WGS84 ellipsoid, when the fix included it. */
    val altitudeMetres: Double? = null,
    /** Ground speed in metres per second, when the fix included it. */
    val speedMetresPerSecond: Float? = null,
) {
    /** Straight-line distance to another reading, ignoring both uncertainties. */
    fun distanceTo(other: TrackPoint): Double = position.distanceTo(other.position)

    /** Elapsed time to another reading. Negative if [other] came first. */
    fun millisTo(other: TrackPoint): Long = other.timestampMillis - timestampMillis

    /**
     * Implied speed between two readings, in metres per second.
     *
     * Returns null when the readings share a timestamp, because a jump in zero
     * time is not a fast journey — it is a bad fix, and the caller should treat
     * it as one.
     */
    fun impliedSpeedTo(other: TrackPoint): Double? {
        val seconds = millisTo(other) / 1000.0
        if (seconds <= 0.0) return null
        return distanceTo(other) / seconds
    }
}
