package com.workout.timer.di

import android.content.Context
import android.util.Log
import com.workout.timer.data.database.WorkoutDatabase
import com.workout.timer.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt Module untuk menyediakan semua dependency layer data:
 * - [WorkoutDatabase] (singleton)
 * - Semua DAO (diambil dari database)
 * - [CoroutineScope] aplikasi dengan CoroutineExceptionHandler agar aman dari crash
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideApplicationScope(): CoroutineScope {
        val handler = CoroutineExceptionHandler { _, exception ->
            Log.e("DatabaseModule", "Unhandled exception in applicationScope: ${exception.message}", exception)
        }
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
    }

    @Singleton
    @Provides
    fun provideWorkoutDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): WorkoutDatabase = WorkoutDatabase.getInstance(context, scope)

    @Provides
    fun provideWorkoutRoutineDao(db: WorkoutDatabase): WorkoutRoutineDao =
        db.workoutRoutineDao()

    @Provides
    fun provideWorkoutBlockDao(db: WorkoutDatabase): WorkoutBlockDao =
        db.workoutBlockDao()

    @Provides
    fun provideWorkoutExerciseDao(db: WorkoutDatabase): WorkoutExerciseDao =
        db.workoutExerciseDao()

    @Provides
    fun provideExerciseTemplateDao(db: WorkoutDatabase): ExerciseTemplateDao =
        db.exerciseTemplateDao()
}
