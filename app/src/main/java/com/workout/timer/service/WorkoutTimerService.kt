package com.workout.timer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.workout.timer.MainActivity
import com.workout.timer.R
import com.workout.timer.data.repository.WorkoutRepository
import com.workout.timer.domain.usecase.FlattenRoutineUseCase
import com.workout.timer.service.audio.AudioHapticManager
import com.workout.timer.service.timer.WorkoutTimerEngine
import com.workout.timer.service.timer.WorkoutTimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * WorkoutTimerService — Foreground Service untuk Timer Latihan (PRD 3. F-06 & 7.4).
 *
 * Menjamin:
 * 1. Timer tetap berjalan akurat saat layar mati / terkunci dengan WakeLock.
 * 2. Menampilkan live notification dengan tombol aksi (Pause, Resume, Skip, Stop).
 * 3. Menyimpan riwayat penyelesaian latihan ke database (markRoutinePerformed) saat selesai.
 */
@AndroidEntryPoint
class WorkoutTimerService : Service() {

    @Inject
    lateinit var timerEngine: WorkoutTimerEngine

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    @Inject
    lateinit var flattenRoutineUseCase: FlattenRoutineUseCase

    @Inject
    lateinit var audioHapticManager: AudioHapticManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceForeground = false

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "workout_timer_active_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.workout.timer.ACTION_START"
        const val ACTION_PAUSE = "com.workout.timer.ACTION_PAUSE"
        const val ACTION_RESUME = "com.workout.timer.ACTION_RESUME"
        const val ACTION_SKIP = "com.workout.timer.ACTION_SKIP"
        const val ACTION_ADD_REST = "com.workout.timer.ACTION_ADD_REST"
        const val ACTION_STOP = "com.workout.timer.ACTION_STOP"

        const val EXTRA_ROUTINE_ID = "extra_routine_id"

        fun startIntent(context: Context, routineId: Long): Intent {
            return Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ROUTINE_ID, routineId)
            }
        }

        fun pauseIntent(context: Context): Intent =
            Intent(context, WorkoutTimerService::class.java).apply { action = ACTION_PAUSE }

        fun resumeIntent(context: Context): Intent =
            Intent(context, WorkoutTimerService::class.java).apply { action = ACTION_RESUME }

        fun skipIntent(context: Context): Intent =
            Intent(context, WorkoutTimerService::class.java).apply { action = ACTION_SKIP }

        fun stopIntent(context: Context): Intent =
            Intent(context, WorkoutTimerService::class.java).apply { action = ACTION_STOP }
    }

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE -> timerEngine.pause()
                ACTION_RESUME -> timerEngine.resume()
                ACTION_SKIP -> timerEngine.skip()
                ACTION_ADD_REST -> timerEngine.addRestTime(15)
                ACTION_STOP -> stopWorkoutSession()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_SKIP)
            addAction(ACTION_ADD_REST)
            addAction(ACTION_STOP)
        }
        ContextCompat.registerReceiver(
            this,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        initWakeLock()
        observeTimerState()
    }

    @SuppressLint("WakelockTimeout")
    private fun initWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WorkoutTimer:ServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_START -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                if (routineId != -1L) {
                    startWorkoutSession(routineId)
                }
            }
            ACTION_PAUSE -> timerEngine.pause()
            ACTION_RESUME -> timerEngine.resume()
            ACTION_SKIP -> timerEngine.skip()
            ACTION_ADD_REST -> timerEngine.addRestTime(15)
            ACTION_STOP -> {
                stopWorkoutSession()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startWorkoutSession(routineId: Long) {
        if (!isServiceForeground) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildInitialNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildInitialNotification())
                }
                isServiceForeground = true
                try {
                    wakeLock?.acquire(2 * 60 * 60 * 1000L) // Safe 2 hours timeout
                } catch (e: Exception) {
                    Log.w("WorkoutTimerService", "Failed to acquire wakeLock: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("WorkoutTimerService", "Failed to startForeground: ${e.message}", e)
            }
        }

        serviceScope.launch {
            try {
                val routine = workoutRepository.getRoutineById(routineId)
                val routineName = routine?.name ?: "Workout"
                val flatExercises = flattenRoutineUseCase(routineId)
                val routineCategory = routine?.category ?: (flatExercises.firstOrNull()?.category ?: "Full Body")

                if (flatExercises.isNotEmpty()) {
                    timerEngine.startWorkout(routineId, routineName, flatExercises, routineCategory)
                } else {
                    Log.w("WorkoutTimerService", "flatExercises is empty for routineId=$routineId")
                    stopWorkoutSession()
                }
            } catch (e: Exception) {
                Log.e("WorkoutTimerService", "Error starting workout session: ${e.message}", e)
                stopWorkoutSession()
            }
        }
    }

    private fun stopWorkoutSession() {
        timerEngine.stop()
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (_: Exception) { }
        }
        if (isServiceForeground) {
            try {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) { }
            isServiceForeground = false
        }
        try {
            getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) { }
        stopSelf()
    }

    private var lastNotifiedPhase: String = ""
    private var lastNotifiedIndex: Int = -1
    private var lastNotifiedPaused: Boolean? = null
    private var lastNotifiedSet: Int? = null
    private var lastNotifiedRestTargetSeconds: Int = 0

    private fun observeTimerState() {
        serviceScope.launch {
            timerEngine.timerState.collect { state ->
                when (state) {
                    is WorkoutTimerState.Idle -> {
                        lastNotifiedPhase = "idle"
                    }
                    is WorkoutTimerState.Preparing -> {
                        if (lastNotifiedPhase != "prep") {
                            lastNotifiedPhase = "prep"
                            lastNotifiedPaused = false
                            val targetTime = System.currentTimeMillis() + (state.countdownSeconds * 1000L)
                            updateNotification(
                                title = "Persiapan: ${state.routineName}",
                                content = "Mulai gerakan pertama segera...",
                                isPaused = false,
                                targetEndTimeMs = targetTime
                            )
                        }
                    }
                    is WorkoutTimerState.Running -> {
                        val currentSet = state.currentExercise.currentSet
                        val phaseChanged = lastNotifiedPhase != "run"
                        val indexChanged = lastNotifiedIndex != state.exerciseIndex
                        val pauseChanged = lastNotifiedPaused != state.isPaused
                        val setChanged = lastNotifiedSet != currentSet

                        if (phaseChanged || indexChanged || pauseChanged || setChanged) {
                            lastNotifiedPhase = "run"
                            lastNotifiedIndex = state.exerciseIndex
                            lastNotifiedPaused = state.isPaused
                            lastNotifiedSet = currentSet

                            val setInfo = if (currentSet != null) {
                                " (Set $currentSet/${state.currentExercise.totalSets})"
                            } else ""

                            val mm = state.remainingSeconds / 60
                            val ss = state.remainingSeconds % 60
                            val timeStr = String.format("%02d:%02d", mm, ss)

                            val title = if (state.isPaused) {
                                "${state.currentExercise.title}$setInfo [Dijeda]"
                            } else {
                                "${state.currentExercise.title}$setInfo"
                            }

                            val content = if (state.isPaused) {
                                "Timer dijeda (Sisa $timeStr) • Gerakan ${state.exerciseIndex + 1}/${state.totalExercises}"
                            } else {
                                "Gerakan ${state.exerciseIndex + 1}/${state.totalExercises}"
                            }

                            val targetTime = if (!state.isPaused) {
                                System.currentTimeMillis() + (state.remainingSeconds * 1000L)
                            } else null

                            updateNotification(
                                title = title,
                                content = content,
                                isPaused = state.isPaused,
                                targetEndTimeMs = targetTime
                            )
                        }
                    }
                    is WorkoutTimerState.Resting -> {
                        val phaseChanged = lastNotifiedPhase != "rest"
                        val indexChanged = lastNotifiedIndex != state.exerciseIndex
                        val pauseChanged = lastNotifiedPaused != state.isPaused
                        val restAdded = state.remainingSeconds > lastNotifiedRestTargetSeconds

                        if (phaseChanged || indexChanged || pauseChanged || restAdded) {
                            lastNotifiedPhase = "rest"
                            lastNotifiedIndex = state.exerciseIndex
                            lastNotifiedPaused = state.isPaused
                            lastNotifiedRestTargetSeconds = state.remainingSeconds

                            val mm = state.remainingSeconds / 60
                            val ss = state.remainingSeconds % 60
                            val timeStr = String.format("%02d:%02d", mm, ss)
                            val nextTitle = state.upcomingExercise?.title ?: "Gerakan Selanjutnya"

                            val title = if (state.isPaused) {
                                "Istirahat [Dijeda]"
                            } else {
                                "Istirahat"
                            }

                            val content = if (state.isPaused) {
                                "Istirahat dijeda (Sisa $timeStr) • Berikutnya: $nextTitle"
                            } else {
                                "Berikutnya: $nextTitle • Gerakan ${state.exerciseIndex + 1}/${state.totalExercises}"
                            }

                            val targetTime = if (!state.isPaused) {
                                System.currentTimeMillis() + (state.remainingSeconds * 1000L)
                            } else null

                            updateNotification(
                                title = title,
                                content = content,
                                isPaused = state.isPaused,
                                targetEndTimeMs = targetTime
                            )
                        }
                    }
                    is WorkoutTimerState.Completed -> {
                        if (lastNotifiedPhase != "completed") {
                            lastNotifiedPhase = "completed"
                            workoutRepository.markRoutinePerformed(state.routineId)

                            val totalMin = state.totalDurationSeconds / 60
                            updateNotification(
                                title = "Latihan Selesai! 🎉",
                                content = "${state.routineName} • Selesai dalam $totalMin menit",
                                isPaused = false,
                                targetEndTimeMs = null,
                                isCompleted = true
                            )
                            if (wakeLock?.isHeld == true) {
                                wakeLock?.release()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateNotification(
        title: String,
        content: String,
        isPaused: Boolean,
        targetEndTimeMs: Long? = null,
        isCompleted: Boolean = false
    ) {
        val notification = buildNotification(title, content, isPaused, targetEndTimeMs, isCompleted)
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildInitialNotification(): Notification {
        return buildNotification("Memulai Latihan...", "Menyiapkan timer...", isPaused = false)
    }

    private fun buildNotification(
        title: String,
        content: String,
        isPaused: Boolean,
        targetEndTimeMs: Long? = null,
        isCompleted: Boolean = false
    ): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOngoing(!isCompleted)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(openAppPendingIntent)

        if (targetEndTimeMs != null && !isPaused && !isCompleted) {
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(targetEndTimeMs)
            builder.setShowWhen(true)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }

        if (!isCompleted) {
            // Action 1: Pause / Resume (Request code terpisah 10 & 11)
            val (playPauseAction, playPauseCode) = if (isPaused) {
                ACTION_RESUME to 11
            } else {
                ACTION_PAUSE to 10
            }
            val playPauseIntent = Intent(playPauseAction).setPackage(packageName)
            val playPausePendingIntent = PendingIntent.getBroadcast(
                this,
                playPauseCode,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseTitle = if (isPaused) "Lanjutkan" else "Jeda"
            val playPauseIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            builder.addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)

            // Action 2: Skip (Request code 20)
            val skipIntent = Intent(ACTION_SKIP).setPackage(packageName)
            val skipPendingIntent = PendingIntent.getBroadcast(
                this,
                20,
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_next, "Lewati", skipPendingIntent)

            // Action 3: Stop (Request code 30)
            val stopIntent = Intent(ACTION_STOP).setPackage(packageName)
            val stopPendingIntent = PendingIntent.getBroadcast(
                this,
                30,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Berhenti", stopPendingIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Workout Timer Aktif",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Status sesi latihan aktif dan kontrol timer"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            unregisterReceiver(notificationActionReceiver)
        } catch (_: Exception) { }
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (_: Exception) { }
        }
        if (isServiceForeground) {
            try {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) { }
            isServiceForeground = false
        }
        // Release ToneGenerator native resource untuk mencegah memory leak
        audioHapticManager.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
