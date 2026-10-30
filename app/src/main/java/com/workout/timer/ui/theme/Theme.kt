package com.workout.timer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme kustom berbasis desain Apple Fitness+ (PRD Section 6).
 *
 * Semua warna disesuaikan ke skema OLED Black agar:
 * 1. Konsumsi daya minimal di layar AMOLED.
 * 2. Kontras warna neon maksimal.
 * 3. Tidak ada elemen UI berwarna putih solid yang menyilaukan.
 */
private val WorkoutDarkColorScheme = darkColorScheme(
    primary = AccentWork,             // Volt Neon Green
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3300),
    onPrimaryContainer = AccentWork,

    secondary = AccentRest,           // Electric Cyan
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003331),
    onSecondaryContainer = AccentRest,

    tertiary = AccentWarmup,          // Amber Yellow
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF332900),
    onTertiaryContainer = AccentWarmup,

    error = AccentHighIntensity,      // Neon Crimson
    onError = Color.Black,
    errorContainer = Color(0xFF330008),
    onErrorContainer = AccentHighIntensity,

    background = BackgroundPrimary,   // #000000 Pure Black
    onBackground = TextPrimary,

    surface = SurfaceCard,            // #1C1C1E Dark Charcoal
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated, // #2C2C2E
    onSurfaceVariant = TextSecondary,
    surfaceTint = AccentWork,

    outline = TextTertiary,
    outlineVariant = SurfaceElevated,

    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundPrimary,
    inversePrimary = Color(0xFF3A5C00),

    scrim = Color(0xCC000000)
)

@Composable
fun WorkoutTimerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WorkoutDarkColorScheme,
        typography = WorkoutTypography,
        content = content
    )
}

/** Extension: Dapatkan warna aksen berdasarkan IntervalType */
fun com.workout.timer.data.model.IntervalType.toAccentColor(): androidx.compose.ui.graphics.Color =
    when (this) {
        com.workout.timer.data.model.IntervalType.WORK -> AccentWork
        com.workout.timer.data.model.IntervalType.REST -> AccentRest
        com.workout.timer.data.model.IntervalType.WARMUP -> AccentWarmup
        com.workout.timer.data.model.IntervalType.COOLDOWN -> AccentCooldown
    }
