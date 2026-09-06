package com.mapme.core.design.theme

import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The squircle is MapMe's most repeated shape, so its maths is worth pinning
 * down. These tests exist mainly to catch the one regression that would matter
 * and would be nearly invisible by eye: someone replacing the superellipse
 * with an ordinary circular arc.
 */
class SquircleShapeTest {

    @Test
    fun `the corner is squarer than a circle`() {
        // At 45° a circular corner sits at r/√2 ≈ 0.707r from the corner's
        // centre on each axis. A superellipse pushes further out — that extra
        // distance IS the squarical look, so if this ever collapses back to
        // 0.707 the shape has silently become a rounded rectangle.
        val circular = 1f / 2f.pow(0.5f)
        val squircle = (0.5f).pow(1f / MAPME_EXPONENT)
        assertTrue(
            "superellipse corner should bulge past a circular one " +
                "($squircle vs $circular)",
            squircle > circular + 0.03f,
        )
    }

    @Test
    fun `the outline stays inside its bounds`() {
        val size = Size(300f, 200f)
        val path = squirclePath(
            size = size,
            topStart = 40f,
            topEnd = 40f,
            bottomEnd = 40f,
            bottomStart = 40f,
            exponent = MAPME_EXPONENT,
        )
        val bounds = path.getBounds()
        assertTrue("path escapes the left edge", bounds.left >= -0.5f)
        assertTrue("path escapes the top edge", bounds.top >= -0.5f)
        assertTrue("path escapes the right edge", bounds.right <= size.width + 0.5f)
        assertTrue("path escapes the bottom edge", bounds.bottom <= size.height + 0.5f)
        // And it should actually fill them, not sit shrunken inside.
        assertEquals(size.width, bounds.width, 0.5f)
        assertEquals(size.height, bounds.height, 0.5f)
    }

    @Test
    fun `a zero radius corner is a square corner`() {
        val path = squirclePath(Size(100f, 100f), 0f, 0f, 0f, 0f, MAPME_EXPONENT)
        val bounds = path.getBounds()
        assertEquals(100f, bounds.width, 0.01f)
        assertEquals(100f, bounds.height, 0.01f)
    }

    @Test
    fun `radii are clamped so a small surface cannot invert itself`() {
        // A 24dp control asking for a 40dp corner must not fold inside out.
        val shape = SquircleShape(all = androidx.compose.ui.unit.Dp(40f))
        val density = androidx.compose.ui.unit.Density(1f)
        val outline = shape.createOutline(
            size = Size(24f, 24f),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            density = density,
        )
        val bounds = (outline as androidx.compose.ui.graphics.Outline.Generic).path.getBounds()
        assertTrue("clamped shape should still be 24 wide", abs(bounds.width - 24f) < 0.5f)
        assertTrue("clamped shape should still be 24 tall", abs(bounds.height - 24f) < 0.5f)
    }
}
