package com.workout.timer.ui.screen.builder

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import com.workout.timer.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import java.io.File
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.data.model.*
import com.workout.timer.ui.theme.*
import com.workout.timer.ui.component.*
import androidx.compose.ui.res.painterResource

/**
 * Layar Workout Builder (F-04) — Implementasi Milestone 2.
 *
 * Fitur:
 * - Metadata editor (nama, kategori, warna aksen)
 * - Manajemen blok dengan set looping
 * - Penambahan & penghapusan gerakan per blok
 * - Quick time presets (F-04 PRD 4.1.3)
 * - Kalkulasi total waktu real-time (F-04 PRD 4.1.4)
 * - Autocomplete kamus gerakan (F-04)
 * - Swipe-to-delete dengan undo snackbar
 * - Perekaman video kamera langsung & upload galeri dengan validasi workout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    routineId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    viewModel: WorkoutBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Master Library Picker state: Pair(blockIndex, exerciseIndex?)
    var libraryPickerTarget by remember { mutableStateOf<Pair<Int, Int?>?>(null) }
    var focusedExerciseTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var expandedDurationTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showDeleteRoutineConfirm by remember { mutableStateOf(false) }
    var renamingBlockIndex by remember { mutableStateOf<Int?>(null) }
    var renameBlockText by remember { mutableStateOf("") }

    // Media picker & Camera recording state
    var pendingMediaBlockIndex by remember { mutableIntStateOf(-1) }
    var pendingMediaExerciseIndex by remember { mutableIntStateOf(-1) }
    var showMediaSourceSheet by remember { mutableStateOf(false) }
    var targetExerciseHasMedia by remember { mutableStateOf(false) }

    var currentTempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var currentTempVideoFile by remember { mutableStateOf<File?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pendingMediaBlockIndex >= 0 && pendingMediaExerciseIndex >= 0) {
            viewModel.attachMediaToExercise(pendingMediaBlockIndex, pendingMediaExerciseIndex, uri)
        }
    }

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = currentTempVideoUri
        val file = currentTempVideoFile
        if (success && uri != null && pendingMediaBlockIndex >= 0 && pendingMediaExerciseIndex >= 0) {
            viewModel.attachMediaToExercise(pendingMediaBlockIndex, pendingMediaExerciseIndex, uri)
        } else {
            file?.delete()
        }
        currentTempVideoUri = null
        currentTempVideoFile = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val (file, uri) = viewModel.createTempCameraVideoUri()
            currentTempVideoFile = file
            currentTempVideoUri = uri
            cameraVideoLauncher.launch(uri)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Izin kamera diperlukan untuk merekam video gerakan.")
            }
        }
    }

    fun startCameraRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val (file, uri) = viewModel.createTempCameraVideoUri()
            currentTempVideoFile = file
            currentTempVideoUri = uri
            cameraVideoLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle navigation to player
    LaunchedEffect(uiState.navigateToPlayer) {
        uiState.navigateToPlayer?.let { id ->
            viewModel.clearNavigation()
            onNavigateToPlayer(id)
        }
    }

    // Handle smooth back navigation on successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Handle snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundPrimary,
        topBar = {
            BuilderTopBar(
                isEditing = routineId != null,
                isSaving = uiState.isSaving,
                onBack = onNavigateBack,
                onDelete = { showDeleteRoutineConfirm = true },
                onSave = { viewModel.saveRoutine(startImmediately = false) }
            )
        },
        bottomBar = {
            BuilderBottomBar(
                totalDurationSec = uiState.totalDurationSec,
                totalExercises = uiState.totalExercises,
                isSaving = uiState.isSaving,
                onSaveAndStart = { viewModel.saveRoutine(startImmediately = true) }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentWork,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Routine Header ───────────────────────────────────────────
                item {
                    RoutineHeader(
                        name = uiState.routineName,
                        category = uiState.category,
                        accentColor = uiState.accentColor,
                        onNameChange = viewModel::onNameChanged,
                        onCategoryChange = viewModel::onCategoryChanged,
                        onColorChange = viewModel::onAccentColorChanged
                    )
                }

                // ── Section Header ───────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STRUKTUR LATIHAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = "${uiState.blocks.size} Blok",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextTertiary
                            )
                        )
                    }
                }

                // ── Blocks List ──────────────────────────────────────────────
                uiState.blocks.forEachIndexed { blockIndex, uiBlock ->
                    item(key = "block_${uiBlock.block.id}_${blockIndex}") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = BorderStroke(1.dp, Color(0xFF2C2C35))
                        ) {
                            Column {
                                // Block Header Row
                                BlockHeaderRow(
                                    uiBlock = uiBlock,
                                    blockIndex = blockIndex,
                                    totalBlocks = uiState.blocks.size,
                                    onRenameBlock = {
                                        renamingBlockIndex = blockIndex
                                        renameBlockText = uiBlock.block.label ?: "Blok ${blockIndex + 1}"
                                    },
                                    onRepeatCountChange = { viewModel.updateBlockRepeatCount(blockIndex, it) },
                                    onMoveUp = { viewModel.moveBlockUp(blockIndex) },
                                    onMoveDown = { viewModel.moveBlockDown(blockIndex) },
                                    onDuplicate = { viewModel.duplicateBlock(blockIndex) },
                                    onDelete = { viewModel.removeBlock(blockIndex) }
                                )

                                HorizontalDivider(color = Color(0xFF2C2C35), thickness = 0.5.dp)

                                // Exercises in this block
                                uiBlock.exercises.forEachIndexed { exIdx, uiExercise ->
                                    val isFocused = focusedExerciseTarget == (blockIndex to exIdx)
                                    val isDurationExpanded = expandedDurationTarget == (blockIndex to exIdx)

                                    ExerciseRowCompact(
                                        uiExercise = uiExercise,
                                        exerciseIndex = exIdx,
                                        blockIndex = blockIndex,
                                        isActive = isFocused,
                                        isDurationExpanded = isDurationExpanded,
                                        autocompleteSuggestions = if (isFocused) uiState.autocompleteSuggestions else emptyList(),
                                        onFocus = {
                                            focusedExerciseTarget = blockIndex to exIdx
                                            viewModel.onExerciseFocused(blockIndex, exIdx)
                                        },
                                        onToggleDurationExpand = {
                                            expandedDurationTarget = if (isDurationExpanded) null else (blockIndex to exIdx)
                                        },
                                        onTitleChange = { title -> viewModel.updateExerciseTitle(blockIndex, exIdx, title) },
                                        onTypeChange = { type -> viewModel.updateExerciseType(blockIndex, exIdx, type) },
                                        onDurationChange = { sec -> viewModel.updateExerciseDuration(blockIndex, exIdx, sec) },
                                        onDurationAdd = { addSec -> viewModel.addDurationPreset(blockIndex, exIdx, addSec) },
                                        onDurationSet = { sec -> viewModel.setDurationPreset(blockIndex, exIdx, sec) },
                                        onOpenLibraryPicker = { libraryPickerTarget = blockIndex to exIdx },
                                        onPickMedia = {
                                            pendingMediaBlockIndex = blockIndex
                                            pendingMediaExerciseIndex = exIdx
                                            targetExerciseHasMedia = uiExercise.resolvedMediaUri != null
                                            showMediaSourceSheet = true
                                        },
                                        onRemoveMedia = {
                                            viewModel.removeMediaFromExercise(blockIndex, exIdx)
                                        },
                                        onDuplicate = { viewModel.duplicateExercise(blockIndex, exIdx) },
                                        onDelete = { viewModel.removeExercise(blockIndex, exIdx) },
                                        onApplyTemplate = { template ->
                                            viewModel.applyTemplate(blockIndex, exIdx, template)
                                            focusedExerciseTarget = null
                                        },
                                        onClearAutocomplete = {
                                            viewModel.clearAutocomplete()
                                            focusedExerciseTarget = null
                                        }
                                    )

                                    if (exIdx < uiBlock.exercises.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFF26262F),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF2C2C35), thickness = 0.5.dp)

                                // Block Footer (Tambah Gerakan & Sisipan Cepat)
                                BlockFooterCompact(
                                    onAddMovement = { libraryPickerTarget = blockIndex to null },
                                    onQuickAdd = { type -> viewModel.addExercise(blockIndex, type) }
                                )
                            }
                        }
                    }
                }

                // ── Add Block Button (Outside Card) ──────────────────────────
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.addBlock() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF383842)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AccentWork, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "+ Tambah Blok / Superset Baru",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // ── Dialogs ──────────────────────────────────────────────────────────
        if (showDeleteRoutineConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteRoutineConfirm = false },
                containerColor = SurfaceCard,
                title = {
                    Text(
                        text = if (routineId != null) "Hapus Rutinitas?" else "Buang Rutinitas?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (routineId != null)
                            "Apakah Anda yakin ingin menghapus rutinitas ini? Tindakan ini tidak dapat dibatalkan."
                        else
                            "Perubahan yang belum disimpan akan dibuang.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteRoutineConfirm = false
                            if (routineId != null) {
                                viewModel.deleteRoutine(onSuccess = onNavigateBack)
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(
                            text = if (routineId != null) "Hapus" else "Buang",
                            color = ColorDanger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteRoutineConfirm = false }) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            )
        }

        if (renamingBlockIndex != null) {
            AlertDialog(
                onDismissRequest = { renamingBlockIndex = null },
                containerColor = SurfaceCard,
                title = { Text("Ubah Nama Blok", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Beri nama atau label khusus untuk blok latihan ini:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = renameBlockText,
                            onValueChange = { renameBlockText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: Pemanasan, Superset A, Cooldown", color = TextTertiary) },
                            colors = builderTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            renamingBlockIndex?.let { bIdx ->
                                viewModel.updateBlockLabel(bIdx, renameBlockText.trim())
                            }
                            renamingBlockIndex = null
                        }
                    ) {
                        Text("Simpan", color = AccentWork, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            renamingBlockIndex?.let { bIdx ->
                                viewModel.updateBlockLabel(bIdx, "")
                            }
                            renamingBlockIndex = null
                        }
                    ) {
                        Text("Reset Default", color = TextTertiary)
                    }
                }
            )
        }

        // ── Media Source Selector Bottom Sheet (Rekam Langsung / Galeri) ──
        if (showMediaSourceSheet) {
            MediaSourceBottomSheet(
                hasExistingMedia = targetExerciseHasMedia,
                onDismiss = { showMediaSourceSheet = false },
                onRecordVideo = {
                    showMediaSourceSheet = false
                    startCameraRecording()
                },
                onPickFromGallery = {
                    showMediaSourceSheet = false
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onRemoveMedia = {
                    showMediaSourceSheet = false
                    if (pendingMediaBlockIndex >= 0 && pendingMediaExerciseIndex >= 0) {
                        viewModel.removeMediaFromExercise(pendingMediaBlockIndex, pendingMediaExerciseIndex)
                    }
                }
            )
        }

        // ── Exercise Library Picker Bottom Sheet (Kamus Gerakan Master 73 Gerakan) ──
        libraryPickerTarget?.let { (targetBlockIndex, targetExerciseIndex) ->
            val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()
            ExerciseLibraryPickerBottomSheet(
                templates = allTemplates,
                onDismiss = { libraryPickerTarget = null },
                onSelectTemplate = { selectedTemplate ->
                    if (targetExerciseIndex != null) {
                        viewModel.applyTemplate(targetBlockIndex, targetExerciseIndex, selectedTemplate)
                    } else {
                        viewModel.addExerciseFromTemplate(targetBlockIndex, selectedTemplate)
                    }
                    libraryPickerTarget = null
                }
            )
        }
    }
}

// ── Top App Bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuilderTopBar(
    isEditing: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Rutinitas",
                    tint = ColorDanger
                )
            }
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(8.dp),
                    color = AccentWork,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Simpan",
                        tint = AccentWork
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
    )
}

// ── Sticky Bottom Bar ────────────────────────────────────────────────────────

@Composable
private fun BuilderBottomBar(
    totalDurationSec: Int,
    totalExercises: Int,
    isSaving: Boolean,
    onSaveAndStart: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFF141418),
        border = BorderStroke(0.5.dp, Color(0xFF2C2C35))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Estimasi:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "${formatDuration(totalDurationSec)} ($totalExercises item)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                )
            }

            Button(
                onClick = onSaveAndStart,
                enabled = !isSaving,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentWork,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Simpan & Mulai",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Routine Header (In-Place Title + 2 Capsule Chips) ─────────────────────────

@Composable
private fun RoutineHeader(
    name: String,
    category: String,
    accentColor: String,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Direct in-place title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (name.isEmpty()) {
                    Text(
                        text = "Ketuk untuk beri nama rutinitas...",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextTertiary
                        )
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(AccentWork),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Two Capsule Chips: [ ⚡ HIIT ▾ ] and [ 🟢 Warna Aksen ▾ ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Chip
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E24),
                    border = BorderStroke(1.dp, Color(0xFF383842)),
                    modifier = Modifier.clickable { showCategoryMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AccentWork,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentWork
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = AccentWork,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (showCategoryMenu) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showCategoryMenu = false },
                        containerColor = SurfaceCard
                    ) {
                        PRESET_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = if (cat == category) AccentWork else TextPrimary) },
                                onClick = { onCategoryChange(cat); showCategoryMenu = false }
                            )
                        }
                    }
                }
            }

            // Accent Color Chip
            val parsedColor = runCatching { Color(android.graphics.Color.parseColor(accentColor)) }
                .getOrDefault(AccentWork)
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E24),
                    border = BorderStroke(1.dp, Color(0xFF383842)),
                    modifier = Modifier.clickable { showColorMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                        )
                        Text(
                            text = "Warna Aksen",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = parsedColor
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = parsedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (showColorMenu) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showColorMenu = false },
                        containerColor = SurfaceCard
                    ) {
                        PRESET_ACCENT_COLORS.forEach { colorHex ->
                            val col = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
                                .getOrDefault(AccentWork)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                        )
                                        Text(
                                            text = colorHex,
                                            color = if (colorHex == accentColor) col else TextPrimary
                                        )
                                    }
                                },
                                onClick = { onColorChange(colorHex); showColorMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Block Header Row ─────────────────────────────────────────────────────────

@Composable
private fun BlockHeaderRow(
    uiBlock: UiBlock,
    blockIndex: Int,
    totalBlocks: Int,
    onRenameBlock: () -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val label = uiBlock.block.label?.takeIf { it.isNotBlank() } ?: "Blok ${blockIndex + 1}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu icon (≡) with dropdown
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Opsi Blok",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showMenu) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { showMenu = false },
                    containerColor = SurfaceCard
                ) {
                    if (blockIndex > 0) {
                        DropdownMenuItem(
                            text = { Text("Pindah ke Atas", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, tint = TextSecondary) },
                            onClick = { onMoveUp(); showMenu = false }
                        )
                    }
                    if (blockIndex < totalBlocks - 1) {
                        DropdownMenuItem(
                            text = { Text("Pindah ke Bawah", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary) },
                            onClick = { onMoveDown(); showMenu = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Duplikasi Blok", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.CopyAll, null, tint = TextSecondary) },
                        onClick = { onDuplicate(); showMenu = false }
                    )
                    if (totalBlocks > 1) {
                        DropdownMenuItem(
                            text = { Text("Hapus Blok", color = ColorDanger) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorDanger) },
                            onClick = { onDelete(); showMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Title + Edit: "Blok 1 (Ketuk untuk ubah nama) ✏️"
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRenameBlock)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (uiBlock.block.label.isNullOrBlank()) {
                Text(
                    text = "(Ketuk untuk ubah nama)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextTertiary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(13.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Set Stepper: "Set: — 1x +"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Set:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary
                )
            )
            IconButton(
                onClick = {
                    if (uiBlock.block.repeatCount > 1) {
                        onRepeatCountChange(uiBlock.block.repeatCount - 1)
                    }
                },
                modifier = Modifier.size(24.dp),
                enabled = uiBlock.block.repeatCount > 1
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Kurang Set",
                    tint = if (uiBlock.block.repeatCount > 1) AccentWork else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "${uiBlock.block.repeatCount}x",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (uiBlock.block.repeatCount > 1) AccentWork else TextPrimary
                )
            )
            IconButton(
                onClick = {
                    if (uiBlock.block.repeatCount < 20) {
                        onRepeatCountChange(uiBlock.block.repeatCount + 1)
                    }
                },
                modifier = Modifier.size(24.dp),
                enabled = uiBlock.block.repeatCount < 20
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Set",
                    tint = AccentWork,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Exercise Row Compact ─────────────────────────────────────────────────────

@Composable
private fun ExerciseRowCompact(
    uiExercise: UiExercise,
    exerciseIndex: Int,
    blockIndex: Int,
    isActive: Boolean,
    isDurationExpanded: Boolean,
    autocompleteSuggestions: List<ExerciseTemplate>,
    onFocus: () -> Unit,
    onToggleDurationExpand: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTypeChange: (IntervalType) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDurationAdd: (Int) -> Unit,
    onDurationSet: (Int) -> Unit,
    onOpenLibraryPicker: () -> Unit,
    onPickMedia: () -> Unit,
    onRemoveMedia: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onApplyTemplate: (ExerciseTemplate) -> Unit,
    onClearAutocomplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = uiExercise.exercise
    val accentColor = exercise.type.toAccentColor()
    val hasMedia = uiExercise.resolvedMediaUri != null && uiExercise.resolvedMediaType != MediaType.NONE
    var showTypeMenu by remember { mutableStateOf(false) }
    var showExerciseMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle (⠿)
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Pindah Urutan",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Vertical Accent Color Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Center Column: Line 1 Title, Line 2 Subtitle (Type Chip + Bullet + Media)
            Column(modifier = Modifier.weight(1f)) {
                // Line 1: In-place editable title
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (exercise.title.isEmpty()) {
                        Text(
                            text = "Nama gerakan...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    BasicTextField(
                        value = exercise.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) onFocus() },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                }

                // Line 2: Type chip + bullet + media badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Type selector
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showTypeMenu = true }
                                .padding(vertical = 2.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = when (exercise.type) {
                                        IntervalType.WORK -> R.drawable.ic_interval_work
                                        IntervalType.REST -> R.drawable.ic_interval_rest
                                        IntervalType.WARMUP -> R.drawable.ic_interval_warmup
                                        IntervalType.COOLDOWN -> R.drawable.ic_interval_cooldown
                                    }
                                ),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when (exercise.type) {
                                    IntervalType.WORK -> "Kerja"
                                    IntervalType.REST -> "Pemulihan"
                                    IntervalType.WARMUP -> "Pemanasan"
                                    IntervalType.COOLDOWN -> "Pendinginan"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        if (showTypeMenu) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showTypeMenu = false },
                                containerColor = SurfaceCard
                            ) {
                                IntervalType.entries.forEach { type ->
                                    val col = type.toAccentColor()
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        id = when (type) {
                                                            IntervalType.WORK -> R.drawable.ic_interval_work
                                                            IntervalType.REST -> R.drawable.ic_interval_rest
                                                            IntervalType.WARMUP -> R.drawable.ic_interval_warmup
                                                            IntervalType.COOLDOWN -> R.drawable.ic_interval_cooldown
                                                        }
                                                    ),
                                                    contentDescription = null,
                                                    tint = col,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = when (type) {
                                                        IntervalType.WORK -> "Kerja (Work)"
                                                        IntervalType.REST -> "Pemulihan (Rest)"
                                                        IntervalType.WARMUP -> "Pemanasan (Warmup)"
                                                        IntervalType.COOLDOWN -> "Pendinginan (Cooldown)"
                                                    },
                                                    color = if (type == exercise.type) col else TextPrimary
                                                )
                                            }
                                        },
                                        onClick = { onTypeChange(type); showTypeMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    // Bullet
                    Text("•", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp))

                    // Media badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onPickMedia)
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (hasMedia) {
                            Icon(
                                imageVector = if (uiExercise.resolvedMediaType == MediaType.GIF) Icons.Default.Movie else Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (uiExercise.resolvedMediaType == MediaType.GIF) "GIF tersedia" else "Video tersedia",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+ Media",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Big Athletic Duration Pill (Volt green or interval color with bold black text)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleDurationExpand)
            ) {
                Text(
                    text = formatSeconds(exercise.durationSeconds),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                )
            }

            // 3-dots Menu Button
            Box {
                IconButton(onClick = { showExerciseMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "Opsi Gerakan", tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
                if (showExerciseMenu) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showExerciseMenu = false },
                        containerColor = SurfaceCard
                    ) {
                        DropdownMenuItem(
                            text = { Text("Pilih dari Kamus Master", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.MenuBook, null, tint = AccentWork) },
                            onClick = { onOpenLibraryPicker(); showExerciseMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (hasMedia) "Ganti GIF / Video" else "Pasang GIF / Video", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.AttachFile, null, tint = AccentRest) },
                            onClick = { onPickMedia(); showExerciseMenu = false }
                        )
                        if (hasMedia) {
                            DropdownMenuItem(
                                text = { Text("Hapus GIF / Video", color = ColorDanger) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = ColorDanger) },
                                onClick = { onRemoveMedia(); showExerciseMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Duplikasi Gerakan", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.CopyAll, null, tint = TextSecondary) },
                            onClick = { onDuplicate(); showExerciseMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Gerakan", color = ColorDanger) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorDanger) },
                            onClick = { onDelete(); showExerciseMenu = false }
                        )
                    }
                }
            }
        }

        // Expanded Duration Quick Stepper & Presets
        AnimatedVisibility(
            visible = isDurationExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    listOf(5, 15, 30).forEach { addSec ->
                        Surface(
                            onClick = { onDurationAdd(addSec) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF262630),
                            border = BorderStroke(0.5.dp, Color(0xFF383842))
                        ) {
                            Text(
                                text = "+${addSec}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    listOf(30, 45, 60, 90).forEach { sec ->
                        val isSelected = exercise.durationSeconds == sec
                        Surface(
                            onClick = { onDurationSet(sec) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) accentColor else Color(0xFF262630),
                            border = BorderStroke(0.5.dp, if (isSelected) accentColor else Color(0xFF383842))
                        ) {
                            Text(
                                text = "${sec}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.Black else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Ketik detik:", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    OutlinedTextField(
                        value = exercise.durationSeconds.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { onDurationChange(it) } },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = builderTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
                        singleLine = true
                    )
                    Text(
                        text = "= ${formatDuration(exercise.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }

        // Autocomplete suggestions
        AnimatedVisibility(
            visible = isActive && autocompleteSuggestions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF23232C)),
                border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentWork,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Kamus Master (${autocompleteSuggestions.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentWork,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onClearAutocomplete,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, "Tutup", tint = TextTertiary, modifier = Modifier.size(13.dp))
                        }
                    }

                    autocompleteSuggestions.take(4).forEach { template ->
                        Surface(
                            onClick = { onApplyTemplate(template) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E24),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            border = BorderStroke(0.5.dp, SurfaceBorder.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(AccentWork.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = getExerciseMovementIconRes(template.category, template.mediaUri != null)),
                                        contentDescription = null,
                                        tint = AccentWork,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        template.difficulty?.let { diff ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (diff.lowercase()) {
                                                    "beginner" -> AccentWork.copy(alpha = 0.2f)
                                                    "intermediate" -> AccentWarmup.copy(alpha = 0.2f)
                                                    else -> AccentRest.copy(alpha = 0.2f)
                                                }
                                            ) {
                                                Text(
                                                    text = diff,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (diff.lowercase()) {
                                                        "beginner" -> AccentWork
                                                        "intermediate" -> AccentWarmup
                                                        else -> AccentRest
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    template.targetMuscle?.let { muscle ->
                                        Text(
                                            text = "🎯 $muscle  /  ${template.defaultDuration}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Pilih",
                                    tint = AccentWork,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Block Footer Compact (Tambah Gerakan & Sisipan Cepat) ───────────────────

@Composable
private fun BlockFooterCompact(
    onAddMovement: () -> Unit,
    onQuickAdd: (IntervalType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // [ 📖 + Tambah Gerakan (Kamus / Manual) ]
        Surface(
            onClick = onAddMovement,
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF23232A),
            border = BorderStroke(1.dp, Color(0xFF383842)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Tambah Gerakan (Kamus / Manual)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }
        }

        // Sisipan cepat: + Istirahat • + Warmup • + Cooldown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sisipan cepat: ",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            )

            // + Istirahat
            Text(
                text = "+ Istirahat",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AccentRest,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onQuickAdd(IntervalType.REST) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Text(" • ", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))

            // + Warmup
            Text(
                text = "+ Warmup",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AccentWarmup,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onQuickAdd(IntervalType.WARMUP) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Text(" • ", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))

            // + Cooldown
            Text(
                text = "+ Cooldown",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AccentCooldown,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onQuickAdd(IntervalType.COOLDOWN) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun builderTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentWork,
    unfocusedBorderColor = SurfaceElevated,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentWork,
    focusedContainerColor = BackgroundPrimary.copy(alpha = 0.5f),
    unfocusedContainerColor = Color.Transparent
)

private fun formatDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (minutes > 0) {
        "${minutes}m ${seconds.toString().padStart(2, '0')}d"
    } else {
        "${seconds}d"
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0 && s > 0) {
        "${m}m ${s}d"
    } else if (m > 0) {
        "${m}m"
    } else {
        "${s}d"
    }
}

// ── Media Source Selector Bottom Sheet (Rekam Langsung / Galeri) ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaSourceBottomSheet(
    hasExistingMedia: Boolean,
    onDismiss: () -> Unit,
    onRecordVideo: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemoveMedia: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = SurfaceElevated)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with custom MediaGuideHeaderBadge
            MediaGuideHeaderBadge(
                title = "PANDUAN GERAKAN LATIHAN"
            )

            Text(
                text = "Pilih Sumber Video & Media",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Tambahkan rekaman kamera langsung atau unggah file video / GIF untuk memandu teknik gerakan saat timer berjalan.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Option 1: Record Video Direct with Camera
            Surface(
                onClick = onRecordVideo,
                shape = RoundedCornerShape(20.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(AccentWork, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Rekam Video Langsung",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentWork.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "KAMERA",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentWork,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Rekam demo postur gerakan secara langsung (2 - 90 detik)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                }
            }

            // Option 2: Pick from Gallery / Files
            Surface(
                onClick = onPickFromGallery,
                shape = RoundedCornerShape(20.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(AccentRest.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = AccentRest,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pilih dari Galeri",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentRest.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "FILE / GIF",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentRest,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Unggah file video MP4, WebM, atau animasi GIF",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                }
            }

            // Option 3: Delete Current Media (if exists)
            if (hasExistingMedia) {
                Surface(
                    onClick = onRemoveMedia,
                    shape = RoundedCornerShape(20.dp),
                    color = ColorDanger.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ColorDanger.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(ColorDanger.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = ColorDanger,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hapus Media Saat Ini",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorDanger,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kembalikan ke visualizer siluet atletik bawaan",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Workout Validation Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BackgroundPrimary,
                border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentWarmup,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ketentuan Validasi Media Workout:",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentWarmup,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Durasi video: Minimal 2 detik, maksimal 90 detik (disarankan 5–30 detik per rep demo).\n• Ukuran file: Maksimal 60MB untuk video & 25MB untuk GIF agar playback instan tanpa lag.\n• Format didukung: MP4, WebM, MKV, GIF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ── Exercise Library Picker Bottom Sheet (Master Data Kamus Gerakan) ────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseLibraryPickerBottomSheet(
    templates: List<ExerciseTemplate>,
    onDismiss: () -> Unit,
    onSelectTemplate: (ExerciseTemplate) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(templates) {
        listOf("Semua") + templates.mapNotNull { it.category }.distinct().sorted()
    }

    val filteredTemplates = remember(templates, searchQuery, selectedCategory) {
        templates.filter { tmpl ->
            val matchQuery = searchQuery.isBlank() ||
                tmpl.name.contains(searchQuery, ignoreCase = true) ||
                (tmpl.targetMuscle?.contains(searchQuery, ignoreCase = true) == true) ||
                (tmpl.primaryMuscle?.contains(searchQuery, ignoreCase = true) == true) ||
                (tmpl.equipment?.contains(searchQuery, ignoreCase = true) == true)
            val matchCategory = selectedCategory == null || selectedCategory == "Semua" ||
                tmpl.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kamus Gerakan Master",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentWork.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${filteredTemplates.size} item",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentWork,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Pilih gerakan terstandarisasi untuk rutinitas",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari gerakan, otot, alat...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AccentWork) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = TextTertiary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = builderTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories, key = { "picker_cat_$it" }) { cat ->
                    val isSelected = (selectedCategory == null && cat == "Semua") || selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = if (cat == "Semua") null else cat
                        },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentWork,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exercise List or Empty State
            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tidak ada gerakan ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Coba kata kunci lain atau pilih kategori 'Semua'",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectTemplate(template)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Category Icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            when (template.category?.lowercase()) {
                                                "cardio", "hiit" -> AccentWork.copy(alpha = 0.15f)
                                                "core" -> AccentWarmup.copy(alpha = 0.15f)
                                                else -> AccentRest.copy(alpha = 0.15f)
                                            },
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = getExerciseMovementIconRes(template.category, template.mediaUri != null)),
                                        contentDescription = null,
                                        tint = when (template.category?.lowercase()) {
                                            "cardio", "hiit" -> AccentWork
                                            "core" -> AccentWarmup
                                            else -> AccentRest
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        template.difficulty?.let { diff ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (diff.lowercase()) {
                                                    "beginner" -> AccentWork.copy(alpha = 0.2f)
                                                    "intermediate" -> AccentWarmup.copy(alpha = 0.2f)
                                                    else -> AccentRest.copy(alpha = 0.2f)
                                                }
                                            ) {
                                                Text(
                                                    text = diff,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (diff.lowercase()) {
                                                        "beginner" -> AccentWork
                                                        "intermediate" -> AccentWarmup
                                                        else -> AccentRest
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        template.targetMuscle?.let { muscle ->
                                            Text(
                                                text = "🎯 $muscle",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "/  ${template.defaultDuration}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                        Text(
                                            text = "/  ${template.equipment ?: "Bodyweight"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Pilih",
                                    tint = AccentWork,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

