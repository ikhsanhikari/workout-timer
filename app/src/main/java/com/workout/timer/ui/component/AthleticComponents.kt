package com.workout.timer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workout.timer.R
import com.workout.timer.data.model.IntervalType
import com.workout.timer.ui.theme.AccentCooldown
import com.workout.timer.ui.theme.AccentRest
import com.workout.timer.ui.theme.AccentWarmup
import com.workout.timer.ui.theme.AccentWork

/**
 * Athletic Interval Type Pill untuk menggantikan titik bulat generik (naked dot AI).
 * Menggunakan Custom Vector SVG atletik khusus per tipe:
 * - WORK     : ic_interval_work.xml (Kinetic Bolt)
 * - REST     : ic_interval_rest.xml (Recovery Drop & Pause)
 * - WARMUP   : ic_interval_warmup.xml (Rising Ignition Spark)
 * - COOLDOWN : ic_interval_cooldown.xml (Calming Wind Wave)
 */
@Composable
fun IntervalTypePill(
    type: IntervalType,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val (label, color, iconRes) = when (type) {
        IntervalType.WORK -> Triple("KERJA", AccentWork, R.drawable.ic_interval_work)
        IntervalType.REST -> Triple("REST", AccentRest, R.drawable.ic_interval_rest)
        IntervalType.WARMUP -> Triple("WARMUP", AccentWarmup, R.drawable.ic_interval_warmup)
        IntervalType.COOLDOWN -> Triple("COOLDOWN", AccentCooldown, R.drawable.ic_interval_cooldown)
    }

    val height = if (isCompact) 22.dp else 28.dp
    val hPadding = if (isCompact) 7.dp else 9.dp
    val fontSize = if (isCompact) 9.5.sp else 11.sp
    val iconSize = if (isCompact) 11.dp else 13.dp

    Surface(
        modifier = modifier
            .height(height)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = hPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
                color = color
            )
        }
    }
}

/**
 * Modern Athletic Category Badge tanpa titik dot bulat AI.
 * Menggunakan Custom Vector SVG tematik:
 * - Strength / Calisthenics : ic_cat_strength.xml (Hex Olympic Dumbbell)
 * - Cardio / Sprint          : ic_cat_cardio.xml (Cardiac Speed Pulse)
 * - HIIT / Tabata            : ic_cat_hiit.xml (High-Voltage Double Strike)
 * - Core / Abs               : ic_cat_core.xml (Six-Pack Geometric Shield)
 * - Mobility / Stretch / Yoga: ic_cat_mobility.xml (Dynamic Flow Lotus)
 */
@Composable
fun AthleticCategoryBadge(
    category: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val normCat = category.lowercase().trim()
    val iconRes = when {
        normCat.contains("strength") || normCat.contains("calisthenics") -> R.drawable.ic_cat_strength
        normCat.contains("hiit") || normCat.contains("tabata") -> R.drawable.ic_cat_hiit
        normCat.contains("mobility") || normCat.contains("stretch") || normCat.contains("yoga") -> R.drawable.ic_cat_mobility
        normCat.contains("core") || normCat.contains("abs") -> R.drawable.ic_cat_core
        else -> R.drawable.ic_cat_cardio
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.16f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
        }
    }
}

/**
 * Pemisah teks metadata atletik berupa slanted slash custom vector (ic_divider_slash.xml)
 * untuk menggantikan bullet dot bulat (`·` dan `•`).
 */
@Composable
fun StatSlashDivider(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.25f)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_divider_slash),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(width = 8.dp, height = 12.dp)
    )
}

/**
 * Filter badge modern berupa kapsul angka kompak menggantikan dot hijau melayang polos.
 */
@Composable
fun FilterActiveCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(16.dp),
        shape = RoundedCornerShape(8.dp),
        color = AccentWork,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
    }
}

/**
 * Header badge panduan media/video dengan ikon custom ic_media_guide_badge.xml
 * menggantikan titik bulat 10dp.
 */
@Composable
fun MediaGuideHeaderBadge(
    title: String = "PANDUAN GERAKAN LATIHAN",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = AccentWork.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, AccentWork.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_media_guide_badge),
                contentDescription = null,
                tint = AccentWork,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = AccentWork,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * Mendapatkan resource ID ikon atletik profesional untuk gerakan latihan (Master Data).
 * Menggantikan ikon dumbbell Material standar dengan custom vector SVG:
 * - Strength / General : ic_dumbbell_pro.xml (Olympic Hex Dumbbell)
 * - Cardio / Sprint    : ic_cat_cardio.xml (Cardiac Speed Pulse)
 * - HIIT / Tabata      : ic_cat_hiit.xml (High-Voltage Double Strike)
 * - Core / Abs         : ic_cat_core.xml (Six-Pack Geometric Shield)
 * - Mobility           : ic_cat_mobility.xml (Dynamic Flow Lotus)
 * - Media Video/GIF    : ic_media_guide_badge.xml (Video Slate)
 */
@androidx.annotation.DrawableRes
fun getExerciseMovementIconRes(category: String?, hasMedia: Boolean = false): Int {
    if (hasMedia) return R.drawable.ic_media_guide_badge
    val norm = category?.lowercase()?.trim() ?: ""
    return when {
        norm.contains("cardio") || norm.contains("sprint") -> R.drawable.ic_cat_cardio
        norm.contains("hiit") || norm.contains("tabata") -> R.drawable.ic_cat_hiit
        norm.contains("core") || norm.contains("abs") -> R.drawable.ic_cat_core
        norm.contains("mobility") || norm.contains("stretch") || norm.contains("yoga") -> R.drawable.ic_cat_mobility
        else -> R.drawable.ic_dumbbell_pro
    }
}
