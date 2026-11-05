package com.workout.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.workout.timer.ui.navigation.WorkoutNavGraph
import com.workout.timer.ui.theme.WorkoutTimerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point utama aplikasi.
 * - Mengaktifkan splash screen (F-01 onboarding).
 * - Mengaktifkan edge-to-edge display untuk tampilan AMOLED maksimal.
 * - Menyerahkan navigasi ke [WorkoutNavGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WorkoutTimerApp()
        }
    }
}

@Composable
fun WorkoutTimerApp() {
    WorkoutTimerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color.Black
        ) {
            val navController = rememberNavController()
            WorkoutNavGraph(navController = navController)
        }
    }
}
