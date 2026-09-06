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

    /**
     * The one that a `cancelAndJoin` test cannot catch.
     *
     * `LaunchedEffect` cancels the outgoing reveal and starts the new one
     * without waiting for the old coroutine to unwind, so the old `finally`
     * lands *after* its replacement is already running. If that cleanup is
     * unconditional it switches the live transition off: the snapshot is never
     * recorded and the reveal draws a stale layer over the interface.
     */
    @Test
    fun `a superseded reveal cannot switch off the one that replaced it`() = runBlocking {
        val transition = ThemeTransition()

        val firstClock = BroadcastFrameClock()
        val first = launch(firstClock + Dispatchers.Unconfined) {
            transition.play(false, 380, LinearEasing) { }
        }
        firstClock.sendFrame(0L)
        firstClock.sendFrame(16_000_000L)
        assertEquals(ThemeTransition.Phase.Revealing, transition.phase)
        firstClock.sendFrame(120_000_000L)

        // Tap again. Nobody joins the outgoing coroutine — exactly as Compose
        // does it — so its cleanup runs whenever it gets round to it.
        val secondClock = BroadcastFrameClock()
        val second = launch(secondClock + Dispatchers.Unconfined) {
            transition.play(false, 380, LinearEasing) { }
        }
        first.join()

        assertTrue(
            "the superseded reveal reset the transition that replaced it",
            transition.phase != ThemeTransition.Phase.Idle,
        )
        second.cancelAndJoin()
    }

    /** An interrupted boundary finishes travelling rather than popping. */
    @Test
    fun `interrupting runs the outgoing boundary out before capturing again`() = runBlocking {
        val transition = ThemeTransition()

        val firstClock = BroadcastFrameClock()
        val first = launch(firstClock + Dispatchers.Unconfined) {
            transition.play(false, 380, LinearEasing) { }
        }
        firstClock.sendFrame(0L)
        firstClock.sendFrame(16_000_000L)
        firstClock.sendFrame(60_000_000L)
        val interruptedAt = transition.progress.value
        assertTrue("expected a reveal in flight to interrupt", interruptedAt < 1f)

        val secondClock = BroadcastFrameClock()
        val second = launch(secondClock + Dispatchers.Unconfined) {
            transition.play(false, 380, LinearEasing) { }
        }
        first.join()

        // Still showing the outgoing boundary, not a captured still.
        assertEquals(ThemeTransition.Phase.Revealing, transition.phase)

        // Drive frames until the new transition starts capturing, and check the
        // invariant at exactly that moment: the old boundary is off the edge,
        // so there is nothing left to jump.
        var t = 0L
        var captured = false
        repeat(40) {
            if (!captured) {
                t += 16_000_000L
                secondClock.sendFrame(t)
                if (transition.phase == ThemeTransition.Phase.Capturing) {
                    assertEquals(
                        "captured mid-boundary — the interface would jump here",
                        1f,
                        transition.progress.value,
                        0.0001f,
                    )
                    captured = true
                }
            }
        }
        assertTrue("the interrupting transition never reached its capture", captured)
        second.cancelAndJoin()
    }

    /**
     * The system bars cannot be recorded, so they are switched rather than
     * revealed — but not at the moment the theme commits, which would leave
     * dark icons on a still-dark screen for the length of the reveal.
     */
    @Test
    fun `the bars are told to change once, after the commit and before the end`() = runBlocking {
        val clock = BroadcastFrameClock()
        val transition = ThemeTransition()
        var committed = false
        var passes = 0
        var progressWhenPassed = -1f

        val job = launch(clock + Dispatchers.Unconfined) {
            transition.play(
                reduceMotion = false,
                durationMillis = 80,
                easing = LinearEasing,
                onBoundaryPassed = {
                    passes++
                    progressWhenPassed = transition.progress.value
                },
            ) { committed = true }
        }

        clock.sendFrame(0L)
        clock.sendFrame(16_000_000L)
        assertTrue("commit must happen before the bars are told anything", committed)
        assertEquals("told too early — the reveal has not started", 0, passes)

        var t = 16_000_000L
        repeat(12) { t += 16_000_000L; clock.sendFrame(t) }

        assertEquals("the bars must be told exactly once", 1, passes)
        assertTrue(
            "told at $progressWhenPassed, before the boundary is clear of the top strip",
            progressWhenPassed >= 0.4f,
        )
        assertTrue("told at the very end, which is just a late flip", progressWhenPassed < 1f)
        job.cancelAndJoin()
    }

    @Test
    fun `reduced motion applies the theme without any reveal at all`() = runBlocking {
        val transition = ThemeTransition()
        var committed = false
        var passes = 0
        transition.play(
            reduceMotion = true,
            durationMillis = 380,
            easing = LinearEasing,
            onBoundaryPassed = { passes++ },
        ) { committed = true }
        assertTrue(committed)
        // Nothing travels, so the bars change with everything else.
        assertEquals(1, passes)
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
