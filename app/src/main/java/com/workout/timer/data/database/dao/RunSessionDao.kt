package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.RunSession
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RunSession): Long

    @Update
    suspend fun update(session: RunSession)

    @Query("SELECT * FROM run_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<RunSession>>

    @Query("SELECT * FROM run_sessions WHERE id = :id")
    suspend fun getById(id: Long): RunSession?

    @Query("SELECT * FROM run_sessions WHERE status = 'running' OR status = 'paused' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): RunSession?

    @Query("DELETE FROM run_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT SUM(distanceMeters) FROM run_sessions WHERE status = 'completed'")
    fun getTotalDistanceMeters(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM run_sessions WHERE status = 'completed'")
    fun getTotalCompletedRuns(): Flow<Int>

    @Query("SELECT SUM(durationSeconds) FROM run_sessions WHERE status = 'completed'")
    fun getTotalDurationSeconds(): Flow<Int?>

    @Query("SELECT SUM(caloriesBurned) FROM run_sessions WHERE status = 'completed'")
    fun getTotalCaloriesBurned(): Flow<Int?>
}
