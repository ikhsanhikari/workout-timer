package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel ExerciseTemplate: "Kamus Gerakan Lokal" milik pengguna.
 *
 * Setiap entri menyimpan nama gerakan yang unik beserta media panduan-nya.
 * Ini memungkinkan fitur auto-complete saat pengguna menambah gerakan baru
 * di Workout Builder — cukup ketik 2-3 huruf dan gerakan yang pernah
 * dipakai akan muncul sebagai saran.
 *
 * Index UNIQUE pada [name] mencegah duplikasi dan mendukung pencarian cepat.
 *
 * Catatan GC: Ketika sebuah template dihapus, [MediaFileManager] memeriksa
 * apakah ada [WorkoutExercise] lain yang masih mereferensikan [mediaUri]-nya
 * sebelum menghapus file fisik dari disk.
 */
@Entity(
    tableName = "exercise_templates",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Nama gerakan yang unik, contoh: "Push Up", "Jumping Jack", "Plank Hold".
     * Digunakan sebagai kunci auto-complete di Workout Builder.
     */
    val name: String,

    /**
     * Path absolut ke file media (video/GIF) di penyimpanan internal:
     * `/data/data/com.workout.timer/files/workout_media/<filename>`
     * Null = tidak ada media untuk gerakan ini.
     */
    val mediaUri: String? = null,

    /** Tipe media yang tersimpan di [mediaUri]. */
    val mediaType: MediaType = MediaType.NONE,

    /**
     * Path absolut ke file thumbnail (JPG) yang diekstrak dari frame pertama video.
     * Null jika media bukan video atau thumbnail belum digenerate.
     */
    val thumbnailUri: String? = null,

    /** Durasi default gerakan ini dalam detik (bisa di-override di WorkoutExercise). */
    val defaultDuration: Int = 30,

    /** Timestamp Unix (ms) terakhir kali template ini digunakan. Untuk sorting auto-complete. */
    val lastUsedAt: Long = System.currentTimeMillis(),

    /** Berapa kali template ini sudah digunakan di berbagai rutinitas. */
    val usageCount: Int = 0
)
