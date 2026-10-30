package com.workout.timer.ui.screen.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.data.model.BlockWithExercises
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.model.WorkoutExercise
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.ui.theme.*

/**
 * Layar Detail Rutinitas (Milestone 2) — Preview sebelum mulai sesi.
 *
 * Menampilkan:
 * - Hero header dengan warna aksen & metadata rutinitas
 * - Struktur blok & gerakan lengkap (collapsible)
 * - Tombol Mulai Latihan di bottom bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val routine = uiState.routine

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.8f))
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    // Favorite toggle
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.8f))
                            .clickable { routine?.let { viewModel.toggleFavorite() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (routine?.isFavorite == true) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorit",
                            tint = if (routine?.isFavorite == true) AccentHighIntensity else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Edit button
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.8f))
                            .clickable(onClick = onNavigateToEdit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            BottomStartBar(
                isReady = !uiState.isLoading && uiState.totalExercises > 0,
                accentColor = routine?.let { r ->
                    runCatching { Color(android.graphics.Color.parseColor(r.accentColor)) }
                        .getOrDefault(AccentWork)
                } ?: AccentWork,
                onStart = onNavigateToPlayer
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentWork)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 0.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                // ── Hero Header ──────────────────────────────────────────────
                item {
                    routine?.let {
                        DetailHeroHeader(
                            routine = it,
                            totalDurationSec = uiState.totalDurationSec,
                            totalExercises = uiState.totalExercises
                        )
                    }
                }

                // ── Blocks Section Label ─────────────────────────────────────
                item {
                    Text(
                        text = "Struktur Latihan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                // ── Blocks List ──────────────────────────────────────────────
                itemsIndexed(uiState.blocks) { blockIndex, blockWithEx ->
                    DetailBlockCard(
                        blockWithExercises = blockWithEx,
                        blockIndex = blockIndex
                    )
                }
            }
        }
    }
}

// ── Hero Header ──────────────────────────────────────────────────────────────

@Composable
private fun DetailHeroHeader(
    routine: WorkoutRoutine,
    totalDurationSec: Int,
    totalExercises: Int
) {
    val accentColor = runCatching {
        Color(android.graphics.Color.parseColor(routine.accentColor))
    }.getOrDefault(AccentWork)

    val bannerRes = getCategoryBannerRes(routine.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        // Hero athletic background image
        Image(
            painter = painterResource(id = bannerRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top subtle gradient scrim for TopAppBar & status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundPrimary.copy(alpha = 0.85f),
                            BackgroundPrimary.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Subtle accent brand tint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom gradient scrim seamlessly fading into BackgroundPrimary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to BackgroundPrimary.copy(alpha = 0.3f),
                        0.7f to BackgroundPrimary.copy(alpha = 0.85f),
                        1.0f to BackgroundPrimary
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 96.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Category badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceCard.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = routine.category.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = routine.name,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailStatItem(
                    icon = Icons.Outlined.Timer,
                    value = formatDuration(totalDurationSec),
                    label = "Durasi",
                    color = accentColor
                )
                DetailStatItem(
                    icon = Icons.Outlined.FitnessCenter,
                    value = "$totalExercises",
                    label = "Gerakan",
                    color = accentColor
                )
                if (routine.lastPerformedAt != null) {
                    DetailStatItem(
                        icon = Icons.Outlined.History,
                        value = "Pernah",
                        label = "Dilakukan",
                        color = AccentRest
                    )
                } else {
                    DetailStatItem(
                        icon = Icons.Outlined.NewReleases,
                        value = "Baru",
                        label = "Belum Pernah",
                        color = AccentWarmup
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

// ── Block Card (Detail View) ─────────────────────────────────────────────────

@Composable
private fun DetailBlockCard(
    blockWithExercises: BlockWithExercises,
    blockIndex: Int
) {
    val block = blockWithExercises.block
    val exercises = blockWithExercises.exercises.sortedBy { it.orderIndex }
    val isLoop = block.repeatCount > 1
    var isExpanded by remember { mutableStateOf(true) }

    val blockDurationSec = exercises.sumOf { it.durationSeconds } * block.repeatCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column {
            // Block header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Block number circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${blockIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = block.label ?: if (isLoop) "Set Loop" else "Blok ${blockIndex + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isLoop) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentWork.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "×${block.repeatCount}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentWork,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    Text(
                        text = "${exercises.size} gerakan · ${formatDuration(blockDurationSec)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Animated exercises list
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)

                    exercises.forEachIndexed { exIdx, exercise ->
                        DetailExerciseRow(exercise = exercise)
                        if (exIdx < exercises.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = SurfaceElevated,
                                thickness = 0.5.dp
                            )
                        }
                    }

                    // Loop repeat footer
                    if (isLoop) {
                        HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = AccentWork,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Diulang ${block.repeatCount}× · Total ${formatDuration(blockDurationSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentWork
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailExerciseRow(exercise: WorkoutExercise) {
    val accentColor = exercise.type.toAccentColor()
    val typeLabel = when (exercise.type) {
        IntervalType.WARMUP -> "Warmup"
        IntervalType.WORK -> "Work"
        IntervalType.REST -> "Rest"
        IntervalType.COOLDOWN -> "Cooldown"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.8f)
            )
        }

        // Duration badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = accentColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = formatSeconds(exercise.durationSeconds),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Bottom Start Bar ─────────────────────────────────────────────────────────

@Composable
private fun BottomStartBar(
    isReady: Boolean,
    accentColor: Color,
    onStart: () -> Unit
) {
    Surface(color = BackgroundPrimary) {
        Column {
            HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStart,
                    enabled = isReady,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, if (isReady) accentColor.copy(alpha = 0.5f) else Color.Transparent),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF24242A),
                        contentColor = TextPrimary,
                        disabledContainerColor = SurfaceElevated,
                        disabledContentColor = TextTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isReady) accentColor else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mulai Latihan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isReady) TextPrimary else TextTertiary
                    )
                }
            }
        }
    }
}

// ── Utility ──────────────────────────────────────────────────────────────────

private fun formatDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return when {
        totalSec == 0 -> "0d"
        minutes == 0 -> "${seconds}d"
        seconds == 0 -> "${minutes} menit"
        else -> "${minutes}m ${seconds}d"
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}:${s.toString().padStart(2, '0')}" else "${s}s"
}
