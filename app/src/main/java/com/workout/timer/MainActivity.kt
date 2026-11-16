package com.workout.timer

import android.Manifest
import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.workout.timer.ui.navigation.WorkoutNavGraph
import com.workout.timer.ui.theme.WorkoutTimerTheme
import com.workout.timer.util.WorkoutReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingNotificationNav(
    val startRoutineId: Long? = null,
    val viewRoutineId: Long? = null,
    val targetTab: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entry point utama aplikasi.
 * - Mengaktifkan splash screen (F-01 onboarding).
 * - Mengaktifkan edge-to-edge display untuk tampilan AMOLED maksimal.
 * - Menangani Intent dari Notifikasi (Mulai Latihan, Lihat Jadwal, Rest Day).
 * - Meminta izin notifikasi runtime di Android 13+ (POST_NOTIFICATIONS) agar alarm & timer lockscreen aktif.
 * - Menyerahkan navigasi ke [WorkoutNavGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_ROUTINE_ID = "EXTRA_START_ROUTINE_ID"
        const val EXTRA_VIEW_ROUTINE_ID = "EXTRA_VIEW_ROUTINE_ID"
        const val EXTRA_TARGET_TAB = "EXTRA_TARGET_TAB"
        const val TAB_PLAN = "PLAN"
        const val TAB_HISTORY = "HISTORY"
        const val TAB_HOME = "HOME"
        const val TAB_SETTINGS = "SETTINGS"
    }

    private val _pendingNav = MutableStateFlow<PendingNotificationNav?>(null)
    val pendingNav: StateFlow<PendingNotificationNav?> = _pendingNav.asStateFlow()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            WorkoutReminderManager.rescheduleAll(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Permintaan izin notifikasi otomatis di Android 13+ (Tiramisu / API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleNotificationIntent(intent)

        setContent {
            WorkoutTimerApp(
                pendingNavFlow = _pendingNav,
                onClearPendingNav = { _pendingNav.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val startRoutineId = if (intent.hasExtra(EXTRA_START_ROUTINE_ID)) {
            intent.getLongExtra(EXTRA_START_ROUTINE_ID, -1L).takeIf { it > 0 }
        } else null

        val viewRoutineId = if (intent.hasExtra(EXTRA_VIEW_ROUTINE_ID)) {
            intent.getLongExtra(EXTRA_VIEW_ROUTINE_ID, -1L).takeIf { it > 0 }
        } else null

        val targetTab = intent.getStringExtra(EXTRA_TARGET_TAB)

        if (startRoutineId != null || viewRoutineId != null || targetTab != null) {
            _pendingNav.value = PendingNotificationNav(
                startRoutineId = startRoutineId,
                viewRoutineId = viewRoutineId,
                targetTab = targetTab,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

@Composable
fun WorkoutTimerApp(
    pendingNavFlow: MutableStateFlow<PendingNotificationNav?>? = null,
    onClearPendingNav: () -> Unit = {}
) {
    WorkoutTimerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            val navController = rememberNavController()
            WorkoutNavGraph(
                navController = navController,
                pendingNavFlow = pendingNavFlow,
                onClearPendingNav = onClearPendingNav
            )
        }
    }
}
