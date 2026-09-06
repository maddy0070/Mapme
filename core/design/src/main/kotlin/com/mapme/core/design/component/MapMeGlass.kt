package com.mapme.core.design.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.matchParentSize
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
 * The choice is about legibility, not taste: the more text a pane holds, the
 * more contrast it has to buy back from whatever is underneath it.
 */
enum class GlassTone {
    /** Small controls. The ground should still read through them. */
    Whisper,

    /** The default pane: floating information over the journey. */
    Standard,

    /** Sheets and dialogs, where the interface has taken the screen. */
    Dense,
}

/**
 * A pane of MapMe glass.
 *
 * Four things stacked in a believable order: the world behind it thrown out of
 * focus, a veil that buys back contrast, a tint that catches light from above,
 * and a bright hairline along the top edge where a real pane would catch the
 * sky.
 *
 * **Glass is rationed.** It is not a texture to spread over the interface —
 * it exists to say *this is floating above your journey*. A screen where
 * everything is glass has said nothing. In practice that means one pane per
 * screen, plus the occasional small control.
 *
 * **The two modes are different materials.** On night, glass is a pale veil
 * catching light. On paper it is genuine frost: much whiter, because a barely
 * tinted pane over a bright ground is just a smudge. Both blur the same.
 *
 * Real backdrop blur needs API 31. Below that the veil thickens and the pane
 * becomes frostier rather than pretending — still glass, just no longer a
 * window.
 *
 * @param backdrop overrides the surrounding [GlassBackdropHost] for this pane.
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

    // Paper needs far more veil than night: white frost over a bright ground
    // has to work much harder to become a surface you can read on.
    val baseVeil = if (colors.isDark) {
        when (tone) {
            GlassTone.Whisper -> 0.20f
            GlassTone.Standard -> 0.34f
            GlassTone.Dense -> 0.58f
        }
    } else {
        when (tone) {
            GlassTone.Whisper -> 0.44f
            GlassTone.Standard -> 0.62f
            GlassTone.Dense -> 0.84f
        }
    }
    val veilColor = if (colors.isDark) colors.surface else Color.White

    val hasBackdrop = backdrop != null || host != null
    val blurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasBackdrop
    val veil = (if (blurAvailable) baseVeil else baseVeil + 0.22f).coerceAtMost(0.96f)

    val tint = colors.glassTint
    val tintBrush = Brush.verticalGradient(
        listOf(
            tint.copy(alpha = (tint.alpha * 1.35f).coerceAtMost(1f)),
            tint.copy(alpha = (tint.alpha * 0.7f).coerceAtMost(1f)),
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
                .background(veilColor.copy(alpha = veil))
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

/** [GlassSurface] with MapMe's standard interior padding. */
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
 * The bright hairline along the top edge — the cheapest thing that makes a
 * translucent rectangle read as a physical pane. It fades towards the corners
 * the way a real highlight would.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopSheen(sheen: Color) {
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.22f to sheen,
            0.78f to sheen,
            1f to Color.Transparent,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, 1.dp.toPx()),
    )
}
