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
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.workout.timer.MainActivity
import com.workout.timer.R
import com.workout.timer.data.model.LocationPoint
import com.workout.timer.data.repository.RunRepository
import com.workout.timer.service.location.LocationTracker
import com.workout.timer.service.location.RunStatsCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * State live tracking lari/jalan yang dapat diobservasi oleh UI.
 */
data class RunTrackingState(
    val sessionId: Long = 0L,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Int = 0,
    val currentPaceSecPerKm: Int = 0,
    val bestPaceSecPerKm: Int = 0,
    val caloriesBurned: Int = 0,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val currentAltitude: Double = 0.0,
    val currentSpeedKmh: Float = 0f,
    val routePoints: List<Pair<Double, Double>> = emptyList()
)

/**
 * Foreground Service untuk pelacakan GPS lari/jalan (Strava-like).
 * Menjamin pelacakan tetap akurat saat layar mati atau aplikasi diminimize.
 */
@AndroidEntryPoint
class RunTrackingService : Service() {

    @Inject
    lateinit var locationTracker: LocationTracker

    @Inject
    lateinit var runRepository: RunRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private var isServiceForeground = false

    private val locationPointBuffer = mutableListOf<LocationPoint>()
    private var lastRecordedPoint: LocationPoint? = null
    private val recentSpeeds = mutableListOf<Float>()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "run_tracking_active_v1"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.workout.timer.run.ACTION_START"
        const val ACTION_PAUSE = "com.workout.timer.run.ACTION_PAUSE"
        const val ACTION_RESUME = "com.workout.timer.run.ACTION_RESUME"
        const val ACTION_STOP = "com.workout.timer.run.ACTION_STOP"

        const val EXTRA_SESSION_ID = "extra_session_id"

        private val _trackingState = MutableStateFlow(RunTrackingState())
        val trackingState: StateFlow<RunTrackingState> = _trackingState.asStateFlow()

        fun startIntent(context: Context, sessionId: Long = 0L): Intent =
            Intent(context, RunTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }

        fun pauseIntent(context: Context): Intent =
            Intent(context, RunTrackingService::class.java).apply { action = ACTION_PAUSE }

        fun resumeIntent(context: Context): Intent =
            Intent(context, RunTrackingService::class.java).apply { action = ACTION_RESUME }

        fun stopIntent(context: Context): Intent =
            Intent(context, RunTrackingService::class.java).apply { action = ACTION_STOP }
    }

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE -> pauseTracking()
                ACTION_RESUME -> resumeTracking()
                ACTION_STOP -> stopTracking()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_STOP)
        }
        ContextCompat.registerReceiver(
            this,
            actionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        initWakeLock()
    }

    @SuppressLint("WakelockTimeout")
    private fun initWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RunTracking:ServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val incomingSessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
                startTracking(incomingSessionId)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking(initialSessionId: Long) {
        if (_trackingState.value.isTracking && !_trackingState.value.isPaused) {
            return
        }

        wakeLock?.acquire()

        serviceScope.launch {
            val sessionId = if (initialSessionId > 0L) {
                initialSessionId
            } else {
                runRepository.startSession()
            }

            _trackingState.value = RunTrackingState(
                sessionId = sessionId,
                isTracking = true,
                isPaused = false
            )

            withContext(Dispatchers.Main) {
                startForegroundNotification()
                startGpsTracking()
                startTimerTicker()
            }
        }
    }

    private fun startGpsTracking() {
        locationTracker.startLocationUpdates(intervalMs = 3000L, minDistanceMeters = 2.0f) { location ->
            handleNewLocation(location)
        }
    }

    private fun handleNewLocation(location: Location) {
        val currentState = _trackingState.value
        if (!currentState.isTracking || currentState.isPaused) {
            // Jika pause, hanya update koordinat kamera tanpa menambah jarak
            _trackingState.update {
                it.copy(
                    currentLat = location.latitude,
                    currentLng = location.longitude,
                    currentAltitude = location.altitude
                )
            }
            return
        }

        val point = LocationPoint(
            sessionId = currentState.sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis()
        )

        var distanceDelta = 0.0
        val lastPoint = lastRecordedPoint
        if (lastPoint != null) {
            val delta = RunStatsCalculator.calculateDistanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                point.latitude, point.longitude
            )
            // Filter anomali GPS teleport jump (> 80 meter dalam 3 detik ≈ 96 km/h)
            if (delta in 1.5..80.0) {
                distanceDelta = delta.toDouble()
            }
        } else {
            // Titik pertama
            distanceDelta = 0.0
        }

        lastRecordedPoint = point

        // Auto-pause sampling
        recentSpeeds.add(location.speed)
        if (recentSpeeds.size > 10) recentSpeeds.removeAt(0)

        // Simpan ke buffer DB
        locationPointBuffer.add(point)
        if (locationPointBuffer.size >= 5) {
            flushPointsToDatabase()
        }

        _trackingState.update { state ->
            val newTotalDistance = state.distanceMeters + distanceDelta
            val pace = RunStatsCalculator.calculatePaceSecPerKm(newTotalDistance, state.durationSeconds)
            val bestPace = if (state.bestPaceSecPerKm == 0 || (pace in 1 until state.bestPaceSecPerKm)) {
                pace
            } else {
                state.bestPaceSecPerKm
            }
            val calories = RunStatsCalculator.estimateCalories(newTotalDistance, state.durationSeconds)
            val newPoints = state.routePoints + Pair(point.latitude, point.longitude)

            state.copy(
                distanceMeters = newTotalDistance,
                currentPaceSecPerKm = pace,
                bestPaceSecPerKm = bestPace,
                caloriesBurned = calories,
                currentLat = location.latitude,
                currentLng = location.longitude,
                currentAltitude = location.altitude,
                currentSpeedKmh = location.speed * 3.6f,
                routePoints = newPoints
            )
        }

        updateNotification()
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (_trackingState.value.isTracking && !_trackingState.value.isPaused) {
                    _trackingState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
                    // Update notification setiap detik
                    if (_trackingState.value.durationSeconds % 2 == 0) {
                        updateNotification()
                    }
                }
            }
        }
    }

    private fun pauseTracking() {
        _trackingState.update { it.copy(isPaused = true) }
        updateNotification()
    }

    private fun resumeTracking() {
        _trackingState.update { it.copy(isPaused = false) }
        updateNotification()
    }

    private fun stopTracking() {
        timerJob?.cancel()
        timerJob = null
        locationTracker.stopLocationUpdates()

        val finalState = _trackingState.value
        serviceScope.launch {
            flushPointsToDatabase()
            if (finalState.sessionId > 0L) {
                runRepository.finishSession(
                    id = finalState.sessionId,
                    distanceMeters = finalState.distanceMeters,
                    durationSeconds = finalState.durationSeconds,
                    avgPaceSecPerKm = finalState.currentPaceSecPerKm,
                    bestPaceSecPerKm = finalState.bestPaceSecPerKm,
                    caloriesBurned = finalState.caloriesBurned
                )
            }
            _trackingState.value = RunTrackingState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            isServiceForeground = false
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            stopSelf()
        }
    }

    private fun flushPointsToDatabase() {
        if (locationPointBuffer.isEmpty()) return
        val toSave = locationPointBuffer.toList()
        locationPointBuffer.clear()
        serviceScope.launch(Dispatchers.IO) {
            try {
                runRepository.addLocationPoints(toSave)
            } catch (e: Exception) {
                Log.e("RunTrackingService", "Gagal menyimpan titik lokasi: ${e.message}")
            }
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isServiceForeground = true
    }

    private fun updateNotification() {
        if (!isServiceForeground) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = _trackingState.value
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (state.isPaused) {
            val resumeIntent = Intent(ACTION_RESUME).setPackage(packageName)
            val pendingResume = PendingIntent.getBroadcast(
                this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Lanjut",
                pendingResume
            ).build()
        } else {
            val pauseIntent = Intent(ACTION_PAUSE).setPackage(packageName)
            val pendingPause = PendingIntent.getBroadcast(
                this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Jeda",
                pendingPause
            ).build()
        }

        val stopIntent = Intent(ACTION_STOP).setPackage(packageName)
        val pendingStop = PendingIntent.getBroadcast(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Selesai",
            pendingStop
        ).build()

        val distStr = RunStatsCalculator.formatDistanceKm(state.distanceMeters) + " km"
        val durStr = RunStatsCalculator.formatDuration(state.durationSeconds)
        val paceStr = RunStatsCalculator.formatPace(state.currentPaceSecPerKm) + "/km"
        val statusPrefix = if (state.isPaused) "[Jeda] " else ""

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${statusPrefix}GasCount — Lari / Jalan")
            .setContentText("$distStr • $durStr • Pace $paceStr")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Pelacakan Lari / Jalan (GPS)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status live tracking jarak, durasi, dan pace GPS."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopLocationUpdates()
        timerJob?.cancel()
        serviceScope.cancel()
        try {
            unregisterReceiver(actionReceiver)
        } catch (_: Exception) {}
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
