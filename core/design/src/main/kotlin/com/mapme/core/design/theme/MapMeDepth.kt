package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One rung of the depth ladder.
 *
 * In a dark interface a drop shadow does almost nothing — black on near-black
 * is invisible. So MapMe carries depth with three cues at once: the surface
 * gets *lighter* as it rises, the top edge picks up more sheen, and the shadow
 * spreads. [elevation] is the least important of the three; it is there to
 * catch the light in light mode and to soften the boundary in dark mode.
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
 */
@Immutable
data class MapMeDepth(
    /** Flush with the canvas. Lists, backgrounds, the map itself. */
    val flat: DepthLevel = DepthLevel(0.dp, Color.Transparent, Color.Transparent),
    /** A card resting on the canvas. */
    val resting: DepthLevel = DepthLevel(
        elevation = 2.dp,
        ambient = Color.Black.copy(alpha = 0.34f),
        spot = Color.Black.copy(alpha = 0.44f),
    ),
    /** A card lifted for attention, or a pressed-then-released control. */
    val raised: DepthLevel = DepthLevel(
        elevation = 8.dp,
        ambient = Color.Black.copy(alpha = 0.40f),
        spot = Color.Black.copy(alpha = 0.52f),
    ),
    /** Glass floating over the map: controls, journey cards, the live panel. */
    val floating: DepthLevel = DepthLevel(
        elevation = 18.dp,
        ambient = Color.Black.copy(alpha = 0.46f),
        spot = Color.Black.copy(alpha = 0.60f),
    ),
    /** Sheets, dialogs, anything that takes over. */
    val overlay: DepthLevel = DepthLevel(
        elevation = 30.dp,
        ambient = Color.Black.copy(alpha = 0.54f),
        spot = Color.Black.copy(alpha = 0.70f),
    ),
)

/**
 * How far the world behind the glass is pushed out of focus.
 *
 * Blur is expensive and, past a point, cheap-looking. Three levels is enough:
 * [whisper] for small controls where you still want to read the map through
 * them, [glass] for the standard panel, [deep] only when the interface has
 * genuinely taken over the screen.
 *
 * Real backdrop blur needs API 31. Below that, [MapMeGlass] compensates with a
 * denser tint instead of pretending — the material stays believable, it just
 * stops being see-through.
 */
@Immutable
data class MapMeBlur(
    val whisper: Dp = 12.dp,
    val glass: Dp = 24.dp,
    val deep: Dp = 40.dp,
)

internal val LocalMapMeDepth = staticCompositionLocalOf { MapMeDepth() }
internal val LocalMapMeBlur = staticCompositionLocalOf { MapMeBlur() }
