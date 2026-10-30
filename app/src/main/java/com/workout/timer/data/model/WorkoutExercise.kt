package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel WorkoutExercise: satu baris = satu gerakan dalam satu blok.
 *
 * Foreign Key ke [WorkoutBlock] dengan CASCADE DELETE.
 * Foreign Key opsional ke [ExerciseTemplate] (kamus gerakan) —
 * Null berarti gerakan ini belum disimpan ke kamus atau dibuat secara ad-hoc.
 *
 * Field [customMediaUri] memungkinkan override media dari template,
 * sehingga gerakan "Push Up" di rutinitas A bisa punya video berbeda
 * dengan gerakan "Push Up" di rutinitas B.
 */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutBlock::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["blockId"]),
        Index(value = ["templateId"])
    ]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID blok induk (FK ke workout_blocks.id). */
    val blockId: Long,

    /**
     * ID template kamus gerakan opsional (FK ke exercise_templates.id).
     * Null = gerakan tidak terhubung ke kamus.
     */
    val templateId: Long? = null,

    /** Urutan gerakan dalam blok (0-based). */
    val orderIndex: Int,

    /** Nama tampilan gerakan, contoh: "Burpees", "Plank", "Rest". */
    val title: String,

    /** Durasi gerakan ini dalam detik. */
    val durationSeconds: Int,

    /**
     * Tipe interval: WARMUP, WORK, REST, atau COOLDOWN.
     * Menentukan warna cincin progres di Player.
     */
    val type: IntervalType,

    /**
     * Path media kustom (override dari template).
     * Format: path absolut ke file di penyimpanan internal aplikasi.
     * Null = gunakan media dari template, atau tidak ada media.
     */
    val customMediaUri: String? = null,

    /**
     * Tipe media untuk [customMediaUri].
     * Diabaikan jika [customMediaUri] null.
     */
    val customMediaType: MediaType = MediaType.NONE
)
