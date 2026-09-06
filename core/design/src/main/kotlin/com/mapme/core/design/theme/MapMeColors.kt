package com.mapme.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour MapMe may use, named by what it means.
 *
 * If a screen needs a colour that is not here, that is a decision to make in
 * the design system — not a hex value to paste into a composable.
 */
@Immutable
data class MapMeColors(
    /** Behind everything. */
    val canvas: Color,
    /** Panels resting on the canvas. */
    val surface: Color,
    /** A panel lifted off another panel. */
    val surfaceRaised: Color,
    /** Sheets and menus that cover content. */
    val surfaceOverlay: Color,
    /** Quiet fills: inputs, tracks, inactive segments. */
    val surfaceSunken: Color,

    val glassTint: Color,
    val glassBorder: Color,
    val glassSheen: Color,
    val scrim: Color,
    /** Shadows are tinted, never neutral black — see [MapMeDepth]. */
    val shadowTint: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    val outline: Color,
    val outlineStrong: Color,

    /** You. Primary actions, the journey, anything alive. */
    val accent: Color,
    /** The accent when it must carry text or sit on a bright ground. */
    val accentDeep: Color,
    val accentSoft: Color,
    /** Text placed on an [accent] fill. Differs by mode by design. */
    val onAccent: Color,

    /** The world. Structure, focus, selection. */
    val focus: Color,
    val focusSoft: Color,

    /** Discovery. A new place, a first, a record. Rare on purpose. */
    val discovery: Color,
    val discoverySoft: Color,
    val onDiscovery: Color,

    val critical: Color,
    val criticalSoft: Color,

    /** The oldest, faintest part of a journey. Decorative, not informational. */
    val trailPast: Color,
    val trailFar: Color,
    val trailNear: Color,
    /** The head. Hot, near-white on night; deep and saturated on paper. */
    val trailHead: Color,
    /** True only in dark mode, where the trail is light and may bloom. */
    val trailGlows: Boolean,

    // --- the basemap ------------------------------------------------------
    //
    // The map is the stage, not the performance. These are deliberately the
    // lowest-contrast tokens in the product: enough structure to read a city,
    // never enough to argue with the journey drawn on top.

    /** Everything that is not water, park or building. */
    val mapLand: Color,

    /** Parks, woodland, anything green enough to orient by. */
    val mapGreen: Color,
    val mapWater: Color,

    /** Residential streets. */
    val mapRoad: Color,

    /** Arterials and motorways — the roads you navigate a city by. */
    val mapRoadMajor: Color,

    /** Separates a road from the ground. Darker than the ground on night. */
    val mapRoadCasing: Color,
    val mapBuilding: Color,
    val mapBoundary: Color,
    val mapLabel: Color,
    val mapLabelHalo: Color,

    val isDark: Boolean,
)

/**
 * Night.
 *
 * A plum-warm charcoal that recedes, with the trail as the only source of
 * light in the room.
 */
fun mapMeDarkColors(): MapMeColors = MapMeColors(
    canvas = MapMePalette.Night00,
    surface = MapMePalette.Night05,
    surfaceRaised = MapMePalette.Night10,
    surfaceOverlay = MapMePalette.Night20,
    surfaceSunken = MapMePalette.Night00,

    glassTint = Color.White.copy(alpha = 0.06f),
    glassBorder = Color.White.copy(alpha = 0.10f),
    glassSheen = Color.White.copy(alpha = 0.20f),
    scrim = MapMePalette.Night00.copy(alpha = 0.66f),
    shadowTint = Color(0xFF05030A),

    textPrimary = MapMePalette.OnNight00,
    textSecondary = MapMePalette.OnNight10,
    textTertiary = MapMePalette.OnNight20,

    outline = Color.White.copy(alpha = 0.10f),
    outlineStrong = Color.White.copy(alpha = 0.22f),

    accent = MapMePalette.Rose,
    accentDeep = MapMePalette.Rose70,
    accentSoft = MapMePalette.Rose.copy(alpha = 0.16f),
    // Ink on hot rose, not white on hot rose: white only reaches 3.6:1 here,
    // and the dark label is the more confident look anyway.
    onAccent = MapMePalette.Night00,

    focus = MapMePalette.Indigo,
    focusSoft = MapMePalette.Indigo.copy(alpha = 0.18f),

    discovery = MapMePalette.Citrus,
    discoverySoft = MapMePalette.Citrus.copy(alpha = 0.16f),
    onDiscovery = MapMePalette.Night00,

    critical = MapMePalette.CriticalDark,
    criticalSoft = MapMePalette.CriticalDark.copy(alpha = 0.16f),

    trailPast = MapMePalette.Rose30,
    trailFar = MapMePalette.Rose50,
    trailNear = MapMePalette.Rose,
    trailHead = MapMePalette.Rose95,
    trailGlows = true,

    mapLand = MapMePalette.MapNightLand,
    mapGreen = MapMePalette.MapNightGreen,
    mapWater = MapMePalette.MapNightWater,
    mapRoad = MapMePalette.MapNightRoad,
    mapRoadMajor = MapMePalette.MapNightRoadMajor,
    mapRoadCasing = MapMePalette.MapNightCasing,
    mapBuilding = MapMePalette.MapNightBuilding,
    mapBoundary = MapMePalette.MapNightBoundary,
    mapLabel = MapMePalette.OnNight10,
    mapLabelHalo = MapMePalette.Night00,

    isDark = true,
)

/**
 * Paper.
 *
 * Not an inversion of night — a different design with the same voice. The
 * ground is a warm white with a rose whisper, cards are pure white floating on
 * it, ink is a soft plum-black rather than a hard black, and depth comes from
 * rose-tinted shadows. The trail stops glowing and becomes pigment: deeper,
 * denser, printed rather than lit.
 */
fun mapMeLightColors(): MapMeColors = MapMeColors(
    canvas = MapMePalette.Paper05,
    surface = MapMePalette.Paper00,
    surfaceRaised = MapMePalette.Paper00,
    surfaceOverlay = MapMePalette.Paper00,
    surfaceSunken = MapMePalette.Paper10,

    glassTint = Color.White.copy(alpha = 0.68f),
    glassBorder = MapMePalette.OnPaper00.copy(alpha = 0.07f),
    glassSheen = Color.White.copy(alpha = 0.92f),
    scrim = MapMePalette.OnPaper00.copy(alpha = 0.28f),
    // A warm shadow on warm paper. Neutral grey here reads as dirt.
    shadowTint = Color(0xFF4A1E33),

    textPrimary = MapMePalette.OnPaper00,
    textSecondary = MapMePalette.OnPaper10,
    textTertiary = MapMePalette.OnPaper20,

    outline = MapMePalette.OnPaper00.copy(alpha = 0.09f),
    outlineStrong = MapMePalette.OnPaper00.copy(alpha = 0.18f),

    // Hot rose is beautiful as a fill but only 3.5:1 on paper, so on light the
    // accent that carries text is the deeper one.
    accent = MapMePalette.Rose60,
    accentDeep = MapMePalette.Rose50,
    accentSoft = MapMePalette.Rose.copy(alpha = 0.12f),
    onAccent = Color.White,

    focus = MapMePalette.Indigo50,
    focusSoft = MapMePalette.Indigo.copy(alpha = 0.12f),

    discovery = MapMePalette.Citrus50,
    discoverySoft = MapMePalette.Citrus.copy(alpha = 0.28f),
    onDiscovery = Color.White,

    critical = MapMePalette.CriticalLight,
    criticalSoft = MapMePalette.CriticalLight.copy(alpha = 0.12f),

    trailPast = MapMePalette.Rose90,
    trailFar = MapMePalette.Rose80,
    trailNear = MapMePalette.Rose70,
    trailHead = MapMePalette.Rose40,
    trailGlows = false,

    mapLand = MapMePalette.MapPaperLand,
    mapGreen = MapMePalette.MapPaperGreen,
    mapWater = MapMePalette.MapPaperWater,
    mapRoad = MapMePalette.MapPaperRoad,
    mapRoadMajor = MapMePalette.MapPaperRoadMajor,
    mapRoadCasing = MapMePalette.MapPaperCasing,
    mapBuilding = MapMePalette.MapPaperBuilding,
    mapBoundary = MapMePalette.MapPaperBoundary,
    mapLabel = MapMePalette.OnPaper10,
    mapLabelHalo = MapMePalette.Paper00,

    isDark = false,
)

internal val LocalMapMeColors = staticCompositionLocalOf { mapMeDarkColors() }
