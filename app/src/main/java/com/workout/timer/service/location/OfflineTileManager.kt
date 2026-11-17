package com.workout.timer.service.location

import android.content.Context
import android.location.Location
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
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
    val minZoom: Int = 13,
    val maxZoom: Int = 16,
    val estimatedMb: Int,
    val distanceKmFromUser: Float? = null
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private val sqlTileWriter by lazy { SqlTileWriter() }

    private val _downloadState = MutableStateFlow(TileDownloadProgress())
    val downloadState: StateFlow<TileDownloadProgress> = _downloadState.asStateFlow()

    /**
     * Daftar preset wilayah kota & spot lari populer di Indonesia.
     */
    val presetRegions: List<OfflineRegion> = listOf(
        // Jabodetabek
        OfflineRegion(
            id = "jakarta",
            name = "DKI Jakarta",
            description = "GBK Senayan, Monas, Sudirman-Thamrin, Tebet Eco Park",
            boundingBox = BoundingBox(-6.10, 106.96, -6.36, 106.70),
            estimatedMb = 28
        ),
        OfflineRegion(
            id = "bogor",
            name = "Kota & Kab. Bogor",
            description = "Kebun Raya Bogor, Sempur, Pajajaran, Sentul City",
            boundingBox = BoundingBox(-6.53, 106.84, -6.65, 106.75),
            estimatedMb = 16
        ),
        OfflineRegion(
            id = "depok",
            name = "Kota Depok",
            description = "Kampus UI, Margonda Raya, Alun-Alun Grand Depok City",
            boundingBox = BoundingBox(-6.34, 106.86, -6.44, 106.78),
            estimatedMb = 14
        ),
        OfflineRegion(
            id = "tangerang",
            name = "Tangerang & BSD City",
            description = "The Breeze, ICE BSD, Scientia Square Park, Alam Sutera",
            boundingBox = BoundingBox(-6.16, 106.69, -6.33, 106.58),
            estimatedMb = 20
        ),
        OfflineRegion(
            id = "bekasi",
            name = "Kota Bekasi",
            description = "Summarecon Bekasi, Stadion Patriot, Ahmad Yani",
            boundingBox = BoundingBox(-6.20, 107.03, -6.29, 106.95),
            estimatedMb = 15
        ),

        // Jawa Barat & Tengah
        OfflineRegion(
            id = "bandung",
            name = "Kota Bandung",
            description = "Lapangan Gasibu, Dago, GOR Saparua, ITB, Kiara Artha",
            boundingBox = BoundingBox(-6.86, 107.67, -6.96, 107.57),
            estimatedMb = 18
        ),
        OfflineRegion(
            id = "semarang",
            name = "Kota Semarang",
            description = "Simpang Lima, Tri Lomba Juang, Kota Lama, Diponegoro",
            boundingBox = BoundingBox(-6.96, 110.45, -7.05, 110.37),
            estimatedMb = 16
        ),
        OfflineRegion(
            id = "solo",
            name = "Solo (Surakarta)",
            description = "Stadion Manahan, Jalan Slamet Riyadi, Balekambang",
            boundingBox = BoundingBox(-7.53, 110.86, -7.59, 110.78),
            estimatedMb = 14
        ),
        OfflineRegion(
            id = "yogyakarta",
            name = "Yogyakarta & Sleman",
            description = "Kawasan UGM, Mandala Krida, Malioboro, Alun-Alun",
            boundingBox = BoundingBox(-7.73, 110.42, -7.83, 110.33),
            estimatedMb = 15
        ),

        // Jawa Timur
        OfflineRegion(
            id = "surabaya",
            name = "Kota Surabaya",
            description = "Taman Bungkul, Tunjungan, Mayjen Sungkono, Kertajaya",
            boundingBox = BoundingBox(-7.23, 112.81, -7.33, 112.69),
            estimatedMb = 22
        ),
        OfflineRegion(
            id = "malang",
            name = "Kota Malang",
            description = "Ijen Boulevard, Lapangan Rampal, Alun-Alun Merdeka",
            boundingBox = BoundingBox(-7.94, 112.66, -8.01, 112.59),
            estimatedMb = 14
        ),

        // Bali & Nusa Tenggara
        OfflineRegion(
            id = "bali_denpasar",
            name = "Denpasar & Sanur (Bali)",
            description = "Lapangan Renon, Pantai Sanur, Bajra Sandhi",
            boundingBox = BoundingBox(-8.63, 115.27, -8.72, 115.19),
            estimatedMb = 16
        ),
        OfflineRegion(
            id = "bali_kuta",
            name = "Kuta, Seminyak & Canggu",
            description = "Jalan Pantai Kuta, Petitenget, Batu Bolong, Echo Beach",
            boundingBox = BoundingBox(-8.64, 115.18, -8.74, 115.12),
            estimatedMb = 16
        ),
        OfflineRegion(
            id = "bali_ubud",
            name = "Ubud (Bali)",
            description = "Campuhan Ridge Walk, Monkey Forest, Penestanan",
            boundingBox = BoundingBox(-8.48, 115.28, -8.54, 115.24),
            estimatedMb = 12
        ),

        // Sumatera
        OfflineRegion(
            id = "medan",
            name = "Kota Medan",
            description = "Merdeka Walk, Lapangan Benteng, Taman Cadika, USU",
            boundingBox = BoundingBox(-3.54, 98.71, -3.63, 98.63),
            estimatedMb = 18
        ),
        OfflineRegion(
            id = "palembang",
            name = "Kota Palembang",
            description = "Jakabaring Sport City, Kambang Iwak, Jembatan Ampera",
            boundingBox = BoundingBox(-2.95, 104.79, -3.03, 104.72),
            estimatedMb = 16
        ),
        OfflineRegion(
            id = "lampung",
            name = "Bandar Lampung",
            description = "Stadion Pahoman, PKOR Way Halim, Teluk Betung",
            boundingBox = BoundingBox(-5.38, 105.31, -5.46, 105.23),
            estimatedMb = 14
        ),
        OfflineRegion(
            id = "pekanbaru",
            name = "Kota Pekanbaru",
            description = "Stadion Utama Riau, Taman Kota, Diponegoro",
            boundingBox = BoundingBox(-0.48, 101.48, -0.56, 101.40),
            estimatedMb = 14
        ),
        OfflineRegion(
            id = "batam",
            name = "Batam Center & Sekupang",
            description = "Alun-Alun Engku Putri, Harbour Bay, Mega Wisata Ocarina",
            boundingBox = BoundingBox(-1.08, 104.07, -1.16, 103.97),
            estimatedMb = 15
        ),

        // Sulawesi & Kalimantan
        OfflineRegion(
            id = "makassar",
            name = "Kota Makassar",
            description = "Pantai Losari, Karebosi, CPI (Center Point of Indonesia)",
            boundingBox = BoundingBox(-5.10, 119.46, -5.19, 119.38),
            estimatedMb = 17
        ),
        OfflineRegion(
            id = "manado",
            name = "Kota Manado",
            description = "Kawasan Megamas, Godbless Park, Boulevard",
            boundingBox = BoundingBox(-1.46, 124.86, -1.52, 124.81),
            estimatedMb = 13
        ),
        OfflineRegion(
            id = "balikpapan",
            name = "Kota Balikpapan",
            description = "Lapangan Merdeka, Pantai Melawai, Grand City",
            boundingBox = BoundingBox(-1.22, 116.89, -1.29, 116.82),
            estimatedMb = 14
        ),
        OfflineRegion(
            id = "samarinda",
            name = "Kota Samarinda",
            description = "Tepian Mahakam, GOR Sempaja, Taman Samarendah",
            boundingBox = BoundingBox(-0.47, 117.18, -0.54, 117.11),
            estimatedMb = 14
        ),
        OfflineRegion(
            id = "banjarmasin",
            name = "Kota Banjarmasin",
            description = "Siring Menara Pandang, Patung Bekantan, Lambung Mangkurat",
            boundingBox = BoundingBox(-3.30, 114.62, -3.36, 114.56),
            estimatedMb = 13
        ),
        OfflineRegion(
            id = "pontianak",
            name = "Kota Pontianak",
            description = "Taman Digulis Untan, Waterfront Kapuas, GOR Pangsuma",
            boundingBox = BoundingBox(-0.01, 109.36, -0.07, 109.31),
            estimatedMb = 13
        )
    )

    /**
     * Mengurutkan daftar wilayah preset berdasarkan jarak terdekat dengan koordinat GPS user saat ini.
     */
    fun getRegionsSortedByProximity(userLat: Double?, userLng: Double?): List<OfflineRegion> {
        if (userLat == null || userLng == null) return presetRegions
        val distResults = FloatArray(1)
        return presetRegions.map { region ->
            val centerLat = (region.boundingBox.latNorth + region.boundingBox.latSouth) / 2.0
            val centerLng = (region.boundingBox.lonEast + region.boundingBox.lonWest) / 2.0
            Location.distanceBetween(userLat, userLng, centerLat, centerLng, distResults)
            val distKm = distResults[0] / 1000f
            region.copy(distanceKmFromUser = distKm)
        }.sortedBy { it.distanceKmFromUser ?: Float.MAX_VALUE }
    }

    /**
     * Membuat BoundingBox untuk "Area Saya" berdasarkan posisi GPS saat ini (radius ~5km).
     */
    fun createMyAreaRegion(latitude: Double, longitude: Double): OfflineRegion {
        val deltaLat = 0.045 // ~5km
        val deltaLng = 0.045 // ~5km
        val north = maxOf(latitude + deltaLat, latitude - deltaLat).coerceIn(-85.0, 85.0)
        val south = minOf(latitude + deltaLat, latitude - deltaLat).coerceIn(-85.0, 85.0)
        val east = maxOf(longitude + deltaLng, longitude - deltaLng).coerceIn(-180.0, 180.0)
        val west = minOf(longitude + deltaLng, longitude - deltaLng).coerceIn(-180.0, 180.0)
        return OfflineRegion(
            id = "my_area",
            name = "Area Sekitar Saya",
            description = "Radius ~5 KM dari posisi GPS Anda saat ini",
            boundingBox = BoundingBox(north, east, south, west),
            minZoom = 13,
            maxZoom = 16,
            estimatedMb = 12,
            distanceKmFromUser = 0f
        )
    }

    /**
     * Membatalkan proses pengunduhan tile yang sedang berjalan.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = TileDownloadProgress(
            isDownloading = false,
            errorMessage = null
        )
        Log.d("OfflineTileManager", "Unduhan tile dibatalkan oleh pengguna.")
    }

    /**
     * Memulai pengunduhan tile untuk suatu wilayah secara asynchronous & aman menggunakan Kotlin Coroutines.
     * Menggunakan SqlTileWriter langsung tanpa AsyncTask / ProgressDialog sehingga 100% bebas dari
     * BadTokenException, TileSourcePolicyException, ataupun force close.
     */
    fun downloadRegion(region: OfflineRegion) {
        cancelDownload()

        downloadJob = scope.launch {
            try {
                _downloadState.value = TileDownloadProgress(
                    regionId = region.id,
                    isDownloading = true,
                    progress = 0f,
                    currentCount = 0,
                    totalCount = 0
                )

                // 1. Dapatkan daftar tile indices yang menutupi wilayah tersebut
                val tileIndices: List<Long> = withContext(Dispatchers.Default) {
                    try {
                        CacheManager.getTilesCoverage(region.boundingBox, region.minZoom, region.maxZoom)
                    } catch (e: Exception) {
                        Log.e("OfflineTileManager", "Gagal kalkulasi tile area: ${e.message}")
                        emptyList()
                    }
                }

                if (tileIndices.isEmpty()) {
                    _downloadState.value = TileDownloadProgress(
                        regionId = region.id,
                        isDownloading = false,
                        errorMessage = "Tidak ada tile untuk wilayah ini"
                    )
                    return@launch
                }

                val totalTiles = tileIndices.size
                _downloadState.value = _downloadState.value.copy(totalCount = totalTiles)

                val userAgent = Configuration.getInstance().userAgentValue
                    ?: context.packageName
                    ?: "GasCount-WorkoutApp/1.0"
                val expirationTime = System.currentTimeMillis() + 30L * 24 * 3600 * 1000 // 30 hari

                var completedCount = 0
                val missingIndices = ArrayList<Long>(totalTiles)

                // 2. Cek apakah ada tile yang sudah tersimpan di cache lokal
                for (tileIndex in tileIndices) {
                    if (sqlTileWriter.exists(TileSourceFactory.MAPNIK, tileIndex)) {
                        completedCount++
                    } else {
                        missingIndices.add(tileIndex)
                    }
                }

                val initialProg = if (totalTiles > 0) completedCount.toFloat() / totalTiles else 0f
                _downloadState.value = _downloadState.value.copy(
                    currentCount = completedCount,
                    progress = initialProg
                )

                // Jika seluruh tile sudah ter-cache
                if (missingIndices.isEmpty()) {
                    _downloadState.value = TileDownloadProgress(
                        regionId = region.id,
                        isDownloading = false,
                        progress = 1.0f,
                        currentCount = totalTiles,
                        totalCount = totalTiles
                    )
                    Log.i("OfflineTileManager", "Wilayah '${region.name}' sudah 100% tersimpan di cache!")
                    return@launch
                }

                // 3. Unduh tile yang belum ada secara concurrent (2 koneksi bersamaan)
                val semaphore = Semaphore(2)
                coroutineScope {
                    missingIndices.map { tileIndex ->
                        async {
                            semaphore.withPermit {
                                if (!isActive) return@withPermit
                                var conn: HttpURLConnection? = null
                                try {
                                    val urlStr = TileSourceFactory.MAPNIK.getTileURLString(tileIndex)
                                    val url = URL(urlStr)
                                    conn = (url.openConnection() as HttpURLConnection).apply {
                                        setRequestProperty("User-Agent", userAgent)
                                        connectTimeout = 8000
                                        readTimeout = 8000
                                        instanceFollowRedirects = true
                                    }
                                    conn.connect()
                                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                        conn.inputStream.use { input ->
                                            synchronized(sqlTileWriter) {
                                                sqlTileWriter.saveFile(
                                                    TileSourceFactory.MAPNIK,
                                                    tileIndex,
                                                    input,
                                                    expirationTime
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("OfflineTileManager", "Gagal unduh tile $tileIndex: ${e.message}")
                                } finally {
                                    try {
                                        conn?.disconnect()
                                    } catch (_: Exception) {}

                                    synchronized(this@OfflineTileManager) {
                                        completedCount++
                                        val prog = completedCount.toFloat() / totalTiles
                                        // Update state bertahap agar UI Compose smooth
                                        if (completedCount % 4 == 0 || completedCount == totalTiles) {
                                            _downloadState.value = _downloadState.value.copy(
                                                currentCount = completedCount,
                                                progress = prog
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }.awaitAll()
                }

                _downloadState.value = TileDownloadProgress(
                    regionId = region.id,
                    isDownloading = false,
                    progress = 1.0f,
                    currentCount = totalTiles,
                    totalCount = totalTiles
                )
                Log.i("OfflineTileManager", "Unduhan wilayah '${region.name}' sukses ($totalTiles tiles)")
            } catch (ce: CancellationException) {
                Log.i("OfflineTileManager", "Unduhan wilayah '${region.name}' dibatalkan")
                _downloadState.value = TileDownloadProgress(isDownloading = false)
            } catch (t: Throwable) {
                Log.e("OfflineTileManager", "Error unduh tile: ${t.message}", t)
                _downloadState.value = TileDownloadProgress(
                    regionId = region.id,
                    isDownloading = false,
                    errorMessage = "Gagal mengunduh: ${t.localizedMessage ?: "Koneksi bermasalah"}"
                )
            }
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
