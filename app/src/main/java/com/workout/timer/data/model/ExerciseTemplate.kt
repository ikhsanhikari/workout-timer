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
    val usageCount: Int = 0,

    /** Kategori gerakan (Cardio, Strength, Core, Mobility, Recovery). */
    val category: String? = null,

    /** Target otot spesifik (misal: "Dada • Bahu • Trisep"). */
    val targetMuscle: String? = null,

    /** Grup otot utama untuk filter (Chest, Legs / Glutes, Abs / Core, Back, Full Body / Cardio). */
    val primaryMuscle: String? = null,

    /** Peralatan yang dibutuhkan (misal: "Tanpa Alat (Bodyweight)", "Matras", "Dumbbell"). */
    val equipment: String? = null,

    /** Tingkat kesulitan (Beginner, Intermediate, Advanced). */
    val difficulty: String? = null,

    /** Tips instruktur profesional saat melakukan gerakan ini. */
    val coachTip: String? = null,

    /** Manfaat kesehatan / biomekanik dari gerakan ini. */
    val benefits: String? = null
)

/**
 * Resolusi otomatis tipe interval [IntervalType] berdasarkan nama dan kategori template.
 */
fun ExerciseTemplate.resolveIntervalType(): IntervalType {
    val cat = category?.lowercase()?.trim().orEmpty()
    val lowerName = name.lowercase().trim()

    return when {
        // 1. REST / RECOVERY
        cat in listOf("recovery", "rest", "istirahat", "pemulihan") ||
        lowerName.contains("istirahat") ||
        lowerName == "rest" ||
        lowerName.startsWith("rest ") ||
        lowerName.endsWith(" rest") ||
        lowerName.contains("pemulihan") ||
        lowerName.contains("pause") ||
        lowerName.contains("jeda") -> IntervalType.REST

        // 2. WARMUP
        cat in listOf("warmup", "pemanasan", "warm-up", "warm up") ||
        lowerName.contains("pemanasan") ||
        lowerName.contains("warmup") ||
        lowerName.contains("warm up") ||
        lowerName.contains("warm-up") -> IntervalType.WARMUP

        // 3. COOLDOWN
        cat in listOf("cooldown", "pendinginan", "cool-down", "cool down") ||
        lowerName.contains("pendinginan") ||
        lowerName.contains("cooldown") ||
        lowerName.contains("cool down") ||
        lowerName.contains("cool-down") -> IntervalType.COOLDOWN

        // 4. Default: WORK (Latihan / Gerakan Aktif)
        else -> IntervalType.WORK
    }
}

/**
 * Resolusi otomatis tipe interval berdasarkan teks judul yang diketik pengguna.
 */
fun resolveIntervalTypeFromTitle(title: String): IntervalType? {
    val lower = title.lowercase().trim()
    return when {
        lower.contains("istirahat") ||
        lower == "rest" ||
        lower.startsWith("rest ") ||
        lower.endsWith(" rest") ||
        lower.contains("pemulihan") ||
        lower.contains("jeda") -> IntervalType.REST

        lower.contains("pemanasan") ||
        lower.contains("warmup") ||
        lower.contains("warm up") ||
        lower.contains("warm-up") -> IntervalType.WARMUP

        lower.contains("pendinginan") ||
        lower.contains("cooldown") ||
        lower.contains("cool down") ||
        lower.contains("cool-down") -> IntervalType.COOLDOWN

        else -> null
    }
}
