package com.workout.timer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel utama: satu baris = satu rutinitas latihan lengkap.
 *
 * Index pada [name] dan [category] memastikan query pencarian/filter
 * berjalan di bawah 16ms sesuai NFR F-03.
 */
@Entity(
    tableName = "workout_routines",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class WorkoutRoutine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Nama rutinitas, contoh: "Full Body HIIT 20 Min" */
    val name: String,

    /**
     * Kategori latihan. Nilai bawaan: "HIIT", "Strength", "Core", "Mobility".
     * Pengguna dapat menambahkan kategori kustom (teks bebas).
     */
    val category: String,

    /** Total durasi keseluruhan rutinitas dalam detik (dihitung otomatis oleh Repository). */
    val totalDurationSec: Int = 0,

    /** Total jumlah gerakan unik dalam rutinitas (tidak termasuk repetisi block). */
    val totalExercises: Int = 0,

    /**
     * Warna aksen rutinitas dalam format Hex string, contoh: "#A4FF00".
     * Digunakan pada header kartu dan cincin progres.
     */
    val accentColor: String = "#A4FF00",

    /**
     * Path absolut ke file thumbnail (JPG/PNG) yang diekstrak dari video gerakan pertama.
     * Null jika tidak ada media yang terlampir.
     */
    val thumbnailPath: String? = null,

    /** Apakah rutinitas ini ditandai sebagai favorit oleh pengguna. */
    val isFavorite: Boolean = false,

    /**
     * Timestamp Unix (ms) kapan rutinitas ini terakhir kali diselesaikan.
     * Null = belum pernah dilakukan.
     */
    val lastPerformedAt: Long? = null,

    /** Timestamp Unix (ms) saat rutinitas ini dibuat. */
    val createdAt: Long = System.currentTimeMillis()
)
