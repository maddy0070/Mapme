package com.mapme.core.design.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.DepthLevel
import com.mapme.core.design.theme.MapMeTheme

/**
 * How solid a pane of MapMe glass is.
 *
 * The choice is about legibility, not decoration: the more text a pane holds,
 * the more it has to earn its contrast back from the map underneath.
 */
enum class GlassTone {
    /** Small controls. You should still be able to read the map through them. */
    Whisper,

    /** The default pane: journey cards, the live panel, floating information. */
    Standard,

    /** Sheets and dialogs, where the interface has taken over the screen. */
    Dense,
}

/**
 * A pane of MapMe glass.
 *
 * The material is four things stacked in a believable order: the world behind
 * it, thrown out of focus; a veil that buys back contrast; a tint that catches
 * light from above; and a bright hairline along the top edge where a real pane
 * would catch the sky. The shadow underneath is the least of it.
 *
 * ## About the blur
 *
 * Android only gained real render-time blur in API 31. Below that, [blur] is a
 * no-op — so instead of shipping a see-through pane nobody can read, MapMe
 * thickens the veil and the glass simply becomes frostier. It still looks like
 * glass; it just stops being a window.
 *
 * What gets blurred is, in order: an explicit [backdrop], or whatever
 * [GlassBackdropHost] this pane sits inside, or nothing — in which case the
 * pane is an opaque frosted veil, which is the right answer over a flat
 * ground anyway.
 *
 * @param backdrop overrides the surrounding host for this pane only.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MapMeTheme.radius.card,
    tone: GlassTone = GlassTone.Standard,
    depth: DepthLevel = MapMeTheme.depth.floating,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    backdrop: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MapMeTheme.colors
    val blurScale = MapMeTheme.blur
    val host = LocalGlassBackdrop.current
    var paneOrigin by remember { mutableStateOf(Offset.Zero) }

    val blurRadius: Dp = when (tone) {
        GlassTone.Whisper -> blurScale.whisper
        GlassTone.Standard -> blurScale.glass
        GlassTone.Dense -> blurScale.deep
    }
    val tintScale = when (tone) {
        GlassTone.Whisper -> 0.8f
        GlassTone.Standard -> 1.0f
        GlassTone.Dense -> 1.35f
    }
    val baseVeil = when (tone) {
        GlassTone.Whisper -> 0.20f
        GlassTone.Standard -> 0.34f
        GlassTone.Dense -> 0.60f
    }
    // No real blur, or nothing behind us to refract: pay for legibility with
    // opacity instead.
    val hasBackdrop = backdrop != null || host != null
    val blurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasBackdrop
    val veil = (if (blurAvailable) baseVeil else baseVeil + 0.24f).coerceAtMost(0.94f)

    val tint = colors.glassTint
    val tintBrush = Brush.verticalGradient(
        listOf(
            tint.copy(alpha = (tint.alpha * tintScale * 1.7f).coerceAtMost(1f)),
            tint.copy(alpha = (tint.alpha * tintScale * 0.85f).coerceAtMost(1f)),
        ),
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = depth.elevation,
                shape = shape,
                clip = false,
                ambientColor = depth.ambient,
                spotColor = depth.spot,
            )
            .clip(shape)
            .onGloballyPositioned { paneOrigin = it.positionInWindow() },
    ) {
        if (hasBackdrop) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(blurRadius, BlurredEdgeTreatment.Unbounded),
            ) {
                when {
                    backdrop != null -> backdrop()
                    host != null -> HostBackdropSlice(host, paneOrigin)
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colors.surface.copy(alpha = veil))
                .background(tintBrush)
                .drawBehind { drawTopSheen(colors.glassSheen) },
        )

        Box(modifier = Modifier.padding(contentPadding), content = content)

        // Painted last so content can never sit on top of the pane's own edge.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 1.dp, color = colors.glassBorder, shape = shape),
        )
    }
}

/**
 * [GlassSurface] with MapMe's standard interior padding. This is the pane you
 * want nine times out of ten.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MapMeTheme.radius.card,
    tone: GlassTone = GlassTone.Standard,
    depth: DepthLevel = MapMeTheme.depth.floating,
    backdrop: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        tone = tone,
        depth = depth,
        contentPadding = PaddingValues(MapMeTheme.space.cardPadding),
        backdrop = backdrop,
        content = content,
    )
}

/**
 * The bright hairline along the top edge. It is the single cheapest thing that
 * makes a translucent rectangle read as a physical pane, and it fades out
 * towards the corners the way a real highlight would.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopSheen(sheen: Color) {
    val strokeHeight = 1.dp.toPx()
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.25f to sheen,
            0.75f to sheen,
            1f to Color.Transparent,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, strokeHeight),
    )
}
