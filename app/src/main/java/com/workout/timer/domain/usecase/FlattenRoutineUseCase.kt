package com.workout.timer.domain.usecase

import com.workout.timer.data.model.*
import com.workout.timer.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use Case: FlattenRoutineUseCase
 *
 * Mengubah struktur hierarkis (Routine → Blocks → Exercises dengan repeatCount)
 * menjadi list linier [FlatExerciseItem] yang siap dikonsumsi oleh Workout Player.
 *
 * Contoh:
 * Block A (repeatCount=3) { Push Up 30s, Rest 15s }
 *
 * → [Push Up Set1, Rest Set1, Push Up Set2, Rest Set2, Push Up Set3, Rest Set3]
 *
 * Ini adalah operasi CPU-bound murni — tidak ada I/O.
 */
class FlattenRoutineUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    /**
     * Hasilkan flat list dari semua gerakan dalam rutinitas.
     *
     * @param routineId ID rutinitas yang akan di-flatten.
     * @return List [FlatExerciseItem] berurutan siap diputar Player.
     */
    suspend operator fun invoke(routineId: Long): List<FlatExerciseItem> {
        val blocksWithExercises = repository.observeBlocksWithExercises(routineId).first()
        val sortedBlocks = blocksWithExercises.sortedBy { it.block.orderIndex }

        return buildList {
            for (blockWithExercises in sortedBlocks) {
                val block = blockWithExercises.block
                val exercises = blockWithExercises.exercises.sortedBy { it.orderIndex }

                repeat(block.repeatCount) { setIndex ->
                    for (exercise in exercises) {
                        val template = if (exercise.templateId != null) {
                            repository.getTemplateById(exercise.templateId)
                        } else {
                            repository.getTemplateByName(exercise.title)
                        }

                        val resolvedMediaUri = exercise.customMediaUri ?: template?.mediaUri
                        val resolvedMediaType = if (exercise.customMediaUri != null) {
                            exercise.customMediaType
                        } else {
                            template?.mediaType ?: MediaType.NONE
                        }

                        add(
                            FlatExerciseItem(
                                exerciseId = exercise.id,
                                title = exercise.title,
                                durationSeconds = exercise.durationSeconds,
                                type = exercise.type,
                                mediaUri = resolvedMediaUri,
                                mediaType = resolvedMediaType,
                                thumbnailUri = template?.thumbnailUri,
                                currentSet = if (block.repeatCount > 1) setIndex + 1 else null,
                                totalSets = if (block.repeatCount > 1) block.repeatCount else null,
                                targetMuscle = template?.targetMuscle,
                                coachTip = template?.coachTip,
                                category = template?.category,
                                difficulty = template?.difficulty,
                                equipment = template?.equipment
                            )
                        )
                    }
                }
            }
        }
    }
}
