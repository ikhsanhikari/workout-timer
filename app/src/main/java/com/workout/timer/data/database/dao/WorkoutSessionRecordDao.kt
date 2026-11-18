package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.WorkoutSessionRecord
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk mengelola entitas [WorkoutSessionRecord] (Log Sesi Latihan).
 */
@Dao
interface WorkoutSessionRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WorkoutSessionRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<WorkoutSessionRecord>): List<Long>

    @Delete
    suspend fun delete(record: WorkoutSessionRecord)

    @Query("DELETE FROM workout_session_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_session_records")
    suspend fun clearAll()

    /** Mengamati seluruh riwayat sesi latihan dari yang paling baru. */
    @Query("SELECT * FROM workout_session_records ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<WorkoutSessionRecord>>

    /** Mengamati n-sesi latihan terakhir. */
    @Query("SELECT * FROM workout_session_records ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WorkoutSessionRecord>>

    /**
     * Hitung total detik latihan dalam kurun waktu minggu ini (completedAt >= weekStartMs).
     */
    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0)
        FROM workout_session_records
        WHERE completedAt >= :weekStartMs
    """)
    fun observeWeeklyTotalSeconds(weekStartMs: Long): Flow<Int>

    /**
     * Hitung total detik latihan dalam rentang waktu tertentu (misal: minggu lalu).
     */
    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0)
        FROM workout_session_records
        WHERE completedAt >= :startMs AND completedAt < :endMs
    """)
    fun observeTotalSecondsInRange(startMs: Long, endMs: Long): Flow<Int>

    /**
     * Hitung jumlah total sesi latihan dalam kurun waktu minggu ini.
     */
    @Query("""
        SELECT COUNT(*)
        FROM workout_session_records
        WHERE completedAt >= :weekStartMs
    """)
    fun observeWeeklySessionCount(weekStartMs: Long): Flow<Int>

    /**
     * Hitung total detik latihan sepanjang masa (all-time).
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM workout_session_records")
    fun observeAllTimeTotalSeconds(): Flow<Int>

    /**
     * Hitung jumlah sesi latihan sepanjang masa (all-time).
     */
    @Query("SELECT COUNT(*) FROM workout_session_records")
    fun observeAllTimeSessionCount(): Flow<Int>

    /** Ambil semua rekaman untuk keperluan export backup. */
    @Query("SELECT * FROM workout_session_records ORDER BY completedAt ASC")
    suspend fun getAll(): List<WorkoutSessionRecord>

    @Query("SELECT COUNT(*) FROM workout_session_records")
    suspend fun count(): Int
}

