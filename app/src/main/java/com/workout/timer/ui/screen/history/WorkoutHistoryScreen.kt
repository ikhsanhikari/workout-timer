package com.workout.timer.ui.screen.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.model.WorkoutSessionRecord
import com.workout.timer.ui.screen.home.HomeBottomNavigation
import com.workout.timer.ui.screen.home.HomeTab
import com.workout.timer.ui.theme.BackgroundPrimary
import com.workout.timer.ui.theme.getCategoryBannerRes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ────────────────────────────────────────────────────────────────────────────
// Pure Frosted Glass Theme Constants (High Information Density & Sleek Cards)
// ────────────────────────────────────────────────────────────────────────────

private val TextMuted = Color(0xFF8E95A5)
private val CardBackground = Color(0xFF13151D)
private val GlassBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.05f)
    )
)

/**
 * WorkoutHistoryScreen — Layar Riwayat & Progres Analitik.
 *
 * Mengadopsi 100% spesifikasi ukuran dan style kartu referensi:
 * 1. Top Bar: Header ramping "Riwayat & Progres" + Subtitle + Tombol aksi kaca di kanan atas.
 * 2. Card 1 (Summary KPI): 3 metrik dalam 1 baris kompak (Total Waktu, Total Sesi, Minggu Ini) ~54dp.
 * 3. Card 2 (Aktivitas Minggu Ini): Header dengan squircle bar chart & badge aktif,
 *    indikator 7 hari menggunakan horizontal capsule pill (putih untuk hari aktif + durasi, pill abu pipih untuk kosong).
 * 4. Card 3 (Distribusi Latihan): Header dengan pie icon, multi-segment progress bar, dan chip kategori filter.
 * 5. Tab Segmented Capsule: "Riwayat Selesai" & "Katalog Rutinitas" berbalut capsule glass.
 * 6. Search Bar: Capsule search input field ramping.
 * 7. Workout History Cards: Ukuran kompak (~80dp) dengan thumbnail ber-badge checkmark,
 *    3 baris informasi (kategori/waktu, nama rutinitas, durasi/gerakan), dan tombol pill putih "Ulangi".
 */
@Composable
fun WorkoutHistoryScreen(
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToRoutineDetail: (Long) -> Unit = {},
    onNavigateToStartWorkout: (Long) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToWorkoutPlan: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            HistoryTopBar(
                hasHistory = uiState.sessionRecords.isNotEmpty(),
                onClearHistory = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showClearConfirmDialog = true
                }
            )
        },
        bottomBar = {
            HomeBottomNavigation(
                currentTab = HomeTab.PROGRESS,
                onTabSelected = { tab ->
                    when (tab) {
                        HomeTab.BERANDA -> onNavigateToHome()
                        HomeTab.LATIHAN -> onNavigateToWorkoutPlan()
                        HomeTab.PROGRESS -> { /* Sedang di Progress */ }
                        HomeTab.PROFIL -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── 1. Summary KPI Card (3 Metrik dalam 1 Baris Kompak) ────
                    item(key = "history_summary_kpi_card") {
                        HistorySummaryCard(
                            allTimeMinutes = uiState.allTimeTotalMinutes,
                            allTimeSessions = uiState.allTimeSessionCount,
                            weeklyMinutes = uiState.weeklyTotalMinutes
                        )
                    }

                    // ── 2. Aktivitas Minggu Ini (7-Day Horizontal Capsule Pills) ─
                    item(key = "history_weekly_activity_card") {
                        WeeklyActivityCard(
                            days = uiState.weeklyDaysActivity,
                            activeDaysCount = uiState.activeDaysCountThisWeek
                        )
                    }

                    // ── 3. Distribusi Latihan (Progress Bar + Filter Chips) ──────
                    item(key = "history_category_distribution_card") {
                        CategoryDistributionCard(
                            categoryBreakdown = uiState.categoryBreakdown,
                            selectedCategoryFilter = uiState.selectedCategoryFilter,
                            onSelectCategory = viewModel::onCategoryFilterSelected
                        )
                    }

                    // ── 4. Segmented Tab Capsule Selector ──────────────────────
                    item(key = "history_tab_selector") {
                        HistoryTabSelector(
                            selectedTab = uiState.selectedTab,
                            performedCount = uiState.sessionRecords.size,
                            totalCount = uiState.allRoutines.size,
                            onTabSelected = { tab ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectTab(tab)
                            }
                        )
                    }

                    // ── 5. Search Bar Input Capsule ────────────────────────────
                    item(key = "history_search_bar") {
                        HistorySearchBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChanged = viewModel::onSearchQueryChanged
                        )
                    }

                    // ── 6. Workout History Session Records / Routines ──────────
                    if (uiState.selectedTab == HistoryTab.COMPLETED) {
                        if (uiState.filteredRecords.isEmpty()) {
                            item(key = "history_empty_records") {
                                CompactHistoryEmptyState(
                                    isCompletedTab = true,
                                    hasAnyRoutines = uiState.allRoutines.isNotEmpty(),
                                    onSwitchToAll = { viewModel.selectTab(HistoryTab.ALL) },
                                    onExploreWorkouts = onNavigateToWorkoutPlan
                                )
                            }
                        } else {
                            items(uiState.filteredRecords, key = { "record_${it.id}" }) { record ->
                                HistorySessionRecordCard(
                                    record = record,
                                    onDetailClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectRecordForDetail(record)
                                    },
                                    onStartClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToStartWorkout(record.routineId)
                                    }
                                )
                            }
                        }
                    } else {
                        if (uiState.filteredRoutines.isEmpty()) {
                            item(key = "history_empty_routines") {
                                CompactHistoryEmptyState(
                                    isCompletedTab = false,
                                    hasAnyRoutines = uiState.allRoutines.isNotEmpty(),
                                    onSwitchToAll = { viewModel.selectTab(HistoryTab.ALL) },
                                    onExploreWorkouts = onNavigateToWorkoutPlan
                                )
                            }
                        } else {
                            items(uiState.filteredRoutines, key = { "routine_${it.id}" }) { routine ->
                                HistoryRoutineCard(
                                    routine = routine,
                                    onDetailClick = { onNavigateToRoutineDetail(routine.id) },
                                    onStartClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToStartWorkout(routine.id)
                                    }
                                )
                            }
                        }
                    }

                    item(key = "history_bottom_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // ── Detail Sesi Latihan Faktual (Modal Bottom Sheet) ──────────────────
        uiState.selectedRecordForDetail?.let { record ->
            WorkoutSessionDetailBottomSheet(
                record = record,
                onDismiss = { viewModel.selectRecordForDetail(null) },
                onRepeatWorkout = { routineId ->
                    onNavigateToStartWorkout(routineId)
                },
                onViewRoutineTemplate = { routineId ->
                    onNavigateToRoutineDetail(routineId)
                },
                onDeleteRecord = { recordId ->
                    viewModel.deleteSessionRecord(recordId)
                }
            )
        }

        // ── Clear History Confirmation Dialog ────────────────────────────────
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = Color(0xFF14171E),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Reset Riwayat Latihan?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "Tindakan ini akan mengosongkan rekaman seluruh sesi latihan. Rutinitas latihan bawaan Anda tidak akan terhapus.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp)
                    )
                },
                confirmButton = {
                    Surface(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearConfirmDialog = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.40f))
                    ) {
                        Text(
                            text = "Reset Riwayat",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Batal", color = TextMuted, fontSize = 13.sp)
                    }
                }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 1. Top Bar
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTopBar(
    hasHistory: Boolean,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Riwayat & Progres",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Analitik aktivitas latihan yang telah diselesaikan",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted
            )
        }

        if (hasHistory) {
            Surface(
                onClick = onClearHistory,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Opsi Riwayat",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 2. Summary KPI Card (3 Kolom Kompak dalam 1 Kartu Kaca)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistorySummaryCard(
    allTimeMinutes: Int,
    allTimeSessions: Int,
    weeklyMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Kolom 1: Total Waktu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = formatMinutes(allTimeMinutes),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Total Waktu",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = TextMuted
                    )
                }
            }

            // Divider 1
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Kolom 2: Total Sesi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "$allTimeSessions",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Total Sesi",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = TextMuted
                    )
                }
            }

            // Divider 2
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Kolom 3: Minggu Ini
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = formatMinutes(weeklyMinutes),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Minggu Ini",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = TextMuted
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 3. Aktivitas Minggu Ini (7-Day Horizontal Capsule Pill Indicators)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyActivityCard(
    days: List<DayActivityUiModel>,
    activeDaysCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Aktivitas Minggu Ini",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Volume harian Senin s/d Minggu",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Text(
                        text = "$activeDaysCount/7 Hari Aktif",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-Day Horizontal Capsule Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    val hasActivity = day.totalMinutes > 0
                    val isToday = day.isToday

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Durasi menit di atas capsule (hanya jika ada aktivitas)
                        if (hasActivity) {
                            Text(
                                text = "${day.totalMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        } else {
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Capsule Pill
                        if (hasActivity) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.12f))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = day.dayNameShort,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (hasActivity || isToday) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (hasActivity || isToday) Color.White else TextMuted
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 4. Distribusi Latihan (Progress Bar + Filter Chips)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryDistributionCard(
    categoryBreakdown: List<CategoryShareUiModel>,
    selectedCategoryFilter: String?,
    onSelectCategory: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Distribusi Latihan",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Fokus kategori berdasarkan total durasi",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment Horizontal Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                if (categoryBreakdown.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        categoryBreakdown.forEach { item ->
                            val weight = item.percentage.coerceAtLeast(1)
                            Box(
                                modifier = Modifier
                                    .weight(weight.toFloat())
                                    .fillMaxHeight()
                                    .background(
                                        when (item.category.lowercase()) {
                                            "mobility", "yoga", "stretch" -> Color.White
                                            "strength" -> Color.White.copy(alpha = 0.75f)
                                            "hiit" -> Color.White.copy(alpha = 0.50f)
                                            "cardio" -> Color.White.copy(alpha = 0.35f)
                                            else -> Color.White.copy(alpha = 0.60f)
                                        }
                                    )
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (categoryBreakdown.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Text(
                                text = "Belum ada data sesi",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                } else {
                    items(categoryBreakdown, key = { it.category }) { item ->
                        val isSelected = selectedCategoryFilter.equals(item.category, ignoreCase = true)
                        Surface(
                            onClick = { onSelectCategory(if (isSelected) null else item.category) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.14f)
                            )
                        ) {
                            Text(
                                text = "${item.category} ${item.percentage}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 5. Segmented Tab Capsule Selector
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTabSelector(
    selectedTab: HistoryTab,
    performedCount: Int,
    totalCount: Int,
    onTabSelected: (HistoryTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Riwayat Selesai
            val isCompleted = selectedTab == HistoryTab.COMPLETED
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onTabSelected(HistoryTab.COMPLETED) },
                shape = RoundedCornerShape(50),
                color = if (isCompleted) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                border = if (isCompleted) BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    )
                ) else null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Riwayat Selesai",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        ),
                        color = if (isCompleted) Color.White else TextMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isCompleted) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "$performedCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isCompleted) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Tab 2: Katalog Rutinitas
            val isAll = selectedTab == HistoryTab.ALL
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onTabSelected(HistoryTab.ALL) },
                shape = RoundedCornerShape(50),
                color = if (isAll) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                border = if (isAll) BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    )
                ) else null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Katalog Rutinitas",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        ),
                        color = if (isAll) Color.White else TextMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isAll) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isAll) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 6. Search Bar Input Capsule
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistorySearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
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
                tint = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Cari riwayat atau nama latihan...",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 12.5.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontSize = 12.5.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChanged("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 7. Workout History Session Card (Ukuran Kompak 80dp & Checkmark Badge)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistorySessionRecordCard(
    record: WorkoutSessionRecord,
    onDetailClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val bannerRes = getCategoryBannerRes(record.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Thumbnail Foto dengan Checkmark Badge di Pojok Kanan Bawah
            Box(modifier = Modifier.size(58.dp)) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = record.routineName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(13.dp))
                )

                // Overlapping circular white badge with dark checkmark
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selesai",
                            tint = Color.Black,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Middle Information Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Baris 1: Kategori & Waktu (MOBILITY • Hari ini, 13:15)
                Text(
                    text = "${record.category.uppercase()} • ${formatLastPerformed(record.completedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF9DA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Baris 2: Judul Rutinitas
                Text(
                    text = record.routineName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Baris 3: Durasi & Jumlah Gerakan
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF9DA3AF),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatDuration(record.durationSeconds),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9DA3AF)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFF9DA3AF),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${record.totalExercisesCompleted}/${record.totalExercisesPlanned} Gerakan",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9DA3AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Tombol Aksi Kanan: Pill Putih Solid "▶ Ulangi"
            Surface(
                onClick = onStartClick,
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Ulangi",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 8. Catalog Routine Card (Tab Katalog Rutinitas)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryRoutineCard(
    routine: WorkoutRoutine,
    onDetailClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val bannerRes = getCategoryBannerRes(routine.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(modifier = Modifier.size(58.dp)) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = routine.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Information Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${routine.category.uppercase()} • ${routine.totalExercises} Gerakan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF9DA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF9DA3AF),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatDuration(routine.totalDurationSec),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9DA3AF)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFF9DA3AF),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${routine.totalExercises} Gerakan",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9DA3AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Tombol Pill Putih Solid "▶ Mulai"
            Surface(
                onClick = onStartClick,
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Mulai",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 9. Bottom Sheet Detail Sesi Latihan
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutSessionDetailBottomSheet(
    record: WorkoutSessionRecord,
    onDismiss: () -> Unit,
    onRepeatWorkout: (Long) -> Unit,
    onViewRoutineTemplate: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1116),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Sesi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = record.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = record.routineName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFullSessionDate(record.completedAt),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Grid 4 Metrik Faktual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricDetailTile(
                    modifier = Modifier.weight(1f),
                    title = "Durasi Riil",
                    value = formatDuration(record.durationSeconds),
                    icon = Icons.Default.Timer
                )
                MetricDetailTile(
                    modifier = Modifier.weight(1f),
                    title = "Gerakan Selesai",
                    value = "${record.totalExercisesCompleted}/${record.totalExercisesPlanned}",
                    icon = Icons.Outlined.FitnessCenter
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricDetailTile(
                    modifier = Modifier.weight(1f),
                    title = "Estimasi Kalori",
                    value = "${estimateCaloriesBurned(record.category, record.durationSeconds)} kkal",
                    icon = Icons.Default.LocalFireDepartment
                )
                MetricDetailTile(
                    modifier = Modifier.weight(1f),
                    title = "Tingkat Selesai",
                    value = if (record.isCompletedFully) "100% Penuh" else "Parsial",
                    icon = Icons.Default.CheckCircle
                )
            }

            // Tombol Aksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        onDismiss()
                        onRepeatWorkout(record.routineId)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ulangi Latihan",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onViewRoutineTemplate(record.routineId)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buka Templat",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            // Hapus Catatan Sesi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hapus Rekaman Sesi Ini",
                        color = Color(0xFFEF4444).copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF14171E),
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Hapus Riwayat Sesi?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Catatan sesi latihan ini akan dihapus dari riwayat aktivitas. Tindakan ini tidak dapat dibatalkan.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Surface(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        onDeleteRecord(record.id)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.40f))
                ) {
                    Text(
                        text = "Hapus",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextMuted, fontSize = 12.5.sp)
                }
            }
        )
    }
}

@Composable
private fun MetricDetailTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = Color.White
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 10. Empty State
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactHistoryEmptyState(
    isCompletedTab: Boolean,
    hasAnyRoutines: Boolean,
    onSwitchToAll: () -> Unit,
    onExploreWorkouts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                text = if (isCompletedTab) "Belum Ada Riwayat Latihan" else "Tidak Ada Latihan Ditemukan",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = Color.White
            )

            Text(
                text = if (isCompletedTab)
                    "Selesaikan latihan pertamamu untuk mencatat durasi, gerakan, dan progres aktivitas secara otomatis di sini."
                else
                    "Coba periksa kata kunci pencarian Anda.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = if (isCompletedTab && hasAnyRoutines) onSwitchToAll else onExploreWorkouts,
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isCompletedTab && hasAnyRoutines) "Lihat Semua Latihan" else "Mulai Latihan Sekarang",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Helpers
// ────────────────────────────────────────────────────────────────────────────

private fun estimateCaloriesBurned(category: String, durationSeconds: Int): Int {
    val met = when (category.lowercase()) {
        "hiit" -> 9.0
        "strength" -> 6.0
        "cardio" -> 7.5
        "core" -> 5.5
        "mobility", "yoga", "stretch" -> 3.5
        else -> 6.5
    }
    val hours = durationSeconds / 3600.0
    val weightKg = 70.0
    return (hours * met * weightKg).toInt().coerceAtLeast(1)
}

private fun formatFullSessionDate(timestampMs: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm 'WIB'", Locale("id", "ID"))
    return sdf.format(Date(timestampMs))
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m > 0) "${h}j ${m}m" else "${h} Jam"
    } else {
        "$minutes Menit"
    }
}

private fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) "${m}m ${s}d" else "${s}d"
}

private fun formatLastPerformed(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestampMs }

    val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    val timeStr = timeFormat.format(Date(timestampMs))

    val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    if (isToday) {
        return "Hari ini, $timeStr"
    }

    now.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) {
        return "Kemarin, $timeStr"
    }

    val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID"))
    return dateFormat.format(Date(timestampMs))
}
