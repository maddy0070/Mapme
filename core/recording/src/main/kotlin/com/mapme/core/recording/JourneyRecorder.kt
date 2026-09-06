package com.mapme.core.recording

import com.mapme.core.model.Journey
import com.mapme.core.model.TrackPoint
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Turning a walk into a journey.
 *
 * ## What this is responsible for
 *
 * The state machine, the decision about which readings to keep, and writing
 * each kept reading down as it arrives. Nothing else. It knows no Android, no
 * map, and no screen — which is why the whole of it can be tested in
 * milliseconds against a fake clock and a temp directory, rather than by
 * walking around the block.
 *
 * ## Why it writes as it goes
 *
 * A journey is only saved at the end in the sense that it is *finalised* at
 * the end. Every accepted reading is appended to the journal the moment it is
 * accepted, because the alternative — holding an hour of someone's walk in
 * memory and writing it when they press stop — loses the whole thing the first
 * time Android kills the process on a cold morning.
 *
 * ## Pause
 *
 * Pausing does not end anything. It closes the current segment and stops
 * considering readings; resuming opens a new segment in the *same* journey.
 * That is what makes a paused walk one journey with an honest hole in it
 * rather than two journeys, or one journey with a straight line drawn through
 * a route nobody took.
 */
class JourneyRecorder(
    private val store: JourneyStore,
    private val filter: LocationFilter = LocationFilter(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {

    private val _snapshot = MutableStateFlow(RecordingSnapshot.Idle)
    val snapshot: StateFlow<RecordingSnapshot> = _snapshot.asStateFlow()

    /** Segments built so far. The last one is open while recording. */
    private val segments = mutableListOf<MutableList<TrackPoint>>()
    private var journeyId: String? = null

    /**
     * The last reading that was *accepted*.
     *
     * Deliberately survives a pause: resuming a hundred metres from where you
     * paused should not draw a line across the gap, and the filter still needs
     * something to judge the next reading against. The segment break is what
     * stops that line being drawn; this is what stops the filter losing its
     * footing.
     */
    private var lastAccepted: TrackPoint? = null

    val state: RecordingState get() = _snapshot.value.state

    /**
     * Begin. Only from [RecordingState.Idle] or a finished journey — starting
     * on top of a live one would silently discard it.
     */
    fun start() {
        if (state.isLive) return
        val id = newId()
        journeyId = id
        segments.clear()
        segments += mutableListOf<TrackPoint>()
        lastAccepted = null
        store.open(id)
        store.appendBreak(id)
        publish(RecordingState.Recording, rejected = 0)
    }

    fun pause() {
        if (state != RecordingState.Recording) return
        publish(RecordingState.Paused)
    }

    fun resume() {
        if (state != RecordingState.Paused) return
        val id = journeyId ?: return
        // A new segment, so the gap is drawn as a gap.
        segments += mutableListOf<TrackPoint>()
        store.appendBreak(id)
        publish(RecordingState.Recording)
    }

    /**
     * Finish and save.
     *
     * Passes through [RecordingState.Stopping] rather than jumping straight to
     * Completed so a screen can disable its controls while the last write
     * happens, instead of letting a second tap start a new journey on top.
     */
    fun stop() {
        if (!state.isLive) return
        val id = journeyId ?: return
        publish(RecordingState.Stopping)
        store.close(id, lastAccepted?.timestampMillis ?: clock())
        publish(RecordingState.Completed)
    }

    /** Throw away a completed journey the person does not want kept. */
    fun discard() {
        val id = journeyId
        if (state == RecordingState.Recording || state == RecordingState.Paused) return
        if (id != null) store.delete(id)
        segments.clear()
        lastAccepted = null
        journeyId = null
        _snapshot.value = RecordingSnapshot.Idle
    }

    /** Back to Idle without deleting anything. */
    fun clear() {
        if (state.isLive) return
        segments.clear()
        lastAccepted = null
        journeyId = null
        _snapshot.value = RecordingSnapshot.Idle
    }

    /**
     * A reading arrived.
     *
     * Ignored unless actually collecting — a fix landing while paused is not a
     * place the person asked to be remembered.
     */
    fun onLocation(point: TrackPoint) {
        if (!state.isCollecting) return
        val id = journeyId ?: return

        when (filter.judge(point, lastAccepted)) {
            is LocationFilter.Verdict.Accept -> {
                if (segments.isEmpty()) segments += mutableListOf<TrackPoint>()
                segments.last() += point
                lastAccepted = point
                store.appendPoint(id, point)
                publish(RecordingState.Recording)
            }
            is LocationFilter.Verdict.Reject -> {
                publish(RecordingState.Recording, rejected = _snapshot.value.rejected + 1)
            }
        }
    }

    /**
     * Something went wrong that recording cannot continue through.
     *
     * The journey is *not* thrown away: whatever was walked before the
     * permission was revoked or the GPS died is still real, still on disk, and
     * still worth keeping.
     */
    fun fail(problem: RecordingProblem) {
        if (!state.isLive) return
        journeyId?.let { store.close(it, lastAccepted?.timestampMillis ?: clock()) }
        _snapshot.value = _snapshot.value.copy(
            state = RecordingState.Error,
            journey = buildJourney(),
            problem = problem,
        )
    }

    /** Re-open a journey the process died in the middle of. */
    fun recover(journey: Journey) {
        if (state.isLive) return
        journeyId = journey.id
        segments.clear()
        journey.segments.forEach { segments += it.toMutableList() }
        lastAccepted = journey.points.lastOrNull()
        _snapshot.value = RecordingSnapshot(state = RecordingState.Completed, journey = journey)
    }

    private fun publish(state: RecordingState, rejected: Int = _snapshot.value.rejected) {
        _snapshot.value = RecordingSnapshot(
            state = state,
            journey = buildJourney(),
            problem = null,
            rejected = rejected,
        )
    }

    private fun buildJourney(): Journey {
        val id = journeyId ?: return Journey.Empty
        val kept = segments.filter { it.isNotEmpty() }.map { it.toList() }
        return Journey(id = id, segments = kept)
    }
}
