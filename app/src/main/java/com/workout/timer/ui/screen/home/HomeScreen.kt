package com.workout.timer.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.ui.graphics.StrokeCap

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
    onNavigateToStartWorkout: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyMinutes by viewModel.weeklyTotalMinutes.collectAsStateWithLifecycle()
    val weeklySessions by viewModel.weeklySessionCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val hasActiveFilters = uiState.selectedDurationFilter != DurationFilter.ALL ||
        uiState.selectedSort != SortOption.CREATED_AT

    val featuredRoutine = remember(uiState.routines) {
        uiState.routines.firstOrNull { it.name.contains("Hikari", ignoreCase = true) }
            ?: uiState.routines.firstOrNull()
    }

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
                containerColor = Color(0xFF222228),
                contentColor = AccentWork,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .size(48.dp)
                    .border(BorderStroke(1.dp, AccentWork.copy(alpha = 0.45f)), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Buat Rutinitas Baru",
                    tint = AccentWork,
                    modifier = Modifier.size(22.dp)
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
                    .height(300.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_home_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.55f)
                )

                // Left Scrim Gradient: fades dark BackgroundPrimary from the left
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    BackgroundPrimary,
                                    BackgroundPrimary.copy(alpha = 0.90f),
                                    BackgroundPrimary.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 720f
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
                                    BackgroundPrimary.copy(alpha = 0.50f),
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
            item {
                HomeHeader(
                    onOpenBackup = { showBackupSheet = true },
                    onSyncCatalog = { viewModel.syncCatalog(isManual = true) },
                    isSyncing = uiState.isSyncing
                )
            }

            // ── Weekly Activity Ring ─────────────────────────────────────────
            item {
                WeeklyActivitySection(
                    totalMinutes = weeklyMinutes,
                    sessionCount = weeklySessions,
                    onClick = { onNavigateToStartWorkout(-1L) }
                )
            }

            // ── Spotlight / Daily Challenge Banner ───────────────────────────
            if (uiState.searchQuery.isBlank() && uiState.selectedCategory == null && !hasActiveFilters && featuredRoutine != null) {
                item {
                    SpotlightBanner(
                        featuredRoutine = featuredRoutine,
                        onStartWorkout = onNavigateToStartWorkout,
                        onViewDetail = onNavigateToRoutineDetail,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // ── Search Bar ──────────────────────────────────────────────────
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    hasActiveFilters = hasActiveFilters,
                    onOpenFilter = { showFilterSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // ── Category Pills (Single Dedicated Row) ────────────────────────
            item {
                CategoryRow(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClick = viewModel::onCategorySelected,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // ── Active Filter Tags (Dismissible) ────────────────────────────
            if (hasActiveFilters) {
                item {
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

            // ── Section Label ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AccentWork,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (uiState.searchQuery.isBlank() && uiState.selectedCategory == null && !hasActiveFilters)
                            "Semua Rutinitas"
                        else
                            "Hasil Pencarian",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E2129)
                    ) {
                        Text(
                            text = "${uiState.routines.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
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
                        CircularProgressIndicator(color = AccentWork)
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
    onOpenBackup: () -> Unit,
    onSyncCatalog: () -> Unit = {},
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
                    color = AccentWork
                )
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
                    .background(AccentWork, RoundedCornerShape(2.dp))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Tombol Sync Katalog Server (Actionable - Preserved)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14161C).copy(alpha = 0.85f))
                    .border(BorderStroke(1.dp, Color(0xFF232733)), CircleShape)
                    .clickable(enabled = !isSyncing, onClick = onSyncCatalog),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AccentWork,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync Katalog Server",
                        tint = AccentWork,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Tombol Backup & Restore (Actionable - Preserved)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14161C).copy(alpha = 0.85f))
                    .border(BorderStroke(1.dp, Color(0xFF232733)), CircleShape)
                    .clickable(onClick = onOpenBackup),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cloud_backup_sleek),
                    contentDescription = "Backup & Restore",
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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Ambient radial glow behind the ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentWork.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                center = center,
                radius = radius * 1.35f
            )

            // Track background
            drawCircle(
                color = Color(0xFF1F2430),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Dynamic progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AccentWork,
                        Color(0xFF00E676),
                        AccentWork
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0.08f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Icon: Athletic Runner
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
            contentDescription = "Aktivitas",
            tint = AccentWork,
            modifier = Modifier.size(28.dp)
        )
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
        border = BorderStroke(1.dp, Color(0xFF222632))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B202A),
                    border = BorderStroke(1.dp, Color(0xFF2A3242))
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
                            color = Color(0xFFFF8C42),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    StatItem(
                        value = "$totalMinutes",
                        unit = "Menit Latihan",
                        color = Color.White
                    )
                    StatItem(
                        value = "$sessionCount",
                        unit = "Sesi Selesai",
                        color = AccentWork,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            // High-precision Activity Progress Ring with Center Runner
            AthleticActivityRing(
                progress = (totalMinutes / 120f).coerceIn(0.18f, 1f),
                modifier = Modifier.size(86.dp)
            )
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

@Composable
private fun SpotlightBanner(
    featuredRoutine: WorkoutRoutine,
    onStartWorkout: (Long) -> Unit,
    onViewDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(188.dp)
            .clickable { onViewDetail(featuredRoutine.id) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    AccentWork.copy(alpha = 0.55f),
                    AccentRest.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        )
    ) {
        val bannerRes = getCategoryBannerRes(featuredRoutine.category, isSpotlightDefault = true)
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
                // Top Tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentWork.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.60f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_flame_streak),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "TANTANGAN HARI INI",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentWork,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    }
                }

                // Middle & Bottom Section with Pagination Dots
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
                            text = featuredRoutine.name,
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
                                text = formatDuration(featuredRoutine.totalDurationSec),
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
                                text = featuredRoutine.category.uppercase(),
                                tint = AccentRest
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Start Quick Button (Actionable - Preserved)
                        Button(
                            onClick = { onStartWorkout(featuredRoutine.id) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentWork,
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

                    // Pagination indicator dots matching screenshot
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(5.dp)
                                .background(AccentWork, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFF4A4E5C), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFF4A4E5C), CircleShape)
                        )
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .border(BorderStroke(1.dp, Color(0xFF232733)), RoundedCornerShape(26.dp)),
            placeholder = {
                Text("Cari rutinitas...", color = TextTertiary, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(visible = query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Hapus",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF14161C),
                unfocusedContainerColor = Color(0xFF14161C),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentWork
            ),
            shape = RoundedCornerShape(26.dp),
            singleLine = true
        )

        // Filter / Tune button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (hasActiveFilters) AccentWork.copy(alpha = 0.15f) else Color(0xFF14161C))
                .border(
                    BorderStroke(
                        1.dp,
                        if (hasActiveFilters) AccentWork else Color(0xFF232733)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onOpenFilter),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter & Urutkan",
                tint = if (hasActiveFilters) AccentWork else TextSecondary,
                modifier = Modifier.size(22.dp)
            )

            // Active filter counter indicator
            if (hasActiveFilters) {
                FilterActiveCounter(
                    count = 1,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                )
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

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val categoryIcon = when (label.lowercase()) {
        "semua", "all" -> Icons.Default.Apps
        "cardio" -> Icons.Outlined.FavoriteBorder
        "core" -> Icons.Outlined.Grid3x3
        "full body" -> Icons.Outlined.FitnessCenter
        "hiit" -> Icons.Default.Bolt
        else -> Icons.Outlined.FitnessCenter
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) AccentWork else Color(0xFF14161C))
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) AccentWork else Color(0xFF232733)
                ),
                RoundedCornerShape(22.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else TextSecondary,
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
                    color = AccentRest,
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
            .background(AccentRest.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, AccentRest.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = AccentRest,
            fontSize = 11.sp
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Hapus filter",
            tint = AccentRest,
            modifier = Modifier.size(13.dp)
        )
    }
}

/**
 * Filter & Sort Bottom Sheet modal (Material 3).
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF383842), RoundedCornerShape(2.dp))
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AccentWork,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Filter & Urutkan",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasActiveFilters) {
                    TextButton(
                        onClick = onResetAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Reset",
                            color = AccentWork,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Section 1: Urutkan Berdasarkan
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "URUTKAN BERDASARKAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val sortItems = listOf(
                    Triple(SortOption.CREATED_AT, "Terbaru Dibuat", "Berdasarkan tanggal ditambahkan"),
                    Triple(SortOption.FAVORITE, "Favorit Teratas", "Rutinitas yang ditandai bintang"),
                    Triple(SortOption.DURATION, "Durasi Tercepat", "Dari latihan tersingkat ke terlama"),
                    Triple(SortOption.LAST_PERFORMED, "Terakhir Dilakukan", "Rutinitas yang baru diselesaikan")
                )

                sortItems.forEach { (sort, title, subtitle) ->
                    val isSelected = selectedSort == sort
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF22222C) else Color.Transparent)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentWork.copy(alpha = 0.5f) else Color.Transparent
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSortClick(sort) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AccentWork else TextPrimary
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Terpilih",
                                tint = AccentWork,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF282832), thickness = 1.dp)

            // Section 2: Durasi Latihan
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RENTANG DURASI",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DurationFilter.entries.take(2).forEach { filter ->
                        DurationFilterPill(
                            filter = filter,
                            isSelected = selectedDuration == filter,
                            onClick = { onDurationClick(filter) },
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
                            onClick = { onDurationClick(filter) },
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
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentWork,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Terapkan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DurationFilterPill(
    filter: DurationFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentRest.copy(alpha = 0.18f) else Color(0xFF1E1E26))
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) AccentRest else Color(0xFF2C2C38)
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = filter.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AccentRest else TextSecondary,
            fontSize = 13.sp
        )
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
        border = BorderStroke(1.dp, Color(0xFF222632))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Category Badge + Favorite + Title + Metadata Stats
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Category Badge & Favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = routine.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                color = accentColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (routine.isFavorite) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorit",
                            tint = if (routine.isFavorite) AccentHighIntensity else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Routine Title
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata Stats (Timer & Exercises)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatDuration(routine.totalDurationSec),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = "|",
                        color = Color(0xFF383C4A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${routine.totalExercises} Gerakan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextSecondary
                        )
                    }
                }
            }

            // Right Column: Exercise/Routine Thumbnail with Centered Frosted Glass Play Button
            val bannerRes = getCategoryBannerRes(routine.category)
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 86.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = routine.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark vignette overlay on thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f))
                )

                // Glassmorphism Play Button in center
                Surface(
                    onClick = onStartClick,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.50f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mulai Latihan",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(start = 2.dp)
                        )
                    }
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
        containerColor = SurfaceCard,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "100% Offline & Pribadi di Memori HP",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Restore Standard Preset Button
            Button(
                onClick = onRestoreStandard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF24242A),
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.45f))
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_dumbbell_pro), contentDescription = null, tint = AccentWork, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Muat Ulang 15 Preset Standar Ilmiah", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            }

            // Export button
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, Color(0xFF383842))
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor Backup (.zip)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            }

            // Import button
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, AccentRest.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = AccentRest, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Impor / Pulihkan Backup (.zip)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            }

            Text(
                text = "Cadangan berupa file .zip berisi database rutinitas dan semua media lokal (foto/GIF/video). Simpan di Google Drive atau memori aman.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                lineHeight = 18.sp
            )
        }
    }
}
