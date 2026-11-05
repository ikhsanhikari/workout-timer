package com.workout.timer.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.workout.timer.R
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.repository.SortOption
import com.workout.timer.ui.theme.*
import com.workout.timer.ui.component.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Layar Home Dashboard (F-02) — Apple Fitness Aesthetic.
 *
 * Layout:
 * - Header: Salam + tombol tambah
 * - Activity Ring Section: Weekly stats
 * - Search Bar (real-time)
 * - Filter Chips (kategori + durasi)
 * - Workout Cards list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToBuilder: () -> Unit,
    onNavigateToRoutineDetail: (Long) -> Unit,
    onNavigateToStartWorkout: (Long) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyMinutes by viewModel.weeklyTotalMinutes.collectAsStateWithLifecycle()
    val weeklySessions by viewModel.weeklySessionCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val hasActiveFilters = uiState.selectedDurationFilter != DurationFilter.ALL ||
        uiState.selectedSort != SortOption.CREATED_AT

    val dailyChallengeItems = remember(
        uiState.favoriteRoutines,
        uiState.latestPerformedRoutine,
        uiState.routines
    ) {
        val favorites = uiState.favoriteRoutines
        val latest = uiState.latestPerformedRoutine
        when {
            // Prioritas 1: HANYA yang di-like (favorit)
            favorites.isNotEmpty() -> favorites to ChallengeSource.FAVORITE
            // Prioritas 2: Terakhir kali dilakukan (jika belum ada yang di-like)
            latest != null -> listOf(latest) to ChallengeSource.LATEST_PERFORMED
            // Prioritas 3: Fallback rekomendasi katalog awal jika belum ada favorit & riwayat
            uiState.routines.isNotEmpty() -> listOf(uiState.routines.first()) to ChallengeSource.RECOMMENDED
            else -> emptyList<WorkoutRoutine>() to ChallengeSource.NONE
        }
    }
    val (challengeRoutines, challengeSource) = dailyChallengeItems

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundPrimary,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToBuilder,
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(50.dp)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Buat Rutinitas Baru",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Generated Hero Athlete Background (Top-Right) ────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_home_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopEnd,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight()
                        .align(Alignment.TopEnd)
                        .alpha(0.70f)
                )

                // Diagonal subtle crystalline glass reflection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                start = Offset(150f, 0f),
                                end = Offset(850f, 650f)
                            )
                        )
                )

                // Left Scrim Gradient: fades dark BackgroundPrimary from the left
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BackgroundPrimary,
                                    BackgroundPrimary,
                                    BackgroundPrimary.copy(alpha = 0.75f),
                                    BackgroundPrimary.copy(alpha = 0.20f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 640f
                            )
                        )
                )

                // Bottom Scrim Gradient: fades smoothly into BackgroundPrimary
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BackgroundPrimary.copy(alpha = 0.55f),
                                    BackgroundPrimary
                                )
                            )
                        )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
            // ── Header ──────────────────────────────────────────────────────
            item(key = "home_header") {
                HomeHeader(
                    nickname = uiState.userNickname,
                    onOpenBackup = { showBackupSheet = true },
                    onSyncCatalog = { viewModel.syncCatalog(isManual = true) },
                    onNavigateToSettings = onNavigateToSettings,
                    isSyncing = uiState.isSyncing
                )
            }

            // ── Weekly Activity Ring ─────────────────────────────────────────
            item(key = "home_weekly_activity") {
                WeeklyActivitySection(
                    totalMinutes = weeklyMinutes,
                    sessionCount = weeklySessions,
                    onClick = onNavigateToHistory
                )
            }

            // ── Spotlight / Daily Challenge Banner ───────────────────────────
            item(key = "home_daily_challenge") {
                AnimatedVisibility(
                    visible = uiState.searchQuery.isBlank() && uiState.selectedCategory == null && !hasActiveFilters && challengeRoutines.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DailyChallengeSection(
                        routines = challengeRoutines,
                        challengeSource = challengeSource,
                        onStartWorkout = onNavigateToStartWorkout,
                        onViewDetail = onNavigateToRoutineDetail,
                        onFavoriteClick = { routineId -> viewModel.toggleFavorite(routineId) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // ── Search Bar ──────────────────────────────────────────────────
            item(key = "home_search_bar") {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    hasActiveFilters = hasActiveFilters,
                    onOpenFilter = { showFilterSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // ── Category Pills (Single Dedicated Row) ────────────────────────
            item(key = "home_category_row") {
                CategoryRow(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClick = viewModel::onCategorySelected,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // ── Active Filter Tags (Dismissible) ────────────────────────────
            if (hasActiveFilters) {
                item(key = "home_active_filters") {
                    ActiveFilterRow(
                        selectedDuration = uiState.selectedDurationFilter,
                        selectedSort = uiState.selectedSort,
                        onResetDuration = { viewModel.onDurationFilterSelected(DurationFilter.ALL) },
                        onResetSort = { viewModel.onSortSelected(SortOption.CREATED_AT) },
                        onResetAll = {
                            viewModel.onDurationFilterSelected(DurationFilter.ALL)
                            viewModel.onSortSelected(SortOption.CREATED_AT)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── Section Label (Riwayat Latihan | Lihat Semua ➔) ─────────────
            item(key = "home_history_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank() && uiState.selectedCategory == null && !hasActiveFilters)
                                "Riwayat Latihan"
                            else
                                "Hasil Pencarian",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                    }

                    if (uiState.searchQuery.isBlank() && uiState.selectedCategory == null && !hasActiveFilters) {
                        Surface(
                            onClick = onNavigateToHistory,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Lihat Semua",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Workout Cards ────────────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else if (uiState.routines.isEmpty()) {
                item {
                    EmptyState(
                        onCreateNew = onNavigateToBuilder,
                        onRestoreStandard = viewModel::restoreStandardRoutines
                    )
                }
            } else {
                items(uiState.routines, key = { it.id }) { routine ->
                    WorkoutCard(
                        routine = routine,
                        onCardClick = { onNavigateToRoutineDetail(routine.id) },
                        onStartClick = { onNavigateToStartWorkout(routine.id) },
                        onFavoriteClick = { viewModel.toggleFavorite(routine.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

        if (showBackupSheet) {
            BackupBottomSheet(
                onDismiss = { showBackupSheet = false },
                onExport = {
                    showBackupSheet = false
                    createDocumentLauncher.launch("workout_backup_${System.currentTimeMillis()}.zip")
                },
                onImport = {
                    showBackupSheet = false
                    openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                },
                onRestoreStandard = {
                    showBackupSheet = false
                    viewModel.restoreStandardRoutines()
                }
            )
        }

        if (showFilterSheet) {
            HomeFilterBottomSheet(
                selectedDuration = uiState.selectedDurationFilter,
                selectedSort = uiState.selectedSort,
                onDurationClick = viewModel::onDurationFilterSelected,
                onSortClick = viewModel::onSortSelected,
                onResetAll = {
                    viewModel.onDurationFilterSelected(DurationFilter.ALL)
                    viewModel.onSortSelected(SortOption.CREATED_AT)
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    nickname: String = "",
    onOpenBackup: () -> Unit,
    onSyncCatalog: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    isSyncing: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nickname.isNotBlank()) {
                    Text(
                        text = "Halo, ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "$nickname 👋",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 28.sp
                        ),
                        color = AccentWork
                    )
                } else {
                    Text(
                        text = "Workout ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Timer",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 30.sp
                        ),
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_flame_streak),
                    contentDescription = "Streak",
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Siap bergerak & bakar kalori hari ini?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(3.5.dp)
                    .background(Color.White.copy(alpha = 0.50f), RoundedCornerShape(2.dp))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Tombol Sync Katalog Server (Actionable - Frosted White Glass)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape)
                    .clickable(enabled = !isSyncing, onClick = onSyncCatalog),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync Katalog Server",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Tombol Backup & Restore (Actionable - Frosted White Glass)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape)
                    .clickable(onClick = onOpenBackup),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cloud_backup_sleek),
                    contentDescription = "Backup & Restore",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tombol Pengaturan (Actionable - Frosted White Glass)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape)
                    .clickable(onClick = onNavigateToSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AthleticActivityRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // 1. Smooth Progress Arc Interpolation
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.08f, 1f),
        animationSpec = tween(
            durationMillis = 1100,
            easing = FastOutSlowInEasing
        ),
        label = "ring_progress"
    )

    // 2. Smooth Floating Breathing Pulse for the Core Lens
    val infiniteTransition = rememberInfiniteTransition(label = "runner_ambience")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "runner_scale"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "runner_glow"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // High-Precision Biometric Dial Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.5.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Dynamic ambient radial aura behind ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = glowPulse * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.45f
                ),
                center = center,
                radius = radius * 1.45f
            )

            // Outer dark frosted glass track
            drawCircle(
                color = Color(0xFF141822).copy(alpha = 0.85f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Subtle inner track guide hairline
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = radius - strokeWidth / 2f,
                center = center,
                style = Stroke(width = 0.8.dp.toPx())
            )

            // Dynamic Gradient Sweep Progress Arc (Pure Crystal White Arc)
            val sweepAngle = 360f * animatedProgress
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.50f),
                        Color.White,
                        Color.White.copy(alpha = 0.85f),
                        Color.White
                    )
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glowing Leading Tip Flare (Pure White Spark)
            val currentAngleDeg = -90f + sweepAngle
            val angleRad = Math.toRadians(currentAngleDeg.toDouble())
            val tipX = center.x + radius * cos(angleRad).toFloat()
            val tipY = center.y + radius * sin(angleRad).toFloat()
            val tipCenter = Offset(tipX, tipY)

            // Radiant bloom around the tip cap
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = tipCenter,
                    radius = strokeWidth * 1.4f
                ),
                center = tipCenter,
                radius = strokeWidth * 1.4f
            )

            // Concentrated bright spark at the center of the tip
            drawCircle(
                color = Color.White,
                center = tipCenter,
                radius = strokeWidth * 0.32f
            )
        }

        // Center Floating Frosted Glass Lens with Smooth Aerodynamic Sprinter
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A2232).copy(alpha = 0.82f),
                            Color(0xFF0D111A).copy(alpha = 0.94f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Internal breathing core glow
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = glowPulse * 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Sleek Aerodynamic Runner Vector with smooth curves & speed trails
            Image(
                painter = painterResource(id = R.drawable.ic_runner_sleek),
                contentDescription = "Aktivitas Lari",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun WeeklyActivitySection(
    totalMinutes: Int,
    sessionCount: Int,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722).copy(alpha = 0.70f)),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                0.0f to Color.White.copy(alpha = 0.35f),    // Top-left crisp specular highlight
                0.55f to Color.White.copy(alpha = 0.12f),   // Translucent body
                1.0f to Color.White.copy(alpha = 0.04f),    // Bottom shadow bevel
                start = Offset(0f, 0f),
                end = Offset(1000f, 600f)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. Subtle Athletic Workout Photography Backdrop (frosted underlay)
            Image(
                painter = painterResource(id = R.drawable.banner_weekly_activity),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.18f)
            )

            // 2. Multi-Directional Dark Frosted Glass Scrim
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F121A).copy(alpha = 0.94f),
                                Color(0xFF0F121A).copy(alpha = 0.82f),
                                Color(0xFF121724).copy(alpha = 0.60f)
                            )
                        )
                    )
            )

            // 3. Ambient Glass Glows (Left Flame Aura & Right Athletic Neon Halo)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF8C42).copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(80f, 50f),
                            radius = 280f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AccentWork.copy(alpha = 0.16f),
                                Color(0xFF00F5EA).copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(720f, 120f),
                            radius = 340f
                        )
                    )
            )

            // 4. Top Glass Specular Shine (Light Glare Reflection)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 5. Foreground Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Frosted Glass Streak Capsule
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF9E44).copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.18f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_flame_streak),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AKTIVITAS MINGGU INI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9E44),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats with subtle frosted divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        StatItem(
                            value = "$totalMinutes",
                            unit = "Menit Latihan",
                            color = Color.White
                        )

                        // Vertical Glass Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        StatItem(
                            value = "$sessionCount",
                            unit = "Sesi Selesai",
                            color = Color.White,
                            icon = Icons.Default.CheckCircle
                        )
                    }
                }

                // Sleek Activity Dial with Smooth Animated Runner Lens
                AthleticActivityRing(
                    progress = (totalMinutes / 120f).coerceIn(0.18f, 1f),
                    modifier = Modifier.size(92.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    unit: String,
    color: Color = Color.White,
    unitColor: Color = Color(0xFFA6ABB8),
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                ),
                color = color
            )
        }
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = unitColor
        )
    }
}

/**
 * Sumber konten untuk Spotlight Tantangan Hari Ini.
 */
enum class ChallengeSource {
    FAVORITE,          // Prioritas 1: HANYA rutinitas yang di-like pengguna
    LATEST_PERFORMED,  // Prioritas 2: Terakhir kali dilakukan (fallback jika belum ada like)
    RECOMMENDED,       // Prioritas 3: Rekomendasi awal katalog (fallback saat app fresh install)
    NONE
}

@Composable
private fun DailyChallengeSection(
    routines: List<WorkoutRoutine>,
    challengeSource: ChallengeSource,
    onStartWorkout: (Long) -> Unit,
    onViewDetail: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (routines.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { routines.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-slide effect when there are multiple items (> 1)
    if (routines.size > 1) {
        LaunchedEffect(pagerState, routines.size) {
            while (true) {
                delay(4500L) // auto-slide every 4.5 seconds
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % routines.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = 12.dp
    ) { page ->
        val routine = routines[page]
        SpotlightRoutineCard(
            routine = routine,
            challengeSource = challengeSource,
            currentPage = pagerState.currentPage,
            pageCount = routines.size,
            onStartWorkout = onStartWorkout,
            onViewDetail = onViewDetail,
            onFavoriteClick = { onFavoriteClick(routine.id) },
            onSelectPage = { targetPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
            }
        )
    }
}

@Composable
private fun SpotlightRoutineCard(
    routine: WorkoutRoutine,
    challengeSource: ChallengeSource,
    currentPage: Int,
    pageCount: Int,
    onStartWorkout: (Long) -> Unit,
    onViewDetail: (Long) -> Unit,
    onFavoriteClick: () -> Unit,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable { onViewDetail(routine.id) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        )
    ) {
        val bannerRes = getCategoryBannerRes(routine.category, isSpotlightDefault = true)
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Athletic Banner Image
            Image(
                painter = painterResource(id = bannerRes),
                contentDescription = "Spotlight Workout",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Horizontal & Vertical Scrim Gradients for text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.92f),
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 820f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tagText = when (challengeSource) {
                        ChallengeSource.FAVORITE -> "TANTANGAN HARI INI"
                        ChallengeSource.LATEST_PERFORMED -> "TANTANGAN HARI INI • TERAKHIR DIGUNAKAN"
                        ChallengeSource.RECOMMENDED -> "TANTANGAN HARI INI • REKOMENDASI"
                        ChallengeSource.NONE -> "TANTANGAN HARI INI"
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_flame_streak),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Like/Favorite Button on Spotlight Card
                    Surface(
                        onClick = onFavoriteClick,
                        shape = CircleShape,
                        color = if (routine.isFavorite) Color(0xFFFF3B30).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (routine.isFavorite) Color(0xFFFF3B30).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (routine.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (routine.isFavorite) "Disukai" else "Sukai",
                                tint = if (routine.isFavorite) Color(0xFFFF453A) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Middle & Bottom Section with Dynamic Pagination Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(
                                iconRes = R.drawable.ic_stat_timer,
                                text = formatDuration(routine.totalDurationSec),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "|",
                                color = Color(0xFF4A4E5C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            StatChip(
                                iconRes = R.drawable.ic_intensity_lightning,
                                text = routine.category.uppercase(),
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Start Quick Button (Frosted White Capsule)
                        Button(
                            onClick = { onStartWorkout(routine.id) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mulai Latihan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }
                    }

                    // Dynamic Pagination indicator dots when pageCount > 1
                    if (pageCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            for (i in 0 until pageCount) {
                                val isSelected = i == currentPage
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 5.dp,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "dotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(dotWidth)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                                        )
                                        .clickable { onSelectPage(i) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hasActiveFilters: Boolean,
    onOpenFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(query) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(query) {
        if (text != query) {
            text = query
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Frosted Glass Search Input Container
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (text.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Cari rutinitas atau gerakan...",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { newText ->
                            text = newText
                            onQueryChange(newText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                }

                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                text = ""
                                onQueryChange("")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Filter / Tune button (Matching 48dp Frosted Glass Orb)
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenFilter()
                },
            shape = RoundedCornerShape(16.dp),
            color = if (hasActiveFilters) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        if (hasActiveFilters) Color.White.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.20f),
                        if (hasActiveFilters) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f)
                    )
                )
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter & Urutkan",
                    tint = if (hasActiveFilters) Color.White else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )

                if (hasActiveFilters) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated single-row category pills containing ONLY workout categories.
 * First item is "Semua", followed by DB categories.
 */
@Composable
private fun CategoryRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Semua" pill
        item(key = "cat_pill_all") {
            CategoryPill(
                label = "Semua",
                isSelected = selectedCategory == null,
                onClick = { onCategoryClick(null) }
            )
        }

        // Specific category pills
        items(categories, key = { "cat_pill_$it" }) { category ->
            CategoryPill(
                label = category,
                isSelected = selectedCategory == category,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

private sealed class CategoryIconSource {
    data class Vector(val imageVector: ImageVector) : CategoryIconSource()
    data class DrawableRes(val resId: Int) : CategoryIconSource()
}

private fun resolveCategoryIcon(categoryName: String): CategoryIconSource {
    val clean = categoryName.trim().lowercase()
    return when {
        clean in listOf("semua", "all") -> CategoryIconSource.Vector(Icons.Outlined.GridView)
        clean.contains("cardio") -> CategoryIconSource.DrawableRes(R.drawable.ic_cat_cardio)
        clean.contains("core") || clean.contains("abs") -> CategoryIconSource.DrawableRes(R.drawable.ic_cat_core)
        clean.contains("hiit") -> CategoryIconSource.DrawableRes(R.drawable.ic_cat_hiit)
        clean.contains("strength") || clean.contains("kekuatan") -> CategoryIconSource.DrawableRes(R.drawable.ic_cat_strength)
        clean.contains("mobility") || clean.contains("recovery") || clean.contains("yoga") -> CategoryIconSource.DrawableRes(R.drawable.ic_cat_mobility)
        clean.contains("full body") || clean.contains("fullbody") -> CategoryIconSource.Vector(Icons.Default.AccessibilityNew)
        clean.contains("upper") -> CategoryIconSource.DrawableRes(R.drawable.ic_cat_strength)
        clean.contains("lower") -> CategoryIconSource.Vector(Icons.Default.DirectionsRun)
        else -> CategoryIconSource.Vector(Icons.Default.FitnessCenter)
    }
}

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val iconSource = remember(label) { resolveCategoryIcon(label) }

    Surface(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (isSelected) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
                    if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val iconTint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
            when (iconSource) {
                is CategoryIconSource.Vector -> {
                    Icon(
                        imageVector = iconSource.imageVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
                is CategoryIconSource.DrawableRes -> {
                    Icon(
                        painter = painterResource(id = iconSource.resId),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Dismissible active filter indicator tags under category pills.
 */
@Composable
private fun ActiveFilterRow(
    selectedDuration: DurationFilter,
    selectedSort: SortOption,
    onResetDuration: () -> Unit,
    onResetSort: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedDuration != DurationFilter.ALL) {
            item(key = "active_filter_duration") {
                ActiveTagChip(
                    text = "Durasi: ${selectedDuration.label}",
                    onDismiss = onResetDuration
                )
            }
        }

        if (selectedSort != SortOption.CREATED_AT) {
            item(key = "active_filter_sort") {
                val sortLabel = when (selectedSort) {
                    SortOption.FAVORITE -> "Favorit"
                    SortOption.LAST_PERFORMED -> "Terakhir Dilakukan"
                    SortOption.DURATION -> "Durasi Tercepat"
                    SortOption.CREATED_AT -> "Terbaru"
                }
                ActiveTagChip(
                    text = "Urutan: $sortLabel",
                    onDismiss = onResetSort
                )
            }
        }

        if (selectedDuration != DurationFilter.ALL && selectedSort != SortOption.CREATED_AT) {
            item(key = "active_filter_reset_all") {
                Text(
                    text = "Hapus Semua",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onResetAll)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveTagChip(
    text: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onDismiss)
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus filter",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

/**
 * Filter & Sort Bottom Sheet modal (Material 3) — Pure White Frosted Glass Standard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeFilterBottomSheet(
    selectedDuration: DurationFilter,
    selectedSort: SortOption,
    onDurationClick: (DurationFilter) -> Unit,
    onSortClick: (SortOption) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasActiveFilters = selectedDuration != DurationFilter.ALL || selectedSort != SortOption.CREATED_AT
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101014),
        scrimColor = Color.Black.copy(alpha = 0.70f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Filter & Urutkan",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Sesuaikan tampilan daftar latihan",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasActiveFilters) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onResetAll()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                        ) {
                            Text(
                                text = "Reset",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Section 1: Urutkan Berdasarkan
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "URUTKAN BERDASARKAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.50f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                val sortItems = listOf(
                    SortItemData(SortOption.CREATED_AT, "Terbaru Dibuat", "Berdasarkan tanggal ditambahkan", Icons.Default.Schedule),
                    SortItemData(SortOption.FAVORITE, "Favorit Teratas", "Rutinitas yang ditandai bintang", Icons.Default.Star),
                    SortItemData(SortOption.DURATION, "Durasi Tercepat", "Dari latihan tersingkat ke terlama", Icons.Default.Timer),
                    SortItemData(SortOption.LAST_PERFORMED, "Terakhir Dilakukan", "Rutinitas yang baru diselesaikan", Icons.Default.History)
                )

                sortItems.forEach { item ->
                    val isSelected = selectedSort == item.sort
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.White.copy(alpha = 0.18f)
                                            )
                                        )
                                    } else {
                                        SolidColor(Color.White.copy(alpha = 0.07f))
                                    }
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSortClick(item.sort)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sort Icon Orb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terpilih",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.18f)), CircleShape)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

            // Section 2: Durasi Latihan
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RENTANG DURASI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.50f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DurationFilter.entries.take(2).forEach { filter ->
                        DurationFilterPill(
                            filter = filter,
                            isSelected = selectedDuration == filter,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDurationClick(filter)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DurationFilter.entries.drop(2).forEach { filter ->
                        DurationFilterPill(
                            filter = filter,
                            isSelected = selectedDuration == filter,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDurationClick(filter)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Section 3: Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0D0D12)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF0D0D12),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terapkan Filter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0D0D12)
                )
            }
        }
    }
}

private data class SortItemData(
    val sort: SortOption,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
private fun DurationFilterPill(
    filter: DurationFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        )
                    } else {
                        SolidColor(Color.White.copy(alpha = 0.08f))
                    }
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Text(
                text = filter.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun WorkoutCard(
    routine: WorkoutRoutine,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(routine.accentColor))
    } catch (e: Exception) {
        AccentWork
    }

    val cardBgColor = Color(0xFF0F1115)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            val bannerRes = getCategoryBannerRes(routine.category)

            // 1. Ambient Glow di sudut kiri atas sesuai kategori (soft subtle tint)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 360f
                        )
                    )
            )

            // 2. Foto Atlet di sisi kanan, MENYATU secara mulus via alpha mask gradasi
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.58f)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        // Fade horizontal dari transparan (kiri) ke solid (kanan)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.22f to Color.Black.copy(alpha = 0.25f),
                                    0.50f to Color.Black.copy(alpha = 0.80f),
                                    0.75f to Color.Black
                                ),
                                startX = 0f,
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                        // Fade vertikal di tepi atas & bawah agar menyatu sempurna ke dalam lengkungan kartu
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.65f),
                                    0.15f to Color.Black,
                                    0.85f to Color.Black,
                                    1.0f to Color.Black.copy(alpha = 0.65f)
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = routine.name,
                    contentScale = ContentScale.Crop,
                    alignment = androidx.compose.ui.BiasAlignment(0.2f, 0f),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.22f, scaleY = 1.22f)
                )
            }

            // 3. Foreground content: Teks & Stats di kiri, Tombol Play Kaca Melayang di kanan
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sisi Kiri: Badge Kategori, Judul Rutinitas, Metadata
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Category Pill Badge (Frosted White Glass)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f))
                            )
                            Text(
                                text = routine.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Judul Rutinitas
                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Stats: ⏱ 10m 30s | ✔ 31 Gerakan
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.5.dp)
                            )
                            Text(
                                text = formatDuration(routine.totalDurationSec),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Text(
                            text = "|",
                            color = Color(0xFF4A5162),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.5.dp)
                            )
                            Text(
                                text = "${routine.totalExercises} Gerakan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Sisi Kanan: Frosted Glass Play Button melayang pas di atas tubuh foto atlet
                Surface(
                    onClick = onStartClick,
                    shape = CircleShape,
                    color = Color(0xFF0F1116).copy(alpha = 0.50f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.70f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mulai Latihan",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }
            }

            // 3. Tombol Favorit / Like di pojok kanan atas kartu
            Surface(
                onClick = onFavoriteClick,
                shape = CircleShape,
                color = if (routine.isFavorite) Color(0xFF1E1418).copy(alpha = 0.85f) else Color(0xFF0F1116).copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (routine.isFavorite) Color(0xFFFF453A).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.22f)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (routine.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (routine.isFavorite) "Disukai" else "Sukai",
                        tint = if (routine.isFavorite) Color(0xFFFF453A) else Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    iconRes: Int,
    text: String,
    tint: Color = TextSecondary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(
    onCreateNew: () -> Unit,
    onRestoreStandard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_workout),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = "Belum Ada Rutinitas",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Mulai dengan 16 preset standar dunia atau racik rutinitas kustommu sendiri",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onRestoreStandard,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24242A), contentColor = TextPrimary),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_stat_exercises),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat 16 Preset Standar Dunia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
        }
        OutlinedButton(
            onClick = onCreateNew,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF383842)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = AccentWork, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buat Rutinitas Kustom", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
        }
    }
}

// ── Utility ──────────────────────────────────────────────────────────────────

private fun formatDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (seconds == 0) "$minutes Menit" else "${minutes}m ${seconds}s"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupBottomSheet(
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRestoreStandard: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101014),
        scrimColor = Color.Black.copy(alpha = 0.70f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cadangkan & Pulihkan",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "100% Offline & Pribadi di Memori HP",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Restore Standard Preset Button
            Button(
                onClick = onRestoreStandard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.10f),
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                )
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_dumbbell_pro), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Muat Ulang 40 Preset Standar Ilmiah", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }

            // Export button
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor Backup (.zip)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
            }

            // Import button
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Impor / Pulihkan Backup (.zip)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
            }

            Text(
                text = "Cadangan berupa file .zip berisi database rutinitas dan semua media lokal (foto/GIF/video). Simpan di Google Drive atau memori aman.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.40f),
                lineHeight = 18.sp
            )
        }
    }
}
