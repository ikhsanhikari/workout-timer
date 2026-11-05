package com.workout.timer.ui.screen.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.model.WorkoutSessionRecord
import com.workout.timer.ui.theme.BackgroundPrimary
import com.workout.timer.ui.theme.ColorDanger
import com.workout.timer.ui.theme.SurfaceCard
import com.workout.timer.ui.theme.TextPrimary
import com.workout.timer.ui.theme.TextSecondary
import com.workout.timer.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutHistoryScreen(
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRoutineDetail: (Long) -> Unit,
    onNavigateToStartWorkout: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            HistoryTopBar(
                onNavigateBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateBack()
                },
                hasHistory = uiState.sessionRecords.isNotEmpty(),
                onClearHistory = { showClearConfirmDialog = true }
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── 1. Summary Metrics Banner ────────────────────────────
                    item(key = "history_summary_banner") {
                        HistorySummaryCard(
                            weeklyMinutes = uiState.weeklyTotalMinutes,
                            weeklySessions = uiState.weeklySessionCount,
                            allTimeMinutes = uiState.allTimeTotalMinutes,
                            allTimeSessions = uiState.allTimeSessionCount
                        )
                    }

                    // ── 2. Tab Selector (Pernah Dilakukan vs Semua) ──────────
                    item(key = "history_tabs") {
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

                    // ── 3. Search Bar (if list is not empty) ─────────────────
                    if (uiState.allRoutines.isNotEmpty() || uiState.sessionRecords.isNotEmpty()) {
                        item(key = "history_search_bar") {
                            HistorySearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = viewModel::onSearchQueryChanged
                            )
                        }
                    }

                    // ── 4. History Items or Empty State ──────────────────────
                    if (uiState.selectedTab == HistoryTab.COMPLETED) {
                        if (uiState.filteredRecords.isEmpty()) {
                            item(key = "history_empty_state_records") {
                                HistoryEmptyState(
                                    isCompletedTab = true,
                                    hasAnyRoutines = uiState.allRoutines.isNotEmpty(),
                                    onSwitchToAll = { viewModel.selectTab(HistoryTab.ALL) },
                                    onExploreWorkouts = onNavigateBack
                                )
                            }
                        } else {
                            items(uiState.filteredRecords, key = { "record_${it.id}" }) { record ->
                                HistorySessionRecordCard(
                                    record = record,
                                    onDetailClick = { onNavigateToRoutineDetail(record.routineId) },
                                    onStartClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNavigateToStartWorkout(record.routineId)
                                    }
                                )
                            }
                        }
                    } else {
                        if (uiState.filteredRoutines.isEmpty()) {
                            item(key = "history_empty_state_routines") {
                                HistoryEmptyState(
                                    isCompletedTab = false,
                                    hasAnyRoutines = uiState.allRoutines.isNotEmpty(),
                                    onSwitchToAll = { viewModel.selectTab(HistoryTab.ALL) },
                                    onExploreWorkouts = onNavigateBack
                                )
                            }
                        } else {
                            items(uiState.filteredRoutines, key = { "history_${it.id}" }) { routine ->
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
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        // ── Clear History Confirmation Dialog ────────────────────────────────
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = SurfaceCard,
                title = {
                    Text(
                        text = "Reset Riwayat Latihan?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Tindakan ini akan mengosongkan status penyelesaian seluruh sesi latihan. Rutinitas latihan Anda tidak akan terhapus.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearConfirmDialog = false
                        }
                    ) {
                        Text(
                            text = "Reset Riwayat",
                            color = ColorDanger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTopBar(
    onNavigateBack: () -> Unit,
    hasHistory: Boolean,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Riwayat Latihan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Catatan penyelesaian aktivitas",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (hasHistory) {
            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Hapus Riwayat",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Summary Metrics Banner ───────────────────────────────────────────────────

@Composable
private fun HistorySummaryCard(
    weeklyMinutes: Int,
    weeklySessions: Int,
    allTimeMinutes: Int,
    allTimeSessions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "RINGKASAN AKTIVITAS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = "Total $allTimeSessions Sesi",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            // Stats 4-column / 2x2 grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    value = formatMinutes(weeklyMinutes),
                    label = "Minggu Ini",
                    sub = "$weeklySessions Sesi Selesai"
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Color.White.copy(alpha = 0.14f))
                )

                MetricColumn(
                    value = formatMinutes(allTimeMinutes),
                    label = "Sepanjang Waktu",
                    sub = "Komitmen Latihan"
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String, sub: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            ),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.85f)
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

// ── Tab Selector ─────────────────────────────────────────────────────────────

@Composable
private fun HistoryTabSelector(
    selectedTab: HistoryTab,
    performedCount: Int,
    totalCount: Int,
    onTabSelected: (HistoryTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tab 1: Pernah Dilakukan
            TabButton(
                title = "Pernah Dilakukan",
                count = performedCount,
                isSelected = selectedTab == HistoryTab.COMPLETED,
                onClick = { onTabSelected(HistoryTab.COMPLETED) },
                modifier = Modifier.weight(1f)
            )

            // Tab 2: Semua Rutinitas
            TabButton(
                title = "Semua Latihan",
                count = totalCount,
                isSelected = selectedTab == HistoryTab.ALL,
                onClick = { onTabSelected(HistoryTab.ALL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color.White.copy(alpha = 0.28f) else Color.Transparent
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

// ── Search Bar ───────────────────────────────────────────────────────────────

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Cari riwayat atau nama latihan...", color = Color.White.copy(alpha = 0.40f)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color.White.copy(alpha = 0.65f))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.45f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
        )
    )
}

// ── History Session Record Card ──────────────────────────────────────────────

@Composable
private fun HistorySessionRecordCard(
    record: WorkoutSessionRecord,
    onDetailClick: () -> Unit,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onDetailClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Category Pill + Completion Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = record.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White
                    )
                }

                // Status Badge (Selesai Penuh vs Parsial)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (record.isCompletedFully) Color.White.copy(alpha = 0.12f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, if (record.isCompletedFully) Color.White.copy(alpha = 0.25f) else Color(0xFFFFB300).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (record.isCompletedFully) Icons.Default.CheckCircle else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (record.isCompletedFully) Color.White else Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (record.isCompletedFully) "SELESAI PENUH" else "SELESAI SEBAGIAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (record.isCompletedFully) Color.White else Color(0xFFFFB300)
                        )
                    }
                }
            }

            // Routine Title
            Text(
                text = record.routineName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration, Exercises, and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatDuration(record.durationSeconds),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )
                    }

                    // Exercises count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${record.totalExercisesCompleted}/${record.totalExercisesPlanned} Gerakan",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )
                    }
                }

                // Timestamp
                Text(
                    text = formatLastPerformed(record.completedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Quick Play Button
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Latihan Lagi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

// ── History Routine Card ─────────────────────────────────────────────────────

@Composable
private fun HistoryRoutineCard(
    routine: WorkoutRoutine,
    onDetailClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val isCompleted = routine.lastPerformedAt != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onDetailClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Category Pill + Completion Timestamp / Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pill (Frosted White Glass)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = routine.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White
                    )
                }

                // Status / Last Performed Time
                if (isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatLastPerformed(routine.lastPerformedAt!!),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = "Belum Dilakukan",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            // Routine Title
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Duration & Exercises Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatDuration(routine.totalDurationSec),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${routine.totalExercises} Gerakan",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )
                }
            }

            // Actions Row: [ Detail ] and [ Mulai Lagi / Latihan ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Lihat Detail",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCompleted) "Mulai Lagi" else "Mulai Latihan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryEmptyState(
    isCompletedTab: Boolean,
    hasAnyRoutines: Boolean,
    onSwitchToAll: () -> Unit,
    onExploreWorkouts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = if (isCompletedTab) "Belum Ada Latihan yang Diselesaikan" else "Tidak Ada Latihan Ditemukan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = if (isCompletedTab)
                    "Selesaikan latihan pertama Anda untuk mulai mencatat riwayat durasi dan perkembangan aktivitas Anda secara otomatis di sini."
                else
                    "Coba periksa kata kunci pencarian Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isCompletedTab && hasAnyRoutines) {
                Button(
                    onClick = onSwitchToAll,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Lihat Semua Latihan Tersedia",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            } else {
                Button(
                    onClick = onExploreWorkouts,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Mulai Latihan Sekarang",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

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

