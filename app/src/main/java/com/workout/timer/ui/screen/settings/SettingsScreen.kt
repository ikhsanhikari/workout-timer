package com.workout.timer.ui.screen.settings

import android.Manifest
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.data.model.WorkoutReminderItem
import com.workout.timer.ui.component.AddReminderDialog
import com.workout.timer.ui.component.EditReminderDialog
import com.workout.timer.ui.component.FrostedReminderCard
import com.workout.timer.ui.screen.home.HomeBottomNavigation
import com.workout.timer.ui.screen.home.HomeTab
import com.workout.timer.util.DeviceReadinessManager
import java.util.Calendar
import java.util.Locale

// ────────────────────────────────────────────────────────────────────────────
// Pure Frosted Glass Theme Constants
// ────────────────────────────────────────────────────────────────────────────

private val TextMuted = Color(0xFF8E95A5)
private val GlassBackground = Color(0xFF101217)
private val GlassBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.03f)
    )
)
private val GlassBorderSelected = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.45f),
        Color.White.copy(alpha = 0.15f)
    )
)

/**
 * SettingsScreen — Layar Pengaturan & Profil (Streamlined Accordion Edition).
 *
 * Pembaruan esensial:
 * 1. Seluruh accordion DEFAULT TERTUTUP (clean & tidak intimidating saat pertama dibuka).
 * 2. Hanya menyisakan 4 kategori pengaturan yang benar-benar esensial dan fungsional:
 *    - 👤 Profil & Target Kebugaran
 *    - ⏰ Pengingat Latihan (Sinkron dengan Beranda, multi-reminder)
 *    - 🔊 Audio & Tampilan Latihan (TTS, BGM, Beep, Haptic, Video)
 *    - 💾 Cadangan & Kesiapan Sistem (Ekspor/Impor ZIP, Izin Notifikasi & Baterai)
 * 3. Menghilangkan seluruh teks deskripsi panjang dan elemen fluff yang tidak terpakai.
 * 4. Dilengkapi ikon-ikon hidup dan bottom bar persisten.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToWorkoutPlan: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reminders by viewModel.customReminders.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(it) }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<WorkoutReminderItem?>(null) }
    var tempNickname by remember { mutableStateOf("") }

    // DEFAULT: SEMUA ACCORDION TERTUTUP (emptySet)
    var openSections by remember { mutableStateOf(emptySet<String>()) }

    fun toggleSection(sectionKey: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        openSections = if (openSections.contains(sectionKey)) {
            openSections - sectionKey
        } else {
            openSections + sectionKey
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isNotificationGranted by remember {
        mutableStateOf(DeviceReadinessManager.isNotificationPermissionGranted(context))
    }
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(DeviceReadinessManager.isBatteryOptimizationIgnored(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationGranted = DeviceReadinessManager.isNotificationPermissionGranted(context)
                isBatteryOptimizationIgnored = DeviceReadinessManager.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF07080B),
        bottomBar = {
            HomeBottomNavigation(
                currentTab = HomeTab.PROFIL,
                onTabSelected = { tab ->
                    when (tab) {
                        HomeTab.BERANDA -> onNavigateToHome()
                        HomeTab.LATIHAN -> onNavigateToWorkoutPlan()
                        HomeTab.PROGRESS -> onNavigateToHistory()
                        HomeTab.PROFIL -> { /* Sedang di Profil */ }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Top Header ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pengaturan & Profil",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Preferensi mandiri & kustomisasi latihan",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }

                    // Settings badge icon
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── ACCORDION 1: PROFIL & TARGET KEBUGARAN ────────────────────
                SettingsAccordionCard(
                    icon = Icons.Default.Person,
                    title = "Profil & Target Kebugaran",
                    badgeText = prefs.nickname.ifBlank { "Atlet" },
                    isExpanded = openSections.contains("profile"),
                    onToggle = { toggleSection("profile") }
                ) {
                    // Profile Info & Edit Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_flame_streak),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prefs.nickname.ifBlank { "Atlet" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${prefs.primaryGoal} • ${prefs.experienceLevel}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = TextMuted
                            )
                        }

                        Surface(
                            onClick = {
                                tempNickname = prefs.nickname
                                showEditNameDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ubah Nama",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text("Ubah", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Kebugaran Selector
                    Text("Target Utama", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Kebugaran Umum", "Bakar Lemak", "Bentuk Otot").forEach { goal ->
                            val isSelected = prefs.primaryGoal.equals(goal, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updatePrimaryGoal(goal) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = goal,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tingkat Pengalaman Selector
                    Text("Tingkat Pengalaman", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Pemula", "Menengah", "Mahir").forEach { level ->
                            val isSelected = prefs.experienceLevel.equals(level, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateExperienceLevel(level) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = level,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── ACCORDION 2: PENGINGAT LATIHAN (MULTI-REMINDER) ───────────
                val activeRemindersCount = reminders.count { it.isEnabled }
                SettingsAccordionCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "Pengingat Latihan",
                    badgeText = "$activeRemindersCount Aktif",
                    isExpanded = openSections.contains("reminders"),
                    onToggle = { toggleSection("reminders") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Reminders List (Ultra-clean Single-row Frosted Glass Card)
                        reminders.forEach { reminder ->
                            FrostedReminderCard(
                                reminder = reminder,
                                onToggle = { isEnabled ->
                                    if (isEnabled && !isNotificationGranted) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            DeviceReadinessManager.openNotificationSettings(context)
                                        }
                                    }
                                    viewModel.toggleReminder(context, reminder.id, isEnabled)
                                },
                                onClick = {
                                    editingReminder = reminder
                                }
                            )
                        }

                        // Action Buttons: + Tambah & Tes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { showAddReminderDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah Pengingat", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Surface(
                                onClick = {
                                    if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.testReminderNotification(context)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Uji Notif", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // ── ACCORDION 3: AUDIO & TAMPILAN LATIHAN ─────────────────────
                val prefActiveCount = listOf(
                    prefs.voiceCoachEnabled,
                    prefs.bgmEnabled,
                    prefs.beepEnabled,
                    prefs.hapticEnabled,
                    prefs.videoEnabled
                ).count { it }

                SettingsAccordionCard(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Audio & Tampilan Latihan",
                    badgeText = "$prefActiveCount/5 Aktif",
                    isExpanded = openSections.contains("preferences"),
                    onToggle = { toggleSection("preferences") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsToggleRowSimple(
                            icon = Icons.Default.RecordVoiceOver,
                            title = "Pelatih Suara (TTS)",
                            checked = prefs.voiceCoachEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setVoiceCoachEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        SettingsToggleRowSimple(
                            icon = Icons.Default.MusicNote,
                            title = "Musik Latar (BGM)",
                            checked = prefs.bgmEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setBgmEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        SettingsToggleRowSimple(
                            icon = Icons.Default.Timer,
                            title = "Nada Hitung Mundur (Beep)",
                            checked = prefs.beepEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setBeepEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        SettingsToggleRowSimple(
                            icon = Icons.Default.Vibration,
                            title = "Getaran Haptic Feedback",
                            checked = prefs.hapticEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setHapticEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        SettingsToggleRowSimple(
                            icon = Icons.Default.Videocam,
                            title = "Video Gerakan Latihan",
                            checked = prefs.videoEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setVideoEnabled(it)
                            }
                        )
                    }
                }

                // ── ACCORDION 4: CADANGAN & KESIAPAN SISTEM ──────────────────
                val systemReady = isNotificationGranted && isBatteryOptimizationIgnored
                SettingsAccordionCard(
                    icon = Icons.Default.FolderZip,
                    title = "Cadangan & Kesiapan Sistem",
                    badgeText = if (systemReady) "Optimal ✓" else "Perlu Izin",
                    isExpanded = openSections.contains("system_backup"),
                    onToggle = { toggleSection("system_backup") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Kesiapan Sistem
                        SettingsActionTileSimple(
                            icon = Icons.Default.Notifications,
                            title = "Izin Notifikasi Layar Kunci",
                            actionLabel = if (isNotificationGranted) "Aktif ✓" else "Izinkan",
                            onClick = {
                                if (!isNotificationGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        DeviceReadinessManager.openNotificationSettings(context)
                                    }
                                } else {
                                    DeviceReadinessManager.openNotificationSettings(context)
                                }
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        SettingsActionTileSimple(
                            icon = Icons.Default.BatteryChargingFull,
                            title = "Proteksi Pembekuan Baterai",
                            actionLabel = if (isBatteryOptimizationIgnored) "Aman ✓" else "Bebaskan",
                            onClick = {
                                DeviceReadinessManager.requestIgnoreBatteryOptimization(context)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // Cadangan Data
                        SettingsActionTileSimple(
                            icon = Icons.Default.UploadFile,
                            title = "Ekspor Cadangan (.zip)",
                            actionLabel = if (uiState.isExporting) "..." else "Ekspor",
                            isLoading = uiState.isExporting,
                            onClick = {
                                createDocumentLauncher.launch("workout_timer_backup_${System.currentTimeMillis()}.zip")
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        SettingsActionTileSimple(
                            icon = Icons.Default.Download,
                            title = "Pulihkan Cadangan (.zip)",
                            actionLabel = if (uiState.isImporting) "..." else "Pulihkan",
                            isLoading = uiState.isImporting,
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer (Dynamic Version)
                val appVersion = remember {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        "v${pInfo.versionName}"
                    } catch (e: Exception) {
                        "v1.0.0"
                    }
                }
                Text(
                    text = "Workout Timer $appVersion • Offline First",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // ── Dialog Ubah Nama Panggilan ───────────────────────────────────────
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                containerColor = Color(0xFF14171E),
                shape = RoundedCornerShape(18.dp),
                title = {
                    Text(
                        text = "Ubah Nama Panggilan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                },
                text = {
                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { tempNickname = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Surface(
                        onClick = {
                            viewModel.updateNickname(tempNickname)
                            showEditNameDialog = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White
                    ) {
                        Text(
                            text = "Simpan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Batal", color = TextMuted)
                    }
                }
            )
        }

        // ── Dialog Edit Pengingat (Multi-Reminder) ───────────────────────────
        editingReminder?.let { reminder ->
            EditReminderDialog(
                reminder = reminder,
                canDelete = reminders.size > 1,
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

        // ── Dialog Tambah Pengingat Baru (Multi-Reminder) ────────────────────
        if (showAddReminderDialog) {
            AddReminderDialog(
                onDismiss = { showAddReminderDialog = false },
                onConfirm = { label, hour, minute, days ->
                    viewModel.addCustomReminder(context, label, hour, minute, days)
                    showAddReminderDialog = false
                }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Reusable Accordion Card Container
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsAccordionCard(
    icon: ImageVector,
    title: String,
    badgeText: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GlassBackground,
        border = BorderStroke(1.dp, if (isExpanded) GlassBorderSelected else GlassBorderBrush),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Accordion Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Leading Icon Container
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                // Optional Status / Summary Badge
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Expand/Collapse Chevron
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Tutup" else "Buka",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(19.dp)
                        .rotate(rotation)
                )
            }

            // Animated Collapsible Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    content = content
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Streamlined Single-Line Setting Rows (No Verbose Subtitles)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRowSimple(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White.copy(alpha = 0.40f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.height(22.dp)
        )
    }
}

@Composable
private fun SettingsActionTileSimple(
    icon: ImageVector,
    title: String,
    actionLabel: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Surface(
            shape = RoundedCornerShape(9.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            modifier = Modifier.clickable(enabled = !isLoading, onClick = onClick)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}
