package com.mapme.core.recording

import com.mapme.core.model.GeoPoint
import com.mapme.core.model.TrackPoint
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The recorder is the part of MapMe that cannot be re-run. A walk happens
 * once, so a bug here costs someone an hour of their life that no fix brings
 * back — which is why the state machine and the writing are tested against a
 * real filesystem rather than trusted.
 */
class JourneyRecorderTest {

    private lateinit var directory: File
    private lateinit var store: FileJourneyStore
    private var now = 1_000_000L

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("mapme-journeys").toFile()
        store = FileJourneyStore(directory)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun recorder(id: String = "walk") = JourneyRecorder(
        store = store,
        clock = { now },
        newId = { id },
    )

    /** Points spaced far enough apart that the filter believes them. */
    private fun step(index: Int, accuracy: Float = 5f) = TrackPoint(
        position = GeoPoint(51.5 + index * 0.0005, -0.12),
        timestampMillis = 1_000_000L + index * 10_000L,
        accuracyMetres = accuracy,
    )

    @Test
    fun `a new recorder is idle and holds nothing`() {
        val recorder = recorder()
        assertEquals(RecordingState.Idle, recorder.state)
        assertTrue(recorder.snapshot.value.journey.isEmpty)
    }

    @Test
    fun `starting collects readings into a journey`() {
        val recorder = recorder()
        recorder.start()
        assertEquals(RecordingState.Recording, recorder.state)

        repeat(3) { recorder.onLocation(step(it)) }

        assertEquals(3, recorder.snapshot.value.journey.points.size)
        assertTrue(recorder.snapshot.value.distanceMetres > 0.0)
    }

    @Test
    fun `readings arriving while paused are not recorded`() {
        val recorder = recorder()
        recorder.start()
        recorder.onLocation(step(0))
        recorder.pause()

        recorder.onLocation(step(1))
        recorder.onLocation(step(2))

        assertEquals(
            "a fix arriving while paused is not a place the person asked to keep",
            1,
            recorder.snapshot.value.journey.points.size,
        )
    }

    /**
     * The one the brief names. Pausing must not fork the record.
     */
    @Test
    fun `pause then resume continues one journey, not two`() {
        val recorder = recorder()
        recorder.start()
        recorder.onLocation(step(0))
        recorder.onLocation(step(1))

        recorder.pause()
        recorder.resume()

        recorder.onLocation(step(6))
        recorder.onLocation(step(7))
        recorder.stop()

        val journey = recorder.snapshot.value.journey
        assertEquals("one journey", "walk", journey.id)
        assertEquals("two segments, because there was one pause", 2, journey.segments.size)
        assertEquals(4, journey.points.size)
    }

    /**
     * The reason segments exist at all. Distance must not include the gap the
     * person did not walk.
     */
    @Test
    fun `the pause gap is not counted as distance travelled`() {
        val paused = recorder("paused")
        paused.start()
        paused.onLocation(step(0))
        paused.onLocation(step(1))
        paused.pause()
        paused.resume()
        // Resumed a long way away — a drive, or simply a long pause.
        paused.onLocation(step(40))
        paused.onLocation(step(41))
        paused.stop()

        val withGap = paused.snapshot.value.journey
        val legs = withGap.segments.sumOf { it.size - 1 }
        assertEquals("two legs, one per segment", 2, legs)

        // Each leg is one 0.0005-degree step; the gap is 39 of them and must
        // not appear in the total.
        val oneStep = step(0).distanceTo(step(1))
        assertEquals(
            "the distance across the pause was added to the total",
            oneStep * 2,
            withGap.distanceMetres,
            1.0,
        )
    }

    @Test
    fun `stopping passes through Stopping and lands Completed`() {
        val recorder = recorder()
        recorder.start()
        recorder.onLocation(step(0))
        recorder.stop()
        assertEquals(RecordingState.Completed, recorder.state)
    }

    @Test
    fun `starting again on top of a live journey is refused`() {
        val recorder = recorder()
        recorder.start()
        recorder.onLocation(step(0))
        val id = recorder.snapshot.value.journey.id

        recorder.start()

        assertEquals("a second start silently threw away a live walk", id, recorder.snapshot.value.journey.id)
        assertEquals(1, recorder.snapshot.value.journey.points.size)
    }

    @Test
    fun `pause and resume are ignored from states where they mean nothing`() {
        val recorder = recorder()
        recorder.pause()
        assertEquals(RecordingState.Idle, recorder.state)
        recorder.resume()
        assertEquals(RecordingState.Idle, recorder.state)

        recorder.start()
        recorder.resume()
        assertEquals("resume while already recording should do nothing", RecordingState.Recording, recorder.state)
    }

    /**
     * The other one the brief names: stop, save, restart the app.
     */
    @Test
    fun `a saved journey survives the process going away`() {
        val recorder = recorder("survivor")
        recorder.start()
        repeat(4) { recorder.onLocation(step(it)) }
        recorder.pause()
        recorder.resume()
        recorder.onLocation(step(9))
        recorder.stop()

        val expected = recorder.snapshot.value.journey

        // Nothing of the old process survives but the directory.
        val reopened = FileJourneyStore(directory).mostRecent()

        assertNotNull("the journey did not survive a restart", reopened)
        assertEquals(expected.id, reopened!!.id)
        assertEquals(expected.points.size, reopened.points.size)
        assertEquals(expected.segments.size, reopened.segments.size)
        assertEquals(expected.distanceMetres, reopened.distanceMetres, 0.5)
        assertEquals(
            expected.points.first().position.latitude,
            reopened.points.first().position.latitude,
            1e-9,
        )
    }

    /**
     * The failure the journal format exists for: the process dies mid-walk,
     * so there is no end marker and no clean shutdown.
     */
    @Test
    fun `a walk interrupted by the process dying is still readable`() {
        val recorder = recorder("interrupted")
        recorder.start()
        repeat(5) { recorder.onLocation(step(it)) }
        // No stop(). The process simply ends here.

        val recovered = FileJourneyStore(directory).read("interrupted")

        assertNotNull("an interrupted walk was lost entirely", recovered)
        assertEquals(5, recovered!!.points.size)
    }

    @Test
    fun `losing permission mid-walk keeps what was already walked`() {
        val recorder = recorder()
        recorder.start()
        repeat(3) { recorder.onLocation(step(it)) }
        recorder.fail(RecordingProblem.PermissionLost)

        assertEquals(RecordingState.Error, recorder.state)
        assertEquals(RecordingProblem.PermissionLost, recorder.snapshot.value.problem)
        assertEquals(
            "the walk so far was thrown away with the error",
            3,
            recorder.snapshot.value.journey.points.size,
        )
        assertNotNull(FileJourneyStore(directory).read("walk"))
    }

    @Test
    fun `discarding removes the journey from disk`() {
        val recorder = recorder("unwanted")
        recorder.start()
        recorder.onLocation(step(0))
        recorder.stop()
        recorder.discard()

        assertEquals(RecordingState.Idle, recorder.state)
        assertNull(FileJourneyStore(directory).read("unwanted"))
    }

    @Test
    fun `a live journey cannot be discarded by accident`() {
        val recorder = recorder("live")
        recorder.start()
        recorder.onLocation(step(0))
        recorder.discard()

        assertEquals(RecordingState.Recording, recorder.state)
        assertEquals(1, recorder.snapshot.value.journey.points.size)
    }

    @Test
    fun `rejected readings are counted rather than drawn`() {
        val recorder = recorder()
        recorder.start()
        recorder.onLocation(step(0))
        // Same place, immediately after: inside the noise.
        recorder.onLocation(
            TrackPoint(
                position = GeoPoint(51.5 + 0.000001, -0.12),
                timestampMillis = 1_000_001L,
                accuracyMetres = 5f,
            ),
        )

        assertEquals(1, recorder.snapshot.value.journey.points.size)
        assertEquals(1, recorder.snapshot.value.rejected)
    }

    @Test
    fun `recovering a journey from disk puts it back on the map`() {
        val first = recorder("recovered")
        first.start()
        repeat(3) { first.onLocation(step(it)) }
        first.stop()

        val fresh = recorder("ignored")
        val onDisk = FileJourneyStore(directory).read("recovered")!!
        fresh.recover(onDisk)

        assertEquals(RecordingState.Completed, fresh.state)
        assertEquals(3, fresh.snapshot.value.journey.points.size)
    }
}
