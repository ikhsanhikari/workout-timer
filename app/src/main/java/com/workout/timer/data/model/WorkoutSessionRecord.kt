package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entitas log riwayat sesi latihan nyata (Workout Session Log).
 *
 * Menyimpan data immutable untuk setiap sesi latihan yang pernah dijalankan,
 * baik yang diselesaikan secara penuh (100%) maupun yang dihentikan sebagian (partial).
 *
 * Mendukung analisis konsistensi harian, streak, serta kalkulasi statistik mingguan/bulanan
 * yang akurat tanpa menimpa sesi-sesi sebelumnya.
 */
@Entity(
    tableName = "workout_session_records",
    indices = [
        Index(value = ["completedAt"]),
        Index(value = ["routineId"])
    ]
)
data class WorkoutSessionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID rutinitas yang dijalankan. */
    val routineId: Long,

    /** Nama rutinitas saat latihan berlangsung (snapshot agar tetap valid jika rutinitas diubah). */
    val routineName: String,

    /** Kategori latihan (misal: "HIIT", "Strength", "Core", "Mobility", dsb). */
    val category: String,

    /** Timestamp waktu sesi berakhir (epoch ms). */
    val completedAt: Long = System.currentTimeMillis(),

    /** Durasi riil latihan yang dijalankan (dalam detik, di luar waktu pause). */
    val durationSeconds: Int,

    /** Jumlah gerakan yang berhasil diselesaikan pada sesi ini. */
    val totalExercisesCompleted: Int,

    /** Total rencana gerakan yang seharusnya dijalankan. */
    val totalExercisesPlanned: Int,

    /** True jika latihan diselesaikan hingga gerakan terakhir, false jika dihentikan di tengah jalan. */
    val isCompletedFully: Boolean = true
)

