package com.workout.timer.ui.navigation

/**
 * Definisi semua route navigasi dalam aplikasi.
 *
 * Menggunakan sealed class + object untuk type-safety dan
 * mencegah typo dalam string route.
 */
sealed class Screen(val route: String) {

    /** Splash / Intro Screen (Hook & Brand Identity) */
    data object Splash : Screen("splash")

    /** Onboarding Flow (Interactive 3-Phase Setup & Curation) */
    data object Onboarding : Screen("onboarding")

    /** Home Dashboard (F-02) */
    data object Home : Screen("home")

    /** Workout Builder — buat rutinitas baru */
    data object WorkoutBuilderCreate : Screen("builder/new")

    /** Workout Builder — edit rutinitas yang ada */
    data object WorkoutBuilderEdit : Screen("builder/{routineId}") {
        fun createRoute(routineId: Long) = "builder/$routineId"
    }

    /** Workout Player — layar sesi latihan berlangsung (F-06) */
    data object WorkoutPlayer : Screen("player/{routineId}") {
        fun createRoute(routineId: Long) = "player/$routineId"
    }

    /** Detail rutinitas (preview sebelum mulai) */
    data object RoutineDetail : Screen("detail/{routineId}") {
        fun createRoute(routineId: Long) = "detail/$routineId"
    }

    /** Jadwal & Program Latihan Mingguan Hybrid (Latihan Tab) */
    data object WorkoutPlan : Screen("plan")

    /** Riwayat Latihan (History Screen) */
    data object WorkoutHistory : Screen("history")

    /** Kamus Gerakan (ExerciseTemplate list) */
    data object ExerciseDictionary : Screen("dictionary")

    /** Backup & Restore (F-07) */
    data object BackupRestore : Screen("backup")

    /** Settings / Preferensi pengguna */
    data object Settings : Screen("settings")

    /** Pelacakan Lari / Jalan — Dashboard */
    data object RunDashboard : Screen("run")

    /** Pelacakan Lari / Jalan — Sesi Live Tracking */
    data object ActiveRun : Screen("run/active/{sessionId}") {
        fun createRoute(sessionId: Long = 0L) = "run/active/$sessionId"
    }

    /** Pelacakan Lari / Jalan — Ringkasan Pasca Lari */
    data object RunSummary : Screen("run/summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "run/summary/$sessionId"
    }

    /** Pelacakan Lari / Jalan — Riwayat Sesi */
    data object RunHistory : Screen("run/history")
}
