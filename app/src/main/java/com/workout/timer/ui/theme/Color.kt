package com.workout.timer.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Design Tokens (PRD Section 6 & Pure White Glass Architecture) ───────────

/** Background utama: Pure OLED Black untuk kontras maksimal & hemat daya AMOLED. */
val BackgroundPrimary = Color(0xFF000000)

/** Surface / Card: Frosted Dark Charcoal untuk kartu dan bottom sheet. */
val SurfaceCard = Color(0xFF141416)

/** Surface Elevated: Untuk filter chips dan tombol sekunder. */
val SurfaceElevated = Color(0xFF1E1E22)

/** Surface level 3 */
val SurfaceLevel3 = Color(0xFF2A2A2E)

/** Surface Border: Garis halus untuk pembatas kartu glassmorphism */
val SurfaceBorder = Color.White.copy(alpha = 0.14f)

// ─── Pure White Glass Design Tokens ──────────────────────────────────────────

/** Surface Glass Sangat Halus (misal inputan idle, background chip sekunder) */
val GlassSurfaceSubtle = Color.White.copy(alpha = 0.06f)

/** Surface Glass Sedang (kartu utama, bottom sheet, floating dialog) */
val GlassSurfaceMedium = Color.White.copy(alpha = 0.10f)

/** Surface Glass Menonjol (chip aktif, item terpilih, hover/pressed) */
val GlassSurfaceElevated = Color.White.copy(alpha = 0.18f)

/** Surface Glass Terang (tombol aksi putih semi-solid) */
val GlassSurfaceBright = Color.White.copy(alpha = 0.28f)

/** Border Kaca Halus (pembatas kartu standar) */
val GlassBorderSubtle = Color.White.copy(alpha = 0.14f)

/** Border Kaca Reflektif (garis tepi kartu berkilau) */
val GlassBorderHighlight = Color.White.copy(alpha = 0.28f)

/** Border Kaca Input Fokus */
val GlassBorderFocused = Color.White.copy(alpha = 0.50f)

/** Input Container Idle & Focused */
val GlassInputContainerIdle = Color.White.copy(alpha = 0.07f)
val GlassInputContainerFocused = Color.White.copy(alpha = 0.12f)
val GlassInputBorderIdle = Color.White.copy(alpha = 0.18f)

// ─── Accent Colors (Ditipiskan untuk Estetika Clean & Non-Mencolok) ───────────

/** Warna aksen WORK/Move: Volt Neon Green (dipertahankan namun penggunaannya di-blend tipis). */
val AccentWork = Color(0xFFA4FF00)

/** Warna aksen High Intensity / HIIT: Neon Crimson. */
val AccentHighIntensity = Color(0xFFFA114F)

/** Warna aksen REST: Electric Cyan. */
val AccentRest = Color(0xFF00F5EA)

/** Warna aksen WARMUP & COOLDOWN: Amber Yellow. */
val AccentWarmup = Color(0xFFFFD60A)

/** Warna aksen ungu untuk phase Cooldown (alternatif). */
val AccentCooldown = Color(0xFFBF5AF2)

/** Subdued Accent Tints (opacity tipis lembut untuk indikator semantik tanpa mencolok) */
val AccentWorkSubtle = Color(0xFFA4FF00).copy(alpha = 0.10f)
val AccentRestSubtle = Color(0xFF00F5EA).copy(alpha = 0.08f)
val AccentWarmupSubtle = Color(0xFFFFD60A).copy(alpha = 0.08f)
val AccentCooldownSubtle = Color(0xFFBF5AF2).copy(alpha = 0.08f)
val AccentHighIntensitySubtle = Color(0xFFFA114F).copy(alpha = 0.12f)

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
