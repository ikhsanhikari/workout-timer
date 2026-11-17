package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas rekam jejak koordinat GPS per titik waktu dari suatu sesi lari.
 */
@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = RunSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("timestamp")
    ]
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID sesi lari pemilik titik ini. */
    val sessionId: Long,

    /** Koordinat garis lintang (latitude). */
    val latitude: Double,

    /** Koordinat garis bujur (longitude). */
    val longitude: Double,

    /** Ketinggian di atas permukaan laut (altitude) dalam meter. */
    val altitude: Double = 0.0,

    /** Kecepatan sesaat dari sensor GPS dalam meter per detik (m/s). */
    val speed: Float = 0f,

    /** Akurasi GPS radius dalam meter (semakin kecil semakin presisi). */
    val accuracy: Float = 0f,

    /** Timestamp perekaman titik koordinat (epoch ms). */
    val timestamp: Long = System.currentTimeMillis()
)
