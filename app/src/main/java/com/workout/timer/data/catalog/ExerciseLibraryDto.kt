package com.workout.timer.data.catalog

import com.google.gson.annotations.SerializedName

/**
 * DTO untuk deserialisasi file exercise_library.json (Master Kamus Gerakan).
 * Berisi 68 gerakan lengkap beserta target otot, biomekanika, alat, tingkat kesulitan,
 * dan tips dari instruktur/pelatih profesional.
 */
data class ExerciseLibraryDto(
    @SerializedName("libraryVersion")
    val libraryVersion: Int = 1,

    @SerializedName("lastUpdated")
    val lastUpdated: String? = null,

    @SerializedName("totalExercises")
    val totalExercises: Int = 0,

    @SerializedName("exercises")
    val exercises: List<ExerciseLibraryItemDto> = emptyList()
)

data class ExerciseLibraryItemDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("targetMuscle")
    val targetMuscle: String? = null,

    @SerializedName("primaryMuscle")
    val primaryMuscle: String? = null,

    @SerializedName("equipment")
    val equipment: String? = null,

    @SerializedName("difficulty")
    val difficulty: String? = null,

    @SerializedName("defaultDuration")
    val defaultDuration: Int = 30,

    @SerializedName("coachTip")
    val coachTip: String? = null,

    @SerializedName("benefits")
    val benefits: String? = null
)
