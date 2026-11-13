package com.workout.timer.data.catalog

import com.workout.timer.data.model.IntervalType
import kotlin.math.abs

/**
 * Resolver untuk foto latar belakang sinematik AI di assets/photo_ai/.
 * Berfungsi sebagai background visual dinamis saat gerakan tidak memiliki video panduan MP4/GIF.
 */
object AssetPhotoResolver {

    const val ASSET_PHOTO_DIR = "file:///android_asset/photo_ai/"

    // 15 Background Photos yang telah dikompresi (768x1376, Q85) & distandarisasi
    const val BG_CARDIO_JOGGING = "${ASSET_PHOTO_DIR}bg_cardio_jogging.jpg"
    const val BG_COOLDOWN_BREATHING = "${ASSET_PHOTO_DIR}bg_cooldown_breathing.jpg"
    const val BG_EQUIPMENT_MAT = "${ASSET_PHOTO_DIR}bg_equipment_mat.jpg"
    const val BG_GYM_WORKOUT = "${ASSET_PHOTO_DIR}bg_gym_workout.jpg"
    const val BG_HIIT_ATHLETIC = "${ASSET_PHOTO_DIR}bg_hiit_athletic.jpg"
    const val BG_MOBILITY_STRETCH = "${ASSET_PHOTO_DIR}bg_mobility_stretch.jpg"
    const val BG_RECOVERY_FOREST = "${ASSET_PHOTO_DIR}bg_recovery_forest.jpg"
    const val BG_RECOVERY_LAKE = "${ASSET_PHOTO_DIR}bg_recovery_lake.jpg"
    const val BG_RECOVERY_MISTY_LAKE = "${ASSET_PHOTO_DIR}bg_recovery_misty_lake.jpg"
    const val BG_REST_REFLECTION = "${ASSET_PHOTO_DIR}bg_rest_reflection.jpg"
    const val BG_STRENGTH_DUMBBELL = "${ASSET_PHOTO_DIR}bg_strength_dumbbell.jpg"
    const val BG_STRENGTH_GYM = "${ASSET_PHOTO_DIR}bg_strength_gym.jpg"
    const val BG_STRENGTH_PULLUP = "${ASSET_PHOTO_DIR}bg_strength_pullup.jpg"
    const val BG_WARMUP_ROOFTOP = "${ASSET_PHOTO_DIR}bg_warmup_rooftop.jpg"
    const val BG_YOGA_CORE = "${ASSET_PHOTO_DIR}bg_yoga_core.jpg"

    private val RECOVERY_POOL = listOf(
        BG_RECOVERY_LAKE,
        BG_RECOVERY_MISTY_LAKE,
        BG_RECOVERY_FOREST,
        BG_REST_REFLECTION,
        BG_COOLDOWN_BREATHING
    )

    private val STRENGTH_DUMBBELL_POOL = listOf(
        BG_STRENGTH_DUMBBELL,
        BG_EQUIPMENT_MAT,
        BG_STRENGTH_GYM
    )

    private val STRENGTH_BARBELL_GYM_POOL = listOf(
        BG_STRENGTH_GYM,
        BG_GYM_WORKOUT,
        BG_STRENGTH_DUMBBELL
    )

    private val CALISTHENICS_POOL = listOf(
        BG_STRENGTH_PULLUP,
        BG_GYM_WORKOUT,
        BG_EQUIPMENT_MAT
    )

    private val CARDIO_HIIT_POOL = listOf(
        BG_CARDIO_JOGGING,
        BG_HIIT_ATHLETIC,
        BG_WARMUP_ROOFTOP
    )

    private val MOBILITY_STRETCH_POOL = listOf(
        BG_MOBILITY_STRETCH,
        BG_WARMUP_ROOFTOP,
        BG_YOGA_CORE
    )

    private val YOGA_CORE_POOL = listOf(
        BG_YOGA_CORE,
        BG_EQUIPMENT_MAT,
        BG_MOBILITY_STRETCH
    )

    /**
     * Memilih foto latar belakang yang paling sesuai berdasarkan jenis interval, kategori,
     * peralatan (equipment), serta nama gerakan.
     */
    fun resolveBackgroundPhoto(
        intervalType: IntervalType? = null,
        category: String? = null,
        equipment: String? = null,
        title: String? = null,
        targetMuscle: String? = null
    ): String {
        val cleanTitle = (title ?: "").lowercase().trim()
        val cleanCat = (category ?: "").lowercase().trim()
        val cleanEq = (equipment ?: "").lowercase().trim()
        val cleanMuscle = (targetMuscle ?: "").lowercase().trim()

        val seed = abs((cleanTitle + cleanCat + cleanEq).hashCode())

        // 1. Fase REST / Pemulihan
        if (intervalType == IntervalType.REST || cleanTitle.contains("rest") || cleanTitle.contains("istirahat")) {
            return RECOVERY_POOL[seed % RECOVERY_POOL.size]
        }

        // 2. Fase COOLDOWN / Pendinginan
        if (intervalType == IntervalType.COOLDOWN || cleanTitle.contains("cooldown") || cleanTitle.contains("pendinginan")) {
            return if (seed % 2 == 0) BG_COOLDOWN_BREATHING else BG_MOBILITY_STRETCH
        }

        // 3. Fase WARMUP / Pemanasan
        if (intervalType == IntervalType.WARMUP || cleanTitle.contains("warmup") || cleanTitle.contains("pemanasan")) {
            return if (seed % 2 == 0) BG_WARMUP_ROOFTOP else BG_HIIT_ATHLETIC
        }

        // 4. Deteksi spesifik berdasarkan Alat (Equipment) & Judul
        if (cleanEq.contains("dumbbell") || cleanTitle.contains("dumbbell") || cleanTitle.contains("kettlebell")) {
            return STRENGTH_DUMBBELL_POOL[seed % STRENGTH_DUMBBELL_POOL.size]
        }
        if (cleanEq.contains("barbell") || cleanEq.contains("smith") || cleanEq.contains("mesin") || cleanEq.contains("cable") || cleanTitle.contains("barbell")) {
            return STRENGTH_BARBELL_GYM_POOL[seed % STRENGTH_BARBELL_GYM_POOL.size]
        }
        if (cleanEq.contains("pull-up") || cleanEq.contains("chin-up") || cleanEq.contains("dip") || cleanTitle.contains("pull-up") || cleanTitle.contains("chin-up") || cleanTitle.contains("dip")) {
            return CALISTHENICS_POOL[seed % CALISTHENICS_POOL.size]
        }
        if (cleanEq.contains("matras") || cleanTitle.contains("yoga") || cleanTitle.contains("mat") || cleanMuscle.contains("abs") || cleanMuscle.contains("core")) {
            return YOGA_CORE_POOL[seed % YOGA_CORE_POOL.size]
        }

        // 5. Deteksi berdasarkan Kategori
        when {
            cleanCat.contains("cardio") || cleanTitle.contains("jog") || cleanTitle.contains("run") || cleanTitle.contains("sprint") -> {
                return CARDIO_HIIT_POOL[seed % CARDIO_HIIT_POOL.size]
            }
            cleanCat.contains("hiit") || cleanTitle.contains("jump") || cleanTitle.contains("burpee") || cleanTitle.contains("sprawl") -> {
                return BG_HIIT_ATHLETIC
            }
            cleanCat.contains("mobility") || cleanCat.contains("flexibility") || cleanTitle.contains("stretch") -> {
                return MOBILITY_STRETCH_POOL[seed % MOBILITY_STRETCH_POOL.size]
            }
            cleanCat.contains("recovery") || cleanCat.contains("pemulihan") -> {
                return RECOVERY_POOL[seed % RECOVERY_POOL.size]
            }
            cleanCat.contains("core") || cleanMuscle.contains("perut") -> {
                return YOGA_CORE_POOL[seed % YOGA_CORE_POOL.size]
            }
            cleanCat.contains("strength") || cleanCat.contains("kekuatan") -> {
                return STRENGTH_BARBELL_GYM_POOL[seed % STRENGTH_BARBELL_GYM_POOL.size]
            }
        }

        // 6. Default Fallback
        val defaultPool = listOf(BG_GYM_WORKOUT, BG_EQUIPMENT_MAT, BG_WARMUP_ROOFTOP)
        return defaultPool[seed % defaultPool.size]
    }
}

