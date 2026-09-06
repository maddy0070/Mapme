package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 4dp grid, with names for the distances that carry meaning.
 *
 * Use the semantic values in screens; reach for the raw steps only when
 * composing something new inside the design system itself. That is what keeps
 * two screens built months apart from feeling like two different apps.
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
    val x20: Dp = 80.dp,

    /** Left and right gutter on every screen. */
    val screenEdge: Dp = 24.dp,
    /** Inside a card or panel. */
    val cardPadding: Dp = 24.dp,
    /** Between two unrelated blocks. */
    val sectionGap: Dp = 36.dp,
    /** Between siblings in a list. */
    val itemGap: Dp = 12.dp,
    /** Between a label and the thing it labels. */
    val labelGap: Dp = 8.dp,
    /** Smallest comfortable tap target. Never go below this. */
    val minTouchTarget: Dp = 48.dp,
    /** Gap between a floating control and the screen edge. */
    val controlInset: Dp = 16.dp,
)

internal val LocalMapMeSpacing = staticCompositionLocalOf { MapMeSpacing() }
internal val LocalMapMeRadius = staticCompositionLocalOf { MapMeRadius() }
