package com.workout.timer.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.workout.timer.data.database.dao.ExerciseTemplateDao
import com.workout.timer.data.database.dao.WorkoutBlockDao
import com.workout.timer.data.database.dao.WorkoutExerciseDao
import com.workout.timer.data.database.dao.WorkoutRoutineDao
import com.workout.timer.data.media.MediaFileManager
import com.workout.timer.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocalBackupManager — Implementasi Penuh Backup & Restore (PRD 3. F-07, P1).
 *
 * Strategi:
 * 1. **Export**:
 *    - Query seluruh tabel Room (Routine, Block, Exercise, Template).
 *    - Serialize ke JSON `workout_data.json` via Gson.
 *    - Masukkan file JSON + seluruh file video/GIF dari `workout_media/` ke dalam satu arsip ZIP.
 *    - Tulis ke [destinationUri] via ContentResolver (SAF).
 * 2. **Import**:
 *    - Ekstrak ZIP dari [sourceUri].
 *    - Parsing `workout_data.json` dan restore seluruh entitas ke database Room secara berurutan.
 *    - Salin kembali seluruh file media ke folder internal aplikasi.
 */
@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineDao: WorkoutRoutineDao,
    private val blockDao: WorkoutBlockDao,
    private val exerciseDao: WorkoutExerciseDao,
    private val templateDao: ExerciseTemplateDao,
    private val mediaFileManager: MediaFileManager,
    private val gson: Gson
) {
    companion object {
        const val BACKUP_DATA_FILENAME = "workout_data.json"
        const val BACKUP_MEDIA_FOLDER = "workout_media/"
        const val BACKUP_VERSION = 1
        const val BACKUP_FILE_EXTENSION = ".workout_backup.zip"
    }

    sealed class BackupResult {
        data class Success(val fileSizeBytes: Long) : BackupResult()
        data class Error(val message: String, val cause: Throwable? = null) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val routineCount: Int) : RestoreResult()
        data class Error(val message: String, val cause: Throwable? = null) : RestoreResult()
    }

    /**
     * Struktur data yang di-serialize ke JSON dalam file backup.
     */
    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val exportedAt: Long = System.currentTimeMillis(),
        val routines: List<WorkoutRoutine> = emptyList(),
        val blocks: List<WorkoutBlock> = emptyList(),
        val exercises: List<WorkoutExercise> = emptyList(),
        val templates: List<ExerciseTemplate> = emptyList()
    )

    /**
     * Export semua data ke file ZIP di Uri tujuan.
     */
    suspend fun exportBackup(destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil seluruh data dari database
            val routines = routineDao.getAll()
            val blocks = blockDao.getAll()
            val exercises = exerciseDao.getAll()
            val templates = templateDao.getAll()

            val backupData = BackupData(
                version = BACKUP_VERSION,
                exportedAt = System.currentTimeMillis(),
                routines = routines,
                blocks = blocks,
                exercises = exercises,
                templates = templates
            )

            // 2. Serialize ke JSON
            val jsonString = gson.toJson(backupData)

            // 3. Tulis ZIP ke ContentResolver
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext BackupResult.Error("Tidak dapat membuka output stream untuk file backup")

            var totalBytesWritten = 0L

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // Entry 1: workout_data.json
                val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
                val jsonEntry = ZipEntry(BACKUP_DATA_FILENAME)
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(jsonBytes)
                zipOut.closeEntry()
                totalBytesWritten += jsonBytes.size

                // Entry 2+: File media internal
                val mediaDir = mediaFileManager.mediaDir
                val mediaFiles = mediaDir.listFiles() ?: emptyArray()

                for (file in mediaFiles) {
                    if (file.isFile) {
                        val mediaEntry = ZipEntry("$BACKUP_MEDIA_FOLDER${file.name}")
                        zipOut.putNextEntry(mediaEntry)
                        FileInputStream(file).buffered().use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                zipOut.write(buffer, 0, read)
                                totalBytesWritten += read
                            }
                        }
                        zipOut.closeEntry()
                    }
                }
            }

            BackupResult.Success(totalBytesWritten)
        } catch (e: Exception) {
            BackupResult.Error("Gagal membuat backup: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    /**
     * Import & restore data dari file ZIP backup.
     */
    suspend fun importBackup(sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext RestoreResult.Error("Tidak dapat membaca file backup")

            var parsedBackupData: BackupData? = null
            val mediaDir = mediaFileManager.mediaDir
            if (!mediaDir.exists()) mediaDir.mkdirs()

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == BACKUP_DATA_FILENAME) {
                        val reader = InputStreamReader(zipIn, Charsets.UTF_8)
                        parsedBackupData = gson.fromJson(reader, BackupData::class.java)
                    } else if (entryName.startsWith(BACKUP_MEDIA_FOLDER) && !entry.isDirectory) {
                        val fileName = entryName.removePrefix(BACKUP_MEDIA_FOLDER)
                        val targetFile = File(mediaDir, fileName)
                        FileOutputStream(targetFile).buffered().use { out ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (zipIn.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            val data = parsedBackupData
                ?: return@withContext RestoreResult.Error("File backup tidak valid: $BACKUP_DATA_FILENAME tidak ditemukan")

            // 4. Restore ke database (hapus data lama dan masukkan data backup)
            exerciseDao.deleteAll()
            blockDao.deleteAll()
            routineDao.deleteAll()
            templateDao.deleteAll()

            if (data.templates.isNotEmpty()) {
                templateDao.insertOrReplaceAll(data.templates)
            }
            if (data.routines.isNotEmpty()) {
                routineDao.insertAll(data.routines)
            }
            if (data.blocks.isNotEmpty()) {
                blockDao.insertAll(data.blocks)
            }
            if (data.exercises.isNotEmpty()) {
                exerciseDao.insertAll(data.exercises)
            }

            RestoreResult.Success(data.routines.size)
        } catch (e: Exception) {
            RestoreResult.Error("Gagal me-restore backup: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    /**
     * Hitung perkiraan ukuran total data yang akan di-backup (database + media).
     */
    suspend fun estimateBackupSize(): Long = withContext(Dispatchers.IO) {
        mediaFileManager.calculateMediaStorageUsage()
    }
}
