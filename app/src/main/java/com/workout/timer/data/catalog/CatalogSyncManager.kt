package com.workout.timer.data.catalog

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.workout.timer.data.database.WorkoutDatabase
import com.workout.timer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncResult {
    data class Success(val newVersion: Int, val updatedCount: Int) : SyncResult
    data class AlreadyUpToDate(val currentVersion: Int) : SyncResult
    data class Error(val message: String) : SyncResult
    object NoNetwork : SyncResult
}

/**
 * Manager untuk mendownload dan menyinkronkan data katalog workout dari GitHub Raw URL
 * ke Room Database secara dinamis tanpa memerlukan update APK.
 */
@Singleton
class CatalogSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: WorkoutDatabase,
    private val userPreferences: UserPreferencesRepository
) {

    companion object {
        private const val TAG = "CatalogSyncManager"

        /**
         * URL default GitHub Raw ke file workout_catalog.json di repository.
         * Pengguna/developer dapat mengubah URL ini kapan saja via preferensi.
         */
        const val DEFAULT_CATALOG_URL =
            "https://raw.githubusercontent.com/Nurikhsan/workout-v2/main/workout_catalog.json"

        private const val TIMEOUT_MS = 10_000
    }

    /**
     * Memeriksa apakah perangkat terhubung ke internet.
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Melakukan pengecekan dan sinkronisasi katalog dari server GitHub.
     *
     * @param force Jika true, paksa update meskipun nomor versi sama.
     * @param overrideUrl URL opsional untuk testing atau custom source.
     */
    suspend fun sync(
        force: Boolean = false,
        overrideUrl: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Sinkronisasi dibatalkan: Perangkat sedang offline.")
            return@withContext SyncResult.NoNetwork
        }

        val targetUrl = overrideUrl
            ?: userPreferences.getCatalogUrl()
            ?: DEFAULT_CATALOG_URL

        Log.d(TAG, "Memulai sinkronisasi katalog dari: $targetUrl")

        try {
            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "WorkoutTimer-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = "Server merespons kode HTTP $responseCode"
                Log.w(TAG, errorMsg)
                return@withContext SyncResult.Error(errorMsg)
            }

            val jsonString = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }
            connection.disconnect()

            val remoteCatalog = CatalogImporter.parseCatalogJson(jsonString)
                ?: return@withContext SyncResult.Error("Gagal membaca format JSON dari server.")

            val localVersion = userPreferences.getCatalogVersion()

            Log.d(TAG, "Versi Lokal: $localVersion | Versi Remote: ${remoteCatalog.catalogVersion}")

            if (!force && remoteCatalog.catalogVersion <= localVersion) {
                Log.d(TAG, "Katalog lokal sudah yang terbaru (Versi $localVersion). Tidak ada pembaruan.")
                return@withContext SyncResult.AlreadyUpToDate(localVersion)
            }

            // Jalankan transaksi update ke Room Database
            val updatedCount = CatalogImporter.importCatalog(
                database = database,
                catalog = remoteCatalog,
                onlyIfMissing = false
            )

            // Simpan nomor versi baru & waktu sync
            userPreferences.saveCatalogVersion(remoteCatalog.catalogVersion)
            userPreferences.recordSyncTimestamp()

            Log.i(TAG, "Sinkronisasi sukses! Versi baru: ${remoteCatalog.catalogVersion}, $updatedCount rutinitas diproses.")
            SyncResult.Success(newVersion = remoteCatalog.catalogVersion, updatedCount = updatedCount)

        } catch (e: Exception) {
            val errorMsg = "Koneksi gagal: ${e.localizedMessage ?: e.message}"
            Log.e(TAG, errorMsg, e)
            SyncResult.Error(errorMsg)
        }
    }
}
