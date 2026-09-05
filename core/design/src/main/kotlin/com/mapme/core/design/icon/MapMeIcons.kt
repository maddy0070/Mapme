package com.mapme.core.design.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.LocalMapMeContentColor
import com.mapme.core.design.theme.MapMeTheme

/**
 * MapMe draws its own icons.
 *
 * Not because the standard set is bad, but because an icon set is a
 * handwriting: a 24dp grid, a 2dp stroke, round caps, round joins, and no
 * detail smaller than the stroke itself. Mixing two handwritings is the single
 * fastest way to make a product look assembled. This set stays small on
 * purpose — an icon is added when a screen genuinely needs one, and it is
 * drawn to these rules.
 */
object MapMeIcons {

    /** Where you are, or where something happened. */
    val Pin: ImageVector by lazy {
        icon("Pin") {
            stroked {
                moveTo(12f, 21.5f)
                curveTo(12f, 21.5f, 4.6f, 14.7f, 4.6f, 9.9f)
                curveTo(4.6f, 5.8f, 7.9f, 2.5f, 12f, 2.5f)
                curveTo(16.1f, 2.5f, 19.4f, 5.8f, 19.4f, 9.9f)
                curveTo(19.4f, 14.7f, 12f, 21.5f, 12f, 21.5f)
                close()
            }
            stroked {
                moveTo(9.4f, 9.7f)
                arcTo(2.6f, 2.6f, 0f, true, true, 14.6f, 9.7f)
                arcTo(2.6f, 2.6f, 0f, true, true, 9.4f, 9.7f)
                close()
            }
        }
    }

    /** A journey. The route with its start behind it and its head ahead. */
    val Trail: ImageVector by lazy {
        icon("Trail") {
            stroked {
                moveTo(5f, 18.5f)
                curveTo(9.2f, 18.5f, 8f, 12f, 12f, 12f)
                curveTo(16f, 12f, 14.8f, 5.5f, 19f, 5.5f)
            }
            filled {
                circle(5f, 18.5f, 2.1f)
            }
            filled {
                circle(19f, 5.5f, 2.1f)
            }
        }
    }

    /** A day, a week, a month. Time as a thing you can pick. */
    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            stroked {
                moveTo(6.5f, 5.5f)
                lineTo(17.5f, 5.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 20f, 8f)
                lineTo(20f, 18f)
                arcTo(2.5f, 2.5f, 0f, false, true, 17.5f, 20.5f)
                lineTo(6.5f, 20.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 4f, 18f)
                lineTo(4f, 8f)
                arcTo(2.5f, 2.5f, 0f, false, true, 6.5f, 5.5f)
                close()
            }
            stroked {
                moveTo(4f, 10.2f)
                lineTo(20f, 10.2f)
            }
            stroked {
                moveTo(8.5f, 3.2f)
                lineTo(8.5f, 7f)
            }
            stroked {
                moveTo(15.5f, 3.2f)
                lineTo(15.5f, 7f)
            }
        }
    }

    /** Replay a day. */
    val Play: ImageVector by lazy {
        icon("Play") {
            stroked {
                moveTo(9f, 6.2f)
                lineTo(18.2f, 12f)
                lineTo(9f, 17.8f)
                close()
            }
        }
    }

    /** Basemap and overlay choices. */
    val Layers: ImageVector by lazy {
        icon("Layers") {
            stroked {
                moveTo(12f, 3f)
                lineTo(21f, 8f)
                lineTo(12f, 13f)
                lineTo(3f, 8f)
                close()
            }
            stroked {
                moveTo(3.4f, 12.4f)
                lineTo(12f, 17.2f)
                lineTo(20.6f, 12.4f)
            }
            stroked {
                moveTo(3.4f, 16.4f)
                lineTo(12f, 21.2f)
                lineTo(20.6f, 16.4f)
            }
        }
    }

    /** Somewhere new. Only ever used for genuine discovery. */
    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            filled {
                moveTo(12f, 2.6f)
                curveTo(12.7f, 8.1f, 15.9f, 11.3f, 21.4f, 12f)
                curveTo(15.9f, 12.7f, 12.7f, 15.9f, 12f, 21.4f)
                curveTo(11.3f, 15.9f, 8.1f, 12.7f, 2.6f, 12f)
                curveTo(8.1f, 11.3f, 11.3f, 8.1f, 12f, 2.6f)
                close()
            }
        }
    }

    /** Go deeper. Always points the way travel happens, never back. */
    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            stroked {
                moveTo(9.5f, 5f)
                lineTo(16.3f, 12f)
                lineTo(9.5f, 19f)
            }
        }
    }
}

/**
 * Draws a MapMe icon.
 *
 * Pass a [contentDescription] when the icon is the only thing carrying the
 * meaning, and leave it null when there is a label next to it — a screen
 * reader announcing "chevron, open day" twice is worse than not announcing the
 * chevron at all.
 */
@Composable
fun MapMeIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified,
) {
    val resolved = tint
        .takeOrElse { LocalMapMeContentColor.current }
        .takeOrElse { MapMeTheme.colors.textPrimary }

    Image(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(resolved),
    )
}

// --- Drawing helpers -------------------------------------------------------

private const val GRID = 24f
private const val STROKE = 2f

private class IconScope(val builder: ImageVector.Builder) {
    fun stroked(block: PathBuilder.() -> Unit) {
        builder.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    fun filled(block: PathBuilder.() -> Unit) {
        builder.path(
            fill = SolidColor(Color.Black),
            pathBuilder = block,
        )
    }
}

private fun icon(name: String, content: IconScope.() -> Unit): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = GRID,
        viewportHeight = GRID,
    )
    IconScope(builder).content()
    return builder.build()
}

/** A full circle, since the path DSL only offers arcs. */
private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}
