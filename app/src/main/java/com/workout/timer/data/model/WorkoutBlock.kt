package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel WorkoutBlock: satu blok dapat berisi beberapa [WorkoutExercise].
 *
 * Blok dengan [repeatCount] > 1 berfungsi sebagai "Set Looping" —
 * semua gerakan di dalam blok ini akan diputar ulang sebanyak [repeatCount] kali
 * tanpa perlu duplikasi manual di database.
 *
 * Foreign Key ke [WorkoutRoutine] dengan CASCADE DELETE agar ketika
 * rutinitas dihapus, semua bloknya otomatis terhapus.
 */
@Entity(
    tableName = "workout_blocks",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routineId"])]
)
data class WorkoutBlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID rutinitas induk (FK ke workout_routines.id). */
    val routineId: Long,

    /** Urutan blok dalam rutinitas (0-based). */
    val orderIndex: Int,

    /**
     * Jumlah pengulangan blok ini.
     * - 1 = Satu kali (normal, bukan loop set)
     * - > 1 = Loop set (misal: 3 = "3 Sets")
     */
    val repeatCount: Int = 1,

    /** Label opsional untuk blok, contoh: "Superset A" atau "Tabata Round". */
    val label: String? = null
)
