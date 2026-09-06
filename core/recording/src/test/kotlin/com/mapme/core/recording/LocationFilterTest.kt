package com.mapme.core.recording

import com.mapme.core.model.GeoPoint
import com.mapme.core.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The filter decides what MapMe is willing to claim about where someone went.
 *
 * Its one hard rule is that it only ever discards. If a test here ever needs
 * to assert a *moved* or *invented* coordinate, the filter has stopped being a
 * filter and the product has started drawing fiction.
 */
class LocationFilterTest {

    private val filter = LocationFilter()

    private fun at(
        lat: Double,
        lon: Double,
        millis: Long,
        accuracy: Float = 5f,
    ) = TrackPoint(GeoPoint(lat, lon), millis, accuracy)

    private fun verdict(candidate: TrackPoint, previous: TrackPoint?) = filter.judge(candidate, previous)

    private fun reasonOf(candidate: TrackPoint, previous: TrackPoint?): LocationFilter.Reason? =
        (verdict(candidate, previous) as? LocationFilter.Verdict.Reject)?.reason

    @Test
    fun `the first reading is always taken`() {
        assertTrue(verdict(at(51.5, -0.12, 0), null) is LocationFilter.Verdict.Accept)
    }

    @Test
    fun `a wildly uncertain fix is not believed at all`() {
        assertEquals(
            LocationFilter.Reason.TooVague,
            reasonOf(at(51.5, -0.12, 0, accuracy = 120f), null),
        )
    }

    @Test
    fun `a fix that teleports across the city is rejected`() {
        val here = at(51.5, -0.12, 0)
        // Ten kilometres in one second.
        val there = at(51.59, -0.12, 1_000)
        assertEquals(LocationFilter.Reason.Impossible, reasonOf(there, here))
    }

    @Test
    fun `a stale reading arriving late does not rewrite history`() {
        val newer = at(51.5, -0.12, 10_000)
        val older = at(51.501, -0.12, 5_000)
        assertEquals(LocationFilter.Reason.OutOfOrder, reasonOf(older, newer))
    }

    /**
     * The rule that stops a phone on a table drawing a bird's nest.
     */
    @Test
    fun `standing still does not create movement`() {
        val here = at(51.5, -0.12, 0)
        // Three metres away, half a minute later: within any city fix's noise.
        val jitter = at(51.500027, -0.12, 30_000)
        assertEquals(LocationFilter.Reason.InsideTheNoise, reasonOf(jitter, here))
    }

    @Test
    fun `walking is movement`() {
        val here = at(51.5, -0.12, 0)
        // Roughly 22 m, which at a walking pace is about fifteen seconds.
        val there = at(51.5002, -0.12, 15_000)
        assertTrue(verdict(there, here) is LocationFilter.Verdict.Accept)
    }

    /**
     * The threshold scales with the fix's own confidence. The same step is
     * meaningful from a good fix and meaningless from a vague one — which is
     * the difference between a trail that follows a pavement and one that
     * wanders through the buildings beside it.
     */
    @Test
    fun `how far counts as movement depends on how sure the fix is`() {
        val here = at(51.5, -0.12, 0)
        val eightMetresLater = at(51.500072, -0.12, 20_000, accuracy = 4f)
        assertTrue(
            "8 m from a fix good to 4 m is a real step",
            verdict(eightMetresLater, here) is LocationFilter.Verdict.Accept,
        )

        val sameStepButVague = at(51.500072, -0.12, 20_000, accuracy = 30f)
        assertEquals(
            "8 m from a fix good only to 30 m says nothing",
            LocationFilter.Reason.InsideTheNoise,
            reasonOf(sameStepButVague, here),
        )
    }

    @Test
    fun `an accepted reading is passed through completely untouched`() {
        // The filter's whole contract: it is a gate, not an editor. Nothing
        // that comes out of it may differ from what went in.
        val original = TrackPoint(
            position = GeoPoint(51.5001234, -0.1209876),
            timestampMillis = 42_000,
            accuracyMetres = 4.25f,
            altitudeMetres = 31.5,
            speedMetresPerSecond = 1.4f,
        )
        assertTrue(verdict(original, null) is LocationFilter.Verdict.Accept)
        // There is deliberately no API here that could return a different
        // point — judge() answers yes or no. This test documents that, so a
        // future "smoothing" change has to delete it on purpose.
        assertEquals(51.5001234, original.position.latitude, 0.0)
        assertEquals(-0.1209876, original.position.longitude, 0.0)
    }

    @Test
    fun `a fast train is still a journey`() {
        val here = at(51.5, -0.12, 0)
        // ~35 m/s, about 125 km/h. Fast, but a real thing that happens.
        val there = at(51.5063, -0.12, 20_000)
        assertTrue(
            "MapMe records trains too; the speed rule is for bad fixes, not fast travel",
            verdict(there, here) is LocationFilter.Verdict.Accept,
        )
    }
}
