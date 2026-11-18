package com.workout.timer.data.model

import java.util.Calendar
import java.util.UUID

/**
 * Model data pengingat latihan kustom (Multi-Reminder).
 *
 * @param id ID unik pengingat (UUID)
 * @param label Nama / label pengingat kustom (misal "Latihan Pagi", "Strength Sore", "Stretching Malam")
 * @param hour Jam alarm (0..23)
 * @param minute Menit alarm (0..59)
 * @param activeDays Daftar hari aktif menggunakan konstanta Calendar (Calendar.SUNDAY=1..Calendar.SATURDAY=7).
 *                   Default: seluruh 7 hari dalam seminggu.
 * @param isEnabled Status aktif/non-aktif pengingat
 */
data class WorkoutReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Latihan Harian",
    val hour: Int = 7,
    val minute: Int = 0,
    val activeDays: List<Int> = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    ),
    val isEnabled: Boolean = true
) {
    /** Request code unik untuk AlarmManager */
    val requestCode: Int
        get() = id.hashCode() and 0x7FFFFFFF

    /** Ringkasan hari aktif dalam bahasa Indonesia */
    val daysSummary: String
        get() {
            if (activeDays.size >= 7) return "Setiap Hari"
            val weekdays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            val weekend = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            if (activeDays.containsAll(weekdays) && activeDays.size == 5) return "Hari Kerja (Sen - Jum)"
            if (activeDays.containsAll(weekend) && activeDays.size == 2) return "Akhir Pekan (Sab - Min)"
            if (activeDays.isEmpty()) return "Tidak ada hari aktif"

            val map = mapOf(
                Calendar.MONDAY to "Sen",
                Calendar.TUESDAY to "Sel",
                Calendar.WEDNESDAY to "Rab",
                Calendar.THURSDAY to "Kam",
                Calendar.FRIDAY to "Jum",
                Calendar.SATURDAY to "Sab",
                Calendar.SUNDAY to "Min"
            )
            val order = listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY
            )
            return order.filter { activeDays.contains(it) }.mapNotNull { map[it] }.joinToString(", ")
        }
}

