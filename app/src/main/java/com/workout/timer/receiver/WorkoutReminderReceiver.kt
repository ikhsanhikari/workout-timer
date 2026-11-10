package com.workout.timer.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.workout.timer.MainActivity
import com.workout.timer.R
import com.workout.timer.data.database.WorkoutDatabase
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.preferences.UserPreferencesRepository
import com.workout.timer.util.WorkoutReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * BroadcastReceiver untuk menangani alarm pengingat latihan harian (Daily Workout Reminder).
 *
 * FITUR UTAMA — SINKRONISASI JADWAL CERDAS:
 * - Mengetahui hari aktif saat alarm berbunyi (Senin s/d Minggu).
 * - Menyesuaikan pesan notifikasi secara dinamis berdasarkan jadwal di WorkoutPlan:
 *   1. Jika hari ini dijadwalkan LATIHAN: Menyebutkan nama rutinitas, kategori, durasi,
 *      dan jumlah gerakan, serta tombol aksi langsung "▶ Mulai Latihan" dan "📅 Lihat Jadwal".
 *   2. Jika hari ini dijadwalkan REST DAY (Pemulihan): Memberi ucapan istirahat/recovery,
 *      tips peregangan & tidur, tanpa memaksa latihan.
 *   3. Jika belum diatur jadwal kustom: Menggunakan fallback cerdas program bawaan.
 */
class WorkoutReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "daily_workout_reminder_channel_v2"
        const val NOTIFICATION_ID = 2001
        const val ACTION_REMINDER = "com.workout.timer.ACTION_DAILY_REMINDER"
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_REMINDER_LABEL = "EXTRA_REMINDER_LABEL"
        private const val TAG = "WorkoutReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Alarm received: action=${intent?.action}")

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Setelah perangkat reboot, jadwalkan ulang seluruh alarm sesuai preferensi pengguna
            WorkoutReminderManager.rescheduleAll(context)
            return
        }

        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID)
        val reminderLabel = intent?.getStringExtra(EXTRA_REMINDER_LABEL)
        val notifId = if (!reminderId.isNullOrBlank()) (reminderId.hashCode() and 0x7FFFFFFF) else NOTIFICATION_ID

        // Gunakan goAsync() agar receiver tidak di-kill saat melakukan query database di Dispatchers.IO
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postScheduleAwareNotification(
                    context = context,
                    reminderLabel = reminderLabel,
                    notificationId = notifId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memproses notifikasi jadwal: ${e.message}", e)
                // Fallback darurat jika terjadi error tak terduga
                showBasicFallbackNotification(context, reminderLabel, notifId)
            } finally {
                // Jadwalkan otomatis kejadian berikutnya untuk pengingat ini / semua pengingat
                WorkoutReminderManager.rescheduleAll(context)
                pendingResult.finish()
            }
        }
    }

    /**
     * Membangun dan menampilkan notifikasi yang 100% tersinkronisasi dengan jadwal harian pengguna.
     */
    suspend fun postScheduleAwareNotification(
        context: Context,
        reminderLabel: String?,
        notificationId: Int = NOTIFICATION_ID
    ) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        ensureNotificationChannel(notificationManager)

        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=SUNDAY, 2=MONDAY, ..., 7=SATURDAY
        val dayName = getDayNameIndonesian(currentDayOfWeek)

        val prefs = UserPreferencesRepository(appContext)
        val weeklyScheduleJson = prefs.weeklyScheduleJsonFlow.firstOrNull()
        val db = WorkoutDatabase.getInstance(appContext)

        // Parse jadwal mingguan pengguna
        val gson = Gson()
        var scheduleMap: Map<Int, Long?>? = null

        if (!weeklyScheduleJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, Long?>>() {}.type
                val rawMap: Map<String, Long?>? = gson.fromJson(weeklyScheduleJson, type)
                scheduleMap = rawMap?.mapKeys { it.key.toIntOrNull() ?: -1 }?.filterKeys { it in 1..7 }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal parse weeklyScheduleJson: ${e.message}")
            }
        }

        // Jika scheduleMap belum ada atau kosong, generate default schedule dari database
        if (scheduleMap.isNullOrEmpty()) {
            val allRoutines = db.workoutRoutineDao().getAll()
            scheduleMap = generateDefaultScheduleMap(allRoutines)
        }

        val hasDayEntry = scheduleMap.containsKey(currentDayOfWeek)
        val routineId = scheduleMap[currentDayOfWeek]
        val isRestDay = hasDayEntry && routineId == null

        val scheduledRoutine: WorkoutRoutine? = if (routineId != null) {
            db.workoutRoutineDao().getById(routineId)
        } else null

        val labelPrefix = if (!reminderLabel.isNullOrBlank()) reminderLabel else "Pengingat Latihan"

        val notification = when {
            // ── KASUS 1: HARI INI JADWAL REST DAY (ISTIRAHAT & PEMULIHAN) ───────
            isRestDay -> {
                buildRestDayNotification(
                    context = appContext,
                    labelPrefix = labelPrefix,
                    dayName = dayName,
                    notificationId = notificationId
                )
            }

            // ── KASUS 2: HARI INI JADWAL LATIHAN (ROUTINE AKTIF) ────────────────
            scheduledRoutine != null -> {
                buildRoutineNotification(
                    context = appContext,
                    labelPrefix = labelPrefix,
                    dayName = dayName,
                    routine = scheduledRoutine,
                    notificationId = notificationId
                )
            }

            // ── KASUS 3: FALLBACK UMUM (Jika jadwal belum disetel spesifik) ──────
            else -> {
                buildGenericReminderNotification(
                    context = appContext,
                    labelPrefix = labelPrefix,
                    dayName = dayName,
                    notificationId = notificationId
                )
            }
        }

        try {
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Schedule-aware notification posted ($notificationId) for day $currentDayOfWeek ($dayName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
        }
    }

    private fun buildRoutineNotification(
        context: Context,
        labelPrefix: String,
        dayName: String,
        routine: WorkoutRoutine,
        notificationId: Int
    ): Notification {
        val durationMins = (routine.totalDurationSec / 60).coerceAtLeast(1)
        val title = "$labelPrefix: ${routine.name}"
        val contentText = "Jadwal $dayName (${routine.category} • $durationMins mnt • ${routine.totalExercises} gerakan). Yuk mulai!"
        val bigText = "${routine.name}\nJadwal $dayName: ${routine.category} • $durationMins Menit • ${routine.totalExercises} Gerakan.\nKonsistensi adalah kunci kebugaran. Siap untuk berlatih hari ini?"

        // Tap notifikasi langsung membuka player / detail rutinitas
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_START_ROUTINE_ID, routine.id)
            putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_PLAN)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "Mulai Latihan" (Langsung ke WorkoutPlayer)
        val startWorkoutIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_START_ROUTINE_ID, routine.id)
        }
        val startPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 10,
            startWorkoutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "Lihat Jadwal" (Ke Tab WorkoutPlan)
        val viewPlanIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_PLAN)
        }
        val planPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 20,
            viewPlanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "▶ Mulai Latihan", startPendingIntent)
            .addAction(0, "📅 Lihat Jadwal", planPendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun buildRestDayNotification(
        context: Context,
        labelPrefix: String,
        dayName: String,
        notificationId: Int
    ): Notification {
        val title = "$labelPrefix • Hari Pemulihan ($dayName) 🧘"
        val contentText = "Hari ini adalah jadwal pemulihan fisik. Istirahatkan ototmu untuk performa optimal berikutnya!"
        val bigText = "Hari Pemulihan & Regenerasi Otot ($dayName) 🧘\nOtot bertumbuh dan memulihkan stamina saat istirahat. Jaga hidrasi harian, konsumsi protein cukup, dan tidur 7-8 jam malam ini!"

        val planIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_PLAN)
        }
        val planPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            planIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(planPendingIntent)
            .addAction(0, "📅 Buka Jadwal", planPendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun buildGenericReminderNotification(
        context: Context,
        labelPrefix: String,
        dayName: String,
        notificationId: Int
    ): Notification {
        val title = "$labelPrefix ⚡"
        val contentText = "Luangkan waktu sejenak di hari $dayName untuk menjaga streak dan kebugaran tubuhmu."
        val bigText = "Waktunya Workout! 💪\nLuangkan waktu sejenak di hari $dayName untuk menjaga streak dan kebugaran tubuhmu. Buka aplikasi untuk memilih atau melanjutkan latihan."

        val planIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_PLAN)
        }
        val planPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            planIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(planPendingIntent)
            .addAction(0, "📅 Buka Jadwal", planPendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun showBasicFallbackNotification(
        context: Context,
        labelPrefix: String?,
        notificationId: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureNotificationChannel(notificationManager)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (!labelPrefix.isNullOrBlank()) "$labelPrefix ⚡" else "Waktunya Workout! 💪"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Luangkan waktu sejenak untuk menjaga streak dan kebugaran tubuhmu hari ini.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun ensureNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Latihan Harian",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat latihan harian yang tersinkronisasi otomatis dengan jadwal mingguan"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getDayNameIndonesian(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> "Hari Ini"
        }
    }

    private fun generateDefaultScheduleMap(routines: List<WorkoutRoutine>): Map<Int, Long?> {
        val fullBody = routines.find { it.category.equals("Full Body", ignoreCase = true) }?.id ?: routines.firstOrNull()?.id
        val hiit = routines.find { it.category.equals("HIIT", ignoreCase = true) }?.id ?: routines.getOrNull(1)?.id
        val core = routines.find { it.category.equals("Core", ignoreCase = true) }?.id ?: routines.getOrNull(2)?.id
        val strength = routines.find { it.category.equals("Strength", ignoreCase = true) }?.id ?: routines.getOrNull(3)?.id
        val mobility = routines.find { it.category.equals("Mobility", ignoreCase = true) }?.id ?: routines.getOrNull(4)?.id

        return mapOf(
            Calendar.MONDAY to fullBody,
            Calendar.TUESDAY to null, // Rest Day
            Calendar.WEDNESDAY to hiit,
            Calendar.THURSDAY to core,
            Calendar.FRIDAY to strength,
            Calendar.SATURDAY to mobility,
            Calendar.SUNDAY to null // Rest Day
        )
    }

    /**
     * Helper uji notifikasi langsung untuk keperluan testing dari UI / ViewModel.
     */
    fun showReminderNotification(
        context: Context,
        title: String = "Waktunya Workout! 💪",
        notificationId: Int = NOTIFICATION_ID
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            postScheduleAwareNotification(
                context = context,
                reminderLabel = title.replace(" ⚡", "").replace(" 💪", ""),
                notificationId = notificationId
            )
        }
    }
}
