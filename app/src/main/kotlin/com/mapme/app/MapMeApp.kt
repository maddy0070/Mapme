package com.mapme.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapme.app.foundation.FoundationScreen
import com.mapme.app.kit.KitScreen
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.design.theme.motionDuration

/**
 * Where MapMe can go.
 *
 * Two destinations today. The graph exists now rather than later so that
 * transitions are a property of the app from the start — screens in MapMe
 * should feel spatially related, and that is decided here, once, instead of
 * being re-invented per screen.
 */
private object Routes {
    const val FOUNDATION = "foundation"
    const val KIT = "kit"
}

@Composable
fun MapMeApp() {
    val navController = rememberNavController()
    val motion = MapMeTheme.motion
    val duration = motionDuration(motion.smooth)

    NavHost(
        navController = navController,
        startDestination = Routes.FOUNDATION,
        // Going deeper rises from below and the screen behind stays put:
        // MapMe screens are layers over one place, not a stack of pages
        // sliding sideways.
        enterTransition = {
            slideInVertically(
                animationSpec = tween(duration, easing = motion.entering),
                initialOffsetY = { it / 6 },
            ) + fadeIn(animationSpec = tween(duration, easing = motion.entering))
        },
        exitTransition = { fadeOut(animationSpec = tween(duration / 2)) },
        popEnterTransition = { fadeIn(animationSpec = tween(duration)) },
        popExitTransition = {
            slideOutVertically(
                animationSpec = tween(duration, easing = motion.exiting),
                targetOffsetY = { it / 6 },
            ) + fadeOut(animationSpec = tween(duration, easing = motion.exiting))
        },
    ) {
        composable(Routes.FOUNDATION) {
            FoundationScreen(onOpenKit = { navController.navigate(Routes.KIT) })
        }
        composable(Routes.KIT) {
            KitScreen(onBack = { navController.popBackStack() })
        }
    }
}
