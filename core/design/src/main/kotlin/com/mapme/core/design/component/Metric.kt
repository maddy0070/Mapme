package com.mapme.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.mapme.core.design.theme.MapMeTheme

enum class MetricEmphasis {
    /** The number a whole screen is built around. */
    Hero,

    /** A number inside a card, alongside others. */
    Standard,
}

/**
 * A statistic, presented as something to feel rather than read.
 *
 * The number is huge, tight and tabular; the unit is quiet and sits on the
 * number's baseline; the label is a small uppercase caption underneath. That
 * ordering matters — "142" lands first, "km" explains it, "walked this month"
 * gives it meaning. Reverse the hierarchy and you have a dashboard.
 *
 * Screen readers get the whole thing as one sentence, because "142", "km",
 * "walked this month" read out as three separate items is nonsense.
 */
@Composable
fun Metric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    emphasis: MetricEmphasis = MetricEmphasis.Standard,
    color: Color = Color.Unspecified,
) {
    val colors = MapMeTheme.colors
    val type = MapMeTheme.type
    val valueColor = color.takeOrElse { colors.textPrimary }

    val valueStyle = when (emphasis) {
        MetricEmphasis.Hero -> type.heroMetric
        MetricEmphasis.Standard -> type.metric
    }

    val spoken = buildString {
        append(value)
        if (unit != null) {
            append(' ')
            append(unit)
        }
        append(", ")
        append(label)
    }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.labelGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x1),
        ) {
            MapMeText(
                text = value,
                style = valueStyle,
                color = valueColor,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            if (unit != null) {
                MapMeText(
                    text = unit,
                    style = if (emphasis == MetricEmphasis.Hero) type.titleMedium else type.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        MapMeText(
            text = label.uppercase(),
            style = type.metricLabel,
            color = colors.textTertiary,
            maxLines = 2,
        )
    }
}
