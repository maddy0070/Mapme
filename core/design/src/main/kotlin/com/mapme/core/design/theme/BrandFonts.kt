package com.mapme.core.design.theme

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * MapMe speaks in two typefaces.
 *
 * **Space Grotesk** carries the personality: titles, the wordmark, and above
 * all the numbers. Its slightly mechanical, slightly odd letterforms are what
 * stop MapMe from looking like every other Android app, and its figures have
 * the weight a distance deserves.
 *
 * **Inter** carries everything you actually read. It is the most legible
 * interface face there is, and it knows to get out of the way.
 *
 * Both are SIL OFL 1.1. Neither is committed to the repository as a binary —
 * `./gradlew fetchBrandFonts` pulls them into assets, and CI runs that before
 * every build. When they are genuinely unavailable the app does not break and
 * does not look broken: it falls back to the platform grotesque and keeps the
 * MapMe metrics — the size ramp, the tracking, the weights, the tabular
 * figures. The type *system* is the design; the typeface is the voice.
 */
@Immutable
data class BrandFonts(
    /** Titles, statistics, the wordmark. */
    val display: FontFamily,
    /** Body copy, labels, everything read at length. */
    val text: FontFamily,
    /** False when the real typefaces were missing at runtime. */
    val isBranded: Boolean,
) {
    companion object {
        private const val ASSET_DIR = "fonts"
        private const val DISPLAY_FILE = "SpaceGrotesk.ttf"
        private const val TEXT_FILE = "Inter.ttf"

        /** What MapMe looks like when the brand faces did not make it aboard. */
        val Fallback = BrandFonts(
            display = FontFamily.SansSerif,
            text = FontFamily.SansSerif,
            isBranded = false,
        )

        /**
         * Resolves the brand families from app assets. Cheap enough to call
         * once per theme; the result is cached by the caller.
         */
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
         * One variable file, several logical weights. Compose applies the
         * `wght` axis per style, so a single 300 kB file covers the ramp.
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
