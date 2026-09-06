package com.mapme.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.mapme.core.model.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Where you are, right now.
 *
 * @param accuracyMetres the radius the system believes it is within. Drawn as
 *   the halo, so that a poor fix *looks* like a poor fix rather than lying
 *   with a confident dot.
 */
data class UserLocation(
    val point: GeoPoint,
    val accuracyMetres: Float,
)

/**
 * The thing that knows where you are.
 *
 * An interface because the map screen must be testable without a GPS radio,
 * and because the recording sprint will want a different sampling strategy
 * behind the same idea.
 */
interface LocationProvider {
    /** Emits while collected. Stops when the collector goes away. */
    fun stream(): Flow<UserLocation>

    fun access(): LocationAccess
}

/** A provider that knows nothing. Used by previews and by tests. */
class NoLocationProvider(private val access: LocationAccess = LocationAccess.Unavailable) : LocationProvider {
    override fun stream(): Flow<UserLocation> = flowOf()
    override fun access(): LocationAccess = access
}

/**
 * The platform one.
 *
 * **Deliberately not Google Play Services.** The fused provider is better at
 * fusing — it really is — but it drags in a proprietary dependency, does not
 * exist on de-Googled devices, and MapMe's constitution says the product does
 * not require anybody's services to show you your own life. `LocationManager`
 * has been on every Android device since the first one, and for "put a dot
 * where I am" the difference is not visible.
 *
 * Nothing here logs a coordinate. Not at debug level, not in an error path.
 * A location in logcat is a location on a bug report.
 */
class SystemLocationProvider(context: Context) : LocationProvider {

    private val appContext = context.applicationContext
    private val manager: LocationManager? =
        ContextCompat.getSystemService(appContext, LocationManager::class.java)

    override fun access(): LocationAccess {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = manager ?: return LocationAccess.Unavailable

        if (!fine && !coarse) {
            // Undecided and permanently-refused look identical from here; only
            // the Activity knows which, so it corrects this after asking.
            return LocationAccess.Denied(permanently = false)
        }
        if (!LocationManagerCompat.isLocationEnabled(manager)) return LocationAccess.Unavailable
        return LocationAccess.Granted(precise = fine)
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Guarded by access() on the line above the callback.
    override fun stream(): Flow<UserLocation> = callbackFlow {
        val manager = manager
        if (manager == null || !access().canLocate) {
            close()
            return@callbackFlow
        }

        val listener = android.location.LocationListener { location -> trySend(location.asUser()) }

        val providers = buildList {
            if (granted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                manager.allProviders.contains(LocationManager.GPS_PROVIDER)
            ) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (manager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        // The last known fix, immediately, so the map has somewhere to go
        // before the first live reading lands — which on a cold GPS can be
        // twenty seconds of an empty map otherwise.
        providers.firstNotNullOfOrNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            ?.let { trySend(it.asUser()) }

        providers.forEach { provider ->
            runCatching {
                manager.requestLocationUpdates(provider, UPDATE_MILLIS, UPDATE_METRES, listener)
            }
        }

        if (providers.isEmpty()) close()

        awaitClose { runCatching { manager.removeUpdates(listener) } }
    }

    private fun Location.asUser() = UserLocation(
        point = GeoPoint(latitude, longitude),
        accuracyMetres = if (hasAccuracy()) accuracy else UNKNOWN_ACCURACY_METRES,
    )

    private companion object {
        /**
         * Two seconds and five metres.
         *
         * This sprint only draws a dot, so it is tuned for the dot looking
         * alive rather than for a route being accurate. Recording will want
         * its own numbers and its own strategy; that is why this is behind an
         * interface.
         */
        const val UPDATE_MILLIS = 2_000L
        const val UPDATE_METRES = 5f

        /** Drawn as a generous halo. Better to look unsure than to look wrong. */
        const val UNKNOWN_ACCURACY_METRES = 60f
    }
}
