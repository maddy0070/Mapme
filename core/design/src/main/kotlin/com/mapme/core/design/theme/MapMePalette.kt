package com.mapme.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * The raw MapMe palette.
 *
 * These are pigments, not roles. Nothing in the app should reference them
 * directly — use [MapMeColors] (`MapMeTheme.colors`) so every colour arrives
 * with a meaning attached.
 *
 * ## Why these hues
 *
 * MapMe is looked at in the dark, over a map, for a long time. The ground is
 * therefore a cold ink with a slight cyan cast — the colour of the world seen
 * from altitude at night — rather than the neutral grey or navy-violet that
 * most dark themes settle on. It recedes, so the journey can come forward.
 *
 * Against that ground sit four signal hues, each with one job:
 *
 * - **Aurora** (electric mint) is the freshest part of your trail, and the
 *   colour of anything positive. It is the hero.
 * - **Beacon** (electric azure) is the far end of the trail — older movement —
 *   and everything to do with selection, focus and discovery.
 * - **Pulse** (hot magenta) is *now*: the live head of the trail, recording
 *   state, the beating dot that is you.
 * - **Ember** (gold) is celebration — milestones, records, streaks — and
 *   nothing else. Used rarely so it keeps its meaning.
 *
 * Beacon → Aurora is a short, adjacent hue sweep, which is what lets the trail
 * stay elegant at every zoom instead of turning into a rainbow. Pulse is the
 * only hue far away from it, which is exactly why the live head always reads.
 */
internal object MapMePalette {

    // --- Ink: the ground -----------------------------------------------------
    val Ink00 = Color(0xFF05080B)
    val Ink05 = Color(0xFF080D12)
    val Ink10 = Color(0xFF0C131A)
    val Ink15 = Color(0xFF101922)
    val Ink20 = Color(0xFF15212C)
    val Ink30 = Color(0xFF1C2C39)
    val Ink40 = Color(0xFF263B4B)
    val Ink50 = Color(0xFF33505F)

    // --- Frost: light ground, and text on ink --------------------------------
    val Frost00 = Color(0xFFFFFFFF)
    val Frost02 = Color(0xFFECF3F6)
    val Frost05 = Color(0xFFE2EBF0)
    val Frost10 = Color(0xFFD3E0E7)
    val Frost20 = Color(0xFFB9CBD5)
    val Frost30 = Color(0xFF9FB4BF)
    val Frost40 = Color(0xFF8FA6B1)
    // Frost50 is the floor for text on ink: it is the lightest step that still
    // clears 4.5:1 against every dark surface, overlay included. Do not darken
    // it without re-running ColorContrastTest.
    val Frost50 = Color(0xFF7C949F)
    val Frost60 = Color(0xFF5A727D)

    // --- Aurora: the trail, alive --------------------------------------------
    val Aurora20 = Color(0xFF052B26)
    val Aurora40 = Color(0xFF0C7D6B)
    val Aurora60 = Color(0xFF14C4A6)
    val Aurora = Color(0xFF2BF5C0)
    val Aurora80 = Color(0xFF7DFFDC)

    // --- Beacon: focus, selection, distance ----------------------------------
    val Beacon20 = Color(0xFF0A1A45)
    val Beacon40 = Color(0xFF2B4BC7)
    val Beacon = Color(0xFF4D7CFF)
    val Beacon80 = Color(0xFF9DB8FF)

    // --- Pulse: now ----------------------------------------------------------
    val Pulse20 = Color(0xFF3D0A24)
    val Pulse40 = Color(0xFFB01C63)
    val Pulse = Color(0xFFFF2E93)
    val Pulse80 = Color(0xFFFF8FC4)

    // --- Ember: celebration --------------------------------------------------
    val Ember20 = Color(0xFF3A2703)
    val Ember40 = Color(0xFF9A6205)
    val Ember = Color(0xFFFFB627)
    val Ember80 = Color(0xFFFFD98A)

    // --- Critical: destructive only ------------------------------------------
    val Critical20 = Color(0xFF3D0D0D)
    val Critical40 = Color(0xFF8E1F18)
    val Critical = Color(0xFFFF4B4B)
    val Critical80 = Color(0xFFFF9A96)
}
