package com.workout.timer.ui.screen.plan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.data.model.WorkoutReminderItem
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.ui.component.AddReminderDialog
import com.workout.timer.ui.component.EditReminderDialog
import com.workout.timer.ui.screen.home.HomeBottomNavigation
import com.workout.timer.ui.screen.home.HomeTab
import com.workout.timer.ui.theme.BackgroundPrimary

private val TextMuted = Color(0xFF8E95A5)

// Standardized Pure Frosted Glass styling matching the application theme
private val GlassBackground = Color(0xFF0F1217).copy(alpha = 0.85f)
private val GlassBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.03f)
    )
)
private val GlassBorderSelected = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.55f),
        Color.White.copy(alpha = 0.20f)
    )
)

/**
 * Layar Hub Latihan & Program Mingguan (Tab "Latihan") — Glassmorphism Edition.
 *
 * Mengusung estetika Glassmorphism murni:
 * - Card semi-transparan dengan border frosted glass sheen
 * - List Workout harian kini menampilkan thumbnail gambar visual atletik
 * - Struktur fokus & action-oriented: Latihan Hari Ini + Strip 7 Hari + Ringkasan Mingguan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToBuilder: () -> Unit,
    onNavigateToRoutineDetail: (Long) -> Unit,
    onNavigateToStartWorkout: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: WorkoutPlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var dayToCustomize by remember { mutableStateOf<DayPlanUiModel?>(null) }
    var editingReminder by remember { mutableStateOf<WorkoutReminderItem?>(null) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showReminderBottomSheet by remember { mutableStateOf(false) }

    val isNotificationGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* User response handled */ }

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
                currentTab = HomeTab.LATIHAN,
                onTabSelected = { tab ->
                    when (tab) {
                        HomeTab.BERANDA -> onNavigateToHome()
                        HomeTab.LATIHAN -> { /* Sedang di Latihan */ }
                        HomeTab.PROGRESS -> onNavigateToHistory()
                        HomeTab.PROFIL -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // ── 1. Header & Program Switcher ────────────────────────────────
            item(key = "section_header") {
                PlanHeader(
                    activeProgramName = uiState.activeProgramName,
                    activeReminderCount = uiState.activeRemindersCount,
                    reminderSummary = uiState.reminderSummary,
                    onOpenReminder = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showReminderBottomSheet = true
                    }
                )
            }

            item(key = "section_preset_chips") {
                PresetProgramRow(
                    activeProgramName = uiState.activeProgramName,
                    onSelectPreset = { presetId ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyPreset(presetId)
                    }
                )
            }

            // ── 2. Featured Focus Card: "LATIHAN HARI INI" ──────────────────
            item(key = "section_focus_card") {
                val selectedDay = uiState.selectedDayPlan
                if (selectedDay != null) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        if (selectedDay.isRestDay || selectedDay.routine == null) {
                            RestDayFocusCard(
                                day = selectedDay
                            )
                        } else {
                            WorkoutDayFocusCard(
                                day = selectedDay,
                                routine = selectedDay.routine,
                                onStartWorkout = { onNavigateToStartWorkout(selectedDay.routine.id) },
                                onViewDetail = { onNavigateToRoutineDetail(selectedDay.routine.id) },
                                onChangeRoutine = { dayToCustomize = selectedDay }
                            )
                        }
                    }
                }
            }

            // ── 3. Strip Kalender Mingguan 7 Hari (SEN - MIN) ───────────────
            item(key = "section_week_strip") {
                val workoutDays = uiState.days.count { !it.isRestDay && it.routine != null }
                val totalMins = uiState.days.mapNotNull { it.routine }.sumOf { it.totalDurationSec } / 60

                WeeklySchedulePlanCard(
                    workoutDays = workoutDays,
                    totalMinutes = totalMins,
                    days = uiState.days,
                    onSelectDay = { dayOfWeek ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectDay(dayOfWeek)
                    }
                )
            }

            // ── 4. Ringkasan Mingguan Lengkap (Dengan Gambar Thumbnail) ─────
            item(key = "section_overview_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ringkasan Mingguan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Ketuk ⇄ untuk menukar",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextMuted
                    )
                }
            }

            items(uiState.days, key = { "day_row_${it.dayOfWeek}" }) { day ->
                DayScheduleRowItem(
                    day = day,
                    isSelected = day.isSelected,
                    onRowClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectDay(day.dayOfWeek)
                    },
                    onChangeRoutine = {
                        dayToCustomize = day
                    }
                )
            }
        }
    }

    // ── Routine Picker Modal Bottom Sheet ───────────────────────────────────
    dayToCustomize?.let { targetDay ->
        ScheduleRoutinePickerBottomSheet(
            day = targetDay,
            routines = uiState.allRoutines,
            onDismiss = { dayToCustomize = null },
            onSelectRoutine = { routineId ->
                viewModel.assignRoutine(targetDay.dayOfWeek, routineId, context)
                dayToCustomize = null
            },
            onSetRestDay = {
                viewModel.assignRoutine(targetDay.dayOfWeek, null, context)
                dayToCustomize = null
            },
            onCreateNewRoutine = {
                dayToCustomize = null
                onNavigateToBuilder()
            }
        )
    }

    // ── Edit Reminder Dialog (Frosted Glass Modal) ──────────────────────────
    editingReminder?.let { reminder ->
        EditReminderDialog(
            reminder = reminder,
            canDelete = uiState.customReminders.size > 1,
            onDismiss = { editingReminder = null },
            onConfirm = { label, hour, minute, days ->
                viewModel.updateFullReminder(context, reminder.id, label, hour, minute, days)
                editingReminder = null
            },
            onDelete = {
                viewModel.deleteCustomReminder(context, reminder.id)
                editingReminder = null
            }
        )
    }

    // ── Add Reminder Dialog (Frosted Glass Modal) ───────────────────────────
    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { label, hour, minute, days ->
                viewModel.addCustomReminder(context, label, hour, minute, days)
                showAddReminderDialog = false
            }
        )
    }

    // ── Schedule Reminder Bottom Sheet (Frosted Glass Sheet) ────────────────
    if (showReminderBottomSheet) {
        ScheduleReminderBottomSheet(
            activeCount = uiState.activeRemindersCount,
            reminderSummary = uiState.reminderSummary,
            customReminders = uiState.customReminders,
            onDismiss = { showReminderBottomSheet = false },
            onTestClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.testReminderNotification(context)
            },
            onToggleEnable = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (uiState.customReminders.isNotEmpty()) {
                    viewModel.toggleReminder(context, enabled = uiState.activeRemindersCount == 0)
                } else {
                    showAddReminderDialog = true
                }
            },
            onEditReminder = { reminder ->
                showReminderBottomSheet = false
                editingReminder = reminder
            },
            onAddReminder = {
                showReminderBottomSheet = false
                showAddReminderDialog = true
            }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 3.5. Schedule Reminder Bottom Sheet (Pure Frosted Glass Modal)
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleReminderBottomSheet(
    activeCount: Int,
    reminderSummary: String,
    customReminders: List<WorkoutReminderItem>,
    onDismiss: () -> Unit,
    onTestClick: () -> Unit,
    onToggleEnable: () -> Unit,
    onEditReminder: (WorkoutReminderItem) -> Unit,
    onAddReminder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Header: Title & Main Toggle Switch
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (activeCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Pengingat Latihan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (activeCount > 0) reminderSummary else "Alarm notifikasi dinonaktifkan",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }
                }

                // Switch Toggle
                Switch(
                    checked = activeCount > 0,
                    onCheckedChange = { onToggleEnable() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // Reminders List or Quick Configure
            if (activeCount > 0 && customReminders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customReminders.forEach { item ->
                        Surface(
                            onClick = { onEditReminder(item) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "%02d:%02d WIB".format(item.hour, item.minute),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                                ) {
                                    Text(
                                        text = "Ubah Jam",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Uji Notifikasi & Selesai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onTestClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uji Notifikasi",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selesai",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 1. Header & Program Switcher (Pure Minimalist Glass)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlanHeader(
    activeProgramName: String,
    activeReminderCount: Int,
    reminderSummary: String,
    onOpenReminder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Jadwal & Program",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.70f))
                )
                Text(
                    text = activeProgramName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Top-Right Frosted Glass Reminder Orb / Capsule
        Surface(
            onClick = onOpenReminder,
            shape = RoundedCornerShape(50),
            color = if (activeReminderCount > 0) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
            border = BorderStroke(
                1.dp,
                if (activeReminderCount > 0) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f)
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (activeReminderCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                    contentDescription = "Pengingat Latihan",
                    tint = if (activeReminderCount > 0) Color.White else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp)
                )
                if (activeReminderCount > 0) {
                    val displayTime = reminderSummary.substringBefore(" •").substringBefore(" WIB").trim()
                    Text(
                        text = if (displayTime.isNotBlank() && displayTime != "Pengingat Latihan") displayTime else "Aktif",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetProgramRow(
    activeProgramName: String,
    onSelectPreset: (String) -> Unit
) {
    val presets = remember {
        listOf(
            Triple("4day", "Athletic Split 4 Hari", "⚡ 4 Hari Split"),
            Triple("3day", "Full Body 3 Hari", "💪 3 Hari Full"),
            Triple("daily", "Daily Athlete 7 Hari", "🔥 7 Hari Aktif")
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { (id, fullName, label) ->
            val isActive = activeProgramName.equals(fullName, ignoreCase = true)
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectPreset(id) },
                shape = RoundedCornerShape(18.dp),
                color = if (isActive) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(
                    1.dp,
                    if (isActive) GlassBorderSelected else GlassBorderBrush
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        ),
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 2. Featured Focus Card: "LATIHAN HARI INI" (Glassmorphism)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutDayFocusCard(
    day: DayPlanUiModel,
    routine: WorkoutRoutine,
    onStartWorkout: () -> Unit,
    onViewDetail: () -> Unit,
    onChangeRoutine: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val bannerRes = resolveHeroBanner(routine.category)
    val durationMinutes = (routine.totalDurationSec / 60).coerceAtLeast(1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Column {
            // Hero Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = routine.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent,
                                    Color(0xFF0F1217)
                                )
                            )
                        )
                )

                // Top Badge: "⚡ LATIHAN HARI INI" or "📅 JADWAL [HARI]"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (day.isToday) Color.White else Color.Black.copy(alpha = 0.70f),
                        border = BorderStroke(
                            1.dp,
                            if (day.isToday) Color.White else Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = if (day.isToday) "⚡ LATIHAN HARI INI" else "📅 JADWAL ${day.dayNameFull.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (day.isToday) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    if (day.isCompletedThisWeek) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Selesai Minggu Ini",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Body Info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata chips: Duration, Category, Exercise count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlanMetaChip(
                        icon = Icons.Outlined.Timer,
                        label = "$durationMinutes Menit"
                    )
                    PlanMetaChip(
                        icon = Icons.Default.FitnessCenter,
                        label = routine.category
                    )
                    PlanMetaChip(
                        icon = Icons.Outlined.Info,
                        label = "${routine.totalExercises} Gerakan"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: Mulai Latihan & Ganti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Button (Clean Crisp White - Highest Contrast)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartWorkout()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mulai Latihan",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        )
                    }

                    // Swap / Change Button (Frosted Glass)
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onChangeRoutine()
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Ganti",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ganti",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestDayFocusCard(
    day: DayPlanUiModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Production-Grade Rest & Recovery Vector Artwork
            Image(
                painter = painterResource(id = R.drawable.ic_rest_recovery),
                contentDescription = "Rest Day Illustration",
                modifier = Modifier.size(108.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Micro Pill: Status Hari
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Text(
                    text = if (day.isToday) "HARI INI • ISTIRAHAT" else "REST DAY • ${day.dayNameFull.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.5.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = "Hari Pemulihan",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Micro-copy (1 baris ringkas)
            Text(
                text = "Waktunya regenerasi energi untuk performa esok hari.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlanMetaChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 3. Card Rencana Latihan Minggu Ini (Standard Pure White Frosted Glass)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklySchedulePlanCard(
    workoutDays: Int,
    totalMinutes: Int,
    days: List<DayPlanUiModel>,
    onSelectDay: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101217)),
        border = BorderStroke(1.dp, GlassBorderBrush)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient soft pure white lighting in top-left corner (Standard Frosted Glass - NO NEON)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.045f),
                                Color.Transparent
                            ),
                            center = Offset(70f, 70f),
                            radius = 340f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Calendar Icon Squircle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // 2. Title & Target Subtitle
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rencana Latihan Minggu Ini",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$workoutDays hari latihan • Target: $totalMinutes menit",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7 Days Mini Strip Row (Sen - Min)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    days.forEach { day ->
                        val isSelected = day.isSelected
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelectDay(day.dayOfWeek) }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Day Name (Sen, Sel, Rab, Kam, Jum, Sab, Min)
                            Text(
                                text = day.dayNameShort,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (day.isToday) FontWeight.ExtraBold else if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (day.isToday || isSelected) Color.White else Color.White.copy(alpha = 0.50f)
                            )

                            Spacer(modifier = Modifier.height(7.dp))

                            // Status Indicator Icon (✔ Selesai, ○ Jadwal, ● Istirahat)
                            Box(
                                modifier = Modifier.size(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    day.isCompletedThisWeek -> {
                                        // Completed: Pure White solid circle with black checkmark (NO NEON)
                                        Box(
                                            modifier = Modifier
                                                .size(17.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selesai",
                                                tint = Color.Black,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                    day.isRestDay -> {
                                        // Rest Day: Muted dot
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.25f))
                                        )
                                    }
                                    else -> {
                                        // Scheduled Workout Day: Hollow ring
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color.White.copy(alpha = 0.38f), CircleShape)
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
}

// ────────────────────────────────────────────────────────────────────────────
// 4. Ringkasan Mingguan Lengkap (DENGAN GAMBAR THUMBNAIL & GLASSMORPHISM)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun DayScheduleRowItem(
    day: DayPlanUiModel,
    isSelected: Boolean,
    onRowClick: () -> Unit,
    onChangeRoutine: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isWorkout = !day.isRestDay && day.routine != null
    val thumbnailRes = if (isWorkout) resolveHeroBanner(day.routine!!.category) else R.drawable.banner_spotlight

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onRowClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.12f) else GlassBackground,
        border = BorderStroke(
            1.dp,
            if (isSelected) GlassBorderSelected else GlassBorderBrush
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // ── GAMBAR THUMBNAIL LATIHAN ──────────────────────────────
                Box(
                    modifier = Modifier
                        .size(width = 82.dp, height = 66.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF14171E))
                ) {
                    if (isWorkout) {
                        Image(
                            painter = painterResource(id = thumbnailRes),
                            contentDescription = day.routine?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Subtle dark gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                    } else {
                        // Rest Day Calm Thumbnail (Pure Frosted Glass)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.08f),
                                            Color.White.copy(alpha = 0.03f)
                                        )
                                    )
                                )
                                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Day Badge Overlay (Top-Left of Thumbnail)
                    Surface(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(6.dp),
                        color = if (day.isToday) Color.White else Color.Black.copy(alpha = 0.70f),
                        border = BorderStroke(
                            0.5.dp,
                            if (day.isToday) Color.White else Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = day.dayNameShort.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = if (day.isToday) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Completed Checkmark Overlay (Bottom-Right of Thumbnail)
                    if (day.isCompletedThisWeek) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selesai",
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Routine Title & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isWorkout) day.routine!!.name else "Hari Istirahat (Rest Day)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = if (day.isRestDay) TextMuted else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    if (isWorkout) {
                        val durationMinutes = (day.routine!!.totalDurationSec / 60).coerceAtLeast(1)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f))
                            ) {
                                Text(
                                    text = day.routine!!.category,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "• $durationMinutes mnt",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = TextMuted
                            )
                        }
                    } else {
                        Text(
                            text = "Pemulihan & Regenerasi Otot",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Frosted Glass Swap Button ⇄
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChangeRoutine()
                    },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Ganti Jadwal",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 5. Routine Picker Modal Bottom Sheet (DENGAN GAMBAR THUMBNAIL)
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleRoutinePickerBottomSheet(
    day: DayPlanUiModel,
    routines: List<WorkoutRoutine>,
    onDismiss: () -> Unit,
    onSelectRoutine: (Long) -> Unit,
    onSetRestDay: () -> Unit,
    onCreateNewRoutine: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111317),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Atur Latihan: ${day.dayNameFull}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color.White
            )

            Text(
                text = "Pilih rutinitas atau tetapkan sebagai hari istirahat",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Rest Day (Hari Istirahat)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetRestDay()
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (day.isRestDay) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(
                    1.dp,
                    if (day.isRestDay) GlassBorderSelected else GlassBorderBrush
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hari Istirahat (Rest Day) 🛌",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Bebas latihan, fokus pemulihan fisik & otot",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextMuted
                        )
                    }
                    if (day.isRestDay) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Katalog Rutinitas (${routines.size})",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Routines list with THUMBNAIL IMAGE
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    val isCurrentRoutine = day.routine?.id == routine.id
                    val thumbRes = resolveHeroBanner(routine.category)
                    val durationMins = (routine.totalDurationSec / 60).coerceAtLeast(1)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectRoutine(routine.id)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrentRoutine) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrentRoutine) GlassBorderSelected else GlassBorderBrush
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Thumbnail Gambar
                            Box(
                                modifier = Modifier
                                    .size(width = 62.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = thumbRes),
                                    contentDescription = routine.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routine.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${routine.category} • $durationMins mnt • ${routine.totalExercises} gerakan",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = TextMuted
                                )
                            }
                            if (isCurrentRoutine) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terpilih",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Create new routine action
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCreateNewRoutine()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Buat Rutinitas Baru",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Helpers
// ────────────────────────────────────────────────────────────────────────────

private fun resolveHeroBanner(category: String): Int {
    return when (category.lowercase()) {
        "full body" -> R.drawable.hero_fullbody
        "hiit" -> R.drawable.hero_hiit
        "core" -> R.drawable.hero_core
        "strength" -> R.drawable.hero_strength
        "mobility" -> R.drawable.hero_mobility
        "cardio" -> R.drawable.hero_cardio
        else -> R.drawable.banner_spotlight
    }
}
