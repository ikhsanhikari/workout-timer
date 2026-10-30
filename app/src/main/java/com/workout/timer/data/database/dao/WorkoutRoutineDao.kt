package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.WorkoutRoutine
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk tabel [WorkoutRoutine].
 *
 * Semua query pencarian menggunakan FTS-compatible LIKE patterns
 * dan berjalan pada index yang sudah didefinisikan di entity,
 * sehingga memenuhi NFR kecepatan < 16ms.
 */
@Dao
interface WorkoutRoutineDao {

    // ─── INSERT / UPDATE / DELETE ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: WorkoutRoutine): Long

    @Update
    suspend fun update(routine: WorkoutRoutine)

    @Delete
    suspend fun delete(routine: WorkoutRoutine)

    @Query("DELETE FROM workout_routines WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ─── SINGLE QUERIES ──────────────────────────────────────────────────────

    @Query("SELECT * FROM workout_routines WHERE id = :id")
    suspend fun getById(id: Long): WorkoutRoutine?

    @Query("SELECT * FROM workout_routines WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): WorkoutRoutine?

    @Query("SELECT * FROM workout_routines WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutRoutine?>

    // ─── MULTI-FILTER SEARCH (F-03) ──────────────────────────────────────────

    /**
     * Query pencarian multi-filter yang menggabungkan:
     * - Keyword search (LIKE pada name)
     * - Filter kategori (EXACT match)
     * - Filter durasi (range totalDurationSec)
     *
     * Parameter null berarti filter tersebut tidak aktif (diabaikan).
     * Sorting dilakukan di layer Repository karena memerlukan kombinasi
     * logika yang tidak bisa di-express murni di SQL tanpa dynamic query.
     */
    @Query("""
        SELECT * FROM workout_routines
        WHERE (:keyword IS NULL OR name LIKE '%' || :keyword || '%')
        AND (:category IS NULL OR category = :category)
        AND (:minDuration IS NULL OR totalDurationSec >= :minDuration)
        AND (:maxDuration IS NULL OR totalDurationSec <= :maxDuration)
        ORDER BY
            CASE WHEN :sortBy = 'FAVORITE' THEN isFavorite END DESC,
            CASE WHEN :sortBy = 'LAST_PERFORMED' THEN lastPerformedAt END DESC,
            CASE WHEN :sortBy = 'DURATION' THEN totalDurationSec END ASC,
            createdAt DESC
    """)
    fun searchAndFilter(
        keyword: String? = null,
        category: String? = null,
        minDuration: Int? = null,
        maxDuration: Int? = null,
        sortBy: String = "CREATED_AT"
    ): Flow<List<WorkoutRoutine>>

    /** Ambil semua kategori unik yang sudah ada di database (untuk membangun filter chips). */
    @Query("SELECT DISTINCT category FROM workout_routines ORDER BY category ASC")
    fun observeAllCategories(): Flow<List<String>>

    // ─── HOME DASHBOARD METRICS (F-02) ──────────────────────────────────────

    /**
     * Hitung total menit latihan dalam 7 hari terakhir.
     * Digunakan untuk Activity Ring di Home Dashboard.
     */
    @Query("""
        SELECT COALESCE(SUM(totalDurationSec), 0)
        FROM workout_routines
        WHERE lastPerformedAt IS NOT NULL
        AND lastPerformedAt >= :weekStartMs
    """)
    fun observeWeeklyTotalSeconds(weekStartMs: Long): Flow<Int>

    /**
     * Hitung jumlah sesi yang diselesaikan dalam 7 hari terakhir.
     */
    @Query("""
        SELECT COUNT(*)
        FROM workout_routines
        WHERE lastPerformedAt IS NOT NULL
        AND lastPerformedAt >= :weekStartMs
    """)
    fun observeWeeklySessionCount(weekStartMs: Long): Flow<Int>

    // ─── TOGGLE FAVORITE ────────────────────────────────────────────────────

    @Query("UPDATE workout_routines SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("UPDATE workout_routines SET lastPerformedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPerformed(id: Long, timestamp: Long)

    @Query("UPDATE workout_routines SET totalDurationSec = :duration, totalExercises = :exercises WHERE id = :id")
    suspend fun updateStats(id: Long, duration: Int, exercises: Int)

    @Query("UPDATE workout_routines SET thumbnailPath = :path WHERE id = :id")
    suspend fun updateThumbnail(id: Long, path: String?)

    // ─── BACKUP & RESTORE ───────────────────────────────────────────────────

    @Query("SELECT * FROM workout_routines")
    suspend fun getAll(): List<WorkoutRoutine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routines: List<WorkoutRoutine>): List<Long>

    @Query("DELETE FROM workout_routines")
    suspend fun deleteAll()

    // ─── COUNT ───────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM workout_routines")
    suspend fun count(): Int
}
