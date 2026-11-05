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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.data.model.BlockWithExercises
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.model.WorkoutExercise
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.ui.component.*
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
    onNavigateToNewRoutine: ((Long) -> Unit)? = null,
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val routine = uiState.routine

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = BackgroundPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateBack()
                            },
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
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                routine?.let { viewModel.toggleFavorite() }
                            },
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
                    // Duplicate button
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.8f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.duplicateRoutine { newId ->
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Rutinitas berhasil diduplikasi!",
                                            actionLabel = "Buka"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onNavigateToNewRoutine?.invoke(newId)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Duplikat",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    // Edit button
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.8f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToEdit()
                            },
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
                onStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToPlayer()
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
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
                item(key = "detail_hero_header") {
                    routine?.let {
                        DetailHeroHeader(
                            routine = it,
                            totalDurationSec = uiState.totalDurationSec,
                            totalExercises = uiState.totalExercises
                        )
                    }
                }

                // ── Blocks Section Label ─────────────────────────────────────
                item(key = "detail_blocks_label") {
                    Text(
                        text = "Struktur Latihan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                // ── Blocks List ──────────────────────────────────────────────
                itemsIndexed(
                    items = uiState.blocks,
                    key = { index, b -> if (b.block.id > 0) "block_${b.block.id}" else "block_$index" }
                ) { blockIndex, blockWithEx ->
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

        // Subtle frosted brand tint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
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
            // Category badge (Frosted White Glass)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Text(
                    text = routine.category.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
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

            // Stats row (Frosted White Glass)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailStatItem(
                    icon = Icons.Outlined.Timer,
                    value = formatDuration(totalDurationSec),
                    label = "Durasi",
                    color = Color.White
                )
                DetailStatItem(
                    iconRes = R.drawable.ic_dumbbell_pro,
                    value = "$totalExercises",
                    label = "Gerakan",
                    color = Color.White
                )
                if (routine.lastPerformedAt != null) {
                    DetailStatItem(
                        icon = Icons.Outlined.History,
                        value = "Pernah",
                        label = "Dilakukan",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                } else {
                    DetailStatItem(
                        icon = Icons.Outlined.NewReleases,
                        value = "Baru",
                        label = "Belum Pernah",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
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
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
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
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${blockIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
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
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "×${block.repeatCount}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${exercises.size} gerakan",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        StatSlashDivider()
                        Text(
                            text = formatDuration(blockDurationSec),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                    exercises.forEachIndexed { exIdx, exercise ->
                        DetailExerciseRow(exercise = exercise)
                        if (exIdx < exercises.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                        }
                    }

                    // Loop repeat footer
                    if (isLoop) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
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
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Diulang ${block.repeatCount}×",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatSlashDivider(tint = Color.White.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total ${formatDuration(blockDurationSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
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
        // Athletic micro-pill with custom SVG and uniform fixed width
        IntervalTypePill(
            type = exercise.type,
            isCompact = true,
            fixedWidth = true
        )
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = exercise.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        // Duration badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Text(
                text = formatSeconds(exercise.durationSeconds),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
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
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.2.dp, if (isReady) Color.White.copy(alpha = 0.7f) else Color.Transparent),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReady) Color.White else SurfaceElevated,
                        contentColor = if (isReady) Color.Black else TextTertiary,
                        disabledContainerColor = SurfaceElevated,
                        disabledContentColor = TextTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isReady) Color.Black else TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mulai Latihan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (isReady) Color.Black else TextTertiary
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
