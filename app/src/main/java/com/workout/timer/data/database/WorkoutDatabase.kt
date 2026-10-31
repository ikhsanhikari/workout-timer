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
 * Versi: 3 (Mendukung Master Kamus Gerakan & 16 Rutinitas Preset Ilmiah Dunia).
 * Data diisi secara dinamis dari assets (default_catalog.json & exercise_library.json) dan dapat diperbarui via server.
 */
@Database(
    entities = [
        WorkoutRoutine::class,
        WorkoutBlock::class,
        WorkoutExercise::class,
        ExerciseTemplate::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(WorkoutTypeConverters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutRoutineDao(): WorkoutRoutineDao
    abstract fun workoutBlockDao(): WorkoutBlockDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun exerciseTemplateDao(): ExerciseTemplateDao

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

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        @Volatile
        private var appContext: Context? = null

        fun getInstance(context: Context, scope: CoroutineScope): WorkoutDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(PrepopulateCallback(context.applicationContext, scope))
                    .addMigrations(MIGRATION_2_3)
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
    }
}
