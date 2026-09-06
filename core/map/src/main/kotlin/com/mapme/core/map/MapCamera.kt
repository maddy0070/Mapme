package com.mapme.core.map

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mapme.core.model.GeoPoint

/** Where the camera is. Engine-independent on purpose. */
data class MapPosition(
    val target: GeoPoint,
    val zoom: Double,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0,
) {
    init {
        require(zoom in MIN_ZOOM..MAX_ZOOM) { "Zoom $zoom is outside $MIN_ZOOM..$MAX_ZOOM" }
    }

    companion object {
        const val MIN_ZOOM = 1.0
        const val MAX_ZOOM = 20.0

        /** Close enough to see streets, far enough to see where they go. */
        const val YOU_ARE_HERE_ZOOM = 16.5

        /**
         * Where the camera sits before it knows anything.
         *
         * Not a city. A world view, so that the first move to a real location
         * reads as *arriving somewhere* rather than as a correction from a
         * place the person has never been.
         */
        val Unlocated = MapPosition(GeoPoint(20.0, 0.0), zoom = 1.4)
    }
}

/**
 * How the camera decides where to look.
 *
 * The rule the product cares about is one line long: **the camera follows you
 * until you touch it, and after that it does not until you ask.** A map that
 * keeps yanking itself back while you are trying to look at the next street is
 * the single most irritating thing a map does, and it is always the result of
 * treating "centred on the user" as a state to be restored rather than as a
 * mode the person is in.
 *
 * So [following] is turned off by a gesture and back on only by
 * [recentre] — never by a new location arriving, never by a style reload,
 * never by the screen being recreated.
 */
@Stable
class MapCameraState(initial: MapPosition = MapPosition.Unlocated) {

    var position: MapPosition by mutableStateOf(initial)
        private set

    var following: Boolean by mutableStateOf(true)
        private set

    /**
     * Set when the camera should move itself, and cleared once the engine has
     * taken it. A request rather than a direct call so that a recentre asked
     * for before the map exists is honoured when it appears, instead of
     * being dropped.
     */
    var pending: MapPosition? by mutableStateOf(null)
        private set

    /** The engine reporting where it ended up. Never re-enables following. */
    fun onCameraSettled(position: MapPosition) {
        this.position = position
    }

    /**
     * The person moved the map. Everything after this is theirs until they
     * ask otherwise.
     */
    fun onUserGesture() {
        following = false
        pending = null
    }

    /** A new fix arrived. Only moves the camera if the person is still following. */
    fun onLocation(point: GeoPoint) {
        if (!following) return
        pending = position.copy(
            target = point,
            // The first fix also chooses a sensible zoom; later ones must not,
            // or the map would fight anyone who pinched while following.
            zoom = if (position == MapPosition.Unlocated) MapPosition.YOU_ARE_HERE_ZOOM else position.zoom,
        )
    }

    /** The explicit ask. The only thing that turns following back on. */
    fun recentre(on: GeoPoint?) {
        following = true
        if (on == null) return
        pending = position.copy(
            target = on,
            zoom = maxOf(position.zoom, MapPosition.YOU_ARE_HERE_ZOOM),
        )
    }

    fun onPendingApplied() {
        pending = null
    }
}
