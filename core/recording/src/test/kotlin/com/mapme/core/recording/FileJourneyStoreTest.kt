package com.mapme.core.recording

import com.mapme.core.model.GeoPoint
import com.mapme.core.model.Journey
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

class FileJourneyStoreTest {

    private lateinit var directory: File
    private lateinit var store: FileJourneyStore

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("mapme-store").toFile()
        store = FileJourneyStore(directory)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun point(index: Int) = TrackPoint(
        position = GeoPoint(51.5 + index * 0.001, -0.12 + index * 0.001),
        timestampMillis = 1_700_000_000_000L + index * 5_000L,
        accuracyMetres = 4.5f + index,
        altitudeMetres = 20.0 + index,
        speedMetresPerSecond = 1.3f,
    )

    private fun write(id: String, segments: List<List<TrackPoint>>) {
        store.open(id)
        segments.forEach { segment ->
            store.appendBreak(id)
            segment.forEach { store.appendPoint(id, it) }
        }
        store.close(id, segments.lastOrNull()?.lastOrNull()?.timestampMillis ?: 0L)
    }

    @Test
    fun `a journey survives a round trip through the file exactly`() {
        val original = listOf(listOf(point(0), point(1), point(2)))
        write("round", original)

        val read = store.read("round")
        assertNotNull("the journey did not come back at all", read)
        assertEquals(1, read!!.segments.size)
        assertEquals(3, read.points.size)

        original.first().zip(read.points).forEach { (before, after) ->
            assertEquals(before.position.latitude, after.position.latitude, 0.0)
            assertEquals(before.position.longitude, after.position.longitude, 0.0)
            assertEquals(before.timestampMillis, after.timestampMillis)
            assertEquals(before.accuracyMetres, after.accuracyMetres, 0.0f)
            assertEquals(before.altitudeMetres!!, after.altitudeMetres!!, 0.0)
        }
    }

    @Test
    fun `segments are preserved, because they are the shape of the walk`() {
        write("segmented", listOf(listOf(point(0), point(1)), listOf(point(5), point(6))))
        val read = store.read("segmented")!!
        assertEquals(2, read.segments.size)
        assertEquals(2, read.segments[0].size)
        assertEquals(2, read.segments[1].size)
    }

    @Test
    fun `a reading with no altitude or speed round trips as absent`() {
        val bare = TrackPoint(GeoPoint(1.0, 2.0), 1_000L, 3f)
        store.open("bare")
        store.appendBreak("bare")
        store.appendPoint("bare", bare)
        store.close("bare", 1_000L)

        val read = store.read("bare")!!.points.single()
        assertNull(read.altitudeMetres)
        assertNull(read.speedMetresPerSecond)
        assertEquals(1.0, read.position.latitude, 0.0)
    }

    /**
     * The crash case the format exists for. No end marker, and a final line
     * that was cut off mid-write because the process died.
     */
    @Test
    fun `a truncated journal keeps everything that was written before the cut`() {
        store.open("torn")
        store.appendBreak("torn")
        repeat(4) { store.appendPoint("torn", point(it)) }
        // Simulate the process dying part way through writing a fifth line.
        File(directory, "torn.mapme").appendText("p 1700000000025 51.505 -0.1")

        val read = store.read("torn")
        assertNotNull("a torn file lost the whole walk", read)
        assertTrue(
            "the four complete readings should have survived",
            read!!.points.size >= 4,
        )
    }

    @Test
    fun `lines from a future version are skipped rather than fatal`() {
        store.open("future")
        store.appendBreak("future")
        store.appendPoint("future", point(0))
        File(directory, "future.mapme").appendText("t A walk to the river\nw sunny\n")
        store.appendPoint("future", point(1))

        val read = store.read("future")
        assertNotNull("an unknown line broke the reader", read)
        assertEquals(2, read!!.points.size)
    }

    @Test
    fun `the most recent journey is the one that comes back`() {
        write("older", listOf(listOf(point(0), point(1))))
        Thread.sleep(1_100) // file timestamps are second-resolution on some filesystems
        write("newer", listOf(listOf(point(2), point(3))))

        assertEquals("newer", store.mostRecent()?.id)
    }

    @Test
    fun `an empty journey is not a journey`() {
        store.open("nothing")
        store.close("nothing", 0L)
        assertNull(store.read("nothing"))
    }

    @Test
    fun `reading a journey that was never written returns nothing`() {
        assertNull(store.read("imaginary"))
        assertNull(store.mostRecent())
    }

    @Test
    fun `an id cannot escape the journeys directory`() {
        val nasty = "../../etc/passwd"
        write(nasty, listOf(listOf(point(0), point(1))))

        val escaped = File(directory.parentFile.parentFile, "etc")
        assertTrue("an id was allowed to write outside its directory", !escaped.exists())
        assertEquals(
            "the journey should still be readable under its sanitised name",
            2,
            store.read(nasty)?.points?.size ?: 0,
        )
    }

    @Test
    fun `deleting removes it`() {
        write("temporary", listOf(listOf(point(0), point(1))))
        assertNotNull(store.read("temporary"))
        store.delete("temporary")
        assertNull(store.read("temporary"))
    }

    @Test
    fun `distance and duration come back the same after a restart`() {
        val segments = listOf(listOf(point(0), point(1), point(2)))
        write("maths", segments)
        val before = Journey("maths", segments)
        val after = store.read("maths")!!

        assertEquals(before.distanceMetres, after.distanceMetres, 0.001)
        assertEquals(before.durationMillis, after.durationMillis)
        assertEquals(before.recordedMillis, after.recordedMillis)
    }
}
