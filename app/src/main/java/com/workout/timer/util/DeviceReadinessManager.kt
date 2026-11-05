package com.workout.timer.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * DeviceReadinessManager — Utility untuk memeriksa dan mengelola kesiapan perangkat (Prio 4).
 *
 * Menjamin:
 * 1. Izin notifikasi runtime (Android 13+ POST_NOTIFICATIONS) agar timer terlihat di lockscreen.
 * 2. Status pengecualian optimasi baterai (Doze Mode Exemption) agar timer tidak dimatikan paksa
 *    oleh OS saat layar mati di saku celana.
 * 3. Navigasi langsung ke pengaturan sistem untuk aktivasi instan.
 */
object DeviceReadinessManager {

    /**
     * Memeriksa apakah izin notifikasi telah diberikan oleh pengguna.
     * Pada Android 12 ke bawah (API < 33), selalu bernilai true.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Memeriksa apakah aplikasi dikecualikan dari Battery Optimization / Doze Mode.
     * Jika true, timer memiliki kekebalan latar belakang penuh.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    /**
     * Membuka dialog permohonan pengecualian optimasi baterai secara langsung
     * atau membuka halaman pengaturan baterai sistem sebagai fallback.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback ke halaman daftar optimasi baterai jika intent spesifik ditolak OEM
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {
                    openAppDetailsSettings(context)
                }
            }
        }
    }

    /**
     * Membuka halaman pengaturan notifikasi aplikasi secara spesifik.
     */
    fun openNotificationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                openAppDetailsSettings(context)
            }
        } else {
            openAppDetailsSettings(context)
        }
    }

    /**
     * Membuka info detail aplikasi di Settings sistem (halaman izin utama).
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignored
        }
    }
}

