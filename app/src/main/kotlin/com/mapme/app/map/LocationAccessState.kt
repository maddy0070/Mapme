package com.mapme.app.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapme.core.location.LocationAccess
import com.mapme.core.location.LocationProvider

/**
 * The permission conversation, in one place.
 *
 * ## The rule this exists to keep
 *
 * MapMe does not draw a fake permission dialog, and it does not fire the real
 * one at launch. The system dialog *is* the permission UI — it is the one
 * surface a person already knows how to read, and duplicating it is both
 * patronising and, since Android stopped showing it after two refusals,
 * dishonest. So: MapMe says why, in its own voice, and the button hands over
 * to Android.
 *
 * ## Why the state is re-read on resume
 *
 * Permission can change while the app is in the background — the person went
 * to Settings, or Android revoked it for disuse. A screen that reads the
 * permission once shows a stale prompt forever after, which is how apps end
 * up with a "grant access" button that does nothing because access is already
 * granted.
 */
@Composable
fun rememberLocationAccess(provider: LocationProvider): LocationAccessController {
    val context = LocalContext.current
    val activity = context.findActivity()
    var access by remember { mutableStateOf<LocationAccess>(LocationAccess.Unknown) }
    var asked by remember { mutableStateOf(false) }

    fun refresh() {
        val current = provider.access()
        access = when {
            // The provider cannot tell "never asked" from "refused for good";
            // only the Activity knows, and only after we have asked once.
            current is LocationAccess.Denied && activity != null ->
                LocationAccess.Denied(permanently = asked && !activity.canStillAsk())
            else -> current
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        asked = true
        // Trust the system's answer, not the map of what we requested: a
        // person can grant coarse while refusing fine, and both are wins.
        val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        access = when {
            fine || coarse -> LocationAccess.Granted(precise = fine)
            activity == null -> LocationAccess.Denied(permanently = false)
            else -> LocationAccess.Denied(permanently = !activity.canStillAsk())
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return remember(access) {
        LocationAccessController(
            access = access,
            request = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
            openAppSettings = { context.openAppSettings() },
            openLocationSettings = { context.openLocationSettings() },
        )
    }
}

/** What a screen can do about location, without knowing any Android. */
class LocationAccessController(
    val access: LocationAccess,
    val request: () -> Unit,
    val openAppSettings: () -> Unit,
    val openLocationSettings: () -> Unit,
)

private fun Activity.canStillAsk(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.openLocationSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
