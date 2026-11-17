package com.workout.timer.data.repository

import com.workout.timer.data.database.dao.LocationPointDao
import com.workout.timer.data.database.dao.RunSessionDao
import com.workout.timer.data.model.LocationPoint
import com.workout.timer.data.model.RunSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository yang mengelola data sesi lari dan titik GPS tracking.
 */
@Singleton
class RunRepository @Inject constructor(
    private val runSessionDao: RunSessionDao,
    private val locationPointDao: LocationPointDao
) {

    fun getAllSessions(): Flow<List<RunSession>> =
        runSessionDao.getAllSessions()

    suspend fun getSessionById(id: Long): RunSession? =
        runSessionDao.getById(id)

    suspend fun getActiveSession(): RunSession? =
        runSessionDao.getActiveSession()

    fun getTotalDistanceMeters(): Flow<Double?> =
        runSessionDao.getTotalDistanceMeters()

    fun getTotalCompletedRuns(): Flow<Int> =
        runSessionDao.getTotalCompletedRuns()

    fun getTotalDurationSeconds(): Flow<Int?> =
        runSessionDao.getTotalDurationSeconds()

    fun getTotalCaloriesBurned(): Flow<Int?> =
        runSessionDao.getTotalCaloriesBurned()

    suspend fun startSession(): Long {
        val session = RunSession(
            startedAt = System.currentTimeMillis(),
            status = "running"
        )
        return runSessionDao.insert(session)
    }

    suspend fun updateSession(session: RunSession) {
        runSessionDao.update(session)
    }

    suspend fun finishSession(
        id: Long,
        distanceMeters: Double,
        durationSeconds: Int,
        avgPaceSecPerKm: Int,
        bestPaceSecPerKm: Int,
        caloriesBurned: Int
    ) {
        val session = runSessionDao.getById(id) ?: return
        runSessionDao.update(
            session.copy(
                completedAt = System.currentTimeMillis(),
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                avgPaceSecPerKm = avgPaceSecPerKm,
                bestPaceSecPerKm = bestPaceSecPerKm,
                caloriesBurned = caloriesBurned,
                status = "completed"
            )
        )
    }

    suspend fun deleteSession(id: Long) {
        locationPointDao.deletePointsForSession(id)
        runSessionDao.deleteById(id)
    }

    suspend fun addLocationPoint(point: LocationPoint) {
        locationPointDao.insert(point)
    }

    suspend fun addLocationPoints(points: List<LocationPoint>) {
        if (points.isNotEmpty()) {
            locationPointDao.insertAll(points)
        }
    }

    suspend fun getPointsForSession(sessionId: Long): List<LocationPoint> =
        locationPointDao.getPointsForSession(sessionId)

    suspend fun getLastPoint(sessionId: Long): LocationPoint? =
        locationPointDao.getLastPoint(sessionId)
}
