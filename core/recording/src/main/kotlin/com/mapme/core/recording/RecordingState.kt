package com.mapme.core.recording

import com.mapme.core.model.Journey

/**
 * What the recorder is doing.
 *
 * One value, not a handful of booleans. `isRecording && !isPaused && !isSaving`
 * is how a recorder ends up in a state nobody designed — recording *and*
 * stopping, or paused with no journey — and the screen then has to guess.
 * Everything the UI shows is derived from this.
 */
enum class RecordingState {
    /** Nothing in progress. The only state from which a journey can begin. */
    Idle,

    /** Collecting. Locations arriving are being considered for the trail. */
    Recording,

    /**
     * Deliberately not collecting, but the journey is alive and its points are
     * kept. Resuming continues the same journey.
     */
    Paused,

    /** Stop was asked for; the journey is being finalised and written. */
    Stopping,

    /** Finished and saved. The journey is on the map to look at. */
    Completed,

    /** Something went wrong that recording cannot continue through. */
    Error,
    ;

    /** Whether an arriving location should be considered at all. */
    val isCollecting: Boolean get() = this == Recording

    /** Whether there is a journey in progress that would be lost by leaving. */
    val isLive: Boolean get() = this == Recording || this == Paused || this == Stopping
}

/**
 * Why recording stopped being possible.
 *
 * Separate from [RecordingState] so the state machine stays small and the
 * screen can still say something specific. Each of these has a different
 * honest sentence and a different next step.
 */
enum class RecordingProblem {
    /** The permission was taken away mid-walk. */
    PermissionLost,

    /** Location services switched off, or no provider answering. */
    LocationUnavailable,

    /** The journey could not be written to disk. */
    StorageFailed,
    ;
}

/**
 * Everything a screen needs to draw itself, in one immutable value.
 *
 * Includes the journey so far, so the map can render the live trail from the
 * same object the recorder is building — there is no second copy of the truth
 * to fall out of step.
 */
data class RecordingSnapshot(
    val state: RecordingState = RecordingState.Idle,
    val journey: Journey = Journey.Empty,
    val problem: RecordingProblem? = null,
    /**
     * Readings the filter refused, this session.
     *
     * Kept because it is the honest way to explain a trail that is not growing
     * while the person is plainly walking: the fixes are arriving and are not
     * good enough to believe. Never used to invent a point.
     */
    val rejected: Int = 0,
) {
    val distanceMetres: Double get() = journey.distanceMetres
    val recordedMillis: Long get() = journey.recordedMillis
    val hasTrail: Boolean get() = journey.segments.any { it.size >= 2 }

    companion object {
        val Idle = RecordingSnapshot()
    }
}
