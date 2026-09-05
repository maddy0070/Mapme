package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour MapMe is allowed to use, named by what it *means*.
 *
 * If a screen needs a colour that is not in here, that is a design decision to
 * make in the design system — not a hex value to paste into a composable.
 */
@Immutable
data class MapMeColors(
    /** Behind everything, including the map. */
    val canvas: Color,
    /** Opaque panels that sit on the canvas. */
    val surface: Color,
    /** A panel lifted off another panel. */
    val surfaceRaised: Color,
    /** Sheets and menus that cover content. */
    val surfaceOverlay: Color,
    /** Quiet fills: input backgrounds, track behind a slider. */
    val surfaceSunken: Color,

    /** The tint of the glass material itself. */
    val glassTint: Color,
    /** The hairline that gives glass a physical edge. */
    val glassBorder: Color,
    /** The bright top edge where light catches the glass. */
    val glassSheen: Color,
    /** Dim the world behind a modal. */
    val scrim: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Text placed on an [accent] fill. */
    val textOnAccent: Color,

    val outline: Color,
    val outlineStrong: Color,

    /** The one colour that means MapMe. Primary actions, positive results. */
    val accent: Color,
    val accentMuted: Color,
    val accentSubtle: Color,

    /** Selection, focus, discovery — and the far end of the trail. */
    val focus: Color,
    val focusSubtle: Color,

    /** Live recording, the present moment, the head of the trail. */
    val live: Color,
    val liveSubtle: Color,

    /** Milestones and personal records. Rare on purpose. */
    val milestone: Color,
    val milestoneSubtle: Color,

    /** Destructive actions only. Never for ordinary warnings. */
    val critical: Color,
    val criticalSubtle: Color,

    /** Oldest point of a journey line. */
    val trailFar: Color,
    /** Newest point of a journey line. */
    val trailNear: Color,
    /** The moving head of a live journey. */
    val trailHead: Color,
    /** Trails from other days, kept present but quiet. */
    val trailPast: Color,

    // The map style is authored against these, so the basemap and the
    // interface can never drift apart.
    val mapLand: Color,
    val mapLandAlt: Color,
    val mapWater: Color,
    val mapRoadMinor: Color,
    val mapRoadMajor: Color,
    val mapBuilding: Color,
    val mapLabel: Color,
    val mapLabelHalo: Color,

    /** True when this scheme is the dark one. Drives system bar icons. */
    val isDark: Boolean,
)

/**
 * Dark is MapMe's real face — designed first, tuned longest.
 */
fun mapMeDarkColors(): MapMeColors = MapMeColors(
    canvas = MapMePalette.Ink00,
    surface = MapMePalette.Ink10,
    surfaceRaised = MapMePalette.Ink15,
    surfaceOverlay = MapMePalette.Ink20,
    surfaceSunken = MapMePalette.Ink05,

    glassTint = MapMePalette.Frost00.copy(alpha = 0.07f),
    glassBorder = MapMePalette.Frost00.copy(alpha = 0.10f),
    glassSheen = MapMePalette.Frost00.copy(alpha = 0.22f),
    scrim = MapMePalette.Ink00.copy(alpha = 0.62f),

    textPrimary = MapMePalette.Frost02,
    textSecondary = MapMePalette.Frost30,
    textTertiary = MapMePalette.Frost50,
    textOnAccent = MapMePalette.Ink00,

    outline = MapMePalette.Frost00.copy(alpha = 0.12f),
    outlineStrong = MapMePalette.Frost00.copy(alpha = 0.24f),

    accent = MapMePalette.Aurora,
    accentMuted = MapMePalette.Aurora60,
    accentSubtle = MapMePalette.Aurora.copy(alpha = 0.14f),

    focus = MapMePalette.Beacon,
    focusSubtle = MapMePalette.Beacon.copy(alpha = 0.16f),

    live = MapMePalette.Pulse,
    liveSubtle = MapMePalette.Pulse.copy(alpha = 0.16f),

    milestone = MapMePalette.Ember,
    milestoneSubtle = MapMePalette.Ember.copy(alpha = 0.16f),

    critical = MapMePalette.Critical,
    criticalSubtle = MapMePalette.Critical.copy(alpha = 0.16f),

    trailFar = MapMePalette.Beacon,
    trailNear = MapMePalette.Aurora,
    trailHead = MapMePalette.Pulse,
    trailPast = MapMePalette.Beacon.copy(alpha = 0.28f),

    mapLand = MapMePalette.Ink05,
    mapLandAlt = Color(0xFF0A1512),
    mapWater = Color(0xFF061218),
    mapRoadMinor = MapMePalette.Ink20,
    mapRoadMajor = MapMePalette.Ink30,
    mapBuilding = MapMePalette.Ink15,
    mapLabel = MapMePalette.Frost40,
    mapLabelHalo = MapMePalette.Ink00,

    isDark = true,
)

/**
 * Light exists so MapMe is usable in direct sun. It borrows the same four
 * signal hues against a cool paper ground; it is not a second design.
 */
fun mapMeLightColors(): MapMeColors = MapMeColors(
    canvas = Color(0xFFEDF2F5),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceOverlay = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFE4ECF0),

    glassTint = MapMePalette.Frost00.copy(alpha = 0.62f),
    glassBorder = MapMePalette.Ink00.copy(alpha = 0.08f),
    glassSheen = MapMePalette.Frost00.copy(alpha = 0.85f),
    scrim = MapMePalette.Ink00.copy(alpha = 0.32f),

    textPrimary = MapMePalette.Ink10,
    textSecondary = Color(0xFF44606E),
    textTertiary = Color(0xFF566A74),
    textOnAccent = MapMePalette.Ink00,

    outline = MapMePalette.Ink00.copy(alpha = 0.10f),
    outlineStrong = MapMePalette.Ink00.copy(alpha = 0.20f),

    // Aurora itself is far too light to sit on paper — this is the same hue
    // taken down until text on it is readable.
    accent = Color(0xFF008E73),
    accentMuted = Color(0xFF00705B),
    accentSubtle = Color(0xFF008E73).copy(alpha = 0.12f),

    focus = Color(0xFF2B5BE8),
    focusSubtle = Color(0xFF2B5BE8).copy(alpha = 0.12f),

    live = Color(0xFFE0177A),
    liveSubtle = Color(0xFFE0177A).copy(alpha = 0.12f),

    milestone = Color(0xFFB77800),
    milestoneSubtle = Color(0xFFB77800).copy(alpha = 0.14f),

    critical = Color(0xFFD32B24),
    criticalSubtle = Color(0xFFD32B24).copy(alpha = 0.12f),

    trailFar = Color(0xFF2B5BE8),
    trailNear = Color(0xFF008E73),
    trailHead = Color(0xFFE0177A),
    trailPast = Color(0xFF2B5BE8).copy(alpha = 0.24f),

    mapLand = Color(0xFFF2F6F8),
    mapLandAlt = Color(0xFFE6F0EC),
    mapWater = Color(0xFFD6E6EF),
    mapRoadMinor = Color(0xFFFFFFFF),
    mapRoadMajor = Color(0xFFE9EFF2),
    mapBuilding = Color(0xFFE8EEF1),
    mapLabel = Color(0xFF5C7683),
    mapLabelHalo = Color(0xFFFFFFFF),

    isDark = false,
)

internal val LocalMapMeColors = staticCompositionLocalOf { mapMeDarkColors() }
