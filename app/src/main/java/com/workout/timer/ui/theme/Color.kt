package com.workout.timer.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Design Tokens (PRD Section 6) ──────────────────────────────────────────

/** Background utama: Pure OLED Black untuk kontras maksimal & hemat daya AMOLED. */
val BackgroundPrimary = Color(0xFF000000)

/** Surface / Card: Dark Charcoal untuk kartu dan bottom sheet. */
val SurfaceCard = Color(0xFF1C1C1E)

/** Surface Elevated: Untuk filter chips dan tombol sekunder. */
val SurfaceElevated = Color(0xFF2C2C2E)

/** Surface level 3 */
val SurfaceLevel3 = Color(0xFF3A3A3C)

/** Surface Border: Garis halus untuk pembatas kartu glassmorphism */
val SurfaceBorder = Color(0xFF2C2C2E)

// ─── Accent Colors ───────────────────────────────────────────────────────────

/** Warna aksen WORK/Move: Volt Neon Green untuk cincin saat fase latihan. */
val AccentWork = Color(0xFFA4FF00)

/** Warna aksen High Intensity / HIIT: Neon Crimson untuk Hold-to-Stop & kartu HIIT. */
val AccentHighIntensity = Color(0xFFFA114F)

/** Warna aksen REST: Electric Cyan untuk cincin saat fase istirahat. */
val AccentRest = Color(0xFF00F5EA)

/** Warna aksen WARMUP & COOLDOWN: Amber Yellow. */
val AccentWarmup = Color(0xFFFFD60A)

/** Warna aksen ungu untuk phase Cooldown (alternatif). */
val AccentCooldown = Color(0xFFBF5AF2)

/** Warna teks primer: Putih bersih. */
val TextPrimary = Color(0xFFFFFFFF)

/** Warna teks sekunder: Abu-abu terang. */
val TextSecondary = Color(0xFF8E8E93)

/** Warna teks tersier: Abu-abu lebih gelap. */
val TextTertiary = Color(0xFF636366)

/** Warna destructive / danger: Red Apple. */
val ColorDanger = Color(0xFFFF453A)

/** Warna success: Green Apple. */
val ColorSuccess = Color(0xFF30D158)

// ─── Gradients ───────────────────────────────────────────────────────────────

/** Gradient gelap ke-atas untuk overlay video di Player. */
val GradientOverlayColors = listOf(
    Color(0x00000000),
    Color(0xCC000000)
)
