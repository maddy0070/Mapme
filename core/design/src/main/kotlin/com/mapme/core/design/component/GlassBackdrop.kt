package com.mapme.core.design.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Lets glass panes see what is actually behind them.
 *
 * A pane of [GlassSurface] inside a host paints the host's backdrop again,
 * translated so the copy lines up exactly with the real thing, and blurs that.
 * The result is genuine refraction rather than a frosted rectangle: move the
 * pane and the world behind it moves correctly.
 *
 * ## The cost, stated plainly
 *
 * The backdrop is *re-composed once per pane*. That is the right trade for a
 * procedural ground — MapMe's aurora is two gradients and a fill — and the
 * wrong one for a live map with tiles and labels. When the map arrives, the
 * host should capture itself into a `GraphicsLayer` once per frame and panes
 * should draw that layer instead; the API here does not change, only what sits
 * behind [GlassBackdrop.content].
 *
 * Do not put glass inside a backdrop. It would compose itself forever.
 */
@Stable
class GlassBackdrop internal constructor(
    internal val content: @Composable () -> Unit,
) {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var size by mutableStateOf(IntSize.Zero)
}

internal val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }

/**
 * Paints [backdrop], then lays [content] over it. Any glass in [content]
 * refracts the backdrop automatically.
 */
@Composable
fun GlassBackdropHost(
    backdrop: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val latest by rememberUpdatedState(backdrop)
    val host = remember { GlassBackdrop { latest() } }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            host.origin = coordinates.positionInWindow()
            host.size = coordinates.size
        },
    ) {
        backdrop()
        CompositionLocalProvider(LocalGlassBackdrop provides host) {
            content()
        }
    }
}

/**
 * The slice of the host backdrop that sits directly behind a pane whose
 * top-left corner is at [paneOrigin] in window coordinates.
 */
@Composable
internal fun HostBackdropSlice(host: GlassBackdrop, paneOrigin: Offset) {
    val density = LocalDensity.current
    val size = host.size
    if (size.width == 0 || size.height == 0) return

    val dx = (paneOrigin.x - host.origin.x).roundToInt()
    val dy = (paneOrigin.y - host.origin.y).roundToInt()

    Box(
        modifier = Modifier
            .requiredSize(
                width = with(density) { size.width.toDp() },
                height = with(density) { size.height.toDp() },
            )
            .offset { IntOffset(-dx, -dy) },
    ) {
        host.content()
    }
}
