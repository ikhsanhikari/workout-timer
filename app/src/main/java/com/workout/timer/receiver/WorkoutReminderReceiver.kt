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
import com.workout.timer.MainActivity
import com.workout.timer.R
import com.workout.timer.util.WorkoutReminderManager

/**
 * BroadcastReceiver untuk menangani alarm pengingat latihan harian (Daily Workout Reminder).
 * Berjalan offline tanpa memerlukan koneksi internet atau server pihak ketiga.
 */
class WorkoutReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "daily_workout_reminder_channel_v2"
        const val NOTIFICATION_ID = 2001
        const val ACTION_REMINDER = "com.workout.timer.ACTION_DAILY_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("WorkoutReminderReceiver", "Alarm received: action=${intent?.action}")

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Setelah perangkat reboot, jadwalkan ulang alarm sesuai preferensi pengguna
            WorkoutReminderManager.rescheduleFromPreferences(context)
            return
        }

        // Tampilkan notifikasi pengingat latihan
        showReminderNotification(context)

        // Jadwalkan otomatis untuk esok hari di jam yang sama
        WorkoutReminderManager.rescheduleNextDay(context)
    }

    fun showReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Latihan Harian",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat rutinitas latihan harian"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waktunya Workout! 💪")
            .setContentText("Luangkan waktu sejenak untuk menjaga streak dan kebugaran tubuhmu hari ini.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Luangkan waktu sejenak untuk menjaga streak dan kebugaran tubuhmu hari ini. Tubuh bugar dimulai dari konsistensi!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d("WorkoutReminderReceiver", "Notification posted successfully")
        } catch (e: Exception) {
            Log.e("WorkoutReminderReceiver", "Failed to post notification: ${e.message}", e)
        }
    }
}
