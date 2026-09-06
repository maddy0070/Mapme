package com.mapme.core.design.icon

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Icons have to fit inside the things they sit in.
 *
 * This exists because of a real bug found on a device: the first version of
 * this set was drawn out to the edges of the 24-unit box, so an arrow inside a
 * squircle button looked like it was breaking out of its container. The glyphs
 * were not mispositioned — they were oversized, and no amount of nudging the
 * placement would have fixed that.
 *
 * Bounds are computed from control points, which over-estimates a Bézier
 * slightly. That is the right direction to be wrong in for a containment test.
 */
class MapMeIconsGeometryTest {

    private val icons = listOf(
        "ArrowRight" to MapMeIcons.ArrowRight,
        "Replay" to MapMeIcons.Replay,
        "Sparkle" to MapMeIcons.Sparkle,
    )

    @Test
    fun `no icon draws outside its safe area`() {
        icons.forEach { (name, icon) ->
            val box = inkBounds(icon)
            assertTrue(
                "$name escapes left: ${box.left} < $ICON_SAFE_MIN",
                box.left >= ICON_SAFE_MIN - 0.01f,
            )
            assertTrue(
                "$name escapes top: ${box.top} < $ICON_SAFE_MIN",
                box.top >= ICON_SAFE_MIN - 0.01f,
            )
            assertTrue(
                "$name escapes right: ${box.right} > $ICON_SAFE_MAX",
                box.right <= ICON_SAFE_MAX + 0.01f,
            )
            assertTrue(
                "$name escapes bottom: ${box.bottom} > $ICON_SAFE_MAX",
                box.bottom <= ICON_SAFE_MAX + 0.01f,
            )
        }
    }

    @Test
    fun `every icon is optically centred on the grid`() {
        // Within half a unit of centre — about a third of a pixel at the 18dp
        // size these are drawn at, which is past the point anyone can see.
        icons.forEach { (name, icon) ->
            val box = inkBounds(icon)
            val cx = (box.left + box.right) / 2f
            val cy = (box.top + box.bottom) / 2f
            assertTrue("$name sits off-centre horizontally at $cx", abs(cx - ICON_GRID / 2f) < 0.5f)
            assertTrue("$name sits off-centre vertically at $cy", abs(cy - ICON_GRID / 2f) < 0.5f)
        }
    }

    @Test
    fun `every icon actually fills its grid`() {
        // An icon that clears the safe area by being tiny is not a pass. Each
        // glyph must span at least two thirds of the live area on its long
        // axis, or the set will look inconsistently weighted.
        val live = ICON_SAFE_MAX - ICON_SAFE_MIN
        icons.forEach { (name, icon) ->
            val box = inkBounds(icon)
            val longest = maxOf(box.right - box.left, box.bottom - box.top)
            assertTrue("$name is too small: spans $longest of $live", longest >= live * 0.66f)
        }
    }

    @Test
    fun `every stroke is the house weight`() {
        icons.forEach { (name, icon) ->
            strokedPaths(icon).forEach { path ->
                assertTrue(
                    "$name uses a ${path.strokeLineWidth} stroke, not $ICON_STROKE",
                    abs(path.strokeLineWidth - ICON_STROKE) < 0.001f,
                )
            }
        }
    }

    // --- walking the vector ------------------------------------------------

    private data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private fun paths(icon: ImageVector): List<VectorPath> {
        val out = mutableListOf<VectorPath>()
        fun walk(node: VectorNode) {
            when (node) {
                is VectorPath -> out += node
                is VectorGroup -> node.forEach { walk(it) }
            }
        }
        walk(icon.root)
        return out
    }

    private fun strokedPaths(icon: ImageVector) = paths(icon).filter { it.stroke != null }

    /** Bounds of everything the icon puts on screen, stroke width included. */
    private fun inkBounds(icon: ImageVector): Box {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE

        paths(icon).forEach { path ->
            val pad = if (path.stroke != null) path.strokeLineWidth / 2f else 0f
            points(path.pathData).forEach { (x, y) ->
                if (x - pad < left) left = x - pad
                if (y - pad < top) top = y - pad
                if (x + pad > right) right = x + pad
                if (y + pad > bottom) bottom = y + pad
            }
        }
        return Box(left, top, right, bottom)
    }

    private fun points(nodes: List<PathNode>): List<Pair<Float, Float>> {
        val out = mutableListOf<Pair<Float, Float>>()
        nodes.forEach { node ->
            when (node) {
                is PathNode.MoveTo -> out += node.x to node.y
                is PathNode.LineTo -> out += node.x to node.y
                is PathNode.CurveTo -> {
                    out += node.x1 to node.y1
                    out += node.x2 to node.y2
                    out += node.x3 to node.y3
                }
                PathNode.Close -> Unit
                // Deliberately loud: a node type this walker does not
                // understand would make the bounds silently wrong, and a
                // containment test that quietly measures nothing is worse
                // than no test at all.
                else -> throw AssertionError(
                    "MapMe icons are drawn with moveTo/lineTo/curveTo only; found $node. " +
                        "Either redraw the icon or teach this walker the new node.",
                )
            }
        }
        return out
    }
}
