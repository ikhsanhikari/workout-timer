package com.workout.timer.data.database

import androidx.room.TypeConverter
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.model.MediaType

/**
 * Room TypeConverters untuk menyimpan enum [IntervalType] dan [MediaType]
 * sebagai String di SQLite agar tetap readable dan tidak kehilangan
 * nilai lama saat ada penambahan enum baru.
 */
class WorkoutTypeConverters {

    @TypeConverter
    fun fromIntervalType(type: IntervalType): String = type.name

    @TypeConverter
    fun toIntervalType(value: String): IntervalType =
        IntervalType.entries.firstOrNull { it.name == value } ?: IntervalType.WORK

    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType =
        MediaType.entries.firstOrNull { it.name == value } ?: MediaType.NONE
}
