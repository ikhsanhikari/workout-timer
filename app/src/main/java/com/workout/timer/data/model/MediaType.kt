package com.workout.timer.data.model

/**
 * Tipe media yang didukung oleh pipeline kustom.
 * - VIDEO : File MP4 / WebM
 * - GIF   : File animasi GIF
 * - NONE  : Tidak ada media (gerakan tanpa panduan visual)
 */
enum class MediaType {
    VIDEO,
    GIF,
    NONE
}
