package com.workout.timer.data.database.dao

import androidx.room.*
import com.workout.timer.data.model.LocationPoint

@Dao
interface LocationPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<LocationPoint>)

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<LocationPoint>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPoint(sessionId: Long): LocationPoint?

    @Query("DELETE FROM location_points WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: Long)
}
