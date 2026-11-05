package com.lockin.app

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockin.app.feature.focus.FocusSchedulesScreen
import com.lockin.app.feature.guardedapps.GuardedAppsScreen
import com.lockin.app.feature.library.LibraryScreen
import com.lockin.app.feature.messages.MessagesScreen
import com.lockin.app.feature.settings.LicensesScreen
import com.lockin.app.feature.settings.PrivacyScreen
import com.lockin.app.feature.settings.SettingsScreen
import com.lockin.app.feature.stats.StatsScreen
import kotlinx.serialization.Serializable

@Serializable private data object LibraryRoute
@Serializable private data object SettingsRoute
@Serializable private data object GuardedAppsRoute
@Serializable private data object PrivacyRoute
@Serializable private data object LicensesRoute
@Serializable private data object StatsRoute
@Serializable private data object FocusRoute
@Serializable private data object MessagesRoute

private val spatialSpring = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 380f)
private val fadeSpec = spring<Float>(stiffness = 700f)

@Composable
fun LockinNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController,
        startDestination = LibraryRoute,
        enterTransition = { slideInHorizontally(spatialSpring) { it / 4 } + fadeIn(fadeSpec) },
        exitTransition = { slideOutHorizontally(spatialSpring) { -it / 8 } + fadeOut(fadeSpec) },
        popEnterTransition = { slideInHorizontally(spatialSpring) { -it / 8 } + fadeIn(fadeSpec) },
        popExitTransition = { slideOutHorizontally(spatialSpring) { it / 4 } + fadeOut(fadeSpec) },
    ) {
        composable<LibraryRoute> {
            LibraryScreen(
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onChooseApps = { navController.navigate(GuardedAppsRoute) },
                onOpenStats = { navController.navigate(StatsRoute) },
                onOpenFocus = { navController.navigate(FocusRoute) },
                onOpenMessages = { navController.navigate(MessagesRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = back,
                onGuardedApps = { navController.navigate(GuardedAppsRoute) },
                onPrivacy = { navController.navigate(PrivacyRoute) },
                onLicenses = { navController.navigate(LicensesRoute) },
            )
        }
        composable<GuardedAppsRoute> { GuardedAppsScreen(onBack = back) }
        composable<PrivacyRoute> { PrivacyScreen(onBack = back) }
        composable<LicensesRoute> { LicensesScreen(onBack = back) }
        composable<StatsRoute> { StatsScreen(onBack = back) }
        composable<FocusRoute> { FocusSchedulesScreen(onBack = back) }
        composable<MessagesRoute> { MessagesScreen(onBack = back) }
    }
}
