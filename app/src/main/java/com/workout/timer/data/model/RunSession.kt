package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas riwayat sesi lari / jalan santai berbasis GPS (Strava-like).
 */
@Entity(
    tableName = "run_sessions",
    indices = [
        Index("startedAt"),
        Index("completedAt")
    ]
)
data class RunSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Timestamp waktu mulai lari (epoch ms). */
    val startedAt: Long = System.currentTimeMillis(),

    /** Timestamp waktu selesai lari (epoch ms), 0 jika masih berlangsung. */
    val completedAt: Long = 0,

    /** Total akumulasi jarak tempuh dalam meter. */
    val distanceMeters: Double = 0.0,

    /** Total durasi aktif dalam detik (tidak termasuk waktu pause). */
    val durationSeconds: Int = 0,

    /** Pace rata-rata dalam detik per kilometer (misal 300 detik = 5'00"/km). */
    val avgPaceSecPerKm: Int = 0,

    /** Pace split kilometer terbaik dalam detik per km. */
    val bestPaceSecPerKm: Int = 0,

    /** Estimasi pembakaran kalori berdasarkan MET dan berat badan. */
    val caloriesBurned: Int = 0,

    /** Status sesi: "running", "paused", "completed". */
    val status: String = "running"
)
