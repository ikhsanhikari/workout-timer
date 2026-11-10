package com.workout.timer.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.workout.timer.data.preferences.UserPreferencesRepository
import com.workout.timer.receiver.WorkoutReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

/**
 * WorkoutReminderManager — Penjadwalan alarm pengingat latihan harian offline.
 *
 * Menjamin:
 * 1. Mendukung zona waktu GMT+7 (WIB / Asia/Jakarta) atau zona waktu perangkat saat ini.
 * 2. Menggunakan `canScheduleExactAlarms()` dengan fallback ke `setAndAllowWhileIdle`
 *    agar tidak pernah throw SecurityException di Android 12/13/14+.
 * 3. Menyediakan kalkulasi countdown menuju alarm berikutnya ("X jam Y menit lagi").
 * 4. Menyediakan pemicu uji coba langsung (`testNotificationNow`).
 */
object WorkoutReminderManager {

    private const val TAG = "WorkoutReminderManager"
    private const val REQUEST_CODE = 3001
    private const val TEST_REQUEST_CODE = 3002

    /**
     * Jadwalkan alarm pengingat harian pada jam & menit tertentu.
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, WorkoutReminderReceiver::class.java).apply {
            action = WorkoutReminderReceiver.ACTION_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Gunakan timezone perangkat pengguna (secara default WIB/GMT+7 di Indonesia)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Jika waktu yang dipilih sudah lewat pada hari ini, jadwalkan untuk besok
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val targetMs = calendar.timeInMillis
        Log.d(TAG, "Menjadwalkan alarm harian pada: ${calendar.time} (epoch: $targetMs)")

        scheduleAlarmInternal(alarmManager, targetMs, pendingIntent)
    }

    /**
     * Jadwalkan uji coba alarm yang akan berbunyi dalam N detik (untuk testing).
     */
    fun scheduleTestReminderInSeconds(context: Context, seconds: Int = 10) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, WorkoutReminderReceiver::class.java).apply {
            action = WorkoutReminderReceiver.ACTION_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetMs = System.currentTimeMillis() + (seconds * 1000L)
        Log.d(TAG, "Menjadwalkan test alarm dalam $seconds detik (targetMs: $targetMs)")

        scheduleAlarmInternal(alarmManager, targetMs, pendingIntent)
    }

    private fun scheduleAlarmInternal(
        alarmManager: AlarmManager,
        targetMs: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetMs,
                        pendingIntent
                    )
                } else {
                    // Fallback jika exact alarm tidak diizinkan sistem
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    targetMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm berhasil terdaftar di AlarmManager sistem.")
        } catch (e: SecurityException) {
            Log.w(TAG, "Izin exact alarm dibatasi, beralih ke setAndAllowWhileIdle: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, targetMs, pendingIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Gagal menjadwalkan alarm cadangan: ${e2.message}", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjadwalkan alarm: ${e.message}", e)
        }
    }

    /**
     * Mengirim notifikasi langsung saat ini juga untuk memverifikasi permission dan UI channel.
     */
    fun testNotificationNow(context: Context) {
        WorkoutReminderReceiver().showReminderNotification(context)
    }

    fun cancelReminder(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, WorkoutReminderReceiver::class.java).apply {
            action = WorkoutReminderReceiver.ACTION_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm pengingat dibatalkan.")
        }
    }

    fun rescheduleNextDay(context: Context) {
        rescheduleAll(context)
    }

    fun rescheduleFromPreferences(context: Context) {
        rescheduleAll(context)
    }

    fun scheduleReminderItem(context: Context, item: com.workout.timer.data.model.WorkoutReminderItem) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (!item.isEnabled || item.activeDays.isEmpty()) {
            cancelReminderItem(context, item.id)
            return
        }

        val intent = Intent(appContext, WorkoutReminderReceiver::class.java).apply {
            action = WorkoutReminderReceiver.ACTION_REMINDER
            putExtra(WorkoutReminderReceiver.EXTRA_REMINDER_ID, item.id)
            putExtra(WorkoutReminderReceiver.EXTRA_REMINDER_LABEL, item.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            item.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetMs = calculateNextTriggerTime(item.hour, item.minute, item.activeDays)
        Log.d(TAG, "Menjadwalkan alarm [${item.label}] pada epoch $targetMs (ReqCode: ${item.requestCode})")

        scheduleAlarmInternal(alarmManager, targetMs, pendingIntent)
    }

    fun cancelReminderItem(context: Context, reminderId: String) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = reminderId.hashCode() and 0x7FFFFFFF

        val intent = Intent(appContext, WorkoutReminderReceiver::class.java).apply {
            action = WorkoutReminderReceiver.ACTION_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm pengingat [$reminderId] dibatalkan.")
        }
    }

    fun calculateNextTriggerTime(hour: Int, minute: Int, activeDays: List<Int>): Long {
        val now = Calendar.getInstance()
        var bestCandidate: Long = Long.MAX_VALUE

        // Periksa 14 hari ke depan untuk menemukan hari dan jam berikutnya yang cocok
        for (dayOffset in 0..14) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (activeDays.contains(dayOfWeek) && candidate.timeInMillis > now.timeInMillis) {
                if (candidate.timeInMillis < bestCandidate) {
                    bestCandidate = candidate.timeInMillis
                    break
                }
            }
        }

        return if (bestCandidate != Long.MAX_VALUE) bestCandidate else {
            now.apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = UserPreferencesRepository(appContext)
            val list = prefs.customRemindersFlow.firstOrNull() ?: emptyList()
            for (item in list) {
                if (item.isEnabled) {
                    scheduleReminderItem(appContext, item)
                } else {
                    cancelReminderItem(appContext, item.id)
                }
            }
        }
    }

    /**
     * Menghitung deskripsi sisa waktu menuju alarm pengingat berikutnya.
     */
    fun getNextReminderDescription(hour: Int, minute: Int): String {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val diffMs = calendar.timeInMillis - now
        val diffMin = ((diffMs + 59999L) / (60 * 1000L)).toInt()
        val diffHours = diffMin / 60
        val remainingMin = diffMin % 60

        val timeString = String.format("%02d:%02d WIB", hour, minute)

        return when {
            diffHours > 0 && remainingMin > 0 -> "Berbunyi dalam $diffHours jam $remainingMin menit lagi ($timeString)"
            diffHours > 0 -> "Berbunyi dalam $diffHours jam lagi ($timeString)"
            diffMin > 1 -> "Berbunyi dalam $diffMin menit lagi ($timeString)"
            diffMin == 1 -> "Berbunyi dalam 1 menit lagi ($timeString)"
            else -> "Segera berbunyi ($timeString)"
        }
    }

    /**
     * Memformat daftar hari aktif menjadi ringkasan yang rapi dan ringkas untuk UI.
     * Contoh: "Setiap Hari", "Hari Kerja (Sen-Jum)", "Akhir Pekan (Sab-Min)", "Sen, Rab, Jum".
     */
    fun formatDaysSummary(activeDays: List<Int>): String {
        if (activeDays.isEmpty()) return "Nonaktif"
        if (activeDays.size >= 7) return "Setiap Hari"
        val sorted = activeDays.sorted()
        val weekdays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        val weekends = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        if (sorted == weekdays) return "Hari Kerja (Sen-Jum)"
        if (sorted == weekends) return "Akhir Pekan (Sab-Min)"

        val dayNames = mapOf(
            Calendar.MONDAY to "Sen",
            Calendar.TUESDAY to "Sel",
            Calendar.WEDNESDAY to "Rab",
            Calendar.THURSDAY to "Kam",
            Calendar.FRIDAY to "Jum",
            Calendar.SATURDAY to "Sab",
            Calendar.SUNDAY to "Min"
        )
        val dayOrder = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        return dayOrder.filter { sorted.contains(it) }.mapNotNull { dayNames[it] }.joinToString(", ")
    }
}
