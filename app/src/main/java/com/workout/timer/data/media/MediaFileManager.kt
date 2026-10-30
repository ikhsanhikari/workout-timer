package com.workout.timer.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.workout.timer.data.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

import androidx.core.content.FileProvider
import java.util.Locale

/**
 * MediaFileManager — Pipeline media kustom (F-05).
 *
 * Tanggung jawab:
 * 1. **Import & Validasi**: Menyalin dan memvalidasi file video/GIF dari Photo Picker atau Kamera
 *    ke penyimpanan internal aplikasi (`files/workout_media/`).
 * 2. **Workout Rules Validation**: Memastikan durasi video (2-90 detik), ukuran file (<60MB),
 *    serta integritas video track masuk akal untuk aplikasi workout timer.
 * 3. **Thumbnail Extraction**: Mengekstrak frame pertama video sebagai
 *    thumbnail JPG menggunakan [MediaMetadataRetriever].
 * 4. **Garbage Collection (GC)**: Menghapus file fisik yang tidak lagi
 *    direferensikan oleh entitas database mana pun.
 *
 * Semua operasi I/O berjalan di [Dispatchers.IO].
 */
@Singleton
class MediaFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Direktori penyimpanan media internal: `/data/data/<package>/files/workout_media/` */
    val mediaDir: File by lazy {
        File(context.filesDir, MEDIA_DIR_NAME).also { it.mkdirs() }
    }

    /** Direktori penyimpanan thumbnail: `/data/data/<package>/files/workout_thumbnails/` */
    val thumbnailDir: File by lazy {
        File(context.filesDir, THUMBNAIL_DIR_NAME).also { it.mkdirs() }
    }

    companion object {
        const val MEDIA_DIR_NAME = "workout_media"
        const val THUMBNAIL_DIR_NAME = "workout_thumbnails"
        const val CAMERA_TEMP_DIR_NAME = "camera_videos"
        const val THUMBNAIL_QUALITY = 80 // JPEG quality (0-100)
        const val THUMBNAIL_MAX_SIZE_PX = 480 // Maksimal dimensi thumbnail

        // ── Aturan Validasi Khusus Aplikasi Workout ──
        const val MIN_VIDEO_DURATION_MS = 2_000L    // Minimal 2 detik untuk melihat bentuk gerakan
        const val MAX_VIDEO_DURATION_MS = 90_000L   // Maksimal 90 detik untuk klip loop demo gerakan
        const val MAX_VIDEO_SIZE_BYTES = 60 * 1024 * 1024L  // Maksimal 60 MB
        const val MAX_GIF_SIZE_BYTES = 25 * 1024 * 1024L    // Maksimal 25 MB
    }

    /**
     * Hasil operasi import media.
     */
    sealed class ImportResult {
        data class Success(
            val savedPath: String,
            val thumbnailPath: String?,
            val mediaType: MediaType,
            val durationMs: Long = 0L
        ) : ImportResult()

        data class ValidationError(val message: String) : ImportResult()

        data class Error(val message: String, val cause: Throwable? = null) : ImportResult()
    }

    sealed class ValidationResult {
        data class Valid(val durationMs: Long = 0L) : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    /**
     * Memvalidasi file video atau GIF agar masuk akal untuk aplikasi panduan latihan.
     */
    suspend fun validateMedia(file: File, mediaType: MediaType): ValidationResult = withContext(Dispatchers.IO) {
        val fileSize = file.length()
        if (fileSize <= 0) {
            return@withContext ValidationResult.Invalid("File media kosong atau gagal direkam.")
        }

        if (mediaType == MediaType.GIF) {
            if (fileSize > MAX_GIF_SIZE_BYTES) {
                val sizeMb = fileSize / (1024.0 * 1024.0)
                return@withContext ValidationResult.Invalid(
                    String.format(Locale.getDefault(), "Ukuran GIF terlalu besar (%.1f MB). Maksimal 25 MB.", sizeMb)
                )
            }
            return@withContext ValidationResult.Valid()
        }

        if (mediaType == MediaType.VIDEO) {
            if (fileSize > MAX_VIDEO_SIZE_BYTES) {
                val sizeMb = fileSize / (1024.0 * 1024.0)
                return@withContext ValidationResult.Invalid(
                    String.format(Locale.getDefault(), "Ukuran video terlalu besar (%.1f MB). Maksimal 60 MB untuk demo gerakan.", sizeMb)
                )
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                if (hasVideo != "yes") {
                    return@withContext ValidationResult.Invalid("File ini tidak memiliki trek video yang valid.")
                }

                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L

                if (durationMs < MIN_VIDEO_DURATION_MS) {
                    val sec = (durationMs + 999) / 1000
                    return@withContext ValidationResult.Invalid(
                        "Durasi video terlalu singkat ($sec dtk). Minimal 2 detik agar gerakan latihan terlihat jelas."
                    )
                }

                if (durationMs > MAX_VIDEO_DURATION_MS) {
                    val sec = durationMs / 1000
                    return@withContext ValidationResult.Invalid(
                        "Durasi video terlalu panjang ($sec dtk). Maksimal 90 detik agar pas untuk demo gerakan."
                    )
                }

                return@withContext ValidationResult.Valid(durationMs)
            } catch (e: Exception) {
                return@withContext ValidationResult.Invalid("Format video tidak dapat diproses: ${e.localizedMessage ?: "File rusak"}")
            } finally {
                retriever.release()
            }
        }

        ValidationResult.Valid()
    }

    /**
     * Membuat URI sementara menggunakan FileProvider untuk merekam video langsung dari kamera.
     */
    fun createTempCameraVideoUri(): Pair<File, Uri> {
        val tempDir = File(context.cacheDir, CAMERA_TEMP_DIR_NAME).also { it.mkdirs() }
        val tempFile = File(tempDir, "record_${System.currentTimeMillis()}.mp4")
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, tempFile)
        return Pair(tempFile, uri)
    }

    /**
     * Import file media dari URI Android Photo Picker / Content URI / FileProvider ke penyimpanan internal.
     * Melakukan validasi durasi dan ukuran file sebelum disimpan permanen.
     */
    suspend fun importMedia(
        sourceUri: Uri,
        customFileName: String? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("temp_import_", ".tmp", context.cacheDir)
        try {
            val rawMimeType = context.contentResolver.getType(sourceUri)
            val mediaType = detectMediaType(rawMimeType, sourceUri)

            if (mediaType == MediaType.NONE) {
                return@withContext ImportResult.ValidationError("Format file tidak didukung: $rawMimeType. Gunakan MP4, WebM, atau GIF.")
            }

            // Salin ke file temporer untuk validasi terlebih dahulu
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 8192)
                }
            } ?: return@withContext ImportResult.Error("Tidak bisa membuka file sumber")

            // Validasi karakteristik video/GIF untuk workout
            when (val validation = validateMedia(tempFile, mediaType)) {
                is ValidationResult.Invalid -> {
                    return@withContext ImportResult.ValidationError(validation.message)
                }
                is ValidationResult.Valid -> {
                    // Validasi sukses -> Salin ke direktori permanen workout_media
                    val extension = getExtension(rawMimeType, sourceUri)
                    val fileName = customFileName?.let { sanitize(it) + extension }
                        ?: "media_${System.currentTimeMillis()}$extension"

                    val destFile = File(mediaDir, fileName)
                    tempFile.copyTo(destFile, overwrite = true)

                    // Ekstrak thumbnail jika video
                    val thumbnailPath = if (mediaType == MediaType.VIDEO) {
                        extractVideoThumbnail(destFile)
                    } else {
                        null
                    }

                    ImportResult.Success(
                        savedPath = destFile.absolutePath,
                        thumbnailPath = thumbnailPath,
                        mediaType = mediaType,
                        durationMs = validation.durationMs
                    )
                }
            }
        } catch (e: IOException) {
            ImportResult.Error("Gagal menyalin file: ${e.message}", e)
        } catch (e: SecurityException) {
            ImportResult.Error("Izin akses ditolak: ${e.message}", e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Ekstrak frame pertama video sebagai thumbnail JPEG.
     *
     * Menggunakan [MediaMetadataRetriever] bawaan Android.
     * Frame diambil pada posisi 0 μs (frame pertama).
     *
     * @param videoFile File video sumber.
     * @return Path absolut thumbnail yang tersimpan, atau null jika gagal.
     */
    suspend fun extractVideoThumbnail(videoFile: File): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@withContext null

            val scaledBitmap = scaleBitmap(bitmap, THUMBNAIL_MAX_SIZE_PX)

            val thumbFileName = "thumb_${videoFile.nameWithoutExtension}.jpg"
            val thumbFile = File(thumbnailDir, thumbFileName)

            FileOutputStream(thumbFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }

            if (!bitmap.isRecycled) bitmap.recycle()
            if (!scaledBitmap.isRecycled) scaledBitmap.recycle()

            thumbFile.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Garbage Collection: Hapus file fisik yang tidak lagi direferensikan.
     *
     * Dipanggil oleh Repository setelah menghapus exercise/routine.
     *
     * @param mediaUri Path absolut file yang potensial bisa dihapus.
     * @param referenceCount Jumlah entitas database yang masih mereferensikan file ini.
     *   Jika 0, file fisik akan dihapus.
     */
    suspend fun deleteIfUnreferenced(
        mediaUri: String,
        referenceCount: Int
    ) = withContext(Dispatchers.IO) {
        if (referenceCount > 0) return@withContext

        val mediaFile = File(mediaUri)
        if (mediaFile.exists()) {
            mediaFile.delete()
        }

        // Hapus thumbnail terkait jika ada
        val thumbFileName = "thumb_${mediaFile.nameWithoutExtension}.jpg"
        val thumbFile = File(thumbnailDir, thumbFileName)
        if (thumbFile.exists()) {
            thumbFile.delete()
        }
    }

    /**
     * Hapus file media dan thumbnail-nya langsung tanpa mengecek referensi.
     * Gunakan hanya ketika yakin file tidak direferensikan lagi.
     */
    suspend fun deleteMediaFile(absolutePath: String) = withContext(Dispatchers.IO) {
        val file = File(absolutePath)
        if (file.exists()) file.delete()
    }

    /**
     * Hitung total ukuran folder media internal (dalam byte).
     * Digunakan di Settings untuk info penggunaan storage.
     */
    suspend fun calculateMediaStorageUsage(): Long = withContext(Dispatchers.IO) {
        (mediaDir.listFiles()?.sumOf { it.length() } ?: 0L) +
                (thumbnailDir.listFiles()?.sumOf { it.length() } ?: 0L)
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private fun detectMediaType(mimeType: String?, sourceUri: Uri? = null): MediaType {
        val lowerMime = mimeType?.lowercase()?.trim()
        if (lowerMime != null) {
            when {
                lowerMime.startsWith("video/") -> return MediaType.VIDEO
                lowerMime == "image/gif" -> return MediaType.GIF
            }
        }
        val fileName = sourceUri?.let { getFileName(it) }?.lowercase() ?: ""
        return when {
            fileName.endsWith(".gif") -> MediaType.GIF
            fileName.endsWith(".mp4") || fileName.endsWith(".webm") || fileName.endsWith(".mkv") -> MediaType.VIDEO
            else -> MediaType.NONE
        }
    }

    private fun getExtension(mimeType: String?, sourceUri: Uri? = null): String {
        val lowerMime = mimeType?.lowercase()?.trim()
        when (lowerMime) {
            "video/mp4" -> return ".mp4"
            "video/webm" -> return ".webm"
            "video/x-matroska" -> return ".mkv"
            "image/gif" -> return ".gif"
        }
        val fileName = sourceUri?.let { getFileName(it) }?.lowercase() ?: ""
        return when {
            fileName.endsWith(".gif") -> ".gif"
            fileName.endsWith(".mp4") -> ".mp4"
            fileName.endsWith(".webm") -> ".webm"
            fileName.endsWith(".mkv") -> ".mkv"
            else -> ".bin"
        }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) return cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment
    }

    /** Sanitasi nama file agar aman sebagai nama file sistem. */
    private fun sanitize(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50)

    /**
     * Scale bitmap proporsional agar sisi terpanjang tidak melebihi [maxSize].
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
