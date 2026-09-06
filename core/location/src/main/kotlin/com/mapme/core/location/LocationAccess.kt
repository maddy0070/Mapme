package com.mapme.core.location

/**
 * What MapMe is allowed to know about where you are.
 *
 * Four states, because four is what actually changes the interface. Android
 * exposes rather more — two separate permissions, a "don't ask again" flag, a
 * per-app precision toggle, and a device that may have no location hardware at
 * all — and screens that reason about those directly end up with a different
 * bug in each one.
 */
sealed interface LocationAccess {

    /** Not yet checked. The state at first composition, and never shown to anyone. */
    data object Unknown : LocationAccess

    /**
     * @param precise false when the person granted approximate location only,
     *   which Android has offered since 12. It is a perfectly good answer —
     *   the map still knows the neighbourhood — and MapMe says so rather than
     *   asking again.
     */
    data class Granted(val precise: Boolean) : LocationAccess

    /**
     * @param permanently true once the system will no longer show its dialog.
     *   The only honest next step then is Settings, and offering the button
     *   that silently does nothing is worse than offering none.
     */
    data class Denied(val permanently: Boolean) : LocationAccess

    /** No location provider on the device, or it is switched off system-wide. */
    data object Unavailable : LocationAccess

    val canLocate: Boolean get() = this is Granted
}

/**
 * What, if anything, MapMe should say about location.
 *
 * The product rule is that the system dialog is the permission UI — MapMe does
 * not draw a fake one — but it is never the *first* thing a person sees. So
 * there is exactly one MapMe-drawn surface, it appears before the dialog to
 * give it a reason, and it changes its offer once the dialog can no longer
 * help.
 */
enum class LocationPrompt {
    /** Say nothing. Either it is granted, or we have not looked yet. */
    None,

    /** Explain, then hand over to the system dialog. */
    Explain,

    /** The dialog is spent. Point at Settings, honestly. */
    Settings,

    /** Nothing to grant. Explain that the map still works. */
    NoProvider,
    ;

    companion object {
        fun of(access: LocationAccess): LocationPrompt = when (access) {
            is LocationAccess.Unknown -> None
            is LocationAccess.Granted -> None
            is LocationAccess.Unavailable -> NoProvider
            is LocationAccess.Denied -> if (access.permanently) Settings else Explain
        }
    }
}
