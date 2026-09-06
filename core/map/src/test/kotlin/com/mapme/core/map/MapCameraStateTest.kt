package com.mapme.core.map

import com.mapme.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The camera has one behaviour worth protecting: it stops following the
 * moment you touch it, and starts again only when you ask.
 *
 * Every irritating map does the opposite — you drag two streets over to see
 * where you are going and it snatches itself back on the next GPS fix. These
 * tests are here because that bug is reintroduced by an innocent-looking
 * "keep the camera in sync with location" change.
 */
class MapCameraStateTest {

    private val here = GeoPoint(51.5072, -0.1276)
    private val alsoHere = GeoPoint(51.5081, -0.1290)

    @Test
    fun `the first fix takes the camera there and chooses a sensible zoom`() {
        val camera = MapCameraState()
        camera.onLocation(here)

        val pending = requireNotNull(camera.pending)
        assertEquals(here, pending.target)
        assertEquals(MapPosition.YOU_ARE_HERE_ZOOM, pending.zoom, 0.001)
    }

    @Test
    fun `touching the map stops it following`() {
        val camera = MapCameraState()
        camera.onUserGesture()
        assertFalse(camera.following)
    }

    @Test
    fun `a new fix does not drag the map back once you have moved it`() {
        val camera = MapCameraState()
        camera.onLocation(here)
        camera.onPendingApplied()
        camera.onCameraSettled(MapPosition(here, MapPosition.YOU_ARE_HERE_ZOOM))

        camera.onUserGesture()
        camera.onLocation(alsoHere)

        assertNull("the camera moved itself after the person had taken it", camera.pending)
    }

    @Test
    fun `recentring is the only thing that resumes following`() {
        val camera = MapCameraState()
        camera.onUserGesture()
        assertFalse(camera.following)

        camera.recentre(here)
        assertTrue(camera.following)
        assertEquals(here, requireNotNull(camera.pending).target)
    }

    @Test
    fun `recentring with no fix yet still resumes following, so the next one lands`() {
        val camera = MapCameraState()
        camera.onUserGesture()
        camera.recentre(null)

        assertTrue(camera.following)
        assertNull(camera.pending)

        camera.onLocation(here)
        assertEquals(here, requireNotNull(camera.pending).target)
    }

    @Test
    fun `following does not fight a pinch`() {
        // Zoomed out deliberately while still following: later fixes must
        // recentre without also resetting the zoom the person chose.
        val camera = MapCameraState()
        camera.onLocation(here)
        camera.onPendingApplied()
        camera.onCameraSettled(MapPosition(here, zoom = 12.0))

        camera.onLocation(alsoHere)

        val pending = requireNotNull(camera.pending)
        assertEquals(alsoHere, pending.target)
        assertEquals("the fix overrode a zoom the person set", 12.0, pending.zoom, 0.001)
    }

    @Test
    fun `recentring from far out comes back in, but never zooms out`() {
        val camera = MapCameraState()
        camera.onCameraSettled(MapPosition(here, zoom = 4.0))
        camera.recentre(here)
        assertEquals(MapPosition.YOU_ARE_HERE_ZOOM, requireNotNull(camera.pending).zoom, 0.001)

        val close = MapCameraState()
        close.onCameraSettled(MapPosition(here, zoom = 19.0))
        close.recentre(here)
        assertEquals("recentring pulled the camera away from the street", 19.0, requireNotNull(close.pending).zoom, 0.001)
    }

    @Test
    fun `a camera settling where the engine put it never resumes following`() {
        val camera = MapCameraState()
        camera.onUserGesture()
        camera.onCameraSettled(MapPosition(here, zoom = 15.0))
        assertFalse("settling is a report, not a request", camera.following)
    }

    @Test
    fun `the map starts looking at the world rather than at somebody else's city`() {
        val camera = MapCameraState()
        assertTrue("a default city is a lie about where you are", camera.position.zoom < 3.0)
    }

    @Test
    fun `a position cannot be built outside the zoom the engine allows`() {
        listOf(0.0, 25.0, -1.0).forEach { zoom ->
            runCatching { MapPosition(here, zoom) }
                .onSuccess { error("zoom $zoom should not have been accepted") }
        }
    }
}
