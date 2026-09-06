package com.mapme.core.map

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
 */
@Composable
fun MapMeMap(
    camera: MapCameraState,
    modifier: Modifier = Modifier,
    user: GeoPoint? = null,
    accuracyMetres: Float? = null,
    userLabel: String = "Your location",
    onLoadStateChange: (MapLoadState) -> Unit = {},
) {
    val colors = MapMeTheme.colors
    val motion = MapMeTheme.motion
    val styleJson = remember(colors) { MapMeStyle.json(colors) }
    val loadState by rememberUpdatedState(onLoadStateChange)

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
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
    LaunchedEffect(map, styleJson) {
        val ready = map ?: return@LaunchedEffect
        loadState(MapLoadState.Loading)
        ready.setStyle(Style.Builder().fromJson(styleJson)) {
            loadState(MapLoadState.Ready)
        }
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

    // Keep the mark under the finger during a pan. Recomputed from the
    // engine's own projection rather than guessed, so it stays glued to the
    // ground through a fling.
    DisposableEffect(map, user) {
        val ready = map
        val point = user
        if (ready == null || point == null) return@DisposableEffect onDispose { }

        fun sync() {
            val screen = ready.projection.toScreenLocation(LatLng(point.latitude, point.longitude))
            userScreen = Offset(screen.x, screen.y)
            val metresPerPixel = ready.projection.getMetersPerPixelAtLatitude(point.latitude)
            haloRadiusPx = if (metresPerPixel > 0.0) {
                ((accuracyMetres ?: 0f) / metresPerPixel).toFloat()
            } else {
                0f
            }
        }
        sync()

        val onMove = MapLibreMap.OnCameraMoveListener { sync() }
        val onIdle = MapLibreMap.OnCameraIdleListener { sync() }
        ready.addOnCameraMoveListener(onMove)
        ready.addOnCameraIdleListener(onIdle)
        onDispose {
            ready.removeOnCameraMoveListener(onMove)
            ready.removeOnCameraIdleListener(onIdle)
        }
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
