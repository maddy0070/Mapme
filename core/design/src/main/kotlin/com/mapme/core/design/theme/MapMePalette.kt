package com.mapme.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * The raw MapMe palette.
 *
 * Pigments, not roles. Nothing outside [MapMeColors] should reference these —
 * every colour reaches a screen with a meaning already attached.
 *
 * ## Why these hues
 *
 * MapMe is a journal of someone's life, so the palette is **warm before it is
 * technical**. The ground is a plum-tinted charcoal at night and a rose-tinted
 * paper by day — never a neutral grey, never a cold cyan-black, because a
 * record of where you have been should feel closer to a photo album than to an
 * instrument panel.
 *
 * Three signal hues, each with exactly one job:
 *
 * - **Rose** is *you*. The journey line, the primary action, anything alive.
 *   It is the hero, and it is warm on purpose — this is the "Me" in MapMe.
 * - **Indigo** is *the world*. Structure, focus, selection, the map beneath.
 *   Cool, confident, and deliberately quieter than Rose.
 * - **Citrus** is *discovery*. A new place, a personal record, a first. It is
 *   used rarely enough that seeing it means something.
 *
 * The trail ramp travels through Rose alone — deep to hot to white-hot. A
 * hue-based gradient from blue to pink passes through purple at its midpoint,
 * which is exactly the look this product must not have.
 */
internal object MapMePalette {

    // --- Night: the dark ground. Charcoal with a plum warmth. ---------------
    val Night00 = Color(0xFF0E0B12)
    val Night05 = Color(0xFF141019)
    val Night10 = Color(0xFF1A1621)
    val Night20 = Color(0xFF241F2C)
    val Night30 = Color(0xFF302938)
    val Night40 = Color(0xFF3E3547)

    // --- Paper: the light ground. Warm white, faint rose undertone. ---------
    val Paper00 = Color(0xFFFFFFFF)
    val Paper05 = Color(0xFFFDFAFB)
    val Paper10 = Color(0xFFF7F2F5)
    val Paper20 = Color(0xFFEDE6EB)
    val Paper30 = Color(0xFFDDD3DA)

    // --- Rose: you, and your journey ----------------------------------------
    val Rose10 = Color(0xFF2A0A16)
    val Rose30 = Color(0xFF7A0E33)
    val Rose40 = Color(0xFF8C0036)
    val Rose50 = Color(0xFFB10040)
    val Rose60 = Color(0xFFD6004F)
    val Rose70 = Color(0xFFE8145C)
    val Rose = Color(0xFFFF2D6F)
    val Rose80 = Color(0xFFFF6B98)
    val Rose90 = Color(0xFFFFC2D6)
    val Rose95 = Color(0xFFFFF0F5)

    // --- Indigo: the world ---------------------------------------------------
    val Indigo10 = Color(0xFF0C1030)
    val Indigo30 = Color(0xFF1E2A7A)
    val Indigo50 = Color(0xFF3040D6)
    val Indigo60 = Color(0xFF3B4FD8)
    val Indigo = Color(0xFF4F6BFF)
    val Indigo80 = Color(0xFF9DAEFF)
    val Indigo95 = Color(0xFFDEE5FF)

    // --- Citrus: discovery ---------------------------------------------------
    val Citrus30 = Color(0xFF3A4D00)
    val Citrus50 = Color(0xFF5E7A00)
    val Citrus70 = Color(0xFF8FB80A)
    val Citrus = Color(0xFFB8F02D)
    val Citrus80 = Color(0xFFD6F97E)

    // --- Text ----------------------------------------------------------------
    // Floors verified by ColorContrastTest against every surface each one can
    // land on. Do not darken (light) or lighten (dark) without re-running it.
    val OnNight00 = Color(0xFFF6F1F5)
    val OnNight10 = Color(0xFFB5AABF)
    val OnNight20 = Color(0xFF8F849B)

    val OnPaper00 = Color(0xFF1A1421)
    val OnPaper10 = Color(0xFF5A5065)
    val OnPaper20 = Color(0xFF6E6479)

    // --- Critical: destructive only ------------------------------------------
    // --- the basemap ------------------------------------------------------
    //
    // The only pigments here that exist purely for the map. The interface
    // palette is mixed for type and controls — small areas, high contrast —
    // and a map needs the opposite: very large, very quiet fields that a
    // rose-coloured line can sit on top of without a fight.
    //
    // Nothing here is rose, and nothing here is saturated. That is the whole
    // rule. The journey is the only vivid thing MapMe ever draws, and a
    // basemap that competes with it has misunderstood the product.

    /** Night ground. A step up from the app canvas, so the map reads as a lit stage. */
    val MapNightLand = Color(0xFF14111A)
    val MapNightGreen = Color(0xFF16211B)
    val MapNightWater = Color(0xFF0F1830)
    val MapNightRoad = Color(0xFF262030)
    val MapNightRoadMajor = Color(0xFF332C3E)

    /** Roads are separated by a casing *darker* than the ground, not a brighter outline. */
    val MapNightCasing = Color(0xFF100D15)
    val MapNightBuilding = Color(0xFF1B1723)
    val MapNightBoundary = Color(0xFF3A3345)

    /** Paper ground. Warm and off-white: a white map is a spreadsheet. */
    val MapPaperLand = Color(0xFFF5F0EE)
    val MapPaperGreen = Color(0xFFE6EDE1)
    val MapPaperWater = Color(0xFFD7E3F0)
    val MapPaperRoad = Color(0xFFFFFFFF)

    /** Arterials warm rather than yellow. Saturated roads are the noisy-map tell. */
    val MapPaperRoadMajor = Color(0xFFFFF8EC)
    val MapPaperCasing = Color(0xFFE3DAD9)
    val MapPaperBuilding = Color(0xFFEAE2E0)
    val MapPaperBoundary = Color(0xFFCFC4CC)

    val CriticalDark = Color(0xFFFF5A4E)
    val CriticalLight = Color(0xFFD62B1F)
}
