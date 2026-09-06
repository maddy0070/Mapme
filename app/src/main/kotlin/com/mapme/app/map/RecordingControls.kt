package com.mapme.app.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.mapme.app.R
import com.mapme.core.design.component.MapMeButton
import com.mapme.core.design.component.MapMeButtonStyle
import com.mapme.core.design.component.MapMePill
import com.mapme.core.design.component.MapMeText
import com.mapme.core.design.component.PillTone
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.recording.RecordingProblem
import com.mapme.core.recording.RecordingSnapshot
import com.mapme.core.recording.RecordingState
import kotlin.math.roundToInt

/**
 * The journey controls.
 *
 * Everything here is derived from one [RecordingSnapshot]. There is no local
 * "is recording" boolean anywhere in the UI, which is the point of having a
 * state machine at all — a screen that keeps its own copy of the state is a
 * screen that will eventually disagree with the recorder about whether a walk
 * is in progress.
 *
 * **Nothing is communicated by colour alone.** The recording state is a word
 * in a pill, not a red dot; paused says "Paused". Someone who cannot
 * distinguish the accent from the outline still knows exactly what MapMe is
 * doing.
 */
@Composable
fun RecordingControls(
    snapshot: RecordingSnapshot,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDone: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    canRecord: Boolean = true,
) {
    // Armed by the first tap on Finish, cleared by anything else. §14: ending
    // a journey must not be one ambiguous tap, and a walk is not recoverable
    // once it has been ended by accident.
    var confirmingFinish by remember(snapshot.state) { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3),
    ) {
        when (snapshot.state) {
            RecordingState.Idle -> {
                MapMeButton(
                    text = stringResource(R.string.journey_start),
                    onClick = onStart,
                    enabled = canRecord,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            RecordingState.Recording, RecordingState.Paused -> {
                val paused = snapshot.state == RecordingState.Paused
                LiveReadout(snapshot = snapshot, paused = paused)

                if (confirmingFinish) {
                    MapMeText(
                        text = stringResource(R.string.journey_finish_confirm),
                        style = MapMeTheme.type.titleSmall,
                        color = MapMeTheme.colors.textPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2)) {
                        MapMeButton(
                            text = stringResource(R.string.journey_keep_walking),
                            onClick = { confirmingFinish = false },
                            style = MapMeButtonStyle.Secondary,
                        )
                        MapMeButton(
                            text = stringResource(R.string.journey_finish_yes),
                            onClick = {
                                confirmingFinish = false
                                onFinish()
                            },
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2)) {
                        MapMeButton(
                            text = stringResource(
                                if (paused) R.string.journey_resume else R.string.journey_pause,
                            ),
                            onClick = if (paused) onResume else onPause,
                            style = MapMeButtonStyle.Secondary,
                        )
                        MapMeButton(
                            text = stringResource(R.string.journey_finish),
                            onClick = { confirmingFinish = true },
                            style = MapMeButtonStyle.Ghost,
                        )
                    }
                }
            }

            RecordingState.Stopping -> {
                MapMeText(
                    text = stringResource(R.string.journey_saving),
                    style = MapMeTheme.type.titleSmall,
                    color = MapMeTheme.colors.textPrimary,
                )
            }

            RecordingState.Completed -> {
                MapMePill(text = stringResource(R.string.journey_done_eyebrow), tone = PillTone.Discovery)
                MapMeText(
                    text = stringResource(R.string.journey_done_headline),
                    style = MapMeTheme.type.titleLarge,
                    color = MapMeTheme.colors.textPrimary,
                )
                Summary(snapshot)
                Row(horizontalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2)) {
                    MapMeButton(text = stringResource(R.string.journey_done_action), onClick = onDone)
                    MapMeButton(
                        text = stringResource(R.string.journey_discard),
                        onClick = onDiscard,
                        style = MapMeButtonStyle.Ghost,
                    )
                }
            }

            RecordingState.Error -> {
                MapMeText(
                    text = stringResource(
                        when (snapshot.problem) {
                            RecordingProblem.PermissionLost -> R.string.journey_error_permission
                            RecordingProblem.LocationUnavailable -> R.string.journey_error_unavailable
                            RecordingProblem.StorageFailed -> R.string.journey_error_storage
                            null -> R.string.journey_error_unavailable
                        },
                    ),
                    style = MapMeTheme.type.bodyMedium,
                    color = MapMeTheme.colors.textSecondary,
                )
                if (!snapshot.journey.isEmpty) Summary(snapshot)
                MapMeButton(
                    text = stringResource(R.string.journey_done_action),
                    onClick = onDone,
                    style = MapMeButtonStyle.Secondary,
                )
            }
        }
    }
}

/**
 * Distance and time while walking.
 *
 * Deliberately two numbers and a word. §11 asks for minimal, and the map with
 * the person's own line growing across it is the thing worth looking at — a
 * dashboard here would cover the only part of the screen that matters.
 */
@Composable
private fun LiveReadout(snapshot: RecordingSnapshot, paused: Boolean) {
    val colors = MapMeTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2)) {
        MapMePill(
            text = stringResource(if (paused) R.string.journey_paused else R.string.journey_recording),
            tone = if (paused) PillTone.Neutral else PillTone.Discovery,
        )

        if (!snapshot.hasTrail && !paused) {
            // Honest about the gap between pressing start and the first fix
            // good enough to believe, rather than showing 0 m as though
            // nothing were happening.
            MapMeText(
                text = stringResource(R.string.journey_waiting_body),
                style = MapMeTheme.type.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            Summary(snapshot)
        }

        if (paused) {
            MapMeText(
                text = stringResource(R.string.journey_paused_body),
                style = MapMeTheme.type.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

/** "1.2 km · 18 min", read to a screen reader as a sentence rather than fragments. */
@Composable
private fun Summary(snapshot: RecordingSnapshot) {
    val distance = formatDistance(snapshot.distanceMetres)
    val duration = formatDuration(snapshot.recordedMillis)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$distance in $duration"
        },
    ) {
        MapMeText(
            text = "$distance  ·  $duration",
            style = MapMeTheme.type.titleMedium,
            color = MapMeTheme.colors.textPrimary,
        )
    }
}

/**
 * Metres under a kilometre, kilometres above it.
 *
 * Rounded to something a person would say. "1.24 km" is a measurement;
 * "1.2 km" is a walk.
 */
internal fun formatDistance(metres: Double): String = when {
    metres < 1_000 -> "${metres.roundToInt()} m"
    else -> "%.1f km".format(metres / 1_000)
}

/** Minutes below an hour, then hours and minutes. Seconds are never useful here. */
internal fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    return when {
        totalMinutes < 1 -> "under a minute"
        totalMinutes < 60 -> "$totalMinutes min"
        else -> "${totalMinutes / 60} h ${totalMinutes % 60} min"
    }
}
