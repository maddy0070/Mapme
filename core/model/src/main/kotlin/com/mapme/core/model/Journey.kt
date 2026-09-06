package com.mapme.core.model

/**
 * A continuous stretch of movement.
 *
 * A journey is not "a day" and not "a trip to somewhere" — it is simply the
 * span between MapMe noticing you started moving and noticing you stopped.
 *
 * ## Why segments
 *
 * A journey is a list of *segments*, not a list of points, because a pause
 * leaves a hole in the record. If a paused journey were one flat list, the
 * only thing MapMe could draw across that hole is a straight line — and a
 * straight line between where you stopped and where you started again is a
 * claim about a route you did not take. It would also add distance you did
 * not walk.
 *
 * So each unbroken stretch of recording is its own segment. Distance is
 * summed *within* segments and never across the gap between them, and the
 * renderer draws each segment separately. One journey, honestly drawn.
 *
 * Points within a segment must be ordered oldest first. Everything below
 * assumes it.
 */
data class Journey(
    val id: String,
    val segments: List<List<TrackPoint>>,
) {
    /** Every reading in the journey, in order, with the pauses closed up. */
    val points: List<TrackPoint> get() = segments.flatten()

    val startedAtMillis: Long? get() = segments.firstOrNull { it.isNotEmpty() }?.firstOrNull()?.timestampMillis
    val endedAtMillis: Long? get() = segments.lastOrNull { it.isNotEmpty() }?.lastOrNull()?.timestampMillis

    /**
     * Wall-clock time from the first reading to the last, pauses included.
     *
     * This is "when did this happen", not "how long were you moving" — see
     * [recordedMillis] for the other question, which is usually the one worth
     * showing someone mid-walk.
     */
    val durationMillis: Long
        get() {
            val start = startedAtMillis ?: return 0L
            val end = endedAtMillis ?: return 0L
            return end - start
        }

    /**
     * Time actually spent recording, with paused stretches removed.
     *
     * A pause is the person saying "do not count this", so counting it would
     * contradict them.
     */
    val recordedMillis: Long
        get() = segments.sumOf { segment ->
            val start = segment.firstOrNull()?.timestampMillis ?: return@sumOf 0L
            val end = segment.lastOrNull()?.timestampMillis ?: return@sumOf 0L
            end - start
        }

    /**
     * Total distance travelled, in metres: the sum of the legs, not the
     * distance between the endpoints.
     *
     * A loop that comes home is not a journey of zero length. This is the
     * number MapMe shows people, and getting it the other way round would make
     * the most satisfying kind of walk look like nothing happened.
     *
     * Legs are summed inside each segment only. The gap across a pause is not
     * distance travelled — nobody teleported.
     */
    val distanceMetres: Double
        get() = segments.sumOf { segment ->
            segment.zipWithNext { a, b -> a.distanceTo(b) }.sum()
        }

    /** Average speed while actually recording, in metres per second. */
    val averageSpeedMetresPerSecond: Double
        get() {
            val seconds = recordedMillis / 1000.0
            return if (seconds <= 0.0) 0.0 else distanceMetres / seconds
        }

    val isEmpty: Boolean get() = segments.all { it.isEmpty() }

    companion object {
        val Empty = Journey(id = "", segments = emptyList())

        /** A journey that was never paused. */
        fun of(id: String, points: List<TrackPoint>): Journey =
            Journey(id, if (points.isEmpty()) emptyList() else listOf(points))
    }
}
