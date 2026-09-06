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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapme.core.design.theme.LocalMapMeContentColor
import com.mapme.core.design.theme.MapMeTheme

/**
 * MapMe draws its own icons, to one handwriting.
 *
 * **The rules.** A 24dp grid. A 2dp stroke, never varied. Round caps and round
 * joins on everything. Curves that ease into their straights rather than
 * meeting them at a corner — the same squarical instinct that governs
 * [com.mapme.core.design.theme.SquircleShape]. No gloss, no gradients, no
 * fills except where a shape is genuinely solid, and no second icon family
 * ever.
 *
 * **The set is deliberately tiny.** Three icons, because three is what the
 * current screens need. An icon gets drawn when a screen genuinely cannot
 * speak without it — a set that grows ahead of its screens is how a product
 * ends up with four visual dialects.
 */
object MapMeIcons {

    /** Forward. Onward. The only directional icon MapMe has. */
    val ArrowRight: ImageVector by lazy {
        icon("ArrowRight") {
            stroked {
                moveTo(4.5f, 12f)
                lineTo(18.2f, 12f)
            }
            stroked {
                // Softened at the elbow rather than mitred to a point.
                moveTo(12.6f, 6.0f)
                curveTo(13.1f, 8.0f, 15.4f, 10.6f, 18.2f, 12f)
                curveTo(15.4f, 13.4f, 13.1f, 16.0f, 12.6f, 18.0f)
            }
        }
    }

    /** Go round again. Used for replaying the intro. */
    val Replay: ImageVector by lazy {
        icon("Replay") {
            stroked {
                // A squircle-flavoured loop, open at the top right.
                moveTo(19.0f, 8.4f)
                curveTo(20.1f, 12.4f, 18.1f, 16.8f, 14.2f, 18.6f)
                curveTo(10.1f, 20.4f, 5.4f, 18.7f, 3.6f, 14.8f)
                curveTo(1.8f, 10.8f, 3.5f, 6.1f, 7.4f, 4.3f)
                curveTo(10.9f, 2.7f, 15.0f, 3.8f, 17.3f, 6.7f)
            }
            stroked {
                moveTo(11.6f, 6.6f)
                lineTo(17.9f, 7.1f)
                lineTo(17.1f, 13.0f)
            }
        }
    }

    /** Somewhere new. Only ever used for genuine discovery. */
    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            filled {
                moveTo(12f, 2.8f)
                curveTo(12.8f, 8.2f, 15.8f, 11.2f, 21.2f, 12f)
                curveTo(15.8f, 12.8f, 12.8f, 15.8f, 12f, 21.2f)
                curveTo(11.2f, 15.8f, 8.2f, 12.8f, 2.8f, 12f)
                curveTo(8.2f, 11.2f, 11.2f, 8.2f, 12f, 2.8f)
                close()
            }
        }
    }
}

/**
 * Draws a MapMe icon.
 *
 * Pass a [contentDescription] only when the icon is the sole carrier of the
 * meaning. Next to a label it should stay null — a screen reader announcing
 * "arrow, Continue" is worse than not announcing the arrow.
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

// --- the handwriting -------------------------------------------------------

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
        builder.path(fill = SolidColor(Color.Black), pathBuilder = block)
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
