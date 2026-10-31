package com.workout.timer.ui.screen.builder

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
                totalDurationSec = uiState.totalDurationSec,
                totalExercises = uiState.totalExercises,
                onBack = onNavigateBack,
                onSave = { viewModel.saveRoutine(startImmediately = false) },
                onStartNow = { viewModel.saveRoutine(startImmediately = true) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Routine Metadata Card ────────────────────────────────────────
            item {
                RoutineMetadataCard(
                    name = uiState.routineName,
                    category = uiState.category,
                    accentColor = uiState.accentColor,
                    onNameChange = viewModel::onNameChanged,
                    onCategoryChange = viewModel::onCategoryChanged,
                    onColorChange = viewModel::onAccentColorChanged
                )
            }

            // ── Section header ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struktur Latihan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${uiState.blocks.size} Blok",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary
                    )
                }
            }

            // ── Blocks ───────────────────────────────────────────────────────
            uiState.blocks.forEachIndexed { blockIndex, uiBlock ->
                item(key = "block_${blockIndex}_${uiBlock.block.id}") {
                    BlockCard(
                        uiBlock = uiBlock,
                        blockIndex = blockIndex,
                        totalBlocks = uiState.blocks.size,
                        autocompleteSuggestions = uiState.autocompleteSuggestions,
                        onToggleExpand = { viewModel.toggleBlockExpanded(blockIndex) },
                        onLabelChange = { viewModel.updateBlockLabel(blockIndex, it) },
                        onRepeatCountChange = { viewModel.updateBlockRepeatCount(blockIndex, it) },
                        onMoveUp = { viewModel.moveBlockUp(blockIndex) },
                        onMoveDown = { viewModel.moveBlockDown(blockIndex) },
                        onDuplicate = { viewModel.duplicateBlock(blockIndex) },
                        onDelete = { viewModel.removeBlock(blockIndex) },
                        onAddExercise = { type -> viewModel.addExercise(blockIndex, type) },
                        onExerciseTitleChange = { exIdx, title ->
                            viewModel.updateExerciseTitle(blockIndex, exIdx, title)
                        },
                        onExerciseDurationChange = { exIdx, sec ->
                            viewModel.updateExerciseDuration(blockIndex, exIdx, sec)
                        },
                        onExerciseDurationAdd = { exIdx, addSec ->
                            viewModel.addDurationPreset(blockIndex, exIdx, addSec)
                        },
                        onExerciseDurationSet = { exIdx, sec ->
                            viewModel.setDurationPreset(blockIndex, exIdx, sec)
                        },
                        onExerciseTypeChange = { exIdx, type ->
                            viewModel.updateExerciseType(blockIndex, exIdx, type)
                        },
                        onExerciseDuplicate = { exIdx -> viewModel.duplicateExercise(blockIndex, exIdx) },
                        onExerciseDelete = { exIdx -> viewModel.removeExercise(blockIndex, exIdx) },
                        onApplyTemplate = { exIdx, template ->
                            viewModel.applyTemplate(blockIndex, exIdx, template)
                        },
                        onClearAutocomplete = viewModel::clearAutocomplete,
                        onExercisePickMedia = { exIdx ->
                            pendingMediaBlockIndex = blockIndex
                            pendingMediaExerciseIndex = exIdx
                            val currentEx = uiBlock.exercises.getOrNull(exIdx)
                            targetExerciseHasMedia = currentEx?.resolvedMediaUri != null
                            showMediaSourceSheet = true
                        },
                        onExerciseRemoveMedia = { exIdx ->
                            viewModel.removeMediaFromExercise(blockIndex, exIdx)
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // ── Add Block Button ─────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.addBlock() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF383842)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AccentWork, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Blok / Set Baru", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
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
    }
}

// ── Top App Bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuilderTopBar(
    isEditing: Boolean,
    isSaving: Boolean,
    totalDurationSec: Int,
    totalExercises: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onStartNow: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isEditing) "Edit Rutinitas" else "Rutinitas Baru",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = TextPrimary)
                }
            },
            actions = {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        color = AccentWork,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Check, "Simpan", tint = AccentWork)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPrimary)
        )
        // Real-time stats bar (PRD 4.1.4)
        Surface(color = SurfaceCard) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge(
                    label = "Total Estimasi",
                    value = formatDuration(totalDurationSec),
                    color = AccentWork
                )
                StatBadge(
                    label = "Total Gerakan",
                    value = "$totalExercises item",
                    color = AccentRest
                )
                Spacer(modifier = Modifier.weight(1f))
                // Quick start button
                if (totalExercises > 0) {
                    Button(
                        onClick = onStartNow,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24242A), contentColor = TextPrimary),
                        border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = AccentWork, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mulai", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
        HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

// ── Routine Metadata Card ────────────────────────────────────────────────────

@Composable
private fun RoutineMetadataCard(
    name: String,
    category: String,
    accentColor: String,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onColorChange: (String) -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Detail Rutinitas", style = MaterialTheme.typography.labelMedium, color = TextSecondary)

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nama rutinitas...", color = TextTertiary) },
                label = { Text("Nama", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = builderTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category + Color row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kategori", color = TextSecondary) },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary,
                                modifier = Modifier.clickable { showCategoryMenu = true })
                        },
                        colors = builderTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
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

            // Accent color picker
            Text("Warna Aksen", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PRESET_ACCENT_COLORS.forEach { colorHex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
                        .getOrDefault(AccentWork)
                    val isSelected = accentColor == colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorChange(colorHex) }
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.align(Alignment.Center).size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Block Card ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockCard(
    uiBlock: UiBlock,
    blockIndex: Int,
    totalBlocks: Int,
    autocompleteSuggestions: List<ExerciseTemplate>,
    onToggleExpand: () -> Unit,
    onLabelChange: (String) -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddExercise: (IntervalType) -> Unit,
    onExerciseTitleChange: (Int, String) -> Unit,
    onExerciseDurationChange: (Int, Int) -> Unit,
    onExerciseDurationAdd: (Int, Int) -> Unit,
    onExerciseDurationSet: (Int, Int) -> Unit,
    onExerciseTypeChange: (Int, IntervalType) -> Unit,
    onExerciseDuplicate: (Int) -> Unit,
    onExerciseDelete: (Int) -> Unit,
    onApplyTemplate: (Int, ExerciseTemplate) -> Unit,
    onClearAutocomplete: () -> Unit,
    onExercisePickMedia: (Int) -> Unit,
    onExerciseRemoveMedia: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBlockMenu by remember { mutableStateOf(false) }
    var focusedExerciseIndex by remember { mutableStateOf<Int?>(null) }

    val isLoop = uiBlock.block.repeatCount > 1

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column {
            // ── Block Header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop badge
                if (isLoop) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentWork.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = "×${uiBlock.block.repeatCount}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentWork,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Label or default name
                Text(
                    text = uiBlock.block.label
                        ?: if (isLoop) "Set ${blockIndex + 1}" else "Blok ${blockIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Exercise count badge
                Text(
                    text = "${uiBlock.exercises.size} gerakan",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // More options
                Box {
                    IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showBlockMenu,
                        onDismissRequest = { showBlockMenu = false },
                        containerColor = SurfaceCard
                    ) {
                        if (blockIndex > 0) {
                            DropdownMenuItem(
                                text = { Text("Pindah ke Atas", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, tint = TextSecondary) },
                                onClick = { onMoveUp(); showBlockMenu = false }
                            )
                        }
                        if (blockIndex < totalBlocks - 1) {
                            DropdownMenuItem(
                                text = { Text("Pindah ke Bawah", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = TextSecondary) },
                                onClick = { onMoveDown(); showBlockMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Duplikasi Blok", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.CopyAll, null, tint = TextSecondary) },
                            onClick = { onDuplicate(); showBlockMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Blok", color = ColorDanger) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorDanger) },
                            onClick = { showDeleteConfirm = true; showBlockMenu = false }
                        )
                    }
                }

                // Expand arrow
                Icon(
                    imageVector = if (uiBlock.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ── Block Settings (Set Looping) ─────────────────────────────────
            AnimatedVisibility(visible = uiBlock.isExpanded) {
                Column {
                    HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)

                    // Label editor
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiBlock.block.label ?: "",
                            onValueChange = onLabelChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Label blok (opsional)...", color = TextTertiary, style = MaterialTheme.typography.bodySmall) },
                            colors = builderTextFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )

                        // Set count stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Set", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onRepeatCountChange(uiBlock.block.repeatCount - 1) },
                                    modifier = Modifier.size(32.dp),
                                    enabled = uiBlock.block.repeatCount > 1
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = if (uiBlock.block.repeatCount > 1) AccentWork else TextTertiary, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "${uiBlock.block.repeatCount}×",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isLoop) AccentWork else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 32.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(
                                    onClick = { onRepeatCountChange(uiBlock.block.repeatCount + 1) },
                                    modifier = Modifier.size(32.dp),
                                    enabled = uiBlock.block.repeatCount < 20
                                ) {
                                    Icon(Icons.Default.Add, null, tint = AccentWork, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)

                    // ── Exercises ─────────────────────────────────────────────
                    uiBlock.exercises.forEachIndexed { exIndex, uiExercise ->
                        ExerciseRow(
                            uiExercise = uiExercise,
                            exerciseIndex = exIndex,
                            isActive = focusedExerciseIndex == exIndex,
                            autocompleteSuggestions = if (focusedExerciseIndex == exIndex) autocompleteSuggestions else emptyList(),
                            onFocus = { focusedExerciseIndex = exIndex },
                            onTitleChange = { onExerciseTitleChange(exIndex, it) },
                            onDurationChange = { onExerciseDurationChange(exIndex, it) },
                            onDurationAdd = { onExerciseDurationAdd(exIndex, it) },
                            onDurationSet = { onExerciseDurationSet(exIndex, it) },
                            onTypeChange = { onExerciseTypeChange(exIndex, it) },
                            onDuplicate = { onExerciseDuplicate(exIndex) },
                            onDelete = { onExerciseDelete(exIndex) },
                            onApplyTemplate = { template ->
                                onApplyTemplate(exIndex, template)
                                focusedExerciseIndex = null
                            },
                            onClearAutocomplete = {
                                onClearAutocomplete()
                                focusedExerciseIndex = null
                            },
                            onPickMedia = { onExercisePickMedia(exIndex) },
                            onRemoveMedia = { onExerciseRemoveMedia(exIndex) }
                        )
                        if (exIndex < uiBlock.exercises.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = SurfaceElevated,
                                thickness = 0.5.dp
                            )
                        }
                    }

                    // ── Add Exercise Buttons ──────────────────────────────────
                    HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)
                    AddExerciseRow(onAddExercise = onAddExercise)
                }
            }
        }
    }

    // Delete confirm dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceCard,
            title = { Text("Hapus Blok?", color = TextPrimary) },
            text = { Text("Semua gerakan dalam blok ini akan dihapus.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Hapus", color = ColorDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

// ── Exercise Row ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseRow(
    uiExercise: UiExercise,
    exerciseIndex: Int,
    isActive: Boolean,
    autocompleteSuggestions: List<ExerciseTemplate>,
    onFocus: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDurationAdd: (Int) -> Unit,
    onDurationSet: (Int) -> Unit,
    onTypeChange: (IntervalType) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onApplyTemplate: (ExerciseTemplate) -> Unit,
    onClearAutocomplete: () -> Unit,
    onPickMedia: () -> Unit,
    onRemoveMedia: () -> Unit
) {
    val exercise = uiExercise.exercise
    val accentColor = exercise.type.toAccentColor()
    val hasMedia = uiExercise.resolvedMediaUri != null && uiExercise.resolvedMediaType != MediaType.NONE
    var showTypeMenu by remember { mutableStateOf(false) }
    var showExerciseMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFocus)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { showTypeMenu = true }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title field
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = exercise.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Nama gerakan...", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    },
                    colors = builderTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                // Autocomplete dropdown
                DropdownMenu(
                    expanded = autocompleteSuggestions.isNotEmpty() && isActive,
                    onDismissRequest = onClearAutocomplete,
                    containerColor = SurfaceElevated
                ) {
                    autocompleteSuggestions.forEach { template ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            template.name,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
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
                                            text = "🎯 $muscle",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${template.defaultDuration}s · ${template.equipment ?: "Bodyweight"}",
                                        color = TextTertiary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            onClick = { onApplyTemplate(template) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Media Badge or Picker Icon
            if (hasMedia) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentRest.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clickable(onClick = onPickMedia)
                        .padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiExercise.resolvedMediaType == MediaType.GIF) Icons.Default.Image else Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = AccentRest,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (uiExercise.resolvedMediaType == MediaType.GIF) "GIF" else "MP4",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentRest,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onPickMedia,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Pasang GIF/Video",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Duration display + picker
            DurationChip(
                seconds = exercise.durationSeconds,
                color = accentColor,
                onClick = onFocus
            )

            // More options
            Box {
                IconButton(onClick = { showExerciseMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showExerciseMenu,
                    onDismissRequest = { showExerciseMenu = false },
                    containerColor = SurfaceCard
                ) {
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
                        text = { Text("Duplikasi", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.CopyAll, null, tint = TextSecondary) },
                        onClick = { onDuplicate(); showExerciseMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = ColorDanger) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorDanger) },
                        onClick = { onDelete(); showExerciseMenu = false }
                    )
                }
            }
        }

        // Type picker row
        AnimatedVisibility(visible = showTypeMenu) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(IntervalType.entries) { type ->
                    val typeColor = type.toAccentColor()
                    val isSelected = exercise.type == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTypeChange(type); showTypeMenu = false },
                        label = {
                            Text(
                                when (type) {
                                    IntervalType.WARMUP -> "Warmup"
                                    IntervalType.WORK -> "Work"
                                    IntervalType.REST -> "Rest"
                                    IntervalType.COOLDOWN -> "Cooldown"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = typeColor,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        // Quick time presets (PRD 4.1.3)
        AnimatedVisibility(visible = isActive) {
            Column {
                // Presets row
                Row(
                    modifier = Modifier.padding(start = 38.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    listOf(5, 15).forEach { addSec ->
                        OutlinedButton(
                            onClick = { onDurationAdd(addSec) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, SurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+${addSec}s", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(30, 45, 60, 90).forEach { sec ->
                        OutlinedButton(
                            onClick = { onDurationSet(sec) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, if (exercise.durationSeconds == sec) accentColor else SurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                "${sec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (exercise.durationSeconds == sec) accentColor else TextSecondary
                            )
                        }
                    }
                }
                // Manual duration input
                Row(
                    modifier = Modifier.padding(start = 38.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = exercise.durationSeconds.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { onDurationChange(it) } },
                        modifier = Modifier.width(80.dp),
                        label = { Text("Detik", color = TextTertiary, style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = builderTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        singleLine = true
                    )
                    Text(
                        "= ${formatDuration(exercise.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

// ── Add Exercise Row ─────────────────────────────────────────────────────────

@Composable
private fun AddExerciseRow(onAddExercise: (IntervalType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            IntervalType.WORK to "Gerakan",
            IntervalType.REST to "Istirahat",
            IntervalType.WARMUP to "Warmup",
            IntervalType.COOLDOWN to "Cooldown"
        ).forEach { (type, label) ->
            val color = type.toAccentColor()
            OutlinedButton(
                onClick = { onAddExercise(type) },
                modifier = Modifier.weight(1f).height(36.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

// ── Duration Chip ────────────────────────────────────────────────────────────

@Composable
private fun DurationChip(seconds: Int, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = formatSeconds(seconds),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
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
    return when {
        minutes == 0 -> "${seconds}d"
        seconds == 0 -> "${minutes} menit"
        else -> "${minutes}m ${seconds}d"
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}:${s.toString().padStart(2, '0')}" else "${s}d"
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(AccentWork, CircleShape)
                )
                Text(
                    text = "PANDUAN GERAKAN LATIHAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentWork,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

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
