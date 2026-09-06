package com.mapme.core.map

import androidx.compose.ui.graphics.Color

/**
 * Just enough JSON to write a MapLibre style, and not one line more.
 *
 * A style is a tree of maps, lists, strings and numbers. Serialising that is
 * about forty lines, and forty lines is cheaper than a serialisation library
 * in a module whose whole job is to stay small — the alternative was
 * kotlinx.serialization plus its plugin for a single output string.
 *
 * The important consequence is that [MapMeStyle] builds a **Kotlin tree**
 * rather than a string. Tests assert against that tree directly, so they check
 * the style's meaning ("the water layer is painted with the water token")
 * rather than its punctuation.
 */
internal fun jsonOf(value: Any?): String = buildString { writeJson(value, this) }

private fun writeJson(value: Any?, out: StringBuilder) {
    when (value) {
        null -> out.append("null")
        is String -> writeString(value, out)
        is Boolean -> out.append(value.toString())
        is Int -> out.append(value.toString())
        is Long -> out.append(value.toString())
        is Float -> out.append(trimNumber(value.toDouble()))
        is Double -> out.append(trimNumber(value))
        is Map<*, *> -> {
            out.append('{')
            var first = true
            value.forEach { (k, v) ->
                if (!first) out.append(',')
                first = false
                writeString(k.toString(), out)
                out.append(':')
                writeJson(v, out)
            }
            out.append('}')
        }
        is List<*> -> {
            out.append('[')
            value.forEachIndexed { i, v ->
                if (i > 0) out.append(',')
                writeJson(v, out)
            }
            out.append(']')
        }
        else -> error("MapMe styles are built from maps, lists and primitives; found ${value::class}")
    }
}

/** Whole numbers lose their `.0`, because a style full of `14.0` reads badly. */
private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun writeString(value: String, out: StringBuilder) {
    out.append('"')
    value.forEach { c ->
        when (c) {
            '"' -> out.append("\\\"")
            '\\' -> out.append("\\\\")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> if (c < ' ') out.append("\\u%04x".format(c.code)) else out.append(c)
        }
    }
    out.append('"')
}

/**
 * A colour as MapLibre wants it.
 *
 * `rgba()` rather than hex because the style spec accepts CSS colour strings
 * and this keeps alpha legible when a layer is deliberately half-there.
 */
internal fun Color.css(alpha: Float = 1f): String {
    val a = (this.alpha * alpha).coerceIn(0f, 1f)
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return if (a >= 1f) "rgb($r,$g,$b)" else "rgba($r,$g,$b,${trimNumber(((a * 100).toInt() / 100.0))})"
}
