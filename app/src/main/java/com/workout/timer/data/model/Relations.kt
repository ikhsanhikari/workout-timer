package com.workout.timer.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relasi agregat: satu [WorkoutExercise] beserta [ExerciseTemplate]-nya (opsional).
 *
 * Digunakan di Workout Builder untuk menampilkan preview media
 * dan di Player untuk pre-loading video berikutnya.
 */
data class ExerciseWithTemplate(
    @Embedded val exercise: WorkoutExercise,
    @Relation(
        parentColumn = "templateId",
        entityColumn = "id"
    )
    val template: ExerciseTemplate?
) {
    /**
     * Resolusi media yang efektif:
     * Prioritas: customMediaUri (override) > template.mediaUri > null
     */
    val resolvedMediaUri: String?
        get() = exercise.customMediaUri ?: template?.mediaUri

    val resolvedMediaType: MediaType
        get() = when {
            exercise.customMediaUri != null -> exercise.customMediaType
            template?.mediaUri != null -> template.mediaType
            else -> MediaType.NONE
        }

    val resolvedThumbnailUri: String?
        get() = template?.thumbnailUri
}

/**
 * Relasi agregat: satu [WorkoutBlock] beserta semua [ExerciseWithTemplate]-nya,
 * sudah diurutkan berdasarkan [WorkoutExercise.orderIndex].
 */
data class BlockWithExercises(
    @Embedded val block: WorkoutBlock,
    @Relation(
        parentColumn = "id",
        entityColumn = "blockId",
        entity = WorkoutExercise::class
    )
    val exercises: List<WorkoutExercise>
) {
    /** Total durasi satu iterasi blok ini (dalam detik). */
    val singleIterationDurationSec: Int
        get() = exercises.sumOf { it.durationSeconds }

    /** Total durasi blok termasuk semua pengulangan (dalam detik). */
    val totalDurationSec: Int
        get() = singleIterationDurationSec * block.repeatCount
}

/**
 * Relasi agregat penuh: satu [WorkoutRoutine] beserta semua [BlockWithExercises]-nya.
 * Ini adalah model utama yang digunakan oleh Player dan Builder.
 */
data class RoutineWithBlocks(
    @Embedded val routine: WorkoutRoutine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId",
        entity = WorkoutBlock::class
    )
    val blocks: List<WorkoutBlock>
)

/**
 * Model flat untuk Player: daftar semua gerakan yang sudah di-expand
 * (block looping sudah di-flatten menjadi list linier).
 *
 * Dihasilkan oleh [com.workout.timer.domain.usecase.FlattenRoutineUseCase].
 */
data class FlatExerciseItem(
    val exerciseId: Long,
    val title: String,
    val durationSeconds: Int,
    val type: IntervalType,
    val mediaUri: String?,
    val mediaType: MediaType,
    val thumbnailUri: String?,
    /** Nomor set saat ini (1-based) jika berada dalam loop block, null jika bukan. */
    val currentSet: Int? = null,
    /** Total set dalam block ini, null jika bukan loop block. */
    val totalSets: Int? = null
)
