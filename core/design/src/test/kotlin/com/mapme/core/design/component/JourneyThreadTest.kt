package com.mapme.core.design.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The onboarding drawing is brand artwork, not a random doodle. It has to be
 * byte-identical on every launch and every device, the way a logo is — and it
 * has to actually fill its canvas, or the camera work in onboarding frames
 * empty space.
 */
class JourneyThreadTest {

    @Test
    fun `the same seed always draws the same line`() {
        val a = JourneyThread.generate()
        val b = JourneyThread.generate()
        assertEquals(a, b)
    }

    @Test
    fun `a different seed draws a different line`() {
        val signature = JourneyThread.generate()
        val other = JourneyThread.generate(seed = JourneyThread.SIGNATURE_SEED + 1)
        assertTrue("seeding should actually change the artwork", signature != other)
    }

    @Test
    fun `the thread fills its space without escaping it`() {
        val points = JourneyThread.generate()
        assertTrue(points.all { it.x in 0f..1f && it.y in 0f..1f })

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        // It should genuinely span the canvas — a thread hugging the middle
        // would leave onboarding's widest shot looking like a mistake.
        assertTrue("thread is too narrow (${maxX - minX})", maxX - minX > 0.55f)
        assertTrue("thread is too short (${maxY - minY})", maxY - minY > 0.55f)
    }

    @Test
    fun `the line never jumps`() {
        // Every step is a person moving, so consecutive samples must stay
        // close. A gap would draw as a straight line across the artwork.
        val points = JourneyThread.generate()
        val longest = points.zipWithNext().maxOf { (a, b) -> (b - a).getDistance() }
        assertTrue("found a $longest jump between samples", longest < 0.08f)
    }

    @Test
    fun `place markers land on the thread and stay spread out`() {
        val points = JourneyThread.generate()
        val markers = JourneyThread.placeMarkers(count = 9, threadSize = points.size)
        assertTrue("markers should exist", markers.isNotEmpty())
        assertTrue("markers must index real samples", markers.all { it in points.indices })
        assertTrue("markers must be ordered", markers.zipWithNext().all { (a, b) -> b > a })
        assertTrue(
            "markers should not clump",
            markers.zipWithNext().all { (a, b) -> b - a >= 20 },
        )
    }
}
