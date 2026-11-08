package com.workout.timer.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workout.timer.data.catalog.CatalogImporter
import com.workout.timer.data.database.dao.*
import com.workout.timer.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database utama aplikasi.
 *
 * Versi: 3 (Mendukung Master Kamus Gerakan & 40 Rutinitas Preset Ilmiah Dunia).
 * Data diisi secara dinamis dari assets (default_catalog.json & exercise_library.json) dan dapat diperbarui via server.
 */
@Database(
    entities = [
        WorkoutRoutine::class,
        WorkoutBlock::class,
        WorkoutExercise::class,
        ExerciseTemplate::class,
        WorkoutSessionRecord::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(WorkoutTypeConverters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutRoutineDao(): WorkoutRoutineDao
    abstract fun workoutBlockDao(): WorkoutBlockDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun exerciseTemplateDao(): ExerciseTemplateDao
    abstract fun workoutSessionRecordDao(): WorkoutSessionRecordDao

    companion object {
        const val DATABASE_NAME = "workout_timer.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN targetMuscle TEXT")
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN primaryMuscle TEXT")
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN equipment TEXT")
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN difficulty TEXT")
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN coachTip TEXT")
                db.execSQL("ALTER TABLE exercise_templates ADD COLUMN benefits TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `workout_session_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routineId` INTEGER NOT NULL,
                        `routineName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `totalExercisesCompleted` INTEGER NOT NULL,
                        `totalExercisesPlanned` INTEGER NOT NULL,
                        `isCompletedFully` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_session_records_completedAt` ON `workout_session_records` (`completedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_session_records_routineId` ON `workout_session_records` (`routineId`)")

                // Backfill dari rutinitas yang pernah dijalankan sebelumnya
                try {
                    db.execSQL("""
                        INSERT INTO workout_session_records (routineId, routineName, category, completedAt, durationSeconds, totalExercisesCompleted, totalExercisesPlanned, isCompletedFully)
                        SELECT id, name, category, lastPerformedAt, totalDurationSec, totalExercises, totalExercises, 1
                        FROM workout_routines
                        WHERE lastPerformedAt IS NOT NULL AND lastPerformedAt > 0
                    """.trimIndent())
                } catch (_: Exception) {
                    // Fail-safe jika tabel workout_routines belum memiliki baris
                }
            }
        }

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        @Volatile
        private var appContext: Context? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): WorkoutDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(PrepopulateCallback(context.applicationContext, scope))
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Mengisi database dengan Kamus Template Gerakan dan 16 Preset Rutinitas
         * Berstandar Ilmiah Internasional dari assets lokal.
         */
        suspend fun populateStandardData(
            context: Context? = null,
            database: WorkoutDatabase,
            onlyIfMissing: Boolean = false
        ) {
            val ctx = context ?: appContext
            if (ctx != null) {
                CatalogImporter.importFromAsset(ctx, database, "default_catalog.json", onlyIfMissing)
                CatalogImporter.importExerciseLibraryFromAsset(ctx, database, "exercise_library.json")
            } else {
                Log.w("WorkoutDatabase", "Context tidak tersedia untuk memuat katalog bawaan.")
            }
        }

        suspend fun populateStandardData(
            database: WorkoutDatabase,
            onlyIfMissing: Boolean = false
        ) {
            populateStandardData(context = null, database = database, onlyIfMissing = onlyIfMissing)
        }
    }

    /**
     * Callback untuk pre-populate database dengan 16 rutinitas bawaan
     * saat database pertama kali dibuat.
     */
    private class PrepopulateCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateStandardData(context, database, onlyIfMissing = false)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val templateDao = database.exerciseTemplateDao()
                        // 1. Selalu sinkronkan Kamus Master dari exercise_library.json saat app dibuka
                        CatalogImporter.importExerciseLibraryFromAsset(context, database, "exercise_library.json")

                        // 2. Periksa dan upgrade seluruh template di DB yang masih menunjuk GIF ke Video MP4 AI
                        com.workout.timer.data.catalog.AssetVideoResolver.upgradeAllTemplatesInDb(templateDao)

                        // 3. Pastikan rutinitas standar tersedia
                        populateStandardData(context, database, onlyIfMissing = true)
                    } catch (e: Exception) {
                        Log.e("WorkoutDatabase", "Gagal auto-update media dan katalog di onOpen: ${e.message}", e)
                    }
                }
            }
        }
    }
}
