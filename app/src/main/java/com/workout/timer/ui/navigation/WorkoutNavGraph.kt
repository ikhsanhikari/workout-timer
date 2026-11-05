package com.workout.timer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workout.timer.ui.screen.home.HomeScreen
import com.workout.timer.ui.screen.builder.WorkoutBuilderScreen
import com.workout.timer.ui.screen.player.WorkoutPlayerScreen
import com.workout.timer.ui.screen.detail.RoutineDetailScreen
import com.workout.timer.ui.screen.history.WorkoutHistoryScreen
import com.workout.timer.ui.screen.splash.SplashScreen
import com.workout.timer.ui.screen.onboarding.OnboardingScreen
import com.workout.timer.ui.screen.settings.SettingsScreen

/**
 * NavGraph utama aplikasi.
 *
 * Start destination: [Screen.Splash]
 *
 * Navigasi antar layar menggunakan lambda callback untuk
 * memisahkan logika navigasi dari Composable (unidirectional).
 *
 * ViewModel untuk Builder & Detail menerima [routineId] via SavedStateHandle
 * dari backStackEntry.arguments secara otomatis oleh Hilt Navigation.
 */
@Composable
fun WorkoutNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── Splash Screen (Hook & Brand Identity) ───────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isOnboardingDone ->
                    if (isOnboardingDone) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Onboarding Flow (Interactive 3-Phase Setup & Curation) ───────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home Dashboard ──────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToBuilder = {
                    navController.navigate(Screen.WorkoutBuilderCreate.route)
                },
                onNavigateToRoutineDetail = { routineId ->
                    navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                },
                onNavigateToStartWorkout = { routineId ->
                    navController.navigate(Screen.WorkoutPlayer.createRoute(routineId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.WorkoutHistory.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ── Workout Builder (Create — routineId = 0 via SavedStateHandle) ───
        composable(
            route = Screen.WorkoutBuilderCreate.route
        ) {
            WorkoutBuilderScreen(
                routineId = null,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { routineId ->
                    navController.navigate(Screen.WorkoutPlayer.createRoute(routineId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // ── Workout Builder (Edit) ───────────────────────────────────────────
        composable(
            route = Screen.WorkoutBuilderEdit.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId")
            WorkoutBuilderScreen(
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { id ->
                    navController.navigate(Screen.WorkoutPlayer.createRoute(id)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // ── Routine Detail ───────────────────────────────────────────────────
        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: return@composable
            RoutineDetailScreen(
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.WorkoutBuilderEdit.createRoute(routineId))
                },
                onNavigateToPlayer = {
                    navController.navigate(Screen.WorkoutPlayer.createRoute(routineId))
                },
                onNavigateToNewRoutine = { newId ->
                    navController.navigate(Screen.RoutineDetail.createRoute(newId))
                }
            )
        }

        // ── Workout Player ───────────────────────────────────────────────────
        composable(
            route = Screen.WorkoutPlayer.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: return@composable
            WorkoutPlayerScreen(
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Workout History ──────────────────────────────────────────────────
        composable(route = Screen.WorkoutHistory.route) {
            WorkoutHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoutineDetail = { routineId ->
                    navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                },
                onNavigateToStartWorkout = { routineId ->
                    navController.navigate(Screen.WorkoutPlayer.createRoute(routineId))
                }
            )
        }

        // ── Centralized Settings Screen ──────────────────────────────────────
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
