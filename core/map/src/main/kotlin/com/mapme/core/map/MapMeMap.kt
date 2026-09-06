package com.mapme.core.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.model.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * The map surface.
 *
 * **This file is the only place in MapMe that knows MapLibre exists.** Its
 * parameters are MapMe and domain types; its imports are the engine's. That
 * boundary is the whole point of the module — swapping engines means
 * rewriting this file and [MapMeStyle], and touching nothing else. There is a
 * test that fails the build if `org.maplibre` is imported anywhere outside
 * `:core:map`.
 *
 * @param user where to draw the "this is me" mark, or null for not-yet-known.
 * @param accuracyMetres the reported accuracy of [user], drawn as the halo.
 * @param trail the journey so far, one list per segment. Segments are drawn
 *   separately so a pause reads as a gap rather than a line nobody walked.
 * @param reloadKey bump to load the style again. This is what makes a retry
 *   button a real retry rather than a state change that looks like one.
 */
@Composable
fun MapMeMap(
    camera: MapCameraState,
    modifier: Modifier = Modifier,
    user: GeoPoint? = null,
    accuracyMetres: Float? = null,
    userLabel: String = "Your location",
    trail: List<List<GeoPoint>> = emptyList(),
    reloadKey: Int = 0,
    onLoadStateChange: (MapLoadState) -> Unit = {},
) {
    val context = LocalContext.current
    val colors = MapMeTheme.colors
    val motion = MapMeTheme.motion
    val styleJson = remember(colors) { MapMeStyle.json(colors) }
    val loadState by rememberUpdatedState(onLoadStateChange)

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val journeyLayer = remember { MapTrail() }
    var userScreen by remember { mutableStateOf(Offset.Unspecified) }
    var haloRadiusPx by remember { mutableStateOf(0f) }

    val mapView = rememberMapView()

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { },
        )

        if (user != null && userScreen != Offset.Unspecified) {
            YouAreHere(
                centre = userScreen,
                accuracyRadiusPx = haloRadiusPx,
                label = userLabel,
            )
        }
    }

    // Attach once. Everything after this is driven by state, not by rebuilding
    // the view — recreating a MapView is a visible flash and a real allocation.
    // Failure has to come from the engine's own signals. Without these the
    // Failed state is unreachable and the error card is decoration: setStyle's
    // callback only ever fires on success, so a dead tile host would leave the
    // map sitting on Loading forever.
    DisposableEffect(mapView, context) {
        val onFail = MapView.OnDidFailLoadingMapListener { message ->
            loadState(MapLoadState.Failed(classifyFailure(context, message)))
        }
        val onStyle = MapView.OnDidFinishLoadingStyleListener {
            loadState(MapLoadState.Ready)
        }
        mapView.addOnDidFailLoadingMapListener(onFail)
        mapView.addOnDidFinishLoadingStyleListener(onStyle)
        onDispose {
            mapView.removeOnDidFailLoadingMapListener(onFail)
            mapView.removeOnDidFinishLoadingStyleListener(onStyle)
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { ready ->
            ready.uiSettings.apply {
                isCompassEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                // Attribution stays. OpenStreetMap's licence requires it and
                // MapMe is not going to be the app that quietly drops it.
                isAttributionEnabled = true
                isLogoEnabled = false
            }
            ready.setMinZoomPreference(MapPosition.MIN_ZOOM)
            ready.setMaxZoomPreference(MapPosition.MAX_ZOOM)

            ready.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    camera.onUserGesture()
                }
            }
            ready.addOnCameraIdleListener { camera.onCameraSettled(ready.readPosition()) }
            map = ready
        }
        onDispose { map = null }
    }

    // The style is a function of the theme, so a theme change reloads it. That
    // is also why the reveal works over the map: the snapshot the transition
    // erases contains the *old* style's pixels, and the new style loads
    // underneath while the boundary travels.
    LaunchedEffect(map, styleJson, reloadKey) {
        val ready = map ?: return@LaunchedEffect
        loadState(MapLoadState.Loading)
        style = null
        ready.setStyle(Style.Builder().fromJson(styleJson)) { loaded ->
            // The trail is reinstalled here rather than once at startup,
            // because loading a style throws away every source and layer that
            // was added to the previous one. Without this the journey
            // disappears the moment someone switches theme mid-walk.
            journeyLayer.install(loaded, colors)
            style = loaded
            loadState(MapLoadState.Ready)
        }
    }

    LaunchedEffect(style, trail) {
        val loaded = style ?: return@LaunchedEffect
        journeyLayer.update(loaded, trail)
    }

    // A camera move the app asked for.
    LaunchedEffect(map, camera.pending) {
        val ready = map ?: return@LaunchedEffect
        val target = camera.pending ?: return@LaunchedEffect
        ready.animateCamera(
            CameraUpdateFactory.newCameraPosition(target.toEngine()),
            motion.travel,
        )
        camera.onPendingApplied()
    }

    // Keep the mark under the finger during a pan. Recomputed from the engine's
    // own projection rather than guessed, so it stays glued to the ground
    // through a fling.
    //
    // The fix and its accuracy are read through rememberUpdatedState rather
    // than being effect keys. As keys they looked harmless and meant both
    // camera listeners were removed and re-added on *every* location update —
    // twice a second, on the screen whose whole job is to not stutter.
    val latestUser by rememberUpdatedState(user)
    val latestAccuracy by rememberUpdatedState(accuracyMetres)

    DisposableEffect(map) {
        val ready = map ?: return@DisposableEffect onDispose { }

        fun sync() {
            val point = latestUser
            if (point == null) {
                userScreen = Offset.Unspecified
                return
            }
            val screen = ready.projection.toScreenLocation(LatLng(point.latitude, point.longitude))
            userScreen = Offset(screen.x, screen.y)
            val metresPerPixel = ready.projection.getMetersPerPixelAtLatitude(point.latitude)
            haloRadiusPx = if (metresPerPixel > 0.0) {
                ((latestAccuracy ?: 0f) / metresPerPixel).toFloat()
            } else {
                0f
            }
        }

        val onMove = MapLibreMap.OnCameraMoveListener { sync() }
        val onIdle = MapLibreMap.OnCameraIdleListener { sync() }
        ready.addOnCameraMoveListener(onMove)
        ready.addOnCameraIdleListener(onIdle)
        sync()
        onDispose {
            ready.removeOnCameraMoveListener(onMove)
            ready.removeOnCameraIdleListener(onIdle)
        }
    }

    // A new fix with a still camera produces no camera event, so nothing above
    // would move the mark. This is the only thing that redraws it while you
    // stand still.
    LaunchedEffect(map, user, accuracyMetres) {
        val ready = map ?: return@LaunchedEffect
        val point = user ?: run { userScreen = Offset.Unspecified; return@LaunchedEffect }
        val screen = ready.projection.toScreenLocation(LatLng(point.latitude, point.longitude))
        userScreen = Offset(screen.x, screen.y)
        val metresPerPixel = ready.projection.getMetersPerPixelAtLatitude(point.latitude)
        haloRadiusPx = if (metresPerPixel > 0.0) ((accuracyMetres ?: 0f) / metresPerPixel).toFloat() else 0f
    }
}

/**
 * A MapView that follows the composition's lifecycle.
 *
 * `onCreate` is called eagerly rather than from an ON_CREATE event, because by
 * the time a composable runs the lifecycle is usually already RESUMED and that
 * event will never arrive — a mistake that produces a map which works until
 * the first time the screen is backgrounded.
 */
@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            id = View.generateViewId()
            onCreate(null)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}

private fun MapPosition.toEngine(): CameraPosition = CameraPosition.Builder()
    .target(LatLng(target.latitude, target.longitude))
    .zoom(zoom)
    .bearing(bearing)
    .tilt(tilt)
    .build()

private fun MapLibreMap.readPosition(): MapPosition = cameraPosition.let { c ->
    val target = c.target
    MapPosition(
        target = GeoPoint(
            latitude = target?.latitude ?: 0.0,
            longitude = target?.longitude ?: 0.0,
        ),
        zoom = c.zoom.coerceIn(MapPosition.MIN_ZOOM, MapPosition.MAX_ZOOM),
        bearing = c.bearing,
        tilt = c.tilt,
    )
}

/**
 * Turning an engine error string into something a person can act on.
 *
 * The engine reports one kind of problem — "loading failed" — for causes that
 * want completely different sentences. Being offline is not the person's
 * mistake and will fix itself; a style that will not parse is our bug and no
 * amount of retrying will help. Connectivity is checked first because it is
 * the only one we can establish rather than infer.
 *
 * The message is matched loosely on purpose: it is an engine-internal string,
 * not an API, so anything unrecognised falls back to the tile case, which is
 * the one where a retry is both harmless and most often right.
 */
private fun classifyFailure(context: Context, message: String?): MapFailure {
    if (!context.hasNetwork()) return MapFailure.Offline
    val text = message.orEmpty().lowercase()
    return if ("style" in text) MapFailure.Style else MapFailure.Tiles
}

private fun Context.hasNetwork(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
