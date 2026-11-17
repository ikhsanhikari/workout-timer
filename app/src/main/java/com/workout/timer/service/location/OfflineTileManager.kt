package com.workout.timer.service.location

import android.content.Context
import android.util.Log
import com.workout.timer.data.model.LocationPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data model wilayah kota / area untuk download peta offline.
 */
data class OfflineRegion(
    val id: String,
    val name: String,
    val description: String,
    val boundingBox: BoundingBox,
    val minZoom: Int = 12,
    val maxZoom: Int = 16,
    val estimatedMb: Int
)

/**
 * State proses download peta offline.
 */
data class TileDownloadProgress(
    val regionId: String? = null,
    val isDownloading: Boolean = false,
    val progress: Float = 0f, // 0.0 .. 1.0
    val currentCount: Int = 0,
    val totalCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Pengelola unduhan tile peta offline menggunakan OSMDroid CacheManager.
 */
@Singleton
class OfflineTileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _downloadState = MutableStateFlow(TileDownloadProgress())
    val downloadState: StateFlow<TileDownloadProgress> = _downloadState.asStateFlow()

    /**
     * Daftar preset wilayah kota populer di Indonesia untuk download offline cepat.
     */
    val presetRegions: List<OfflineRegion> = listOf(
        OfflineRegion(
            id = "jakarta",
            name = "DKI Jakarta & Sekitar",
            description = "Monas, Sudirman, Senayan, GBK, Kelapa Gading",
            boundingBox = BoundingBox(-6.08, 106.98, -6.38, 106.68),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 28
        ),
        OfflineRegion(
            id = "bandung",
            name = "Kota Bandung",
            description = "Gasibu, Dago, Saparua, ITB, Alun-alun",
            boundingBox = BoundingBox(-6.86, 107.68, -6.97, 107.56),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 18
        ),
        OfflineRegion(
            id = "surabaya",
            name = "Kota Surabaya",
            description = "Tunjungan, Kertajaya, Mayjen Sungkono",
            boundingBox = BoundingBox(-7.22, 112.82, -7.34, 112.68),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 22
        ),
        OfflineRegion(
            id = "yogyakarta",
            name = "Yogyakarta & UGM",
            description = "Malioboro, UGM, Alun-alun, Stadion Mandala Krida",
            boundingBox = BoundingBox(-7.74, 110.42, -7.83, 110.33),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 15
        ),
        OfflineRegion(
            id = "bali",
            name = "Denpasar & Sanur (Bali)",
            description = "Renon, Sanur Beach, Puputan Badung",
            boundingBox = BoundingBox(-8.62, 115.28, -8.72, 115.18),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 16
        )
    )

    /**
     * Membuat BoundingBox untuk "Area Saya" berdasarkan posisi GPS saat ini (radius ~5km).
     */
    fun createMyAreaRegion(latitude: Double, longitude: Double): OfflineRegion {
        val deltaLat = 0.045 // ~5km
        val deltaLng = 0.045 // ~5km
        return OfflineRegion(
            id = "my_area",
            name = "Area Sekitar Saya",
            description = "Radius ~5 KM dari lokasi GPS Anda saat ini",
            boundingBox = BoundingBox(
                latitude + deltaLat,
                longitude + deltaLng,
                latitude - deltaLat,
                longitude - deltaLng
            ),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 12
        )
    }

    /**
     * Memulai pengunduhan tile untuk suatu wilayah secara asynchronous.
     */
    fun downloadRegion(region: OfflineRegion, mapView: MapView) {
        if (_downloadState.value.isDownloading) return

        _downloadState.value = TileDownloadProgress(
            regionId = region.id,
            isDownloading = true,
            progress = 0f,
            currentCount = 0,
            totalCount = 0
        )

        try {
            val cacheManager = CacheManager(mapView)
            val totalTiles = cacheManager.possibleTilesInArea(
                region.boundingBox,
                region.minZoom,
                region.maxZoom
            )

            _downloadState.value = _downloadState.value.copy(totalCount = totalTiles)

            cacheManager.downloadAreaAsync(
                context,
                region.boundingBox,
                region.minZoom,
                region.maxZoom,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        _downloadState.value = TileDownloadProgress(
                            regionId = region.id,
                            isDownloading = false,
                            progress = 1.0f,
                            currentCount = totalTiles,
                            totalCount = totalTiles
                        )
                        Log.i("OfflineTileManager", "Download region '${region.name}' selesai!")
                    }

                    override fun onTaskFailed(errors: Int) {
                        _downloadState.value = TileDownloadProgress(
                            regionId = region.id,
                            isDownloading = false,
                            progress = 0f,
                            errorMessage = "Download gagal dengan $errors error jaringan."
                        )
                        Log.e("OfflineTileManager", "Download region '${region.name}' gagal: $errors errors")
                    }

                    override fun updateProgress(progress: Int, current: Int, total: Int, percent: Int) {
                        _downloadState.value = _downloadState.value.copy(
                            progress = percent / 100f,
                            currentCount = current,
                            totalCount = total
                        )
                    }

                    override fun downloadStarted() {
                        Log.d("OfflineTileManager", "Download started untuk ${region.name}")
                    }

                    override fun setPossibleTilesInArea(total: Int) {
                        _downloadState.value = _downloadState.value.copy(totalCount = total)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("OfflineTileManager", "Gagal inisialisasi download cache: ${e.message}", e)
            _downloadState.value = TileDownloadProgress(
                regionId = region.id,
                isDownloading = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Menghitung total ukuran cache tile peta yang tersimpan di storage (dalam MB).
     */
    suspend fun getCacheSizeMb(): Float = withContext(Dispatchers.IO) {
        try {
            val cacheDir = Configuration.getInstance().osmdroidTileCache
            if (cacheDir != null && cacheDir.exists()) {
                val totalBytes = calculateFolderSize(cacheDir)
                return@withContext totalBytes / (1024f * 1024f)
            }
        } catch (e: Exception) {
            Log.e("OfflineTileManager", "Gagal hitung ukuran cache: ${e.message}")
        }
        return@withContext 0f
    }

    /**
     * Menghapus seluruh cache tile offline.
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = Configuration.getInstance().osmdroidTileCache
            if (cacheDir != null && cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("OfflineTileManager", "Gagal hapus cache: ${e.message}")
        }
        return@withContext false
    }

    private fun calculateFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += calculateFolderSize(child)
        }
        return size
    }
}
