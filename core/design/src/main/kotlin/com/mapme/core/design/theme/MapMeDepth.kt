package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One rung of the depth ladder.
 *
 * MapMe tints its shadows. A neutral black shadow on warm paper reads as dirt;
 * a plum-tinted one reads as light falling through something warm. The tint
 * comes from [MapMeColors.shadowTint], so depth is a property of the mode, not
 * a constant.
 */
@Immutable
data class DepthLevel(
    val elevation: Dp,
    val ambient: Color,
    val spot: Color,
)

/**
 * Four rungs, and no more. If something needs to sit between two of them, the
 * hierarchy is wrong, not the scale.
 *
 * The two modes carry depth differently, which is why this is built per mode
 * rather than shared:
 *
 * - **Night** barely uses shadow at all — black on near-black is invisible.
 *   Depth comes from the surface getting lighter as it rises and the top edge
 *   picking up more sheen.
 * - **Paper** leans on shadow entirely, because every surface is already white
 *   and lightness has nothing left to say. So the elevations are larger and
 *   the tint does real work.
 */
@Immutable
data class MapMeDepth(
    val flat: DepthLevel,
    val resting: DepthLevel,
    val raised: DepthLevel,
    val floating: DepthLevel,
    val overlay: DepthLevel,
)

fun mapMeDepth(colors: MapMeColors): MapMeDepth {
    val tint = colors.shadowTint
    fun level(elevation: Dp, ambient: Float, spot: Float) = DepthLevel(
        elevation = elevation,
        ambient = tint.copy(alpha = ambient),
        spot = tint.copy(alpha = spot),
    )

    return if (colors.isDark) {
        MapMeDepth(
            flat = DepthLevel(0.dp, Color.Transparent, Color.Transparent),
            resting = level(2.dp, 0.36f, 0.46f),
            raised = level(8.dp, 0.42f, 0.54f),
            floating = level(16.dp, 0.48f, 0.62f),
            overlay = level(28.dp, 0.56f, 0.72f),
        )
    } else {
        MapMeDepth(
            flat = DepthLevel(0.dp, Color.Transparent, Color.Transparent),
            resting = level(3.dp, 0.10f, 0.13f),
            raised = level(10.dp, 0.12f, 0.16f),
            floating = level(20.dp, 0.14f, 0.19f),
            overlay = level(32.dp, 0.17f, 0.23f),
        )
    }
}

/**
 * How far the world behind the glass is pushed out of focus.
 *
 * Three levels is enough. [whisper] for small controls where the ground should
 * still read through them, [glass] for the standard panel, [deep] only when
 * the interface has genuinely taken over the screen.
 *
 * Real backdrop blur needs API 31; below that [com.mapme.core.design.component.GlassSurface]
 * thickens its veil instead of pretending.
 */
@Immutable
data class MapMeBlur(
    val whisper: Dp = 14.dp,
    val glass: Dp = 28.dp,
    val deep: Dp = 44.dp,
)

internal val LocalMapMeDepth = staticCompositionLocalOf { mapMeDepth(mapMeDarkColors()) }
internal val LocalMapMeBlur = staticCompositionLocalOf { MapMeBlur() }
