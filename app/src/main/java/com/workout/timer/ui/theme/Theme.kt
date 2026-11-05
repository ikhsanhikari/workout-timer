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
    primary = Color.White,             // Pure White untuk estetika minimalis elegan
    onPrimary = Color.Black,
    primaryContainer = GlassSurfaceElevated,
    onPrimaryContainer = Color.White,

    secondary = Color.White.copy(alpha = 0.85f),
    onSecondary = Color.Black,
    secondaryContainer = GlassSurfaceMedium,
    onSecondaryContainer = Color.White,

    tertiary = AccentWarmupSubtle,
    onTertiary = Color.White,
    tertiaryContainer = GlassSurfaceSubtle,
    onTertiaryContainer = Color.White,

    error = ColorDanger,
    onError = Color.White,
    errorContainer = ColorDanger.copy(alpha = 0.15f),
    onErrorContainer = ColorDanger,

    background = BackgroundPrimary,   // #000000 Pure Black
    onBackground = TextPrimary,

    surface = SurfaceCard,            // Frosted Dark Charcoal
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Color.White,

    outline = GlassBorderSubtle,
    outlineVariant = GlassSurfaceSubtle,

    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundPrimary,
    inversePrimary = Color.White.copy(alpha = 0.75f),

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
