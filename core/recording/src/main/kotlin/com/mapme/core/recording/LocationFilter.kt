package com.mapme.core.recording

import com.mapme.core.model.TrackPoint

/**
 * Deciding which readings to believe.
 *
 * ## The problem
 *
 * A phone does not report where you are. It reports where it currently thinks
 * you are, with a confidence radius, and both change constantly. Standing
 * still at a bus stop for five minutes produces a cloud of fixes scattered
 * over twenty metres; walking past a tall building produces one fix on the
 * wrong side of it. Drawing every reading gives a scribble that says you paced
 * in circles and crossed the road four times, which is not what happened.
 *
 * ## The principle
 *
 * **Make the trail beautiful without lying about where the person went.** That
 * rules out the two easy answers. Heavy smoothing produces a lovely line
 * through places you were not, and MapMe is a record of a life rather than a
 * drawing of one. Interpolating over a rejected fix invents a coordinate.
 *
 * So this filter only ever *discards*. It never moves a point, never averages
 * two, never fabricates one to bridge a gap. Every coordinate in a saved
 * journey is a reading the device actually produced. A rejected fix leaves no
 * trace except the count in [RecordingSnapshot.rejected].
 *
 * ## The rules, in order
 *
 * 1. **Too vague to mean anything.** Beyond [ACCURACY_CEILING] the reading
 *    carries no information about which street you are on.
 * 2. **Out of order.** A fix older than the last accepted one is a stale
 *    delivery, not time travel.
 * 3. **Impossible.** Faster than [MAX_SPEED_MPS] between two fixes is a bad
 *    fix, not a fast walk. This is what stops the wild jumps.
 * 4. **Inside the noise.** Movement smaller than the uncertainty is not
 *    evidence of movement. This is the rule that keeps a stationary phone from
 *    drawing a bird's nest, and it scales with the reported accuracy rather
 *    than using one fixed radius — an 8 m step means something when the fix is
 *    good to 4 m and nothing when it is good to 30.
 *
 * Anything else is accepted exactly as it arrived.
 */
class LocationFilter(
    private val accuracyCeilingMetres: Float = ACCURACY_CEILING,
    private val maxSpeedMetresPerSecond: Double = MAX_SPEED_MPS,
    private val minimumStepMetres: Double = MINIMUM_STEP,
) {

    sealed interface Verdict {
        data object Accept : Verdict
        data class Reject(val reason: Reason) : Verdict
    }

    enum class Reason {
        /** The fix is not confident enough to place someone on a street. */
        TooVague,

        /** Older than the reading before it. */
        OutOfOrder,

        /** Implies a speed no walk, and few vehicles, could produce. */
        Impossible,

        /** Movement within the margin of error. Standing still, most likely. */
        InsideTheNoise,
    }

    /**
     * @param previous the last reading that was *accepted*, not the last that
     *   arrived. Comparing against a rejected fix would let one bad reading
     *   drag the whole filter off course.
     */
    fun judge(candidate: TrackPoint, previous: TrackPoint?): Verdict {
        if (candidate.accuracyMetres > accuracyCeilingMetres) {
            return Verdict.Reject(Reason.TooVague)
        }
        if (previous == null) return Verdict.Accept

        if (candidate.timestampMillis <= previous.timestampMillis) {
            return Verdict.Reject(Reason.OutOfOrder)
        }

        val speed = previous.impliedSpeedTo(candidate)
        if (speed != null && speed > maxSpeedMetresPerSecond) {
            return Verdict.Reject(Reason.Impossible)
        }

        // The threshold is the larger of a fixed floor and the fix's own
        // uncertainty: a reading good to 3 m earns a 5 m threshold, one good
        // to 30 m has to move 30 m before it has said anything.
        val threshold = maxOf(minimumStepMetres, candidate.accuracyMetres.toDouble())
        if (previous.distanceTo(candidate) < threshold) {
            return Verdict.Reject(Reason.InsideTheNoise)
        }

        return Verdict.Accept
    }

    companion object {
        /**
         * 50 m. Beyond this a fix cannot tell you which side of a park someone
         * is on, and a trail drawn from such readings is decoration.
         */
        const val ACCURACY_CEILING = 50f

        /**
         * 55 m/s, about 200 km/h. Deliberately generous — MapMe records trains
         * and motorways too, and the job here is catching a fix that has
         * teleported across a city, not policing how someone travels.
         */
        const val MAX_SPEED_MPS = 55.0

        /**
         * 5 m floor. Below this even a good urban fix is mostly noise, and a
         * walking trail loses nothing by not recording a two-metre shuffle.
         */
        const val MINIMUM_STEP = 5.0
    }
}
