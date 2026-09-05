package com.mapme.core.model

/**
 * A continuous stretch of movement.
 *
 * A journey is not "a day" and not "a trip to somewhere" — it is simply the
 * span between MapMe noticing you started moving and noticing you stopped.
 * What counts as a stop is a product decision that belongs to the recording
 * sprint, not to this type.
 *
 * [points] must be ordered oldest first. Everything below assumes it.
 */
data class Journey(
    val id: String,
    val points: List<TrackPoint>,
) {
    val startedAtMillis: Long? get() = points.firstOrNull()?.timestampMillis
    val endedAtMillis: Long? get() = points.lastOrNull()?.timestampMillis

    /** How long the journey lasted, in milliseconds. Zero for a single point. */
    val durationMillis: Long
        get() {
            val start = startedAtMillis ?: return 0L
            val end = endedAtMillis ?: return 0L
            return end - start
        }

    /**
     * Total distance travelled, in metres: the sum of the legs, not the
     * distance between the endpoints.
     *
     * A loop that comes home is not a journey of zero length. This is the
     * number MapMe shows people, and getting it the other way round would make
     * the most satisfying kind of walk look like nothing happened.
     */
    val distanceMetres: Double
        get() = points.zipWithNext { a, b -> a.distanceTo(b) }.sum()

    /** Average speed over the whole journey, in metres per second. */
    val averageSpeedMetresPerSecond: Double
        get() {
            val seconds = durationMillis / 1000.0
            return if (seconds <= 0.0) 0.0 else distanceMetres / seconds
        }

    val isEmpty: Boolean get() = points.isEmpty()

    companion object {
        val Empty = Journey(id = "", points = emptyList())
    }
}
