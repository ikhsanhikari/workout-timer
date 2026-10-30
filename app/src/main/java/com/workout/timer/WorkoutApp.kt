package com.workout.timer

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
