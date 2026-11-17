package com.workout.timer

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.workout.timer.receiver.WorkoutReminderReceiver
import com.workout.timer.service.WorkoutTimerService
import com.workout.timer.util.WorkoutReminderManager
import dagger.hilt.android.HiltAndroidApp
import java.io.File

/**
 * Application class dengan anotasi @HiltAndroidApp untuk mengaktifkan
 * dependency injection Hilt di seluruh aplikasi.
 *
 * Mengimplementasikan [ImageLoaderFactory] agar Coil mendukung pemutaran
 * animasi GIF secara global. Dilengkapi global crash logger agar bila terjadi
 * error tidak langsung silent crash tanpa jejak.
 */
@HiltAndroidApp
class WorkoutApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WorkoutApp", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            try {
                val crashFile = File(filesDir, "crash_log.txt")
                crashFile.writeText(Log.getStackTraceString(throwable))
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Daftarkan semua notification channel secara global di awal aplikasi booting
        createNotificationChannels()

        // Inisialisasi OSMDroid configuration (Peta Offline & Cache)
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = File(cacheDir, "osmdroid_tiles")
            tileFileSystemCacheMaxBytes = 200L * 1024 * 1024 // Maks 200MB
        }

        // Pastikan alarm terjadwal jika pengguna memiliki pengingat aktif
        WorkoutReminderManager.rescheduleAll(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            // 1. Channel Pengingat Latihan Harian
            val reminderChannel = NotificationChannel(
                WorkoutReminderReceiver.CHANNEL_ID,
                "Pengingat Latihan Harian",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat latihan harian yang tersinkronisasi otomatis dengan jadwal mingguan"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // 2. Channel Workout Timer Aktif (Foreground Service)
            val serviceChannel = NotificationChannel(
                WorkoutTimerService.NOTIFICATION_CHANNEL_ID,
                "Workout Timer Aktif",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status sesi latihan aktif dan kontrol timer"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            // 3. Channel Live Pelacakan GPS Lari / Jalan
            val runChannel = NotificationChannel(
                com.workout.timer.service.RunTrackingService.NOTIFICATION_CHANNEL_ID,
                "Pelacakan Lari / Jalan (GPS)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status live tracking jarak, durasi, dan pace lari GPS"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, serviceChannel, runChannel))
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
