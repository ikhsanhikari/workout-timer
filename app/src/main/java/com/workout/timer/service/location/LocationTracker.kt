package com.workout.timer.service.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper FusedLocationProviderClient untuk stream GPS real-time dengan akurasi tinggi.
 */
@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var activeCallback: LocationCallback? = null

    /**
     * Memeriksa apakah izin lokasi sudah diberikan pengguna.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Memulai pembaruan lokasi reguler untuk foreground tracking.
     * Menggunakan interval adaptif (default 3000ms, fastest 1500ms) dengan filter min distance 0.5m.
     * Mengambil last known location secara instan agar titik lokasi langsung muncul di detik awal.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        intervalMs: Long = 3000L,
        minDistanceMeters: Float = 0.5f,
        onLocationReceived: (Location) -> Unit
    ) {
        if (!hasLocationPermission()) {
            Log.w("LocationTracker", "Izin lokasi belum diberikan.")
            return
        }

        stopLocationUpdates()

        // 1. Ambil lokasi terakhir yang tersimpan secara instan (detik 0)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    onLocationReceived(lastLoc)
                }
            }
        } catch (_: Exception) {}

        // 2. Daftarkan pembaruan berkala GPS
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    // Filter outlier: toleransi akurasi hingga 60 meter agar tidak drop saat awal / indoor
                    if (location.accuracy <= 60f) {
                        onLocationReceived(location)
                    }
                }
            }
        }

        activeCallback = callback
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            Log.d("LocationTracker", "Mulai requestLocationUpdates (interval: ${intervalMs}ms, minDistance: ${minDistanceMeters}m)")
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "SecurityException saat request location: ${e.message}")
        }
    }

    /**
     * Menghentikan pembaruan lokasi.
     */
    fun stopLocationUpdates() {
        activeCallback?.let { callback ->
            try {
                fusedLocationClient.removeLocationUpdates(callback)
                Log.d("LocationTracker", "removeLocationUpdates dihentikan")
            } catch (e: Exception) {
                Log.e("LocationTracker", "Gagal menghentikan pembaruan lokasi: ${e.message}")
            }
        }
        activeCallback = null
    }

    /**
     * Mengambil lokasi terakhir yang diketahui secara instan.
     */
    @SuppressLint("MissingPermission")
    fun getLastLocation(onResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc -> onResult(loc) }
                .addOnFailureListener { onResult(null) }
        } catch (e: SecurityException) {
            onResult(null)
        }
    }

    /**
     * Flow representasi lokasi GPS untuk Compose / UI observation jika diperlukan.
     */
    @SuppressLint("MissingPermission")
    fun getLocationFlow(intervalMs: Long = 3000L): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    if (loc.accuracy <= 30f) {
                        trySend(loc)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
