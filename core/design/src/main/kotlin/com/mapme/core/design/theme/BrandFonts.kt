package com.mapme.core.design.theme

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * MapMe speaks in two typefaces, and they are chosen to match the voice.
 *
 * The MapMe voice is short, warm and confident. A rigid geometric face would
 * fight that; a rounded cartoon face would undercut it. So:
 *
 * **Bricolage Grotesque** carries headlines — a grotesque with deliberate
 * irregularities that give a large sentence a personality rather than merely a
 * size.
 *
 * **Plus Jakarta Sans** carries everything read at length — smooth, modern,
 * friendly small, and content to disappear.
 *
 * Both are SIL OFL 1.1. Neither is committed as a binary:
 * `./gradlew fetchBrandFonts` pulls them into assets and CI runs it before
 * every build. When they are genuinely unavailable the app does not break and
 * does not look broken — it falls back to the platform grotesque and keeps the
 * MapMe metrics: the ramp, the tracking, the weights, the tabular figures.
 * The type *system* is the design; the typeface is the voice.
 */
@Immutable
data class BrandFonts(
    val display: FontFamily,
    val text: FontFamily,
    /** False when the real typefaces were missing at runtime. */
    val isBranded: Boolean,
) {
    companion object {
        private const val ASSET_DIR = "fonts"
        private const val DISPLAY_FILE = "BricolageGrotesque.ttf"
        private const val TEXT_FILE = "PlusJakartaSans.ttf"

        val Fallback = BrandFonts(
            display = FontFamily.SansSerif,
            text = FontFamily.SansSerif,
            isBranded = false,
        )

        fun resolve(context: Context): BrandFonts {
            val assets = context.applicationContext.assets
            val present = runCatching { assets.list(ASSET_DIR)?.toSet().orEmpty() }
                .getOrDefault(emptySet())

            val display = if (DISPLAY_FILE in present) {
                variableFamily(assets, "$ASSET_DIR/$DISPLAY_FILE", DISPLAY_WEIGHTS)
            } else {
                null
            }
            val text = if (TEXT_FILE in present) {
                variableFamily(assets, "$ASSET_DIR/$TEXT_FILE", TEXT_WEIGHTS)
            } else {
                null
            }

            if (display == null && text == null) return Fallback

            return BrandFonts(
                display = display ?: text ?: FontFamily.SansSerif,
                text = text ?: FontFamily.SansSerif,
                isBranded = display != null && text != null,
            )
        }

        private val DISPLAY_WEIGHTS = listOf(
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
        )

        private val TEXT_WEIGHTS = listOf(
            FontWeight.Normal,
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
        )

        /**
         * One variable file, several logical weights. Compose drives the
         * `wght` axis per style, so a single file covers the whole ramp; any
         * other axes the file carries keep their defaults.
         */
        private fun variableFamily(
            assets: AssetManager,
            path: String,
            weights: List<FontWeight>,
        ): FontFamily = FontFamily(
            weights.map { weight -> Font(path = path, assetManager = assets, weight = weight) },
        )
    }
}
