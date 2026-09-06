package com.mapme.core.design.theme

import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The squircle is MapMe's most repeated shape, so its maths is worth pinning
 * down.
 *
 * These run against [squircleOutline] rather than the `Path` it feeds, because
 * `Path` wraps `android.graphics.Path` and simply does not exist in a JVM unit
 * test. Testing the points instead means the corner is proven on every build
 * rather than eyeballed once on a device.
 */
class SquircleShapeTest {

    private fun outline(
        size: Size = Size(300f, 200f),
        radius: Float = 40f,
    ) = squircleOutline(size, radius, radius, radius, radius, MAPME_EXPONENT)

    @Test
    fun `the corner is squarer than a circle`() {
        // At 45° a circular corner sits at r/√2 ≈ 0.707r from the corner's
        // centre on each axis. A superellipse pushes further out, and that
        // extra distance IS the squarical look. If this ever collapses back to
        // 0.707 the shape has silently become an ordinary rounded rectangle.
        val circular = 1f / 2f.pow(0.5f)
        val squircle = (0.5f).pow(1f / MAPME_EXPONENT)
        assertTrue(
            "superellipse corner should bulge past a circular one ($squircle vs $circular)",
            squircle > circular + 0.03f,
        )
    }

    @Test
    fun `the outline fills its bounds without escaping them`() {
        val size = Size(300f, 200f)
        val points = outline(size)
        assertTrue("outline should have real geometry", points.size > 40)

        assertTrue("escapes left", points.all { it.x >= -0.01f })
        assertTrue("escapes top", points.all { it.y >= -0.01f })
        assertTrue("escapes right", points.all { it.x <= size.width + 0.01f })
        assertTrue("escapes bottom", points.all { it.y <= size.height + 0.01f })

        // And it should actually reach the edges, not sit shrunken inside.
        assertEquals(0f, points.minOf { it.x }, 0.01f)
        assertEquals(0f, points.minOf { it.y }, 0.01f)
        assertEquals(size.width, points.maxOf { it.x }, 0.01f)
        assertEquals(size.height, points.maxOf { it.y }, 0.01f)
    }

    @Test
    fun `a zero radius corner is a square corner`() {
        val points = squircleOutline(Size(100f, 100f), 0f, 0f, 0f, 0f, MAPME_EXPONENT)
        // Only the four corners survive; nothing is sampled.
        assertTrue("expected a plain rectangle, got ${points.size} points", points.size <= 8)
        assertEquals(100f, points.maxOf { it.x }, 0.01f)
        assertEquals(100f, points.maxOf { it.y }, 0.01f)
    }

    @Test
    fun `radii are clamped so a small surface cannot invert itself`() {
        // A 24dp control asking for a 40dp corner must not fold inside out.
        val size = Size(24f, 24f)
        assertEquals(12f, clampRadius(40f, size), 0.001f)
        assertEquals(0f, clampRadius(-5f, size), 0.001f)

        val points = squircleOutline(size, 40f, 40f, 40f, 40f, MAPME_EXPONENT)
        assertEquals(24f, points.maxOf { it.x }, 0.01f)
        assertEquals(24f, points.maxOf { it.y }, 0.01f)
        assertTrue("clamped shape escaped its bounds", points.all { it.x >= -0.01f && it.y >= -0.01f })
    }

    @Test
    fun `corner samples are spaced evenly along the curve`() {
        // Uniform steps in the angle parameter leave the corner three times
        // coarser at one end than the other — about half a pixel of visible
        // flattening on a 40dp corner. Even arc-length spacing is what keeps
        // the curve clean, so it is worth asserting rather than trusting.
        val radius = 40f
        val points = outline(Size(300f, 200f), radius)
        val steps = points.zipWithNext()
            .map { (a, b) -> hypot(b.x - a.x, b.y - a.y) }
            .filter { it < radius } // the four straight edges are much longer
        assertTrue("no corner samples found", steps.size > 40)

        val longest = steps.max()
        val mean = steps.average().toFloat()
        assertTrue(
            "corner sampling is uneven: longest $longest vs mean $mean",
            longest < mean * 1.15f,
        )
        assertTrue("corner step of $longest is too coarse for r=$radius", longest < radius * 0.15f)
    }

    @Test
    fun `an exponent of two gives an ordinary circular corner back`() {
        // The safety net for the whole idea: at n = 2 the superellipse must
        // reduce exactly to a circle, so if this drifts the formula is wrong.
        val r = 50f
        val points = squircleOutline(Size(200f, 200f), r, r, r, r, exponent = 2f)
        val centre = androidx.compose.ui.geometry.Offset(r, r)
        val topLeftArc = points.filter { it.x <= r && it.y <= r }
        assertTrue("expected sampled arc points", topLeftArc.size > 8)
        topLeftArc.forEach {
            val d = hypot(it.x - centre.x, it.y - centre.y)
            assertTrue("point $it is $d from centre, expected $r", abs(d - r) < 0.05f)
        }
    }
}
