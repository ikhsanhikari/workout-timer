package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.WorkoutBlock
import com.workout.timer.data.model.BlockWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: WorkoutBlock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<WorkoutBlock>): List<Long>

    @Update
    suspend fun update(block: WorkoutBlock)

    @Delete
    suspend fun delete(block: WorkoutBlock)

    @Query("DELETE FROM workout_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_blocks WHERE routineId = :routineId")
    suspend fun deleteAllByRoutineId(routineId: Long)

    @Query("SELECT * FROM workout_blocks WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getByRoutineId(routineId: Long): List<WorkoutBlock>

    @Query("SELECT * FROM workout_blocks WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun observeByRoutineId(routineId: Long): Flow<List<WorkoutBlock>>

    @Transaction
    @Query("SELECT * FROM workout_blocks WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun observeBlocksWithExercises(routineId: Long): Flow<List<BlockWithExercises>>

    /** Perbarui orderIndex semua blok secara batch (untuk drag-and-drop). */
    @Query("UPDATE workout_blocks SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrder(id: Long, orderIndex: Int)

    @Query("UPDATE workout_blocks SET repeatCount = :repeatCount WHERE id = :id")
    suspend fun updateRepeatCount(id: Long, repeatCount: Int)

    @Query("SELECT * FROM workout_blocks")
    suspend fun getAll(): List<WorkoutBlock>

    @Query("DELETE FROM workout_blocks")
    suspend fun deleteAll()
}
