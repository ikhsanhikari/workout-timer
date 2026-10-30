package com.workout.timer.data.catalog

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.workout.timer.data.database.WorkoutDatabase
import com.workout.timer.data.model.ExerciseTemplate
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.model.WorkoutBlock
import com.workout.timer.data.model.WorkoutExercise
import com.workout.timer.data.model.WorkoutRoutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pengelola impor dan upsert katalog data ke Room Database secara atomic.
 * Digunakan untuk:
 * 1. Prepopulation awal dari file asset lokal (default_catalog.json) saat fresh install / offline.
 * 2. Dynamic sync dari remote server / GitHub Raw JSON tanpa merusak data kustom atau favorit pengguna.
 */
object CatalogImporter {

    private const val TAG = "CatalogImporter"
    private val gson = Gson()

    /**
     * Mem-parse string JSON ke [CatalogDto].
     */
    fun parseCatalogJson(jsonString: String): CatalogDto? {
        return try {
            gson.fromJson(jsonString, CatalogDto::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mem-parse JSON katalog: ${e.message}", e)
            null
        }
    }

    /**
     * Membaca file JSON dari assets dan mengimpornya ke database.
     */
    suspend fun importFromAsset(
        context: Context,
        database: WorkoutDatabase,
        assetName: String = "default_catalog.json",
        onlyIfMissing: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val catalog = parseCatalogJson(jsonString)
            if (catalog != null) {
                importCatalog(database, catalog, onlyIfMissing)
            } else {
                Log.w(TAG, "Katalog aset '$assetName' kosong atau gagal diparse.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca katalog dari aset '$assetName': ${e.message}", e)
            0
        }
    }

    /**
     * Mengimpor atau memperbarui [CatalogDto] ke [WorkoutDatabase] dalam 1 transaksi atomic.
     *
     * @param onlyIfMissing Jika true, lewati rutinitas yang namanya sudah ada di database.
     *                      Jika false, perbarui rutinitas yang ada tanpa menghapus data favorit/riwayat pengguna.
     * @return Jumlah rutinitas yang berhasil diimpor / diperbarui.
     */
    suspend fun importCatalog(
        database: WorkoutDatabase,
        catalog: CatalogDto,
        onlyIfMissing: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        var processedCount = 0

        database.withTransaction {
            val routineDao = database.workoutRoutineDao()
            val blockDao = database.workoutBlockDao()
            val exerciseDao = database.workoutExerciseDao()
            val templateDao = database.exerciseTemplateDao()

            // ── 1. Update / Insert Kamus Template Gerakan ─────────────────────
            for (tpl in catalog.templates) {
                if (templateDao.getByName(tpl.name) == null) {
                    templateDao.insert(
                        ExerciseTemplate(
                            name = tpl.name,
                            defaultDuration = tpl.defaultDuration
                        )
                    )
                }
            }

            // Buat lookup cache ID template dalam memori
            val allTemplates = templateDao.getAll()
            val templateMap = allTemplates.associate { it.name.lowercase().trim() to it.id }

            fun resolveTemplateId(title: String): Long? {
                val clean = title.lowercase().trim()
                return templateMap[clean]
                    ?: templateMap.entries.firstOrNull { it.key.contains(clean) || clean.contains(it.key) }?.value
            }

            // ── 2. Import / Upsert Rutinitas ─────────────────────────────────
            for (routineDto in catalog.routines) {
                val existingRoutine = routineDao.getByName(routineDto.name)

                if (onlyIfMissing && existingRoutine != null) {
                    continue
                }

                // Hitung total durasi dan jumlah gerakan
                var totalDurationSec = 0
                var totalExercises = 0

                for (block in routineDto.blocks) {
                    var blockDuration = 0
                    for (ex in block.exercises) {
                        blockDuration += ex.durationSeconds
                        totalExercises += 1
                    }
                    totalDurationSec += (blockDuration * block.repeatCount.coerceAtLeast(1))
                }

                val routineId: Long
                if (existingRoutine != null) {
                    // Perbarui metadata, TETAPI pertahankan data pengguna:
                    // isFavorite, lastPerformedAt, thumbnailPath kustom, dan createdAt
                    routineId = existingRoutine.id
                    routineDao.update(
                        existingRoutine.copy(
                            category = routineDto.category,
                            accentColor = routineDto.accentColor ?: existingRoutine.accentColor,
                            totalDurationSec = totalDurationSec,
                            totalExercises = totalExercises
                        )
                    )
                    // Hapus blok lama untuk rutinitas ini (Cascade delete akan menghapus latihan lama)
                    blockDao.deleteAllByRoutineId(routineId)
                } else {
                    // Tambahkan sebagai rutinitas baru
                    routineId = routineDao.insert(
                        WorkoutRoutine(
                            name = routineDto.name,
                            category = routineDto.category,
                            accentColor = routineDto.accentColor ?: "#A4FF00",
                            totalDurationSec = totalDurationSec,
                            totalExercises = totalExercises
                        )
                    )
                }

                // ── 3. Insert Blok & Gerakan ─────────────────────────────────
                routineDto.blocks.forEachIndexed { blockIdx, blockDto ->
                    val blockId = blockDao.insert(
                        WorkoutBlock(
                            routineId = routineId,
                            orderIndex = if (blockDto.orderIndex != 0) blockDto.orderIndex else blockIdx,
                            repeatCount = blockDto.repeatCount.coerceAtLeast(1),
                            label = blockDto.label
                        )
                    )

                    blockDto.exercises.forEachIndexed { exIdx, exDto ->
                        val intervalType = try {
                            IntervalType.valueOf(exDto.type.uppercase())
                        } catch (e: Exception) {
                            IntervalType.WORK
                        }

                        exerciseDao.insert(
                            WorkoutExercise(
                                blockId = blockId,
                                orderIndex = exIdx,
                                templateId = resolveTemplateId(exDto.title),
                                title = exDto.title,
                                durationSeconds = exDto.durationSeconds,
                                type = intervalType
                            )
                        )
                    }
                }

                processedCount++
            }
        }

        Log.d(TAG, "Berhasil memproses $processedCount rutinitas dari katalog (Versi ${catalog.catalogVersion}).")
        processedCount
    }
}
