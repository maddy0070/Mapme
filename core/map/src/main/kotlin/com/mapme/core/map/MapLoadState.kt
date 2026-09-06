package com.mapme.core.map

/**
 * What the map is doing, as far as anyone outside this module needs to know.
 *
 * Deliberately four states and no engine detail. A screen should be able to
 * decide what to draw without knowing that MapLibre distinguishes a style load
 * failure from a tile fetch failure — those differences matter here and
 * nowhere else.
 */
sealed interface MapLoadState {

    /** The surface exists; the first tiles have not arrived. */
    data object Loading : MapLoadState

    /** Tiles are drawing. */
    data object Ready : MapLoadState

    data class Failed(val reason: MapFailure) : MapLoadState
}

/**
 * Why a map did not appear.
 *
 * Split by *what the person can do about it*, not by what threw. Offline and
 * a dead tile host produce the same stack trace and want different sentences.
 */
enum class MapFailure {
    /** No usable network. Retrying now will not help; retrying later will. */
    Offline,

    /** Network exists, tiles did not arrive. Worth another go. */
    Tiles,

    /** The style itself would not load. A bug in MapMe, not in the person's day. */
    Style,
    ;

    /**
     * Whether offering a retry is honest.
     *
     * Nothing is more irritating than a retry button that cannot work, and
     * a style that failed to parse will fail identically the second time.
     */
    val retryable: Boolean get() = this != Style
}
