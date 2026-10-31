package com.workout.timer.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.workout.timer.data.database.WorkoutDatabase
import com.workout.timer.data.database.dao.*
import com.workout.timer.data.media.MediaFileManager
import com.workout.timer.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk semua operasi data latihan (Routine, Block, Exercise).
 *
 * Layer ini adalah single source of truth antara ViewModel dan database.
 * Semua operasi yang melibatkan perubahan media memanggil [MediaFileManager]
 * untuk menjalankan garbage collection otomatis.
 */
@Singleton
class WorkoutRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val database: WorkoutDatabase,
    private val routineDao: WorkoutRoutineDao,
    private val blockDao: WorkoutBlockDao,
    private val exerciseDao: WorkoutExerciseDao,
    private val templateDao: ExerciseTemplateDao,
    private val mediaFileManager: MediaFileManager
) {

    // ─── WORKOUT ROUTINE ────────────────────────────────────────────────────

    fun searchRoutines(
        keyword: String? = null,
        category: String? = null,
        minDuration: Int? = null,
        maxDuration: Int? = null,
        sortBy: SortOption = SortOption.CREATED_AT
    ): Flow<List<WorkoutRoutine>> = routineDao.searchAndFilter(
        keyword = keyword?.ifBlank { null },
        category = category,
        minDuration = minDuration,
        maxDuration = maxDuration,
        sortBy = sortBy.name
    )

    fun observeAllCategories(): Flow<List<String>> = routineDao.observeAllCategories()

    fun observeWeeklyStats(weekStartMs: Long) = Pair(
        routineDao.observeWeeklyTotalSeconds(weekStartMs),
        routineDao.observeWeeklySessionCount(weekStartMs)
    )

    /** Hapus semua blok (dan semua exercises-nya via CASCADE) berdasarkan routineId. */
    suspend fun deleteAllBlocksByRoutineId(routineId: Long) {
        blockDao.deleteAllByRoutineId(routineId)
    }

    suspend fun ensureStandardRoutinesLoaded() {
        WorkoutDatabase.populateStandardData(context, database, onlyIfMissing = true)
    }

    suspend fun restoreStandardRoutines() {
        WorkoutDatabase.populateStandardData(context, database, onlyIfMissing = false)
    }

    suspend fun getRoutineById(id: Long): WorkoutRoutine? = routineDao.getById(id)


    fun observeRoutine(id: Long): Flow<WorkoutRoutine?> = routineDao.observeById(id)

    suspend fun saveRoutine(routine: WorkoutRoutine): Long = routineDao.insert(routine)

    suspend fun updateRoutine(routine: WorkoutRoutine) = routineDao.update(routine)

    /**
     * Menyimpan seluruh rutinitas (Routine, Blocks, Exercises) secara atomik
     * menggunakan Room Database Transaction dalam 1 operasi batch cepat.
     * Mencegah multiple Flow emissions dan disk I/O thrashing.
     */
    suspend fun saveFullRoutine(
        routine: WorkoutRoutine,
        blocksWithExercises: List<Pair<WorkoutBlock, List<WorkoutExercise>>>,
        isEdit: Boolean
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val savedRoutineId = if (isEdit && routine.id > 0) {
                routineDao.update(routine)
                blockDao.deleteAllByRoutineId(routine.id)
                routine.id
            } else {
                routineDao.insert(routine)
            }

            blocksWithExercises.forEachIndexed { blockIdx, (block, exercises) ->
                val savedBlockId = blockDao.insert(
                    block.copy(id = 0, routineId = savedRoutineId, orderIndex = blockIdx)
                )
                if (exercises.isNotEmpty()) {
                    val reindexedExercises = exercises.mapIndexed { exIdx, ex ->
                        ex.copy(id = 0, blockId = savedBlockId, orderIndex = exIdx)
                    }
                    exerciseDao.insertAll(reindexedExercises)
                }
            }

            savedRoutineId
        }
    }

    suspend fun deleteRoutine(routineId: Long) {
        // Room CASCADE DELETE menangani block + exercise secara otomatis.
        // Kita perlu GC media setelah delete.
        val exercises = exerciseDao.getAllByRoutineId(routineId)
        routineDao.deleteById(routineId)
        runMediaGarbageCollection(exercises)
    }

    suspend fun toggleFavorite(routineId: Long) = routineDao.toggleFavorite(routineId)

    suspend fun markRoutineCompleted(routineId: Long) {
        routineDao.updateLastPerformed(routineId, System.currentTimeMillis())
    }

    /**
     * Recalculate dan update statistik rutinitas (total durasi & jumlah gerakan).
     * Dipanggil setelah Builder menyimpan perubahan.
     */
    suspend fun recalculateRoutineStats(routineId: Long) {
        val blocks = blockDao.getByRoutineId(routineId)
        var totalDuration = 0
        var totalExercises = 0

        blocks.forEach { block ->
            val exercises = exerciseDao.getByBlockId(block.id)
            val blockDuration = exercises.sumOf { it.durationSeconds } * block.repeatCount
            totalDuration += blockDuration
            totalExercises += exercises.size
        }

        routineDao.updateStats(routineId, totalDuration, totalExercises)
    }

    // ─── WORKOUT BLOCK ──────────────────────────────────────────────────────

    suspend fun insertBlock(block: WorkoutBlock): Long = blockDao.insert(block)

    suspend fun updateBlock(block: WorkoutBlock) = blockDao.update(block)

    suspend fun deleteBlock(blockId: Long) {
        val exercises = exerciseDao.getByBlockId(blockId)
        blockDao.deleteById(blockId)
        runMediaGarbageCollection(exercises)
    }

    suspend fun reorderBlocks(blockOrderMap: Map<Long, Int>) {
        blockOrderMap.forEach { (id, order) ->
            blockDao.updateOrder(id, order)
        }
    }

    fun observeBlocksWithExercises(routineId: Long): Flow<List<BlockWithExercises>> =
        blockDao.observeBlocksWithExercises(routineId)

    // ─── WORKOUT EXERCISE ───────────────────────────────────────────────────

    suspend fun insertExercise(exercise: WorkoutExercise): Long = exerciseDao.insert(exercise)

    suspend fun updateExercise(exercise: WorkoutExercise) = exerciseDao.update(exercise)

    suspend fun deleteExercise(exerciseId: Long, mediaUri: String?) {
        exerciseDao.deleteById(exerciseId)
        if (mediaUri != null) {
            val refCountInExercises = exerciseDao.countByMediaUri(mediaUri)
            val refCountInTemplates = templateDao.countByMediaUri(mediaUri)
            mediaFileManager.deleteIfUnreferenced(mediaUri, refCountInExercises + refCountInTemplates)
        }
    }

    suspend fun reorderExercises(exerciseOrderMap: Map<Long, Int>) {
        exerciseOrderMap.forEach { (id, order) ->
            exerciseDao.updateOrder(id, order)
        }
    }

    suspend fun duplicateExercise(exerciseId: Long, targetBlockId: Long): Long? {
        val exercise = exerciseDao.getByBlockId(targetBlockId)
            .firstOrNull { it.id == exerciseId } ?: return null
        val maxOrder = exerciseDao.getByBlockId(targetBlockId).maxOfOrNull { it.orderIndex } ?: -1
        return exerciseDao.insert(
            exercise.copy(id = 0, orderIndex = maxOrder + 1)
        )
    }

    // ─── EXERCISE TEMPLATE ──────────────────────────────────────────────────

    fun searchTemplates(query: String): Flow<List<ExerciseTemplate>> =
        templateDao.searchByName(query)

    fun observeAllTemplates(): Flow<List<ExerciseTemplate>> = templateDao.observeAll()

    suspend fun saveTemplate(template: ExerciseTemplate): Long = templateDao.insert(template)

    suspend fun getOrCreateTemplate(name: String): ExerciseTemplate {
        return templateDao.searchByName(name, 1).first()
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: run {
                val id = templateDao.insert(ExerciseTemplate(name = name))
                ExerciseTemplate(id = id, name = name)
            }
    }

    suspend fun deleteTemplate(templateId: Long, mediaUri: String?) {
        templateDao.deleteById(templateId)
        if (mediaUri != null) {
            val refCountInExercises = exerciseDao.countByMediaUri(mediaUri)
            val refCountInTemplates = templateDao.countByMediaUri(mediaUri)
            mediaFileManager.deleteIfUnreferenced(mediaUri, refCountInExercises + refCountInTemplates)
        }
    }

    suspend fun incrementTemplateUsage(templateId: Long) =
        templateDao.incrementUsage(templateId)

    // ─── MEDIA OPERATIONS ───────────────────────────────────────────────────

    /**
     * Import media dari Photo Picker URI ke penyimpanan internal,
     * lalu update entitas yang bersangkutan.
     */
    suspend fun importMediaForExercise(
        exerciseId: Long,
        sourceUri: Uri,
        oldMediaUri: String?
    ): MediaFileManager.ImportResult {
        val result = mediaFileManager.importMedia(sourceUri)
        if (result is MediaFileManager.ImportResult.Success) {
            exerciseDao.updateMedia(
                id = exerciseId,
                uri = result.savedPath,
                type = result.mediaType.name
            )
            // GC old media jika ada
            if (oldMediaUri != null) {
                val refCount = exerciseDao.countByMediaUri(oldMediaUri) +
                        templateDao.countByMediaUri(oldMediaUri)
                mediaFileManager.deleteIfUnreferenced(oldMediaUri, refCount)
            }
        }
        return result
    }

    suspend fun importMediaForTemplate(
        templateId: Long,
        sourceUri: Uri,
        oldMediaUri: String?
    ): MediaFileManager.ImportResult {
        val result = mediaFileManager.importMedia(sourceUri)
        if (result is MediaFileManager.ImportResult.Success) {
            templateDao.updateMedia(
                id = templateId,
                uri = result.savedPath,
                type = result.mediaType.name,
                thumbUri = result.thumbnailPath
            )
            if (oldMediaUri != null) {
                val refCount = exerciseDao.countByMediaUri(oldMediaUri) +
                        templateDao.countByMediaUri(oldMediaUri)
                mediaFileManager.deleteIfUnreferenced(oldMediaUri, refCount)
            }
        }
        return result
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private suspend fun runMediaGarbageCollection(exercises: List<WorkoutExercise>) {
        exercises.mapNotNull { it.customMediaUri }.toSet().forEach { mediaUri ->
            val refCountInExercises = exerciseDao.countByMediaUri(mediaUri)
            val refCountInTemplates = templateDao.countByMediaUri(mediaUri)
            mediaFileManager.deleteIfUnreferenced(mediaUri, refCountInExercises + refCountInTemplates)
        }
    }

    /** Catat waktu rutinitas terakhir dilakukan untuk Activity Rings dan sorting. */
    suspend fun markRoutinePerformed(routineId: Long, timestamp: Long = System.currentTimeMillis()) {
        routineDao.updateLastPerformed(routineId, timestamp)
    }

    suspend fun getTemplateById(id: Long): ExerciseTemplate? = templateDao.getById(id)

    suspend fun getTemplateByName(name: String): ExerciseTemplate? = templateDao.getByName(name)
}

/** Opsi pengurutan untuk daftar rutinitas di Home Dashboard (F-03). */
enum class SortOption {
    CREATED_AT,   // Default: terbaru dibuat
    FAVORITE,     // Favorit di atas
    LAST_PERFORMED, // Terakhir dilakukan
    DURATION      // Durasi tercepat
}
