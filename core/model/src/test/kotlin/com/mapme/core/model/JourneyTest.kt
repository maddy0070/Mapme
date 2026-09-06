package com.mapme.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyTest {

    private fun reading(lat: Double, lon: Double, atMillis: Long) = TrackPoint(
        position = GeoPoint(lat, lon),
        timestampMillis = atMillis,
        accuracyMetres = 8f,
    )

    @Test
    fun `an empty journey has no length and no duration`() {
        val journey = Journey.Empty
        assertTrue(journey.isEmpty)
        assertEquals(0.0, journey.distanceMetres, 0.0)
        assertEquals(0L, journey.durationMillis)
        assertNull(journey.startedAtMillis)
    }

    @Test
    fun `a single reading is a journey of no distance`() {
        val journey = Journey.of("j", listOf(reading(51.5, -0.12, 1_000)))
        assertEquals(0.0, journey.distanceMetres, 0.0)
        assertEquals(0L, journey.durationMillis)
    }

    @Test
    fun `distance sums the legs rather than measuring the endpoints`() {
        // Out one degree of latitude and back again: 222 km travelled, and
        // zero metres from home. The first number is the one that matters.
        val journey = Journey.of(
            id = "loop",
            points = listOf(
                reading(0.0, 0.0, 0),
                reading(1.0, 0.0, 60_000),
                reading(0.0, 0.0, 120_000),
            ),
        )
        assertEquals(222_390.0, journey.distanceMetres, 200.0)
        assertEquals(120_000L, journey.durationMillis)
    }

    @Test
    fun `average speed is distance over elapsed time`() {
        val journey = Journey.of(
            id = "walk",
            points = listOf(
                reading(0.0, 0.0, 0),
                reading(0.0, 0.001, 100_000),
            ),
        )
        // ~111.2 m in 100 s.
        assertEquals(1.112, journey.averageSpeedMetresPerSecond, 0.01)
    }

    @Test
    fun `implied speed refuses to divide by zero elapsed time`() {
        val a = reading(0.0, 0.0, 5_000)
        val b = reading(1.0, 0.0, 5_000)
        assertNull("Two readings at the same instant cannot imply a speed", a.impliedSpeedTo(b))
    }

    @Test
    fun `implied speed reports how fast a leg would have been`() {
        val a = reading(0.0, 0.0, 0)
        val b = reading(0.0, 0.001, 10_000)
        assertEquals(11.12, a.impliedSpeedTo(b)!!, 0.05)
    }
}
