package com.mapme.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapme.app.home.HomeScreen
import com.mapme.app.onboarding.OnboardingScreen
import com.mapme.core.design.theme.MapMeTheme
import com.mapme.core.design.theme.motionDuration

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

/**
 * Where MapMe can go.
 *
 * Two destinations, and the transition between them is doing real work: home
 * does not slide in from the side like a new page, it *settles into place*
 * from slightly further away, as though the introduction resolved into it.
 * MapMe screens are layers over one place, not a stack of cards.
 */
@Composable
fun MapMeApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember(context.applicationContext) { OnboardingPrefs(context) }
    val motion = MapMeTheme.motion
    val duration = motionDuration(motion.flowing)

    NavHost(
        navController = navController,
        startDestination = if (prefs.seen) Routes.HOME else Routes.ONBOARDING,
        enterTransition = {
            fadeIn(tween(duration, easing = motion.entering)) +
                scaleIn(tween(duration, easing = motion.entering), initialScale = 1.06f)
        },
        exitTransition = {
            fadeOut(tween(duration / 2, easing = motion.exiting)) +
                scaleOut(tween(duration, easing = motion.exiting), targetScale = 0.97f)
        },
        popEnterTransition = {
            fadeIn(tween(duration, easing = motion.entering)) +
                scaleIn(tween(duration, easing = motion.entering), initialScale = 0.97f)
        },
        popExitTransition = {
            fadeOut(tween(duration / 2, easing = motion.exiting)) +
                scaleOut(tween(duration, easing = motion.exiting), targetScale = 1.06f)
        },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    prefs.seen = true
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onReplayIntro = { navController.navigate(Routes.ONBOARDING) },
            )
        }
    }
}
