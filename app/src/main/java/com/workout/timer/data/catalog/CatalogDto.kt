package com.workout.timer.data.catalog

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTO) untuk format data JSON katalog latihan.
 * Cocok untuk data lokal (assets) maupun sinkronisasi remote dari GitHub.
 */
data class CatalogDto(
    @SerializedName("catalogVersion")
    val catalogVersion: Int = 1,

    @SerializedName("lastUpdated")
    val lastUpdated: String? = null,

    @SerializedName("templates")
    val templates: List<TemplateDto> = emptyList(),

    @SerializedName("routines")
    val routines: List<RoutineDto> = emptyList()
)

data class TemplateDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("defaultDuration")
    val defaultDuration: Int = 30
)

data class RoutineDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("accentColor")
    val accentColor: String? = "#A4FF00",

    @SerializedName("blocks")
    val blocks: List<BlockDto> = emptyList()
)

data class BlockDto(
    @SerializedName("orderIndex")
    val orderIndex: Int = 0,

    @SerializedName("repeatCount")
    val repeatCount: Int = 1,

    @SerializedName("label")
    val label: String? = null,

    @SerializedName("exercises")
    val exercises: List<ExerciseDto> = emptyList()
)

data class ExerciseDto(
    @SerializedName("title")
    val title: String,

    @SerializedName("durationSeconds")
    val durationSeconds: Int = 30,

    @SerializedName("type")
    val type: String = "WORK"
)
