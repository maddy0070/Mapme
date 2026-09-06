package com.mapme.core.design.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The thread.
 *
 * MapMe's onboarding is built around one continuous line that the person is
 * shown at three different distances. This generates that line.
 *
 * ## This is artwork, not data
 *
 * These points are **not** a journey, not a recording, and not a stand-in for
 * one. They are a deterministic drawing, generated from a fixed seed so that
 * every launch of MapMe shows the identical artwork — the way a logo is the
 * same every time. Nothing here pretends to be a place anyone went, and no
 * screen presents it as a statistic. When real journeys arrive they will
 * render through [TrailLine] exactly like this does, which is the point: the
 * onboarding is a promise the product can actually keep.
 *
 * ## How it moves
 *
 * A pure random walk looks like static; a sine wave looks like a graph.
 * Neither looks like a person. So the walk integrates its *turn rate* rather
 * than its heading — the line keeps curving the way it was already curving,
 * and changes its mind gradually. Damping stops it spiralling, and a gentle
 * pull home keeps it from wandering off the canvas without ever making the
 * shape look contained.
 */
object JourneyThread {

    /** The seed. Changing this changes MapMe's onboarding drawing. */
    const val SIGNATURE_SEED: Int = 20_260_906

    /**
     * @param count how many samples to walk. ~240 gives a line that stays
     *   smooth when the camera is pulled all the way back.
     */
    fun generate(count: Int = 240, seed: Int = SIGNATURE_SEED): List<Offset> {
        val random = Random(seed)
        var x = 0.5
        var y = 0.62
        var heading = -1.1
        var turnRate = 0.0
        val step = 0.021
        val raw = ArrayList<Offset>(count)

        repeat(count) {
            // Integrating the turn rate is what makes this read as travel
            // rather than noise.
            turnRate += (random.nextDouble() - 0.5) * 0.34
            turnRate *= 0.87
            heading += turnRate

            val dx = x - 0.5
            val dy = y - 0.5
            val distance = hypot(dx, dy)
            if (distance > 0.30) {
                val home = atan2(-dy, -dx)
                var diff = home - heading
                while (diff > Math.PI) diff -= 2 * Math.PI
                while (diff < -Math.PI) diff += 2 * Math.PI
                heading += diff * ((distance - 0.30) * 5.0).coerceAtMost(0.5)
            }

            x += cos(heading) * step
            y += sin(heading) * step
            raw += Offset(x.toFloat(), y.toFloat())
        }

        return normalise(raw)
    }

    /**
     * Indices along the thread that read as places worth remembering.
     *
     * Spaced by an irregular stride so they do not look metronomic — a life
     * does not produce evenly spaced memories.
     */
    fun placeMarkers(count: Int, threadSize: Int): List<Int> {
        val strides = intArrayOf(37, 29, 43, 31, 47, 23)
        val out = ArrayList<Int>(count)
        var index = 18
        var i = 0
        while (out.size < count && index < threadSize - 6) {
            out += index
            index += strides[i % strides.size]
            i++
        }
        return out
    }

    /** Fits the walk into 0..1 on both axes with a little breathing room. */
    private fun normalise(points: List<Offset>): List<Offset> {
        if (points.isEmpty()) return points
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        points.forEach {
            if (it.x < minX) minX = it.x
            if (it.x > maxX) maxX = it.x
            if (it.y < minY) minY = it.y
            if (it.y > maxY) maxY = it.y
        }
        val spanX = (maxX - minX).takeIf { it > 1e-4f } ?: 1f
        val spanY = (maxY - minY).takeIf { it > 1e-4f } ?: 1f
        val span = maxOf(spanX, spanY)
        val offsetX = (span - spanX) / 2f
        val offsetY = (span - spanY) / 2f
        val inset = 0.06f
        val scale = 1f - inset * 2f
        return points.map {
            Offset(
                inset + ((it.x - minX) + offsetX) / span * scale,
                inset + ((it.y - minY) + offsetY) / span * scale,
            )
        }
    }
}

/**
 * Where the camera is looking at the thread.
 *
 * Onboarding never changes the thread — it only changes this. That is what
 * makes moving between pages feel like pulling back from one physical drawing
 * instead of swapping two pictures.
 *
 * @param scale 1 shows the whole thread; larger moves closer.
 * @param focus the point in thread space (0..1) held at the centre of view.
 */
@Immutable
data class ThreadCamera(
    val scale: Float,
    val focus: Offset,
)

@Composable
fun rememberJourneyThread(count: Int = 240): List<Offset> =
    remember(count) { JourneyThread.generate(count) }
