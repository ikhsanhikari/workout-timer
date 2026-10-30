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

/**
 * NavGraph utama aplikasi.
 *
 * Start destination: [Screen.Home]
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
        startDestination = Screen.Home.route
    ) {
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
    }
}
