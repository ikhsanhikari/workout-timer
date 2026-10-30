package com.workout.timer.data.model

/**
 * Tipe interval untuk setiap gerakan dalam latihan.
 * Setiap tipe memiliki warna aksen tersendiri di UI Player.
 * - WARMUP   : Amber Yellow  (#FFD60A)
 * - WORK     : Volt Neon Green (#A4FF00)
 * - REST     : Electric Cyan  (#00F5EA)
 * - COOLDOWN : Amber Yellow   (#FFD60A) (sama dengan warmup)
 */
enum class IntervalType {
    WARMUP,
    WORK,
    REST,
    COOLDOWN
}
