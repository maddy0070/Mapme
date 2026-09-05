package com.mapme.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 4dp grid, with names for the handful of distances that carry meaning.
 *
 * Use the semantic values ([screenEdge], [cardPadding], [sectionGap]…) in
 * screens; reach for the raw steps only when composing something new inside
 * the design system itself. That is what keeps two screens built months apart
 * from feeling like two different apps.
 */
@Immutable
data class MapMeSpacing(
    val x1: Dp = 4.dp,
    val x2: Dp = 8.dp,
    val x3: Dp = 12.dp,
    val x4: Dp = 16.dp,
    val x5: Dp = 20.dp,
    val x6: Dp = 24.dp,
    val x8: Dp = 32.dp,
    val x10: Dp = 40.dp,
    val x12: Dp = 48.dp,
    val x16: Dp = 64.dp,

    /** Left and right gutter on every screen. */
    val screenEdge: Dp = 20.dp,
    /** Inside a glass card or panel. */
    val cardPadding: Dp = 20.dp,
    /** Between two unrelated blocks of content. */
    val sectionGap: Dp = 32.dp,
    /** Between siblings in a list. */
    val itemGap: Dp = 12.dp,
    /** Between a label and the thing it labels. */
    val labelGap: Dp = 6.dp,
    /** Smallest comfortable tap target. Never go below this. */
    val minTouchTarget: Dp = 48.dp,
    /** Gap between floating map controls and the screen edge. */
    val controlInset: Dp = 16.dp,
)

/**
 * Corner radii.
 *
 * MapMe is generously rounded — the interface is glass panes floating over a
 * map, and glass panes have polished edges. The rule is: the larger the
 * surface, the larger the radius, so nothing ever looks like a scaled-up
 * version of something smaller.
 */
@Immutable
data class MapMeRadius(
    val xs: Dp = 6.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 22.dp,
    val xl: Dp = 28.dp,
    val xxl: Dp = 36.dp,
) {
    /** Chips, badges, the recording indicator. */
    val chip: Shape get() = RoundedCornerShape(percent = 50)
    /** All buttons. MapMe buttons are pills; there are no square buttons. */
    val button: Shape get() = RoundedCornerShape(percent = 50)
    /** Small floating controls: zoom, locate, layer toggles. */
    val control: Shape get() = RoundedCornerShape(md)
    /** The standard glass card. */
    val card: Shape get() = RoundedCornerShape(lg)
    /** A large, important panel. */
    val panel: Shape get() = RoundedCornerShape(xl)
    /** Bottom sheets: rounded at the top only, square where they meet the edge. */
    val sheet: Shape get() = RoundedCornerShape(topStart = xxl, topEnd = xxl)
    /** Images and map thumbnails. */
    val thumbnail: Shape get() = RoundedCornerShape(md)
}

internal val LocalMapMeSpacing = staticCompositionLocalOf { MapMeSpacing() }
internal val LocalMapMeRadius = staticCompositionLocalOf { MapMeRadius() }
