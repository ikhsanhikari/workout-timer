package com.workout.timer.ui.theme

import androidx.annotation.DrawableRes
import com.workout.timer.R

/**
 * Mendapatkan ID resource banner foto atletik resolusi tinggi berdasarkan kategori latihan.
 * Digunakan secara konsisten di Spotlight Banner (Home), WorkoutCard (List Home), dan RoutineDetailScreen.
 */
@DrawableRes
fun getCategoryBannerRes(category: String, isSpotlightDefault: Boolean = false): Int {
    if (isSpotlightDefault) return R.drawable.banner_spotlight
    return when (category.lowercase().trim()) {
        "cardio", "sprint" -> R.drawable.hero_cardio
        "strength", "calisthenics" -> R.drawable.hero_strength
        "hiit", "tabata" -> R.drawable.hero_hiit
        "mobility", "stretch", "yoga" -> R.drawable.hero_mobility
        "core", "abs" -> R.drawable.hero_core
        else -> R.drawable.hero_fullbody
    }
}
