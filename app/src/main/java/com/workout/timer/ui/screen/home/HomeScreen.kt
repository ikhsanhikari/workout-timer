package com.workout.timer.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import android.app.TimePickerDialog
import java.util.Locale
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
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import java.util.Calendar
import com.workout.timer.data.model.WorkoutReminderItem
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
    onNavigateToWorkoutPlan: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyMinutes by viewModel.weeklyTotalMinutes.collectAsStateWithLifecycle()
    val weeklySessions by viewModel.weeklySessionCount.collectAsStateWithLifecycle()
    val weeklyDeltaMinutes by viewModel.weeklyDeltaMinutes.collectAsStateWithLifecycle()
    val customReminders by viewModel.customReminders.collectAsStateWithLifecycle()
    val isAnyReminderActive by viewModel.isAnyReminderActive.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    val hasActiveFilters = uiState.selectedDurationFilter != DurationFilter.ALL ||
        uiState.selectedSort != SortOption.CREATED_AT

    val dailyChallengeItems = remember(
        uiState.todayScheduledRoutine,
        uiState.favoriteRoutines,
        uiState.latestPerformedRoutine,
        uiState.routines
    ) {
        val scheduled = uiState.todayScheduledRoutine
        val favorites = uiState.favoriteRoutines
        val latest = uiState.latestPerformedRoutine
        when {
            // Prioritas 1: Jadwal Latihan Hari Ini (jika ada jadwal di Workout Plan)
            scheduled != null -> listOf(scheduled) to ChallengeSource.SCHEDULED_TODAY
            // Prioritas 2: HANYA yang di-like (favorit)
            favorites.isNotEmpty() -> favorites to ChallengeSource.FAVORITE
            // Prioritas 3: Terakhir kali dilakukan (jika belum ada yang di-like)
            latest != null -> listOf(latest) to ChallengeSource.LATEST_PERFORMED
            // Prioritas 4: Fallback rekomendasi katalog awal jika belum ada favorit & riwayat
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
        bottomBar = {
            HomeBottomNavigation(
                currentTab = HomeTab.BERANDA,
                onTabSelected = { tab ->
                    when (tab) {
                        HomeTab.BERANDA -> { /* Sedang di Beranda */ }
                        HomeTab.LATIHAN -> onNavigateToWorkoutPlan()
                        HomeTab.PROGRESS -> onNavigateToHistory()
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
            // ── Header ──────────────────────────────────────────────────────
            item(key = "home_header") {
                HomeHeader(
                    nickname = uiState.userNickname,
                    isReminderActive = isAnyReminderActive,
                    onOpenBackup = { showBackupSheet = true },
                    onSyncCatalog = { viewModel.syncCatalog(isManual = true) },
                    onNavigateToSettings = onNavigateToSettings,
                    onOpenNotifications = { showNotificationSheet = true },
                    isSyncing = uiState.isSyncing
                )
            }

            // ── Weekly Activity Section ─────────────────────────────────────
            item(key = "home_weekly_activity") {
                WeeklyActivitySection(
                    totalMinutes = weeklyMinutes,
                    sessionCount = weeklySessions,
                    weeklyDeltaMinutes = weeklyDeltaMinutes,
                    targetMinutes = uiState.weeklyTargetMinutes,
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

            // ── Section Label (Koleksi Rutinitas | + Buat Baru) ─────────────
            item(key = "home_routines_header") {
                val isSearchingOrFiltering = uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null || hasActiveFilters
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
                            imageVector = if (isSearchingOrFiltering) Icons.Default.Tune else Icons.Outlined.GridView,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = if (isSearchingOrFiltering)
                                "Hasil Pencarian (${uiState.routines.size})"
                            else
                                "Koleksi Rutinitas (${uiState.routines.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                    }

                    if (!isSearchingOrFiltering) {
                        Surface(
                            onClick = onNavigateToBuilder,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Buat Baru",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
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

        if (showNotificationSheet) {
            NotificationReminderBottomSheet(
                reminders = customReminders,
                spotlightRoutine = uiState.todayScheduledRoutine ?: challengeRoutines.firstOrNull(),
                isTodayRestDay = uiState.isTodayRestDay,
                onToggleReminder = { id, enabled ->
                    viewModel.toggleReminder(context, id, enabled)
                },
                onUpdateReminder = { id, label, h, m, days ->
                    viewModel.updateFullReminder(context, id, label, h, m, days)
                },
                onAddReminder = { label, h, m, days ->
                    viewModel.addCustomReminder(context, label, h, m, days)
                },
                onDeleteReminder = { id ->
                    viewModel.deleteCustomReminder(context, id)
                },
                onStartWorkout = onNavigateToStartWorkout,
                onDismiss = { showNotificationSheet = false }
            )
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    nickname: String = "",
    isReminderActive: Boolean = true,
    onOpenBackup: () -> Unit,
    onSyncCatalog: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    isSyncing: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Section: Circular Profile Avatar with Frosted Glass Ring + Greeting
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Circular Athlete Avatar with Frosted White Glass Ring (Personalized Initial or Icon)
            val initial = nickname.trim().take(1).uppercase()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.50f)), CircleShape)
                    .clickable(onClick = onNavigateToSettings),
                contentAlignment = Alignment.Center
            ) {
                if (initial.isNotBlank()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profil Atlet",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Greeting Texts
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val displayName = if (nickname.isNotBlank()) nickname else "Atlet"
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Halo, ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color.White
                    )
                }
                Text(
                    text = "Siap bergerak hari ini?",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.1.sp
                    ),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }

        // Right Section: Sleek Notification Bell with White Glass Unread Indicator Dot
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenNotifications),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifikasi & Pengingat Latihan",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            // Clean White Frosted Indicator Dot on Top-Right Corner of Bell
            if (isReminderActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(7.5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun WeeklyActivitySection(
    totalMinutes: Int,
    sessionCount: Int,
    weeklyDeltaMinutes: Int = 0,
    targetMinutes: Int = 60,
    onClick: () -> Unit = {}
) {
    val daysRemaining = remember {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2, ..., Saturday = 7
        if (dayOfWeek == Calendar.SUNDAY) 0 else (Calendar.SATURDAY - dayOfWeek + 1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D25)),
        border = BorderStroke(1.dp, Color(0xFF252934))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient soft green lighting radiating from the right edge
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(1100f, 250f),
                            radius = 420f
                        )
                    )
            )

            // Content Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Primary Progress Section (Left)
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_flame_streak),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Minggu Ini",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFA0A5B2)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "$totalMinutes",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "/ $targetMinutes menit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFF9DA3AF),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Horizontal Progress Bar with round pill caps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2B303C))
                    ) {
                        val progressFraction = (totalMinutes / targetMinutes.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (progressFraction > 0f) progressFraction.coerceAtLeast(0.06f) else 0.04f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                }

                // Vertical Divider between Progress and Middle Stats
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(42.dp)
                        .background(Color(0xFF2B303C))
                )

                // 2. Sesi Selesai (Middle Left)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$sessionCount Sesi",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Selesai",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF7E8494)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 3. Hari Tersisa (Middle Right)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFFA0A5B2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$daysRemaining Hari",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Tersisa",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF7E8494)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 4. Trend Sub-Card (Right Elevated Glass Widget)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val deltaLabel = if (weeklyDeltaMinutes >= 0) "+$weeklyDeltaMinutes mnt" else "$weeklyDeltaMinutes mnt"
                        Text(
                            text = deltaLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "dari minggu lalu",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sumber konten untuk Spotlight Tantangan Hari Ini.
 */
enum class ChallengeSource {
    SCHEDULED_TODAY,   // Prioritas 1: Jadwal Latihan Hari Ini (dari Program / Kalender)
    FAVORITE,          // Prioritas 2: HANYA rutinitas yang di-like pengguna
    LATEST_PERFORMED,  // Prioritas 3: Terakhir kali dilakukan (fallback jika belum ada like)
    RECOMMENDED,       // Prioritas 4: Rekomendasi awal katalog (fallback saat app fresh install)
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
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .clickable { onViewDetail(routine.id) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E12)),
        border = BorderStroke(1.dp, Color(0xFF222630).copy(alpha = 0.50f))
    ) {
        val bannerRes = getCategoryBannerRes(routine.category, isSpotlightDefault = true)
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Athlete Image positioned exclusively on the RIGHT HALF (like reference image)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.68f)
                    .align(Alignment.CenterEnd)
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = "Spotlight Workout",
                    contentScale = ContentScale.Crop,
                    alignment = androidx.compose.ui.BiasAlignment(-0.25f, 0f),
                    modifier = Modifier.fillMaxSize()
                )

                // Smooth horizontal fade on the left edge of the photo blending into deep dark card body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0C0E12),
                                    Color(0xFF0C0E12).copy(alpha = 0.65f),
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 260f
                            )
                        )
                )

                // Subtle vertical top and bottom shadow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0C0E12).copy(alpha = 0.40f),
                                    Color.Transparent,
                                    Color(0xFF0C0E12).copy(alpha = 0.60f)
                                )
                            )
                        )
                )
            }

            // 2. Striking Angular Electric Green Geometric Flairs at the Bottom-Left Corner (Exact detail from reference image)
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 90.dp)
                    .align(Alignment.BottomStart)
            ) {
                // Soft glow background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = Offset(0f, 260f),
                                radius = 260f
                            )
                        )
                )

                // Angular geometric slashes matching frosted glass styling
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Primary bold angled stripe
                    val path1 = Path().apply {
                        moveTo(0f, h)
                        lineTo(w * 0.45f, h)
                        lineTo(w * 0.85f, 0f)
                        lineTo(w * 0.40f, 0f)
                        close()
                    }
                    drawPath(
                        path = path1,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            start = Offset(0f, h),
                            end = Offset(w * 0.85f, 0f)
                        )
                    )

                    // Secondary outer accent stripe
                    val path2 = Path().apply {
                        moveTo(w * 0.52f, h)
                        lineTo(w * 0.65f, h)
                        lineTo(w, h * 0.15f)
                        lineTo(w * 0.87f, h * 0.15f)
                        close()
                    }
                    drawPath(
                        path = path2,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent
                            ),
                            start = Offset(w * 0.52f, h),
                            end = Offset(w, h * 0.15f)
                        )
                    )
                }
            }

            // 3. Foreground Content (Pure typography & clear layout)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag Row: Frosted White Glass Capsule Badge (Kiri) & Tombol Like Kaca (Kanan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (badgeIcon, badgeText) = when (challengeSource) {
                        ChallengeSource.SCHEDULED_TODAY -> Icons.Default.EventAvailable to "Jadwal Latihan Hari Ini"
                        ChallengeSource.FAVORITE -> Icons.Filled.Favorite to "Rutinitas Favorit Anda"
                        ChallengeSource.LATEST_PERFORMED -> Icons.Default.History to "Latihan Terakhir Dilakukan"
                        ChallengeSource.RECOMMENDED -> Icons.Default.Bolt to "Rekomendasi Latihan"
                        ChallengeSource.NONE -> Icons.Default.FitnessCenter to "Latihan Pilihan"
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Tombol Favorit / Like Melayang di Sudut Kanan Atas Spotlight Card
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteClick()
                        },
                        shape = CircleShape,
                        color = if (routine.isFavorite) Color(0xFF261318).copy(alpha = 0.88f) else Color(0xFF0F1116).copy(alpha = 0.60f),
                        border = BorderStroke(
                            1.dp,
                            if (routine.isFavorite) Color(0xFFFF453A).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (routine.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (routine.isFavorite) "Disukai" else "Sukai",
                                tint = if (routine.isFavorite) Color(0xFFFF453A) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Middle & Bottom Section (Text on the left, Button on bottom left, Dots on bottom right)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title & Clean Inline Metadata
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatDuration(routine.totalDurationSec),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "|",
                                color = Color(0xFF555966),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = routine.category,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Bottom Row: CTA Button (Left) & Circular Pagination Dots (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Workout Solid White Pill Button (Chunky & Bold)
                        Button(
                            onClick = { onStartWorkout(routine.id) },
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mulai Latihan",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = Color.Black
                            )
                        }

                        // Circular Pagination Dots (Active = Neon Green, Inactive = Muted Grey)
                        if (pageCount > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
                            ) {
                                for (i in 0 until pageCount) {
                                    val isSelected = i == currentPage
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 7.5.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
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
        clean.contains("lower") -> CategoryIconSource.Vector(Icons.AutoMirrored.Filled.DirectionsRun)
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
                    SortItemData(SortOption.FAVORITE, "Favorit Teratas", "Rutinitas yang Anda sukai", Icons.Filled.Favorite),
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
    val haptic = LocalHapticFeedback.current
    val accentColor = try {
        Color(android.graphics.Color.parseColor(routine.accentColor))
    } catch (e: Exception) {
        Color.White
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
                .height(124.dp)
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

            // 3. Sisi Kiri: Badge Kategori, Judul Rutinitas, Metadata
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.66f)
                    .padding(start = 18.dp, top = 14.dp, bottom = 14.dp),
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

            // 4. Dedicated Right Action Column: Favorite (Top-Right) & Play (Bottom-Right)
            // Dipisahkan secara vertikal sehingga tidak pernah bertabrakan atau numpuk
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 10.dp, bottom = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Tombol Favorit / Like di pojok kanan atas kartu
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFavoriteClick()
                    },
                    shape = CircleShape,
                    color = if (routine.isFavorite) Color(0xFF261318).copy(alpha = 0.90f) else Color(0xFF0F1116).copy(alpha = 0.65f),
                    border = BorderStroke(
                        1.dp,
                        if (routine.isFavorite) Color(0xFFFF453A).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (routine.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (routine.isFavorite) "Disukai" else "Sukai",
                            tint = if (routine.isFavorite) Color(0xFFFF453A) else Color.White.copy(alpha = 0.80f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Tombol Play / Quick Start di pojok kanan bawah kartu (Solid White pill-circle, high-contrast CTA)
                Surface(
                    onClick = onStartClick,
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(38.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mulai Latihan",
                            tint = Color.Black,
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
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = "Belum Ada Rutinitas",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Mulai dengan 40 preset standar ilmiah dunia atau racik rutinitas kustommu sendiri",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.60f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onRestoreStandard,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat 40 Preset Standar Ilmiah", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
        }
        OutlinedButton(
            onClick = onCreateNew,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buat Rutinitas Kustom", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
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

// ── Bottom Navigation Bar (Matched to Reference Screenshot) ─────────────────

enum class HomeTab {
    BERANDA,
    LATIHAN,
    PROGRESS,
    PROFIL
}

@Composable
fun HomeBottomNavigation(
    currentTab: HomeTab = HomeTab.BERANDA,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFF0F1217).copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNavItem(
                icon = Icons.Filled.Home,
                label = "Beranda",
                isSelected = currentTab == HomeTab.BERANDA,
                onClick = { onTabSelected(HomeTab.BERANDA) },
                modifier = Modifier.weight(1f)
            )

            HomeNavItem(
                icon = Icons.Outlined.FitnessCenter,
                label = "Latihan",
                isSelected = currentTab == HomeTab.LATIHAN,
                onClick = { onTabSelected(HomeTab.LATIHAN) },
                modifier = Modifier.weight(1f)
            )

            HomeNavItem(
                icon = Icons.Outlined.BarChart,
                label = "Progress",
                isSelected = currentTab == HomeTab.PROGRESS,
                onClick = { onTabSelected(HomeTab.PROGRESS) },
                modifier = Modifier.weight(1f)
            )

            HomeNavItem(
                icon = Icons.Outlined.Person,
                label = "Profil",
                isSelected = currentTab == HomeTab.PROFIL,
                onClick = { onTabSelected(HomeTab.PROFIL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.45f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(23.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            color = if (isSelected) activeColor else inactiveColor
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Horizontal pill indicator below active item
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.White)
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

// ── Notification & Multi-Reminder Modal Bottom Sheet ────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationReminderBottomSheet(
    reminders: List<WorkoutReminderItem>,
    spotlightRoutine: WorkoutRoutine?,
    isTodayRestDay: Boolean = false,
    onToggleReminder: (id: String, Boolean) -> Unit,
    onUpdateReminder: (id: String, label: String, hour: Int, minute: Int, days: List<Int>) -> Unit,
    onAddReminder: (label: String, hour: Int, minute: Int, days: List<Int>) -> Unit,
    onDeleteReminder: (id: String) -> Unit,
    onStartWorkout: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<WorkoutReminderItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF13151B),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3E4452))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Pengingat Latihan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${reminders.count { it.isEnabled }} dari ${reminders.size} pengingat aktif",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF8E95A5)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Reminders List (Ultra-clean Single-row Frosted Glass Cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reminders.forEach { reminder ->
                    FrostedReminderCard(
                        reminder = reminder,
                        onToggle = { onToggleReminder(reminder.id, it) },
                        onClick = { editingReminder = reminder }
                    )
                }
            }

            // Button: Tambah Pengingat Baru (+)
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tambah Pengingat Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color.White
                )
            }

            // Card 2: Quick Action / Active Routine Preview (if available)
            if (spotlightRoutine != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21)),
                    border = BorderStroke(1.dp, Color(0xFF262A36))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Target Latihan Hari Ini",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = spotlightRoutine.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onStartWorkout(spotlightRoutine.id)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Mulai",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            } else if (isTodayRestDay) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21)),
                    border = BorderStroke(1.dp, Color(0xFF262A36))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🧘", fontSize = 22.sp)
                        Column {
                            Text(
                                text = "Hari Pemulihan (Rest Day)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Fokus pada peregangan ringan, tidur cukup, dan hidrasi optimal.",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }

            // Footer Notice
            Text(
                text = "💡 Alarm pengingat berjalan secara offline di perangkat Anda dan tetap berbunyi tepat waktu meskipun aplikasi sedang ditutup.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }

    // Dialog Edit Pengingat (Multi-Reminder)
    editingReminder?.let { reminder ->
        EditReminderDialog(
            reminder = reminder,
            canDelete = reminders.size > 1,
            onDismiss = { editingReminder = null },
            onConfirm = { label, h, m, days ->
                onUpdateReminder(reminder.id, label, h, m, days)
                editingReminder = null
            },
            onDelete = {
                onDeleteReminder(reminder.id)
                editingReminder = null
            }
        )
    }

    // Dialog Tambah Pengingat Baru
    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, h, m, days ->
                onAddReminder(label, h, m, days)
                showAddDialog = false
            }
        )
    }
}
