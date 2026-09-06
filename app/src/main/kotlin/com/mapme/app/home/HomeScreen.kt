package com.mapme.app.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mapme.app.R
import com.mapme.app.map.rememberLocationAccess
import com.mapme.core.design.component.GlassCard
import com.mapme.core.design.component.GlassTone
import com.mapme.core.design.component.MapMeButton
import com.mapme.core.design.component.MapMeButtonStyle
import com.mapme.core.design.component.MapMePill
import com.mapme.core.design.component.MapMeText
import com.mapme.core.design.component.MapMeWordmark
import com.mapme.core.design.component.PillTone
import com.mapme.core.design.haptics.LocalMapMeHaptics
import com.mapme.core.design.icon.MapMeIcon
import com.mapme.core.design.icon.MapMeIcons
import com.mapme.core.design.theme.LocalAppearance
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.design.theme.ThemeMode
import com.mapme.core.location.LocationPrompt
import com.mapme.core.location.LocationProvider
import com.mapme.core.location.SystemLocationProvider
import com.mapme.core.location.UserLocation
import com.mapme.core.map.MapCameraState
import com.mapme.core.map.MapFailure
import com.mapme.core.map.MapLoadState
import com.mapme.core.map.MapMeMap

/**
 * Home, now that the map is real.
 *
 * The previous version was a mark, a headline and a card on an empty ground,
 * because there was nothing else true to show. There is now: this is the
 * person's actual surroundings, and the interface's job changes from filling
 * a void to staying out of the way of one.
 *
 * So the map is the whole screen and everything else floats: a wordmark and
 * the appearance control at the top, one glass card at the bottom, one
 * control beside it. The card still carries the emotional line the previous
 * home was built around — it just no longer has to be the entire screen.
 *
 * Nothing here invents a statistic. Recording does not exist yet, and a home
 * screen that says "0 km this week" to fill space has started lying on day one.
 */
@Composable
fun HomeScreen(
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
    provider: LocationProvider? = null,
) {
    val context = LocalContext.current
    val locationProvider = provider
        ?: remember(context.applicationContext) { SystemLocationProvider(context) }

    val access = rememberLocationAccess(locationProvider)
    val camera = remember { MapCameraState() }
    var fix by remember { mutableStateOf<UserLocation?>(null) }
    var load by remember { mutableStateOf<MapLoadState>(MapLoadState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }

    // Collected only while permitted, and dropped the moment it is not: the
    // provider stops the moment this effect leaves, so nothing is listening to
    // the GPS behind a screen that is not showing it.
    LaunchedEffect(access.access) {
        if (!access.access.canLocate) {
            fix = null
            return@LaunchedEffect
        }
        locationProvider.stream().collect { location ->
            fix = location
            camera.onLocation(location.point)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapMeMap(
            camera = camera,
            modifier = Modifier.fillMaxSize(),
            user = fix?.point,
            accuracyMetres = fix?.accuracyMetres,
            reloadKey = reloadKey,
            onLoadStateChange = { load = it },
        )

        MapLoading(visible = load is MapLoadState.Loading)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MapMeTheme.space.screenEdge)
                    .padding(top = MapMeTheme.space.x2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapMeWordmark(markSize = 26.dp, textStyle = MapMeTheme.type.titleSmall)
                AppearanceToggle()
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MapMeTheme.space.screenEdge),
                horizontalArrangement = Arrangement.End,
            ) {
                RecentreControl(
                    following = camera.following,
                    enabled = fix != null,
                    onClick = { camera.recentre(fix?.point) },
                )
            }

            Spacer(Modifier.height(MapMeTheme.space.x3))

            BottomCard(
                load = load,
                prompt = LocationPrompt.of(access.access),
                onGrant = access.request,
                onAppSettings = access.openAppSettings,
                onLocationSettings = access.openLocationSettings,
                // Bumping the key is what actually reloads the style. Setting
                // the state alone would only redraw the card.
                onRetry = { reloadKey++ },
                onReplayIntro = onReplayIntro,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MapMeTheme.space.screenEdge)
                    .padding(bottom = MapMeTheme.space.x5),
            )
        }
    }
}

/**
 * One card, whose contents depend on what is in the way.
 *
 * Ordered by urgency rather than by category: a map that failed to load is
 * more pressing than a permission that has not been granted, because without
 * tiles there is nothing for the dot to sit on.
 */
@Composable
private fun BottomCard(
    load: MapLoadState,
    prompt: LocationPrompt,
    onGrant: () -> Unit,
    onAppSettings: () -> Unit,
    onLocationSettings: () -> Unit,
    onRetry: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, tone = GlassTone.Dense) {
        Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x3)) {
            when {
                load is MapLoadState.Failed -> {
                    val failure = load.reason
                    Message(
                        headline = stringResource(
                            when (failure) {
                                MapFailure.Offline -> R.string.map_offline_headline
                                MapFailure.Tiles -> R.string.map_tiles_headline
                                MapFailure.Style -> R.string.map_style_headline
                            },
                        ),
                        body = stringResource(
                            when (failure) {
                                MapFailure.Offline -> R.string.map_offline_body
                                MapFailure.Tiles -> R.string.map_tiles_body
                                MapFailure.Style -> R.string.map_style_body
                            },
                        ),
                    )
                    // Only where pressing it could change anything. A retry on
                    // a style that will not parse is a button that lies.
                    if (failure.retryable) {
                        MapMeButton(
                            text = stringResource(R.string.map_retry),
                            onClick = onRetry,
                            style = MapMeButtonStyle.Secondary,
                        )
                    }
                }

                prompt == LocationPrompt.Explain -> {
                    MapMePill(
                        text = stringResource(R.string.map_permission_eyebrow),
                        tone = PillTone.Discovery,
                        icon = MapMeIcons.Locate,
                    )
                    Message(
                        headline = stringResource(R.string.map_permission_headline),
                        body = stringResource(R.string.map_permission_body),
                    )
                    MapMeButton(
                        text = stringResource(R.string.map_permission_action),
                        onClick = onGrant,
                    )
                }

                prompt == LocationPrompt.Settings -> {
                    Message(
                        headline = stringResource(R.string.map_permission_blocked_headline),
                        body = stringResource(R.string.map_permission_blocked_body),
                    )
                    MapMeButton(
                        text = stringResource(R.string.map_permission_blocked_action),
                        onClick = onAppSettings,
                        style = MapMeButtonStyle.Secondary,
                    )
                }

                prompt == LocationPrompt.NoProvider -> {
                    Message(
                        headline = stringResource(R.string.map_no_provider_headline),
                        body = stringResource(R.string.map_no_provider_body),
                    )
                    MapMeButton(
                        text = stringResource(R.string.map_no_provider_action),
                        onClick = onLocationSettings,
                        style = MapMeButtonStyle.Secondary,
                    )
                }

                else -> {
                    MapMePill(
                        text = stringResource(R.string.home_map_eyebrow),
                        tone = PillTone.Neutral,
                    )
                    Message(
                        headline = stringResource(R.string.home_map_headline),
                        body = stringResource(R.string.home_map_body),
                    )
                    MapMeButton(
                        text = stringResource(R.string.home_replay_intro),
                        onClick = onReplayIntro,
                        style = MapMeButtonStyle.Ghost,
                        trailing = {
                            MapMeIcon(MapMeIcons.Replay, contentDescription = null, size = 18.dp)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Message(headline: String, body: String) {
    val colors = MapMeTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x2)) {
        MapMeText(headline, style = MapMeTheme.type.titleLarge, color = colors.textPrimary)
        MapMeText(body, style = MapMeTheme.type.bodyMedium, color = colors.textSecondary)
    }
}

/**
 * Back to where I am.
 *
 * Dimmed rather than hidden while the camera is already following: a control
 * that disappears when it would do nothing is a control people stop looking
 * for. The accent ring is the second signal, so the state does not rest on
 * colour alone.
 */
@Composable
private fun RecentreControl(
    following: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MapMeTheme.colors
    val haptics = LocalMapMeHaptics.current
    val shape = MapMeTheme.radius.control
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = MapMeTheme.motion.snappy(),
        label = "RecentrePress",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(52.dp)
            .clip(shape)
            .background(colors.surfaceOverlay.copy(alpha = 0.86f))
            .border(1.dp, if (following) colors.accent else colors.outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = stringResource(R.string.map_recentre),
            ) {
                haptics.select()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        MapMeIcon(
            icon = MapMeIcons.Locate,
            contentDescription = stringResource(R.string.map_recentre),
            size = 22.dp,
            tint = when {
                !enabled -> colors.textTertiary
                following -> colors.accent
                else -> colors.textPrimary
            },
        )
    }
}

/**
 * Waiting for the world.
 *
 * Painted in the map's own ground colour so that when tiles arrive they
 * arrive *into* this rather than replacing it — there is no flash, because
 * the two are already the same colour. The only motion is the wordmark's
 * mark, which is the journey line at logo scale; nothing here pretends to be
 * map content.
 */
@Composable
private fun MapLoading(visible: Boolean, modifier: Modifier = Modifier) {
    val colors = MapMeTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(MapMeTheme.motion.flowing)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.mapLand),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MapMeTheme.space.x4),
            ) {
                MapMeWordmark(markSize = 44.dp, textStyle = MapMeTheme.type.titleMedium)
                MapMeText(
                    text = stringResource(R.string.map_loading),
                    style = MapMeTheme.type.bodySmall,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

/**
 * Auto → Light → Dark, in one tap.
 *
 * A word rather than an icon: "Auto" is a state no sun-or-moon glyph has ever
 * communicated on the first try, and MapMe would rather be understood than
 * clever. A small accent dot marks the two modes that are an explicit choice.
 *
 * It reports the centre of itself in window coordinates when tapped, because
 * the new appearance spreads from exactly this point — see `ThemeTransition`.
 */
@Composable
private fun AppearanceToggle(modifier: Modifier = Modifier) {
    val appearance = LocalAppearance.current ?: return
    val haptics = LocalMapMeHaptics.current
    val colors = MapMeTheme.colors
    val shape = MapMeTheme.radius.control
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = MapMeTheme.motion.snappy(),
        label = "AppearanceTogglePress",
    )

    var centre by remember { mutableStateOf(Offset.Unspecified) }
    val explicit = appearance.mode != ThemeMode.System

    Row(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            // Over a map this needs to be a surface, not a tint: a translucent
            // control drifting over roads and parks is unreadable half the time.
            .background(colors.surfaceOverlay.copy(alpha = 0.86f))
            .border(1.dp, if (explicit) colors.outlineStrong else colors.outline, shape)
            .onGloballyPositioned {
                val topLeft = it.positionInWindow()
                centre = Offset(
                    topLeft.x + it.size.width / 2f,
                    topLeft.y + it.size.height / 2f,
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = stringResource(R.string.home_appearance_action),
            ) {
                haptics.select()
                appearance.cycle(centre)
            }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (explicit) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(MapMeTheme.radius.chip)
                    .background(colors.accent),
            )
        }
        MapMeText(
            text = appearance.mode.label,
            style = MapMeTheme.type.labelSmall,
            color = if (explicit) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
    }
}
