package com.workout.timer.service.timer

import android.os.SystemClock
import com.workout.timer.data.model.FlatExerciseItem
import com.workout.timer.data.model.IntervalType
import com.workout.timer.service.audio.AudioHapticManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * WorkoutTimerEngine — Mesin Timer Utama Presisi Tinggi (Zero-Drift) (PRD 3. F-06 & 7.1).
 *
 * Menggunakan [SystemClock.elapsedRealtime] untuk menghitung sisa waktu secara absolut,
 * sehingga tidak terpengaruh oleh jitter coroutine delay atau CPU sleep saat Doze Mode.
 * Akurasi memenuhi standar NFR-1 (drift < 50ms per jam).
 */
@Singleton
class WorkoutTimerEngine @Inject constructor(
    private val audioHapticManager: AudioHapticManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private val _timerState = MutableStateFlow<WorkoutTimerState>(WorkoutTimerState.Idle)
    val timerState: StateFlow<WorkoutTimerState> = _timerState.asStateFlow()

    // Data sesi aktif
    private var activeRoutineId: Long = -1L
    private var activeRoutineName: String = ""
    private var exercises: List<FlatExerciseItem> = emptyList()
    private var currentExerciseIndex: Int = 0

    // Waktu & status
    private var isPaused: Boolean = false
    private var pausedAt: Long = 0L
    private var stepStartTime: Long = 0L
    private var stepEndTime: Long = 0L
    private var stepTotalDurationMs: Long = 0L
    private var workoutStartElapsedRealtime: Long = 0L
    private var totalPausedDurationMs: Long = 0L

    // Tracker beep agar tidak berulang dalam 1 detik yang sama
    private var lastBeepSecond: Int = -1

    /**
     * Memulai sesi latihan dari daftar gerakan yang sudah di-flatten.
     */
    fun startWorkout(
        routineId: Long,
        routineName: String,
        flatExercises: List<FlatExerciseItem>
    ) {
        if (flatExercises.isEmpty()) return

        stop()

        activeRoutineId = routineId
        activeRoutineName = routineName
        exercises = flatExercises
        currentExerciseIndex = 0
        isPaused = false
        totalPausedDurationMs = 0L
        workoutStartElapsedRealtime = SystemClock.elapsedRealtime()

        startPreparationPhase()
    }

    /**
     * Fase hitung mundur persiapan 3 - 2 - 1 - GO.
     */
    private fun startPreparationPhase() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var prepSeconds = 3
            val firstExercise = exercises.firstOrNull()

            while (prepSeconds > 0 && isActive) {
                _timerState.value = WorkoutTimerState.Preparing(
                    countdownSeconds = prepSeconds,
                    routineId = activeRoutineId,
                    routineName = activeRoutineName,
                    firstExercise = firstExercise
                )
                audioHapticManager.playCountdownBeep()
                delay(1000)
                prepSeconds--
            }

            if (isActive) {
                audioHapticManager.playStartTone()
                startExerciseAt(0)
            }
        }
    }

    /**
     * Memulai eksekusi gerakan pada indeks tertentu.
     */
    private fun startExerciseAt(index: Int) {
        if (index !in exercises.indices) {
            completeWorkout()
            return
        }

        currentExerciseIndex = index
        val currentEx = exercises[index]
        stepTotalDurationMs = currentEx.durationSeconds * 1000L
        stepStartTime = SystemClock.elapsedRealtime()
        stepEndTime = stepStartTime + stepTotalDurationMs
        isPaused = false
        lastBeepSecond = -1

        // Play cue suara transisi fase
        if (currentEx.type == IntervalType.REST) {
            audioHapticManager.playRestTone()
        }

        runTimerLoop()
    }

    /**
     * Loop timer 50ms untuk animasi mulus 60fps & akurasi zero-drift.
     */
    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                if (isPaused) {
                    delay(100)
                    continue
                }

                val now = SystemClock.elapsedRealtime()
                val remainingMs = max(0L, stepEndTime - now)
                val remainingSeconds = (remainingMs + 999) / 1000 // Ceil second
                val elapsedInStepMs = stepTotalDurationMs - remainingMs
                val progress = if (stepTotalDurationMs > 0) {
                    (elapsedInStepMs.toFloat() / stepTotalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }

                val totalElapsedSec = ((now - workoutStartElapsedRealtime - totalPausedDurationMs) / 1000).toInt()
                val currentEx = exercises.getOrNull(currentExerciseIndex) ?: break
                val nextEx = exercises.getOrNull(currentExerciseIndex + 1)

                // Trigger beep 3, 2, 1 sebelum gerakan selesai
                val remSecInt = remainingSeconds.toInt()
                if (remSecInt in 1..3 && remSecInt != lastBeepSecond) {
                    lastBeepSecond = remSecInt
                    audioHapticManager.playCountdownBeep()
                }

                // Update UI state
                if (currentEx.type == IntervalType.REST) {
                    _timerState.value = WorkoutTimerState.Resting(
                        routineId = activeRoutineId,
                        routineName = activeRoutineName,
                        exerciseIndex = currentExerciseIndex,
                        totalExercises = exercises.size,
                        currentExercise = currentEx,
                        upcomingExercise = nextEx,
                        remainingSeconds = remSecInt,
                        totalSeconds = (stepTotalDurationMs / 1000).toInt(),
                        progress = progress,
                        totalElapsedSeconds = totalElapsedSec,
                        isPaused = isPaused
                    )
                } else {
                    _timerState.value = WorkoutTimerState.Running(
                        routineId = activeRoutineId,
                        routineName = activeRoutineName,
                        exerciseIndex = currentExerciseIndex,
                        totalExercises = exercises.size,
                        currentExercise = currentEx,
                        nextExercise = nextEx,
                        remainingSeconds = remSecInt,
                        totalSeconds = (stepTotalDurationMs / 1000).toInt(),
                        progress = progress,
                        totalElapsedSeconds = totalElapsedSec,
                        isPaused = isPaused
                    )
                }

                // Cek apakah waktu gerakan sudah habis
                if (remainingMs <= 0) {
                    val nextIndex = currentExerciseIndex + 1
                    if (nextIndex < exercises.size) {
                        val nextExercise = exercises[nextIndex]
                        if (nextExercise.type != IntervalType.REST) {
                            audioHapticManager.playStartTone()
                        }
                        startExerciseAt(nextIndex)
                    } else {
                        completeWorkout()
                    }
                    break
                }

                delay(50) // 50ms tick rate
            }
        }
    }

    /**
     * Pause timer.
     */
    fun pause() {
        if (isPaused) return
        isPaused = true
        pausedAt = SystemClock.elapsedRealtime()
        updateCurrentStatePausedFlag(true)
    }

    /**
     * Resume timer dari kondisi pause.
     */
    fun resume() {
        if (!isPaused) return
        val pauseDuration = SystemClock.elapsedRealtime() - pausedAt
        stepEndTime += pauseDuration
        totalPausedDurationMs += pauseDuration
        isPaused = false
        updateCurrentStatePausedFlag(false)
    }

    /**
     * Toggle Pause / Resume.
     */
    fun togglePause() {
        if (isPaused) resume() else pause()
    }

    /**
     * Lewati gerakan saat ini (Skip).
     */
    fun skip() {
        val nextIndex = currentExerciseIndex + 1
        if (nextIndex < exercises.size) {
            audioHapticManager.vibrateLightTick()
            startExerciseAt(nextIndex)
        } else {
            completeWorkout()
        }
    }

    /**
     * Kembali ke gerakan sebelumnya (Previous).
     * Jika sudah berjalan > 2 detik, restart gerakan sekarang.
     */
    fun previous() {
        val now = SystemClock.elapsedRealtime()
        val elapsedInStep = now - stepStartTime
        if (elapsedInStep > 2000L || currentExerciseIndex == 0) {
            startExerciseAt(currentExerciseIndex)
        } else {
            startExerciseAt(currentExerciseIndex - 1)
        }
    }

    /**
     * Tambah waktu istirahat secara dinamis (+15 detik) on-the-fly (PRD 4.2.3).
     */
    fun addRestTime(seconds: Int = 15) {
        val extraMs = seconds * 1000L
        stepTotalDurationMs += extraMs
        stepEndTime += extraMs
        audioHapticManager.vibrateLightTick()
    }

    /**
     * Selesaikan sesi latihan sepenuhnya.
     */
    private fun completeWorkout() {
        timerJob?.cancel()
        val totalSec = ((SystemClock.elapsedRealtime() - workoutStartElapsedRealtime - totalPausedDurationMs) / 1000).toInt()
        _timerState.value = WorkoutTimerState.Completed(
            routineId = activeRoutineId,
            routineName = activeRoutineName,
            totalDurationSeconds = totalSec,
            totalExercisesCompleted = exercises.size
        )
        audioHapticManager.playWorkoutCompleteTone()
    }

    /**
     * Menghentikan latihan dan me-reset ke Idle.
     */
    fun stop() {
        timerJob?.cancel()
        timerJob = null
        isPaused = false
        _timerState.value = WorkoutTimerState.Idle
    }

    private fun updateCurrentStatePausedFlag(paused: Boolean) {
        when (val state = _timerState.value) {
            is WorkoutTimerState.Running -> _timerState.value = state.copy(isPaused = paused)
            is WorkoutTimerState.Resting -> _timerState.value = state.copy(isPaused = paused)
            else -> {}
        }
    }
}
