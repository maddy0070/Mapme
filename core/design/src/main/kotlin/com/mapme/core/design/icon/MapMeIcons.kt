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
 * **The grid.** 24 units square. All ink — including the stroke's own width —
 * stays inside the [ICON_SAFE_MIN]..[ICON_SAFE_MAX] live area, leaving a 3-unit
 * margin on every side. `MapMeIconsGeometryTest` fails the build if any glyph
 * breaks out.
 *
 * That rule exists because of a real bug: the first version of this set was
 * drawn to the edges of the 24 box, so once an icon sat inside a squircle
 * button the arrow looked like it was escaping its container. Nothing was
 * mispositioned — the glyphs were simply too big for their frame, and no
 * amount of nudging the *placement* would have fixed a sizing problem.
 *
 * **The strokes.** 2 units, never varied. Round caps, round joins. Curves that
 * ease into their straights, the same squarical instinct that governs
 * [com.mapme.core.design.theme.SquircleShape]. No gloss, no gradients, no 3D,
 * and never a second icon family.
 *
 * **The set is deliberately tiny** — three icons, because three is what the
 * current screens need. A set that grows ahead of its screens is how a product
 * ends up with four visual dialects.
 */
object MapMeIcons {

    /**
     * Forward. Onward. The only directional icon MapMe has.
     *
     * Shaft and head are one continuous gesture, symmetric about the centre of
     * the grid on both axes so it reads as centred inside a round or squircle
     * container without needing an optical nudge.
     */
    val ArrowRight: ImageVector by lazy {
        icon("ArrowRight") {
            stroked {
                moveTo(5.5f, 12f)
                lineTo(18.5f, 12f)
                // The head bows very slightly rather than mitring to a point,
                // which is what keeps it in the same family as the squircle.
                moveTo(13.3f, 6.9f)
                curveTo(14.8f, 8.6f, 16.7f, 10.5f, 18.5f, 12f)
                curveTo(16.7f, 13.5f, 14.8f, 15.4f, 13.3f, 17.1f)
            }
        }
    }

    /**
     * Go round again. Used for replaying the introduction.
     *
     * An open loop with a solid head at its leading end. The head is filled
     * rather than stroked because at 18dp a stroked arrowhead closes up into a
     * blob.
     */
    val Replay: ImageVector by lazy {
        icon("Replay") {
            stroked {
                moveTo(17.35f, 8.9f)
                curveTo(16.05f, 6.45f, 13.32f, 5.12f, 10.59f, 5.6f)
                curveTo(7.86f, 6.08f, 5.75f, 8.26f, 5.36f, 11.01f)
                curveTo(4.98f, 13.75f, 6.4f, 16.44f, 8.89f, 17.65f)
                curveTo(11.39f, 18.87f, 14.38f, 18.34f, 16.3f, 16.35f)
            }
            filled {
                moveTo(16.5f, 7.29f)
                lineTo(19.9f, 9.22f)
                lineTo(16.19f, 11.19f)
                close()
            }
        }
    }

    /** Somewhere new. Only ever used for genuine discovery. */
    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            filled {
                moveTo(12f, 4f)
                curveTo(12.7f, 8.69f, 15.31f, 11.3f, 20f, 12f)
                curveTo(15.31f, 12.7f, 12.7f, 15.31f, 12f, 20f)
                curveTo(11.3f, 15.31f, 8.69f, 12.7f, 4f, 12f)
                curveTo(8.69f, 11.3f, 11.3f, 8.69f, 12f, 4f)
                close()
            }
        }
    }
}

/** The 24-unit grid every MapMe icon is drawn on. */
const val ICON_GRID: Float = 24f

/** Stroke width, in grid units. Never varied. */
const val ICON_STROKE: Float = 2f

/** No ink, stroke included, may fall outside this range on either axis. */
const val ICON_SAFE_MIN: Float = 3f
const val ICON_SAFE_MAX: Float = 21f

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

private class IconScope(val builder: ImageVector.Builder) {
    fun stroked(block: PathBuilder.() -> Unit) {
        builder.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE,
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
        viewportWidth = ICON_GRID,
        viewportHeight = ICON_GRID,
    )
    IconScope(builder).content()
    return builder.build()
}
