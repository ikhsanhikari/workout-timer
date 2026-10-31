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
    data class Success(val newVersion: Int, val updatedCount: Int, val details: String? = null) : SyncResult
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
         * Mendukung otomatis branch master atau main.
         */
        const val DEFAULT_CATALOG_URL =
            "https://raw.githubusercontent.com/ikhsanhikari/workout-timer/main/workout_catalog.json"

        const val FALLBACK_CATALOG_URL =
            "https://raw.githubusercontent.com/ikhsanhikari/workout-timer/master/workout_catalog.json"

        const val DEFAULT_EXERCISE_LIBRARY_URL =
            "https://raw.githubusercontent.com/ikhsanhikari/workout-timer/main/exercise_library.json"

        const val FALLBACK_EXERCISE_LIBRARY_URL =
            "https://raw.githubusercontent.com/ikhsanhikari/workout-timer/master/exercise_library.json"

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
     * Melakukan pengecekan dan sinkronisasi seluruh data (Katalog Rutinitas + Kamus Gerakan) dari GitHub.
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
            ?: userPreferences.getCatalogUrl()?.takeIf { !it.contains("Nurikhsan/workout-v2") }
            ?: DEFAULT_CATALOG_URL

        Log.d(TAG, "Memulai sinkronisasi katalog dari: $targetUrl")

        try {
            var jsonString: String? = fetchJsonString(targetUrl)

            // Jika URL default mengembalikan null/404, coba fallback ke branch master
            if (jsonString == null && overrideUrl == null && userPreferences.getCatalogUrl() == null) {
                Log.d(TAG, "Mencoba fallback branch master: $FALLBACK_CATALOG_URL")
                jsonString = fetchJsonString(FALLBACK_CATALOG_URL)
            }

            var catalogUpdated = false
            var updatedRoutineCount = 0
            var catalogVersion = userPreferences.getCatalogVersion()

            if (jsonString != null) {
                val remoteCatalog = CatalogImporter.parseCatalogJson(jsonString)
                if (remoteCatalog != null) {
                    val localVersion = userPreferences.getCatalogVersion()
                    if (force || remoteCatalog.catalogVersion > localVersion) {
                        updatedRoutineCount = CatalogImporter.importCatalog(
                            database = database,
                            catalog = remoteCatalog,
                            onlyIfMissing = false
                        )
                        catalogVersion = remoteCatalog.catalogVersion
                        userPreferences.saveCatalogVersion(remoteCatalog.catalogVersion)
                        catalogUpdated = true
                        Log.i(TAG, "Katalog rutinitas berhasil diperbarui ke v$catalogVersion.")
                    }
                }
            }

            // Sync Master Kamus Gerakan (exercise_library.json)
            val libraryResult = syncExerciseLibrary(force = force)
            val libraryUpdated = libraryResult is SyncResult.Success

            if (catalogUpdated || libraryUpdated) {
                userPreferences.recordSyncTimestamp()
                val details = if (libraryUpdated) "Katalog & Kamus Gerakan terupdate" else "$updatedRoutineCount rutinitas terupdate"
                SyncResult.Success(
                    newVersion = catalogVersion,
                    updatedCount = updatedRoutineCount,
                    details = details
                )
            } else {
                SyncResult.AlreadyUpToDate(catalogVersion)
            }

        } catch (e: Exception) {
            val errorMsg = "Koneksi gagal: ${e.localizedMessage ?: e.message}"
            Log.e(TAG, errorMsg, e)
            SyncResult.Error(errorMsg)
        }
    }

    /**
     * Menyinkronkan Master Kamus Gerakan dari GitHub.
     */
    suspend fun syncExerciseLibrary(
        force: Boolean = false,
        overrideUrl: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext SyncResult.NoNetwork
        }

        val targetUrl = overrideUrl
            ?: userPreferences.getExerciseLibraryUrl()?.takeIf { !it.contains("Nurikhsan/workout-v2") }
            ?: DEFAULT_EXERCISE_LIBRARY_URL

        try {
            var jsonString: String? = fetchJsonString(targetUrl)
            if (jsonString == null && overrideUrl == null && userPreferences.getExerciseLibraryUrl() == null) {
                jsonString = fetchJsonString(FALLBACK_EXERCISE_LIBRARY_URL)
            }

            if (jsonString == null) {
                return@withContext SyncResult.Error("File exercise library tidak ditemukan di GitHub.")
            }

            val remoteLibrary = CatalogImporter.parseExerciseLibraryJson(jsonString)
                ?: return@withContext SyncResult.Error("Format JSON exercise library tidak valid.")

            val localVersion = userPreferences.getExerciseLibraryVersion()
            if (!force && remoteLibrary.libraryVersion <= localVersion) {
                return@withContext SyncResult.AlreadyUpToDate(localVersion)
            }

            val updatedCount = CatalogImporter.importExerciseLibrary(database, remoteLibrary)
            userPreferences.saveExerciseLibraryVersion(remoteLibrary.libraryVersion)

            Log.i(TAG, "Master Kamus Gerakan sukses disinkronkan: $updatedCount gerakan (v${remoteLibrary.libraryVersion}).")
            SyncResult.Success(newVersion = remoteLibrary.libraryVersion, updatedCount = updatedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal sinkronisasi kamus gerakan: ${e.message}", e)
            SyncResult.Error(e.localizedMessage ?: "Gagal sync kamus gerakan")
        }
    }

    private fun fetchJsonString(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "WorkoutTimer-Android")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = BufferedReader(InputStreamReader(connection.inputStream)).use {
                    it.readText()
                }
                connection.disconnect()
                content
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
