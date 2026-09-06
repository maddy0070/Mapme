package com.mapme.core.design.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The theme reveal has one hard requirement beyond looking good: it must never
 * strand the interface.
 *
 * Tapping the control repeatedly cancels a reveal mid-flight, and a cancelled
 * reveal that forgot to clean up would leave a frozen screenshot of the old
 * theme sitting over the live one — an app that appears to have hung. These
 * tests drive the state machine with a hand-cranked frame clock so that
 * guarantee is checked on every build rather than by tapping quickly and
 * hoping.
 */
class ThemeTransitionTest {

    @Test
    fun `a completed reveal commits the theme and packs itself away`() = runBlocking {
        val clock = BroadcastFrameClock()
        val transition = ThemeTransition()
        var committed = false

        val job = launch(clock + Dispatchers.Unconfined) {
            transition.play(
                reduceMotion = false,
                durationMillis = 40,
                easing = LinearEasing,
            ) { committed = true }
        }

        // Waiting for the frame that records the old interface.
        assertEquals(ThemeTransition.Phase.Capturing, transition.phase)
        assertTrue("must not change the theme before the snapshot exists", !committed)

        clock.sendFrame(0L)
        clock.sendFrame(16_000_000L)

        assertTrue("theme should be applied once the snapshot is safe", committed)
        assertEquals(ThemeTransition.Phase.Revealing, transition.phase)

        // Run the reveal out.
        repeat(12) { clock.sendFrame(32_000_000L + it * 16_000_000L) }
        job.cancelAndJoin()
        assertEquals(ThemeTransition.Phase.Idle, transition.phase)
    }

    @Test
    fun `a reveal cancelled part way never leaves the overlay up`() = runBlocking {
        val clock = BroadcastFrameClock()
        val transition = ThemeTransition()

        val job = launch(clock + Dispatchers.Unconfined) {
            transition.play(false, 380, LinearEasing) { }
        }
        clock.sendFrame(0L)
        clock.sendFrame(16_000_000L)
        assertEquals(ThemeTransition.Phase.Revealing, transition.phase)

        job.cancelAndJoin()
        assertEquals(
            "a cancelled reveal must clear itself, or the old theme stays frozen on screen",
            ThemeTransition.Phase.Idle,
            transition.phase,
        )
    }

    @Test
    fun `interrupting repeatedly always settles back to idle`() = runBlocking {
        // Six switches in a row, each cancelling the last: Auto to Light to
        // Dark and back round again, faster than any of them can finish.
        val transition = ThemeTransition()
        repeat(6) {
            val clock = BroadcastFrameClock()
            val job = launch(clock + Dispatchers.Unconfined) {
                transition.play(false, 380, LinearEasing) { }
            }
            clock.sendFrame(0L)
            clock.sendFrame(16_000_000L)
            job.cancelAndJoin()
            assertEquals(ThemeTransition.Phase.Idle, transition.phase)
        }
    }

    @Test
    fun `reduced motion applies the theme without any reveal at all`() = runBlocking {
        val transition = ThemeTransition()
        var committed = false
        transition.play(reduceMotion = true, durationMillis = 380, easing = LinearEasing) {
            committed = true
        }
        assertTrue(committed)
        assertEquals(ThemeTransition.Phase.Idle, transition.phase)
    }

    @Test
    fun `the reveal reaches every corner from wherever it started`() {
        val size = Size(1080f, 2400f)
        // The control lives in the top-right, which is the worst case: the
        // opposite corner is nearly the full diagonal away.
        listOf(
            Offset(1000f, 120f),
            Offset(0f, 0f),
            Offset(540f, 1200f),
            Offset(1080f, 2400f),
        ).forEach { origin ->
            val full = revealRadius(1f, origin, size)
            val corners = listOf(
                Offset(0f, 0f), Offset(size.width, 0f),
                Offset(0f, size.height), Offset(size.width, size.height),
            )
            corners.forEach { corner ->
                val distance = (corner - origin).getDistance()
                assertTrue(
                    "from $origin the reveal stops $distance short of $corner",
                    full >= distance,
                )
            }
        }
    }

    @Test
    fun `the reveal grows from nothing and never runs backwards`() {
        val origin = Offset(900f, 100f)
        val size = Size(1080f, 2400f)
        assertEquals(0f, revealRadius(0f, origin, size), 0.001f)

        var previous = -1f
        for (step in 0..20) {
            val r = revealRadius(step / 20f, origin, size)
            assertTrue("radius went backwards at $step", r >= previous)
            previous = r
        }
    }

    @Test
    fun `the control walks Auto to Light to Dark and back`() {
        assertEquals(ThemeMode.Light, ThemeMode.System.next())
        assertEquals(ThemeMode.Dark, ThemeMode.Light.next())
        assertEquals(ThemeMode.System, ThemeMode.Dark.next())
    }

    @Test
    fun `Auto follows the phone and the others do not`() {
        assertTrue(ThemeMode.System.resolve(systemDark = true))
        assertTrue(!ThemeMode.System.resolve(systemDark = false))
        assertTrue(ThemeMode.Dark.resolve(systemDark = false))
        assertTrue(!ThemeMode.Light.resolve(systemDark = true))
    }
}
