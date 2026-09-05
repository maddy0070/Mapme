package com.mapme.app.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mapme.app.R
import com.mapme.core.design.component.GlassCard
import com.mapme.core.design.component.GlassSurface
import com.mapme.core.design.component.GlassTone
import com.mapme.core.design.component.MapMeBackground
import com.mapme.core.design.component.MapMeButton
import com.mapme.core.design.component.MapMeButtonSize
import com.mapme.core.design.component.MapMeButtonStyle
import com.mapme.core.design.component.MapMePill
import com.mapme.core.design.component.MapMeText
import com.mapme.core.design.component.Metric
import com.mapme.core.design.component.MetricEmphasis
import com.mapme.core.design.component.PillTone
import com.mapme.core.design.component.TrailLine
import com.mapme.core.design.haptics.LocalMapMeHaptics
import com.mapme.core.design.icon.MapMeIcon
import com.mapme.core.design.icon.MapMeIcons
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.design.theme.motionDuration
import kotlinx.coroutines.launch

/**
 * Every piece of the design system, on a real phone.
 *
 * This is a workshop screen, not a product screen: it is how colour, type,
 * glass, motion and haptics get judged on the hardware they have to work on,
 * in the light the person is actually standing in. Previews lie about all five.
 *
 * It ships in the app on purpose — a design system nobody can look at drifts.
 */
@Composable
fun KitScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MapMeTheme.colors
    val space = MapMeTheme.space

    MapMeBackground(modifier = modifier.fillMaxSize(), animated = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = space.screenEdge),
            verticalArrangement = Arrangement.spacedBy(space.sectionGap),
        ) {
            Spacer(Modifier.height(space.x2))

            Column(verticalArrangement = Arrangement.spacedBy(space.x3)) {
                MapMeButton(
                    text = stringResource(R.string.kit_back),
                    onClick = onBack,
                    style = MapMeButtonStyle.Ghost,
                )
                MapMeText(
                    text = stringResource(R.string.kit_title),
                    style = MapMeTheme.type.displayMedium,
                )
                MapMeText(
                    text = stringResource(R.string.kit_subtitle),
                    style = MapMeTheme.type.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            ColourSection()
            TypeSection()
            TrailSection()
            GlassSection()
            ButtonSection()
            NumberSection()
            IconSection()
            HapticSection()

            Spacer(Modifier.height(space.x12))
        }
    }
}

// --- Sections --------------------------------------------------------------

@Composable
private fun ColourSection() {
    val colors = MapMeTheme.colors
    Section(
        title = stringResource(R.string.kit_section_colour),
        note = stringResource(R.string.kit_section_colour_note),
    ) {
        SwatchRow(
            listOf(
                "canvas" to colors.canvas,
                "surface" to colors.surface,
                "raised" to colors.surfaceRaised,
                "overlay" to colors.surfaceOverlay,
            ),
        )
        SwatchRow(
            listOf(
                "accent" to colors.accent,
                "focus" to colors.focus,
                "live" to colors.live,
                "milestone" to colors.milestone,
                "critical" to colors.critical,
            ),
        )
        SwatchRow(
            listOf(
                "text 1" to colors.textPrimary,
                "text 2" to colors.textSecondary,
                "text 3" to colors.textTertiary,
            ),
        )
    }
}

@Composable
private fun TypeSection() {
    val type = MapMeTheme.type
    val colors = MapMeTheme.colors
    Section(
        title = stringResource(R.string.kit_section_type),
        note = stringResource(R.string.kit_section_type_note),
    ) {
        if (!MapMeTheme.fonts.isBranded) {
            MapMeText(
                text = stringResource(R.string.kit_section_type_fallback),
                style = type.bodySmall,
                color = colors.milestone,
            )
        }
        Specimen("displayMedium", "Where have you been?", type.displayMedium)
        Specimen("titleLarge", "Tuesday, 14 October", type.titleLarge)
        Specimen("titleMedium", "Morning walk", type.titleMedium)
        Specimen("bodyLarge", "You went a little further than usual.", type.bodyLarge)
        Specimen("bodyMedium", "Four kilometres, mostly along the river.", type.bodyMedium)
        Specimen("label", "This month", type.label)
        Specimen("labelSmall", "New place", type.labelSmall)
    }
}

@Composable
private fun TrailSection() {
    val motion = MapMeTheme.motion
    val duration = motionDuration(motion.epic)
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(1f) }

    Section(
        title = stringResource(R.string.kit_section_trail),
        note = stringResource(R.string.kit_section_trail_note),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x4)) {
                TrailLine(
                    points = SampleJourney,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    progress = progress.value,
                )
                MapMeButton(
                    text = stringResource(R.string.kit_replay_trail),
                    onClick = {
                        scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(duration, easing = motion.entering))
                        }
                    },
                    style = MapMeButtonStyle.Secondary,
                    leading = {
                        MapMeIcon(MapMeIcons.Play, contentDescription = null, size = 16.dp)
                    },
                )
            }
        }
    }
}

@Composable
private fun GlassSection() {
    Section(
        title = stringResource(R.string.kit_section_glass),
        note = stringResource(R.string.kit_section_glass_note),
    ) {
        listOf(
            GlassTone.Whisper to R.string.kit_glass_whisper,
            GlassTone.Standard to R.string.kit_glass_standard,
            GlassTone.Dense to R.string.kit_glass_dense,
        ).forEach { (tone, label) ->
            GlassCard(tone = tone, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MapMeText(text = stringResource(label), style = MapMeTheme.type.titleSmall)
                    MapMeIcon(MapMeIcons.Layers, contentDescription = null, size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun ButtonSection() {
    Section(
        title = stringResource(R.string.kit_section_buttons),
        note = stringResource(R.string.kit_section_buttons_note),
    ) {
        MapMeButton(
            text = stringResource(R.string.kit_button_primary),
            onClick = {},
            size = MapMeButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
            leading = { MapMeIcon(MapMeIcons.Pin, contentDescription = null, size = 18.dp) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3)) {
            MapMeButton(
                text = stringResource(R.string.kit_button_secondary),
                onClick = {},
                style = MapMeButtonStyle.Secondary,
            )
            MapMeButton(
                text = stringResource(R.string.kit_button_ghost),
                onClick = {},
                style = MapMeButtonStyle.Ghost,
            )
        }
        MapMeButton(
            text = stringResource(R.string.kit_button_primary),
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
private fun NumberSection() {
    val colors = MapMeTheme.colors
    Section(
        title = stringResource(R.string.kit_section_metrics),
        note = stringResource(R.string.kit_section_metrics_note),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x6)) {
                Metric(
                    value = "142",
                    unit = "km",
                    label = stringResource(R.string.kit_metric_distance_label),
                    emphasis = MetricEmphasis.Hero,
                    color = colors.accent,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x8)) {
                    Metric(value = "7", label = stringResource(R.string.kit_metric_places_label))
                    Metric(value = "23", label = stringResource(R.string.kit_metric_days_label))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2)) {
                    MapMePill(stringResource(R.string.kit_pill_recording), tone = PillTone.Live)
                    MapMePill(
                        stringResource(R.string.kit_pill_today),
                        tone = PillTone.Neutral,
                        icon = MapMeIcons.Calendar,
                    )
                    MapMePill(
                        stringResource(R.string.kit_pill_record),
                        tone = PillTone.Milestone,
                        icon = MapMeIcons.Sparkle,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconSection() {
    val icons: List<Pair<ImageVector, String>> = listOf(
        MapMeIcons.Pin to "Pin",
        MapMeIcons.Trail to "Trail",
        MapMeIcons.Calendar to "Calendar",
        MapMeIcons.Play to "Play",
        MapMeIcons.Layers to "Layers",
        MapMeIcons.Sparkle to "Sparkle",
        MapMeIcons.ChevronRight to "Chevron",
    )
    Section(
        title = stringResource(R.string.kit_section_icons),
        note = stringResource(R.string.kit_section_icons_note),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x4),
        ) {
            icons.forEach { (icon, name) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2),
                ) {
                    GlassSurface(
                        shape = MapMeTheme.radius.control,
                        tone = GlassTone.Whisper,
                        depth = MapMeTheme.depth.resting,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            MapMeIcon(icon, contentDescription = null)
                        }
                    }
                    MapMeText(
                        text = name,
                        style = MapMeTheme.type.labelSmall,
                        color = MapMeTheme.colors.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HapticSection() {
    val haptics = LocalMapMeHaptics.current
    Section(
        title = stringResource(R.string.kit_section_haptics),
        note = stringResource(R.string.kit_section_haptics_note),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2),
        ) {
            MapMeButton(stringResource(R.string.kit_haptic_select), { haptics.select() }, style = MapMeButtonStyle.Secondary)
            MapMeButton(stringResource(R.string.kit_haptic_confirm), { haptics.confirm() }, style = MapMeButtonStyle.Secondary)
            MapMeButton(stringResource(R.string.kit_haptic_tick), { haptics.tick() }, style = MapMeButtonStyle.Secondary)
            MapMeButton(stringResource(R.string.kit_haptic_milestone), { haptics.milestone() }, style = MapMeButtonStyle.Secondary)
            MapMeButton(stringResource(R.string.kit_haptic_warn), { haptics.warn() }, style = MapMeButtonStyle.Secondary)
        }
    }
}

// --- Building blocks -------------------------------------------------------

private val SampleJourney = listOf(
    Offset(0.05f, 0.75f),
    Offset(0.20f, 0.40f),
    Offset(0.34f, 0.55f),
    Offset(0.50f, 0.20f),
    Offset(0.66f, 0.48f),
    Offset(0.82f, 0.28f),
    Offset(0.95f, 0.62f),
)

@Composable
private fun Section(
    title: String,
    note: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3)) {
        MapMeText(
            text = title.uppercase(),
            style = MapMeTheme.type.label,
            color = MapMeTheme.colors.accent,
        )
        MapMeText(
            text = note,
            style = MapMeTheme.type.bodySmall,
            color = MapMeTheme.colors.textTertiary,
        )
        Spacer(Modifier.height(MapMeTheme.space.x1))
        content()
    }
}

@Composable
private fun SwatchRow(entries: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3),
    ) {
        entries.forEach { (name, color) ->
            Column(
                verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(width = 64.dp, height = 48.dp)
                        .clip(MapMeTheme.radius.control)
                        .background(color)
                        .border(1.dp, MapMeTheme.colors.outline, MapMeTheme.radius.control),
                )
                MapMeText(
                    text = name,
                    style = MapMeTheme.type.labelSmall,
                    color = MapMeTheme.colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun Specimen(name: String, sample: String, style: TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MapMeText(
            text = name,
            style = MapMeTheme.type.labelSmall,
            color = MapMeTheme.colors.textTertiary,
        )
        MapMeText(text = sample, style = style)
        Spacer(Modifier.height(MapMeTheme.space.x2))
    }
}
