package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.ExerciseTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseTemplateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(template: ExerciseTemplate): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<ExerciseTemplate>): List<Long>

    @Update
    suspend fun update(template: ExerciseTemplate)

    @Delete
    suspend fun delete(template: ExerciseTemplate)

    @Query("DELETE FROM exercise_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercise_templates WHERE id = :id")
    suspend fun getById(id: Long): ExerciseTemplate?

    @Query("SELECT * FROM exercise_templates WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseTemplate?

    /**
     * Auto-complete: ambil template yang namanya mengandung [query],
     * diurutkan berdasarkan frekuensi penggunaan (sering dipakai = muncul pertama).
     */
    @Query("""
        SELECT * FROM exercise_templates
        WHERE name LIKE '%' || :query || '%'
        ORDER BY usageCount DESC, lastUsedAt DESC
        LIMIT :limit
    """)
    fun searchByName(query: String, limit: Int = 10): Flow<List<ExerciseTemplate>>

    @Query("SELECT * FROM exercise_templates ORDER BY usageCount DESC, lastUsedAt DESC")
    fun observeAll(): Flow<List<ExerciseTemplate>>

    /** Increment usage count saat template dipakai di rutinitas. */
    @Query("""
        UPDATE exercise_templates
        SET usageCount = usageCount + 1, lastUsedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE exercise_templates SET mediaUri = :uri, mediaType = :type, thumbnailUri = :thumbUri WHERE id = :id")
    suspend fun updateMedia(id: Long, uri: String?, type: String, thumbUri: String?)

    /**
     * Untuk garbage collection: cek apakah [mediaUri] masih
     * direferensikan oleh ExerciseTemplate mana pun.
     */
    @Query("SELECT COUNT(*) FROM exercise_templates WHERE mediaUri = :mediaUri")
    suspend fun countByMediaUri(mediaUri: String): Int

    @Query("SELECT * FROM exercise_templates")
    suspend fun getAll(): List<ExerciseTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(templates: List<ExerciseTemplate>): List<Long>

    @Query("DELETE FROM exercise_templates")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM exercise_templates")
    suspend fun count(): Int
}
