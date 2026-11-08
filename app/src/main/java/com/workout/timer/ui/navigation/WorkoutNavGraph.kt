package com.workout.timer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workout.timer.MainActivity
import com.workout.timer.PendingNotificationNav
import com.workout.timer.ui.screen.home.HomeScreen
import com.workout.timer.ui.screen.builder.WorkoutBuilderScreen
import com.workout.timer.ui.screen.player.WorkoutPlayerScreen
import com.workout.timer.ui.screen.detail.RoutineDetailScreen
import com.workout.timer.ui.screen.history.WorkoutHistoryScreen
import com.workout.timer.ui.screen.splash.SplashScreen
import com.workout.timer.ui.screen.onboarding.OnboardingScreen
import com.workout.timer.ui.screen.settings.SettingsScreen
import com.workout.timer.ui.screen.plan.WorkoutPlanScreen
import kotlinx.coroutines.flow.StateFlow

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
fun WorkoutNavGraph(
    navController: NavHostController,
    pendingNavFlow: StateFlow<PendingNotificationNav?>? = null,
    onClearPendingNav: () -> Unit = {}
) {
    // Tangani navigasi dinamis dari notifikasi saat aplikasi sedang berjalan (warm start)
    LaunchedEffect(pendingNavFlow) {
        pendingNavFlow?.collect { nav ->
            if (nav != null) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                    when {
                        nav.startRoutineId != null -> {
                            navController.navigate(Screen.WorkoutPlayer.createRoute(nav.startRoutineId)) {
                                launchSingleTop = true
                            }
                        }
                        nav.viewRoutineId != null -> {
                            navController.navigate(Screen.RoutineDetail.createRoute(nav.viewRoutineId)) {
                                launchSingleTop = true
                            }
                        }
                        nav.targetTab == MainActivity.TAB_PLAN -> {
                            navController.navigate(Screen.WorkoutPlan.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    }
                    onClearPendingNav()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── Splash Screen (Hook & Brand Identity) ───────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isOnboardingDone ->
                    if (isOnboardingDone) {
                        val nav = pendingNavFlow?.value
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        if (nav != null) {
                            when {
                                nav.startRoutineId != null -> {
                                    navController.navigate(Screen.WorkoutPlayer.createRoute(nav.startRoutineId))
                                }
                                nav.viewRoutineId != null -> {
                                    navController.navigate(Screen.RoutineDetail.createRoute(nav.viewRoutineId))
                                }
                                nav.targetTab == MainActivity.TAB_PLAN -> {
                                    navController.navigate(Screen.WorkoutPlan.route)
                                }
                            }
                            onClearPendingNav()
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
                onNavigateToWorkoutPlan = {
                    navController.navigate(Screen.WorkoutPlan.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.WorkoutHistory.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Hybrid Weekly Workout Plan (Latihan Tab) ────────────────────────
        composable(Screen.WorkoutPlan.route) {
            WorkoutPlanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
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
                    navController.navigate(Screen.WorkoutHistory.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
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
                },
                onNavigateToEditRoutine = { newId ->
                    navController.navigate(Screen.WorkoutBuilderEdit.createRoute(newId))
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

        // ── Workout History (Progress Tab) ──────────────────────────────────
        composable(route = Screen.WorkoutHistory.route) {
            WorkoutHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoutineDetail = { routineId ->
                    navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                },
                onNavigateToStartWorkout = { routineId ->
                    navController.navigate(Screen.WorkoutPlayer.createRoute(routineId))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToWorkoutPlan = {
                    navController.navigate(Screen.WorkoutPlan.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Centralized Settings Screen (Profil Tab) ─────────────────────────
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToWorkoutPlan = {
                    navController.navigate(Screen.WorkoutPlan.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.WorkoutHistory.route) {
                        popUpTo(Screen.Home.route)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
