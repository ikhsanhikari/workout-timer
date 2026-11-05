package com.workout.timer.ui.screen.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.ui.screen.onboarding.ThemeAccent
import com.workout.timer.ui.theme.*
import com.workout.timer.util.DeviceReadinessManager
import com.workout.timer.util.WorkoutReminderManager

/**
 * SettingsScreen — Layar Pengaturan & Preferensi Terpusat (Standard Frosted Glass).
 *
 * Mengikuti pedoman desain AMOLED Pure Black (#07080B), Pure White Frosted Glass,
 * dan tipografi minimalis tanpa elemen warna-warni yang mengganggu fokus.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    var tempNickname by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        containerColor = Color(0xFF07080B)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ambient Radial Background Glow Canvas (Subtle Pure White Frosted Depth)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.035f),
                            Color.Transparent
                        ),
                        center = Offset(canvasW * 0.5f, canvasH * 0.10f),
                        radius = canvasW * 0.85f
                    ),
                    radius = canvasW * 0.85f,
                    center = Offset(canvasW * 0.5f, canvasH * 0.10f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 44.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Top Header with Back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "PENGATURAN",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "Kustomisasi Pengalaman Latihan Mandiri",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        )
                    }
                }

                // ── SECTION 1: PROFIL & IDENTITAS VISUAL ─────────────────────
                SettingsSectionTitle("PROFIL & TEMA VISUAL")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar + Name + Edit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flame_streak),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prefs.nickname.ifBlank { "Atlet" },
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Target: ${prefs.primaryGoal} • Level: ${prefs.experienceLevel}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.60f)
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    tempNickname = prefs.nickname
                                    showEditNameDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ubah Nama",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Theme Accent Palette
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "WARNA AKSEN ENERGI",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.50f),
                                    letterSpacing = 1.sp
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeAccent.entries.forEach { accent ->
                                    val isSelected = accent.id == prefs.themeAccent
                                    val color = Color(accent.colorHex)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                                        border = BorderStroke(
                                            if (isSelected) 1.2.dp else 0.8.dp,
                                            if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.updateThemeAccent(accent.id)
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .then(
                                                        if (isSelected) Modifier.border(BorderStroke(1.5.dp, Color.White), CircleShape)
                                                        else Modifier
                                                    )
                                            )
                                            Text(
                                                text = accent.label.substringBefore(" "),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 10.5.sp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── SECTION 2: AUDIO & PELATIH SUARA ─────────────────────────
                SettingsSectionTitle("SUARA & PELATIH SUARA (OFFLINE)")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        SettingsToggleRow(
                            icon = Icons.Default.RecordVoiceOver,
                            title = "Pelatih Suara Lokal (TTS)",
                            subtitle = "Aba-aba suara untuk persiapan, mulai, & setengah jalan",
                            checked = prefs.voiceCoachEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setVoiceCoachEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsToggleRow(
                            icon = Icons.Default.MusicNote,
                            title = "Musik Latar Adaptif (BGM)",
                            subtitle = "Musik energik yang meredup otomatis saat istirahat",
                            checked = prefs.bgmEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setBgmEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsToggleRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = "Nada Hitung Mundur (Beep)",
                            subtitle = "Suara chime studio pada detik 3, 2, 1",
                            checked = prefs.beepEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setBeepEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsToggleRow(
                            icon = Icons.Default.Vibration,
                            title = "Getaran Haptic Feedback",
                            subtitle = "Sentuhan getaran presisi saat pergantian interval",
                            checked = prefs.hapticEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setHapticEnabled(it)
                            }
                        )
                    }
                }

                // ── SECTION 3: PLAYER & VIDEO ────────────────────────────────
                SettingsSectionTitle("LAYAR & PEMUTAR VIDEO")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        SettingsToggleRow(
                            icon = Icons.Default.Videocam,
                            title = "Tampilkan Video Gerakan",
                            subtitle = "Matikan untuk Pure Focus Mode (lebih hemat baterai)",
                            checked = prefs.videoEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setVideoEnabled(it)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsInfoRow(
                            icon = Icons.Default.WbSunny,
                            title = "Layar Tetap Menyala",
                            value = "Otomatis Aktif"
                        )
                    }
                }

                // ── SECTION 4: PENGINGAT & KONSISTENSI HARIAN ─────────────────
                SettingsSectionTitle("PENGINGAT & DISIPLIN HARIAN")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        SettingsToggleRow(
                            icon = Icons.Default.Notifications,
                            title = "Pengingat Latihan Harian",
                            subtitle = "Alarm offline lokal untuk menjaga konsistensi streak Anda",
                            checked = prefs.reminderEnabled,
                            onCheckedChange = { isEnabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isEnabled && !isNotificationGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        DeviceReadinessManager.openNotificationSettings(context)
                                    }
                                }
                                viewModel.setReminderEnabled(context, isEnabled)
                            }
                        )

                        if (prefs.reminderEnabled) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            val reminderDesc = WorkoutReminderManager.getNextReminderDescription(
                                prefs.reminderHour,
                                prefs.reminderMinute
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Waktu Pengingat",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = reminderDesc,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    )
                                }

                                Surface(
                                    onClick = {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                viewModel.setReminderTime(context, hourOfDay, minute)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Pengingat diset ke ${String.format("%02d:%02d", hourOfDay, minute)} WIB"
                                                    )
                                                }
                                            },
                                            prefs.reminderHour,
                                            prefs.reminderMinute,
                                            true
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d", prefs.reminderHour, prefs.reminderMinute),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Quick Testing Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.testReminderNotification(context)
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Tes Notif Langsung", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (!isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.testReminderIn10Seconds(context)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Alarm pengingat akan berbunyi dalam 10 detik!")
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Uji Alarm 10 Detik", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // ── SECTION 5: KESIAPAN PERANGKAT & LATAR BELAKANG ───────────
                SettingsSectionTitle("KESIAPAN PERANGKAT & LATAR BELAKANG")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 1. Izin Notifikasi (Live Lockscreen Timer)
                        SettingsActionTile(
                            icon = Icons.Default.NotificationsActive,
                            title = "Notifikasi & Layar Kunci",
                            subtitle = if (isNotificationGranted) {
                                "Live timer & kontrol jeda/lanjut aktif di layar kunci"
                            } else {
                                "Diperlukan agar waktu timer terlihat saat HP terkunci di saku"
                            },
                            actionLabel = if (isNotificationGranted) "Aktif ✓" else "Izinkan",
                            isLoading = false,
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

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // 2. Proteksi Penghemat Baterai (Doze Mode)
                        SettingsActionTile(
                            icon = Icons.Default.BatteryChargingFull,
                            title = "Proteksi Pembekuan Baterai",
                            subtitle = if (isBatteryOptimizationIgnored) {
                                "Kekebalan penuh: OS tidak akan membekukan timer saat layar padam"
                            } else {
                                "Dibatasi: Sistem dapat mematikan timer saat layar mati lama di saku"
                            },
                            actionLabel = if (isBatteryOptimizationIgnored) "Aman ✓" else "Buka Proteksi",
                            isLoading = false,
                            onClick = {
                                DeviceReadinessManager.requestIgnoreBatteryOptimization(context)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // 3. Foreground Health Service Status
                        SettingsInfoRow(
                            icon = Icons.Default.Security,
                            title = "Layanan Timer Tingkat Sistem",
                            value = "Foreground Health"
                        )
                    }
                }

                // ── SECTION 5: DATA, KATALOG & BACKUP ────────────────────────
                SettingsSectionTitle("MANAJEMEN DATA & PEMULIHAN")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Sinkronisasi Katalog
                        SettingsActionTile(
                            icon = Icons.Default.CloudSync,
                            title = "Sinkronisasi Katalog 40+ Latihan",
                            subtitle = "Periksa pembaruan rutinitas bawaan dari repository",
                            actionLabel = if (uiState.isSyncing) "Menyinkronkan..." else "Sinkronkan",
                            isLoading = uiState.isSyncing,
                            onClick = { viewModel.syncCatalog() }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Ekspor Backup ZIP
                        SettingsActionTile(
                            icon = Icons.Default.UploadFile,
                            title = "Ekspor Cadangan Lokal (.zip)",
                            subtitle = "Simpan semua rutinitas & riwayat ke file arsip ZIP",
                            actionLabel = if (uiState.isExporting) "Mengekspor..." else "Ekspor",
                            isLoading = uiState.isExporting,
                            onClick = {
                                createDocumentLauncher.launch("workout_timer_backup_${System.currentTimeMillis()}.zip")
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Impor Backup ZIP
                        SettingsActionTile(
                            icon = Icons.Default.Download,
                            title = "Impor / Pulihkan Cadangan (.zip)",
                            subtitle = "Pulihkan database latihan Anda dari file cadangan",
                            actionLabel = if (uiState.isImporting) "Memulihkan..." else "Pulihkan",
                            isLoading = uiState.isImporting,
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Muat Ulang 40 Katalog
                        SettingsActionTile(
                            icon = Icons.Default.Refresh,
                            title = "Muat Ulang 40 Katalog Standar",
                            subtitle = "Pulihkan 40 program latihan bawaan jika terhapus",
                            actionLabel = "Muat Ulang",
                            isLoading = false,
                            onClick = { viewModel.reloadDefaultCatalog() }
                        )
                    }
                }

                // ── SECTION 5: TENTANG APLIKASI ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "WORKOUT TIMER • KELAS ATAS",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.45f),
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = "Versi 2.0 • 100% Offline & Privat • Bebas Server Pihak Ketiga",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    )
                }
            }
        }

        // ── DIALOG UBAH NAMA PANGGILAN ────────────────────────────────────────
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFF111319),
                title = {
                    Text(
                        text = "Ubah Nama Panggilan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { tempNickname = it },
                        placeholder = { Text("Nama panggilan Anda...", color = Color.White.copy(alpha = 0.35f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.60f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateNickname(tempNickname)
                            showEditNameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF07080B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Batal", color = Color.White.copy(alpha = 0.70f))
                    }
                }
            )
        }
    }
}

// ─── REUSABLE SETTINGS SUBCOMPONENTS ─────────────────────────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            color = Color.White.copy(alpha = 0.50f),
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color.White
                )
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 15.sp
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF07080B),
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White.copy(alpha = 0.45f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(19.dp)
            )
        }

        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = Color.White
            ),
            modifier = Modifier.weight(1f)
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f)
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color.White
                )
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 15.sp
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
            modifier = Modifier.clickable(enabled = !isLoading, onClick = onClick)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = actionLabel,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
