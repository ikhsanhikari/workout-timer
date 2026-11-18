package com.workout.timer.service.location

import android.location.Location

/**
 * Utilitas murni untuk menghitung metrik statistik lari:
 * jarak (GPS distance), pace, estimasi kalori MET, dan auto-pause detection.
 */
object RunStatsCalculator {

    /**
     * Menghitung jarak antara 2 titik koordinat (dalam meter) menggunakan WGS-84 ellipsoid.
     */
    fun calculateDistanceBetween(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Menghitung pace rata-rata dalam detik per kilometer.
     * Mengembalikan 0 jika jarak terlalu pendek (< 10 meter).
     */
    fun calculatePaceSecPerKm(distanceMeters: Double, durationSeconds: Int): Int {
        if (distanceMeters < 10.0 || durationSeconds <= 0) return 0
        val pace = ((durationSeconds.toDouble() / distanceMeters) * 1000.0).toInt()
        // Cap di 3600 (maks 60 menit/km) untuk mencegah angka aneh saat baru mulai
        return pace.coerceIn(0, 3600)
    }

    /**
     * Format pace detik/km menjadi string representasi standar Strava (contoh: 5'42").
     */
    fun formatPace(paceSecPerKm: Int): String {
        if (paceSecPerKm <= 0 || paceSecPerKm >= 3600) return "-'--\""
        val min = paceSecPerKm / 60
        val sec = paceSecPerKm % 60
        return "$min'${sec.toString().padStart(2, '0')}\""
    }

    /**
     * Format durasi detik menjadi HH:mm:ss atau mm:ss.
     */
    fun formatDuration(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    /**
     * Format jarak meter menjadi kilometer dengan 2 desimal (contoh: "3.24").
     */
    fun formatDistanceKm(meters: Double): String {
        val km = meters / 1000.0
        return String.format(java.util.Locale.US, "%.2f", km)
    }

    /**
     * Estimasi kalori terbakar berdasarkan standar MET (Metabolic Equivalent of Task).
     *
     * Kecepatan:
     * - < 4 km/h: MET 2.5 (jalan santai)
     * - 4 - 6.5 km/h: MET 3.8 (jalan cepat)
     * - 6.5 - 8.5 km/h: MET 7.0 (jogging santai)
     * - 8.5 - 11 km/h: MET 9.8 (lari sedang)
     * - > 11 km/h: MET 12.0 (lari cepat / sprint)
     *
     * Rumus: Kalori = MET × Berat Badan (kg) × Waktu (jam)
     */
    fun estimateCalories(
        distanceMeters: Double,
        durationSeconds: Int,
        weightKg: Float = 70f
    ): Int {
        if (durationSeconds <= 0 || distanceMeters <= 0.0) return 0
        val speedKmh = (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
        val met = when {
            speedKmh < 4.0 -> 2.5
            speedKmh < 6.5 -> 3.8
            speedKmh < 8.5 -> 7.0
            speedKmh < 11.0 -> 9.8
            else -> 12.0
        }
        val hours = durationSeconds / 3600.0
        return (met * weightKg * hours).toInt().coerceAtLeast(0)
    }

    /**
     * Deteksi apakah pengguna sedang berhenti / diam untuk auto-pause.
     * Jika rata-rata kecepatan terakhir di bawah threshold selama minimal `minSamples` pengujian.
     */
    fun shouldAutoPause(
        recentSpeeds: List<Float>,
        thresholdMs: Float = 0.35f, // ~1.26 km/h
        minSamples: Int = 4
    ): Boolean {
        if (recentSpeeds.size < minSamples) return false
        val samples = recentSpeeds.takeLast(minSamples)
        return samples.all { it < thresholdMs }
    }
}
