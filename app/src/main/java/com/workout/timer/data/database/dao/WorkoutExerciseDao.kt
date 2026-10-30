package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.WorkoutExercise
import com.workout.timer.data.model.ExerciseWithTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: WorkoutExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<WorkoutExercise>): List<Long>

    @Update
    suspend fun update(exercise: WorkoutExercise)

    @Delete
    suspend fun delete(exercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_exercises WHERE blockId = :blockId")
    suspend fun deleteAllByBlockId(blockId: Long)

    @Query("SELECT * FROM workout_exercises WHERE blockId = :blockId ORDER BY orderIndex ASC")
    suspend fun getByBlockId(blockId: Long): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises WHERE blockId = :blockId ORDER BY orderIndex ASC")
    fun observeByBlockId(blockId: Long): Flow<List<WorkoutExercise>>

    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE blockId = :blockId ORDER BY orderIndex ASC")
    fun observeWithTemplates(blockId: Long): Flow<List<ExerciseWithTemplate>>

    /** Perbarui orderIndex untuk drag-and-drop reordering dalam blok. */
    @Query("UPDATE workout_exercises SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrder(id: Long, orderIndex: Int)

    @Query("UPDATE workout_exercises SET customMediaUri = :uri, customMediaType = :type WHERE id = :id")
    suspend fun updateMedia(id: Long, uri: String?, type: String)

    @Query("UPDATE workout_exercises SET durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun updateDuration(id: Long, durationSeconds: Int)

    /**
     * Untuk garbage collection media: cek apakah mediaUri tertentu
     * masih direferensikan oleh WorkoutExercise mana pun.
     */
    @Query("SELECT COUNT(*) FROM workout_exercises WHERE customMediaUri = :mediaUri")
    suspend fun countByMediaUri(mediaUri: String): Int

    /**
     * Ambil semua exercise dalam satu rutinitas (lintas semua blok).
     * Digunakan untuk menghitung total durasi dan jumlah gerakan.
     */
    @Query("""
        SELECT we.* FROM workout_exercises we
        INNER JOIN workout_blocks wb ON we.blockId = wb.id
        WHERE wb.routineId = :routineId
        ORDER BY wb.orderIndex ASC, we.orderIndex ASC
    """)
    suspend fun getAllByRoutineId(routineId: Long): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises")
    suspend fun getAll(): List<WorkoutExercise>

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAll()
}
