package com.workout.timer.service.timer

import com.workout.timer.data.model.FlatExerciseItem
import com.workout.timer.data.model.IntervalType

/**
 * Representasi State Mesin Timer Latihan (F-06).
 */
sealed interface WorkoutTimerState {

    /** Kondisi timer belum berjalan / di-reset */
    data object Idle : WorkoutTimerState

    /** Hitung mundur persiapan 3 - 2 - 1 - GO sebelum mulai */
    data class Preparing(
        val countdownSeconds: Int,
        val routineId: Long,
        val routineName: String,
        val firstExercise: FlatExerciseItem?
    ) : WorkoutTimerState

    /** Sedang melakukan gerakan (WARMUP, WORK, atau COOLDOWN) */
    data class Running(
        val routineId: Long,
        val routineName: String,
        val exerciseIndex: Int,
        val totalExercises: Int,
        val currentExercise: FlatExerciseItem,
        val nextExercise: FlatExerciseItem?,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val progress: Float,
        val totalElapsedSeconds: Int,
        val isPaused: Boolean
    ) : WorkoutTimerState

    /** Sedang fase istirahat (REST) — Menampilkan preview gerakan yang akan datang */
    data class Resting(
        val routineId: Long,
        val routineName: String,
        val exerciseIndex: Int,
        val totalExercises: Int,
        val currentExercise: FlatExerciseItem,
        val upcomingExercise: FlatExerciseItem?,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val progress: Float,
        val totalElapsedSeconds: Int,
        val isPaused: Boolean
    ) : WorkoutTimerState

    /** Sesi latihan telah diselesaikan sepenuhnya */
    data class Completed(
        val routineId: Long,
        val routineName: String,
        val totalDurationSeconds: Int,
        val totalExercisesCompleted: Int
    ) : WorkoutTimerState
}
