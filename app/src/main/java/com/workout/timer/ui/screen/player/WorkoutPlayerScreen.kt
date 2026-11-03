package com.workout.timer.ui.screen.player

import android.app.Activity
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.workout.timer.R
import kotlin.math.cos
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.workout.timer.data.model.FlatExerciseItem
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.model.MediaType
import com.workout.timer.service.timer.WorkoutTimerState
import com.workout.timer.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

/**
 * WorkoutPlayerScreen — Layar Eksekusi Latihan Kelas Atas (Apple Fitness+ Vibe) (PRD Milestone 3 & 4).
 *
 * Fitur:
 * - Dual-Buffer ExoPlayer: Maksimal 2 instance player dengan pre-load di detik ke-3 dan crossfade 200ms
 *   untuk menjamin zero black-screen transition (PRD 4.2.1 & 7.3).
 * - Rest State Preview: Menampilkan klip video/GIF gerakan yang akan datang saat fase istirahat (PRD 4.2.2).
 * - Adaptive Layout: Otomatis split 2 kolom pada Landscape/Tablet dan vertikal pada Portrait (PRD 4.2.6).
 * - 60fps Canvas Progress Ring dengan warna aksen neon adaptif (Volt Green, Electric Cyan, Amber).
 * - Tipografi Tabular Numbers ("tnum") 62sp agar angka detik stabil dan tidak bergeser.
 * - Sweat-Proof Controls: Ketuk tengah ring untuk Pause/Resume, Hold-to-Stop 1.5 detik.
 * - On-the-Fly Rest Adjustment: Tombol +15s dan Skip Rest.
 * - Keep Screen On otomatis saat sesi berjalan.
 */
@Composable
fun WorkoutPlayerScreen(
    routineId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    var hasStarted by remember { mutableStateOf(false) }

    // Jalankan timer saat layar pertama kali dibuka
    LaunchedEffect(routineId) {
        if (routineId > 0) {
            viewModel.startRoutine(routineId)
        }
    }

    // Pantau transisi timerState: jika pernah berjalan lalu menjadi Idle (misal via notifikasi 'Berhenti'),
    // otomatis kembali ke layar sebelumnya agar tidak stuck di loading.
    LaunchedEffect(timerState) {
        if (timerState !is WorkoutTimerState.Idle) {
            hasStarted = true
        } else if (hasStarted) {
            onNavigateBack()
        }
    }

    // Safety timeout: jika dalam 5 detik tetap Idle dan belum sempat mulai (misal routine kosong),
    // otomatis kembali agar user tidak stuck di loading spinner.
    LaunchedEffect(Unit) {
        if (routineId > 0) {
            delay(5000)
            if (viewModel.timerState.value is WorkoutTimerState.Idle && !hasStarted) {
                onNavigateBack()
            }
        }
    }

    // Keep Screen On saat player terbuka
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Intersep tombol back fisik
    BackHandler {
        viewModel.stopWorkout()
        onNavigateBack()
    }

    Scaffold(
        containerColor = BackgroundPrimary
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isLandscape = maxWidth > maxHeight

            val isBgmEnabled by viewModel.isBgmEnabled.collectAsStateWithLifecycle()

            when (val state = timerState) {
                is WorkoutTimerState.Idle -> {
                    if (routineId <= 0) {
                        CompletedWorkoutView(
                            state = WorkoutTimerState.Completed(
                                routineId = 1L,
                                routineName = "Hikari Workout",
                                totalDurationSeconds = 630,
                                totalExercisesCompleted = 31
                            ),
                            onFinish = onNavigateBack
                        )
                    } else {
                        LoadingPlayerView(onCancel = onNavigateBack)
                    }
                }
                is WorkoutTimerState.Preparing -> {
                    PreparingView(
                        state = state,
                        onCancel = {
                            viewModel.stopWorkout()
                            onNavigateBack()
                        }
                    )
                }
                is WorkoutTimerState.Running -> {
                    ActiveWorkoutView(
                        routineName = state.routineName,
                        exerciseIndex = state.exerciseIndex,
                        totalExercises = state.totalExercises,
                        currentExercise = state.currentExercise,
                        nextExercise = state.nextExercise,
                        remainingSeconds = state.remainingSeconds,
                        totalSeconds = state.totalSeconds,
                        totalElapsedSeconds = state.totalElapsedSeconds,
                        progress = state.progress,
                        isPaused = state.isPaused,
                        intervalType = state.currentExercise.type,
                        isLandscape = isLandscape,
                        isBgmEnabled = isBgmEnabled,
                        onToggleBgm = { viewModel.toggleBgm() },
                        onTogglePause = { viewModel.togglePause() },
                        onSkip = { viewModel.skip() },
                        onPrevious = { viewModel.previous() },
                        onStop = {
                            viewModel.stopWorkout()
                            onNavigateBack()
                        }
                    )
                }
                is WorkoutTimerState.Resting -> {
                    ActiveWorkoutView(
                        routineName = state.routineName,
                        exerciseIndex = state.exerciseIndex,
                        totalExercises = state.totalExercises,
                        currentExercise = state.currentExercise,
                        nextExercise = state.upcomingExercise,
                        remainingSeconds = state.remainingSeconds,
                        totalSeconds = state.totalSeconds,
                        totalElapsedSeconds = state.totalElapsedSeconds,
                        progress = state.progress,
                        isPaused = state.isPaused,
                        intervalType = IntervalType.REST,
                        isLandscape = isLandscape,
                        isBgmEnabled = isBgmEnabled,
                        onToggleBgm = { viewModel.toggleBgm() },
                        onTogglePause = { viewModel.togglePause() },
                        onSkip = { viewModel.skip() },
                        onPrevious = { viewModel.previous() },
                        onAddRest = { viewModel.addRestTime(15) },
                        onStop = {
                            viewModel.stopWorkout()
                            onNavigateBack()
                        }
                    )
                }
                is WorkoutTimerState.Completed -> {
                    CompletedWorkoutView(
                        state = state,
                        onFinish = {
                            viewModel.stopWorkout()
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

// ─── VISUAL GUIDE & DYNAMIC ILLUSTRATION CARD (APPLE FITNESS+ STYLE) ──────────

data class ExerciseVisualGuide(
    val iconRes: Int,
    val targetMuscles: String,
    val coachTip: String
)

private fun getExerciseGuide(title: String, type: IntervalType): ExerciseVisualGuide {
    if (type == IntervalType.REST) {
        return ExerciseVisualGuide(
            iconRes = R.drawable.ic_pose_meditation,
            targetMuscles = "Pemulihan & Detak Jantung",
            coachTip = "Tarik napas dalam dari hidung, hembuskan perlahan dari mulut."
        )
    }
    val lower = title.lowercase()
    return when {
        lower.contains("push up") || lower.contains("dip") || lower.contains("row") -> ExerciseVisualGuide(
            iconRes = R.drawable.ic_pose_push_up,
            targetMuscles = "Dada / Bahu / Trisep / Punggung",
            coachTip = "Kunci otot inti, turunkan tubuh perlahan dan dorong kuat."
        )
        lower.contains("squat") || lower.contains("lunge") -> ExerciseVisualGuide(
            iconRes = R.drawable.ic_pose_squats,
            targetMuscles = "Paha Depan / Glutes / Betis",
            coachTip = "Beban pada tumit, jaga dada tegak dan lutut sejajar jari kaki."
        )
        lower.contains("plank") || lower.contains("crunch") || lower.contains("scissor") || lower.contains("back up") -> ExerciseVisualGuide(
            iconRes = R.drawable.ic_pose_plank,
            targetMuscles = "Otot Perut / Inti Tubuh / Pinggang",
            coachTip = "Pertahankan garis lurus dari bahu hingga tumit, jangan melengkung."
        )
        lower.contains("knee") || lower.contains("jump") || lower.contains("climber") || lower.contains("burpee") || lower.contains("swimmer") -> ExerciseVisualGuide(
            iconRes = R.drawable.ic_pose_high_knees,
            targetMuscles = "Kardio / Kelincahan / Pembakaran Kalori",
            coachTip = "Angkat lutut setinggi pinggang, jaga ritme pernapasan konstan."
        )
        else -> ExerciseVisualGuide(
            iconRes = R.drawable.ic_pose_high_knees,
            targetMuscles = "Seluruh Tubuh / Daya Tahan",
            coachTip = "Jaga postur tetap tegak dan pertahankan fokus penuh."
        )
    }
}

@Composable
private fun DynamicExerciseIllustrationCard(
    currentExercise: FlatExerciseItem,
    nextExercise: FlatExerciseItem?,
    intervalType: IntervalType,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isRest = intervalType == IntervalType.REST
    val guide = if (isRest) {
        val nextTitle = nextExercise?.title ?: "Gerakan Berikutnya"
        getExerciseGuide(nextTitle, IntervalType.WORK)
    } else {
        getExerciseGuide(currentExercise.title, intervalType)
    }

    val activeTargetMuscles = if (isRest) {
        nextExercise?.targetMuscle ?: guide.targetMuscles
    } else {
        currentExercise.targetMuscle ?: guide.targetMuscles
    }

    val activeCoachTip = if (isRest) {
        nextExercise?.coachTip ?: guide.coachTip
    } else {
        currentExercise.coachTip ?: guide.coachTip
    }

    val infiniteTransition = rememberInfiniteTransition(label = "illustration_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_anim"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient background glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 450f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Phase Badge & Muscle Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isRest) R.drawable.ic_pose_meditation else R.drawable.ic_intensity_lightning
                                ),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRest) "BERSIAP BERIKUTNYA" else "PANDUAN POSTUR",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevated
                    ) {
                        Text(
                            text = "🎯 $activeTargetMuscles",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Center: Big Floating Pose Illustration
                Box(
                    modifier = Modifier
                        .size(125.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = guide.iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(105.dp)
                            .graphicsLayer {
                                translationY = floatY.dp.toPx()
                            }
                    )
                }

                // Bottom: Title & Coach Tip
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRest) (nextExercise?.title ?: "Gerakan Berikutnya") else currentExercise.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "💡 $activeCoachTip",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

// ─── ACTIVE WORKOUT VIEW (ADAPTIVE PORTRAIT & LANDSCAPE) ──────────────────────

@Composable
private fun ActiveWorkoutView(
    routineName: String,
    exerciseIndex: Int,
    totalExercises: Int,
    currentExercise: FlatExerciseItem,
    nextExercise: FlatExerciseItem?,
    remainingSeconds: Int,
    totalSeconds: Int,
    totalElapsedSeconds: Int,
    progress: Float,
    isPaused: Boolean,
    intervalType: IntervalType,
    isLandscape: Boolean,
    isBgmEnabled: Boolean = true,
    onToggleBgm: (() -> Unit)? = null,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onAddRest: (() -> Unit)? = null,
    onStop: () -> Unit
) {
    val accentColor = when (intervalType) {
        IntervalType.WORK -> AccentWork
        IntervalType.REST -> AccentRest
        IntervalType.WARMUP, IntervalType.COOLDOWN -> AccentWarmup
    }

    val displayMediaUri = if (intervalType == IntervalType.REST) {
        currentExercise.mediaUri ?: com.workout.timer.data.catalog.AssetVideoResolver.resolveVideo(currentExercise.title, null, intervalType)
    } else {
        currentExercise.mediaUri
    }

    val displayMediaType = if (intervalType == IntervalType.REST) {
        val resolvedUri = displayMediaUri
        if (com.workout.timer.data.catalog.AssetVideoResolver.isVideo(resolvedUri)) MediaType.VIDEO else (currentExercise.mediaType.takeIf { it != MediaType.NONE } ?: MediaType.VIDEO)
    } else {
        currentExercise.mediaType
    }

    // Ambient living background glow & watermark motion (isolated State to prevent recomposition storm)
    val infiniteTransition = rememberInfiniteTransition(label = "player_ambient")
    val pulseGlow = infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val floatWatermarkY = infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "watermark_float"
    )

    // Tactile haptic feedback for sweat-proof workout experience
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds in 1..3 && !isPaused) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Living Ambient Radial Glow Canvas behind the entire workout (read in DrawScope only)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = pulseGlow.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.75f
                ),
                radius = size.minDimension * 0.75f,
                center = center
            )
        }

        // Faint stylized pose silhouette in background (graphicsLayer translation avoids recomposition)
        val bgGuide = getExerciseGuide(currentExercise.title, intervalType)
        Icon(
            painter = painterResource(id = bgGuide.iconRes),
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.04f),
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY = floatWatermarkY.value.dp.toPx()
                }
        )

        if (isLandscape) {
            // ── LANDSCAPE / TABLET LAYOUT (SPLIT 2 COLUMNS) ───────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                TopHeaderBar(
                    routineName = routineName,
                    exerciseIndex = exerciseIndex,
                    totalExercises = totalExercises,
                    remainingSeconds = remainingSeconds,
                    totalElapsedSeconds = totalElapsedSeconds,
                    intervalType = intervalType,
                    accentColor = accentColor,
                    isBgmEnabled = isBgmEnabled,
                    onToggleBgm = onToggleBgm,
                    onStop = onStop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        WorkoutMediaContainer(
                            mediaUri = displayMediaUri,
                            mediaType = displayMediaType,
                            nextMediaUri = nextExercise?.mediaUri,
                            nextMediaType = nextExercise?.mediaType ?: MediaType.NONE,
                            currentExercise = currentExercise,
                            nextExercise = nextExercise,
                            remainingSeconds = remainingSeconds,
                            isPaused = isPaused,
                            isRestPreview = false,
                            previewTitle = nextExercise?.title,
                            accentColor = accentColor,
                            intervalType = intervalType,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTogglePause()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressRing(
                                progress = progress,
                                strokeWidth = 18.dp,
                                accentColor = accentColor,
                                trackColor = accentColor.copy(alpha = 0.15f),
                                remainingSeconds = remainingSeconds
                            )
                            RingCenterContent(
                                intervalType = intervalType,
                                accentColor = accentColor,
                                remainingSeconds = remainingSeconds,
                                currentExercise = currentExercise,
                                isPaused = isPaused,
                                isCompact = false
                            )
                        }

                        BottomPlaybackControls(
                            intervalType = intervalType,
                            accentColor = accentColor,
                            isPaused = isPaused,
                            onTogglePause = onTogglePause,
                            onSkip = onSkip,
                            onPrevious = onPrevious,
                            onAddRest = onAddRest
                        )
                    }
                }
            }
        } else {
            // ── PORTRAIT LAYOUT (FULLSCREEN VIDEO & ULTRA-TRANSPARENT GLASS HUD) ──
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. FULLSCREEN VIDEO LAYER (EDGE-TO-EDGE)
                WorkoutMediaContainer(
                    mediaUri = displayMediaUri,
                    mediaType = displayMediaType,
                    nextMediaUri = nextExercise?.mediaUri,
                    nextMediaType = nextExercise?.mediaType ?: MediaType.NONE,
                    currentExercise = currentExercise,
                    nextExercise = nextExercise,
                    remainingSeconds = remainingSeconds,
                    isPaused = isPaused,
                    isRestPreview = false,
                    previewTitle = nextExercise?.title,
                    accentColor = accentColor,
                    intervalType = intervalType,
                    modifier = Modifier.fillMaxSize(),
                    isFullscreen = true
                )

                // 2. ULTRA-SUBTLE VIGNETTE (Only top status bar protection & subtle bottom shade)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f))
                            )
                        )
                )

                // 3. FLOATING ULTRA-TRANSPARENT HUD OVERLAY
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // TOP HEADER WITH SESSION PROGRESS & HEARTBEAT (GLASS MODE)
                    TopHeaderBar(
                        routineName = routineName,
                        exerciseIndex = exerciseIndex,
                        totalExercises = totalExercises,
                        remainingSeconds = remainingSeconds,
                        totalElapsedSeconds = totalElapsedSeconds,
                        intervalType = intervalType,
                        accentColor = accentColor,
                        isBgmEnabled = isBgmEnabled,
                        onToggleBgm = onToggleBgm,
                        onStop = onStop,
                        isGlassMode = true
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 60FPS COMPACT CIRCULAR PROGRESS RING (130dp WITH ULTRA-TRANSPARENT GLASS DISC)
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.10f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePause()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressRing(
                            progress = progress,
                            strokeWidth = 4.dp,
                            accentColor = Color.White,
                            trackColor = Color.White.copy(alpha = 0.18f),
                            remainingSeconds = remainingSeconds,
                            isGlassMode = true
                        )
                        RingCenterContent(
                            intervalType = intervalType,
                            accentColor = accentColor,
                            remainingSeconds = remainingSeconds,
                            currentExercise = currentExercise,
                            isPaused = isPaused,
                            isCompact = true,
                            isGlassMode = true
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // FLOATING UP NEXT BANNER & CONTROLS DOCK
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // UP NEXT BANNER (GLASS MODE)
                        nextExercise?.let { next ->
                            UpNextBanner(
                                next = next,
                                isGlassMode = true
                            )
                        }

                        // BOTTOM CONTROLS DOCK (GLASS MODE)
                        BottomPlaybackControls(
                            intervalType = intervalType,
                            accentColor = accentColor,
                            isPaused = isPaused,
                            onTogglePause = onTogglePause,
                            onSkip = onSkip,
                            onPrevious = onPrevious,
                            onAddRest = onAddRest,
                            isGlassMode = true
                        )
                    }
                }
            }
        }
    }
}

// ─── DUAL-BUFFER EXOPLAYER & MEDIA PIPELINE (ZERO BLACK-SCREEN) ──────────────

@Composable
private fun WorkoutMediaContainer(
    mediaUri: String?,
    mediaType: MediaType,
    nextMediaUri: String?,
    nextMediaType: MediaType,
    currentExercise: FlatExerciseItem,
    nextExercise: FlatExerciseItem?,
    remainingSeconds: Int,
    isPaused: Boolean,
    isRestPreview: Boolean,
    previewTitle: String?,
    accentColor: Color,
    intervalType: IntervalType,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false
) {
    val content = @Composable {
        // Prioritaskan video AI beresolusi tinggi daripada animasi GIF
        // Saat fase REST, putar video istirahat/recovery (bukan video gerakan berikutnya)
        val effectiveMediaUri = com.workout.timer.data.catalog.AssetVideoResolver.resolveVideo(
            title = currentExercise.title,
            currentUri = mediaUri,
            intervalType = intervalType
        )
        val effectiveMediaType = com.workout.timer.data.catalog.AssetVideoResolver.getEffectiveMediaType(effectiveMediaUri, mediaType)

        val effectiveNextUri = com.workout.timer.data.catalog.AssetVideoResolver.resolveVideo(
            title = nextExercise?.title,
            currentUri = nextMediaUri,
            intervalType = nextExercise?.type
        )
        val effectiveNextType = com.workout.timer.data.catalog.AssetVideoResolver.getEffectiveMediaType(effectiveNextUri, nextMediaType)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Media bertipe Video MP4 / WebM: Jalankan Dual-Buffer ExoPlayer
                effectiveMediaType == MediaType.VIDEO && !effectiveMediaUri.isNullOrBlank() -> {
                    DualBufferExoPlayerView(
                        activeUri = effectiveMediaUri,
                        nextUri = if (effectiveNextType == MediaType.VIDEO) effectiveNextUri else null,
                        remainingSeconds = remainingSeconds,
                        isPaused = isPaused,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Media bertipe GIF: Tampilkan via Coil
                effectiveMediaType == MediaType.GIF && !effectiveMediaUri.isNullOrBlank() -> {
                    val context = LocalContext.current
                    val imageRequest = remember(effectiveMediaUri) {
                        val modelData = when {
                            effectiveMediaUri.startsWith("file:///android_asset/") -> android.net.Uri.parse(effectiveMediaUri)
                            effectiveMediaUri.startsWith("/") -> File(effectiveMediaUri)
                            else -> effectiveMediaUri
                        }
                        ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(true)
                            .build()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isFullscreen) Color.Black else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            imageLoader = context.imageLoader,
                            contentDescription = "Animasi Gerakan",
                            contentScale = if (isFullscreen) ContentScale.Crop else ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Tidak ada media kustom: Tampilkan Apple Fitness+ Style Dynamic Motion Card
                else -> {
                    DynamicExerciseIllustrationCard(
                        currentExercise = currentExercise,
                        nextExercise = nextExercise,
                        intervalType = intervalType,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Overlay Badge khusus saat Video / GIF aktif dalam mode non-fullscreen card
            if (!isFullscreen && !isRestPreview && (mediaType == MediaType.VIDEO || mediaType == MediaType.GIF) && !mediaUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Top-Left: Status Tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_media_guide_badge),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (mediaType == MediaType.VIDEO) "PANDUAN VIDEO" else "ANIMASI TEKNIK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Bottom-Right: Looping Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "∞ LOOP",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Overlay Badge khusus jika sedang dalam mode REST PREVIEW (PRD 4.2.2) pada video kustom
            if (isRestPreview && (mediaType == MediaType.VIDEO || mediaType == MediaType.GIF) && !mediaUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 16.dp),
                        color = AccentRest.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PREVIEW: ${previewTitle ?: "Gerakan Berikutnya"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (isFullscreen) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
            modifier = modifier.clip(RoundedCornerShape(24.dp))
        ) {
            content()
        }
    }
}

/**
 * Dual-Buffer ExoPlayer: Memutar video saat ini sambil me-preload video gerakan berikutnya
 * pada 3 detik terakhir untuk transisi instan tanpa kedipan layar hitam (Crossfade 200ms).
 */
@OptIn(UnstableApi::class)
@Composable
private fun DualBufferExoPlayerView(
    activeUri: String,
    nextUri: String?,
    remainingSeconds: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Instance 1 & 2 (Maksimal 2 sesuai PRD NFR-3)
    val playerA = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    val playerB = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    // 0 = playerA aktif, 1 = playerB aktif
    var activePlayerIndex by remember { mutableIntStateOf(0) }
    var currentLoadedUri by remember { mutableStateOf("") }
    var preloadedUri by remember { mutableStateOf<String?>(null) }

    // Bersihkan kedua player saat keluar dari screen
    DisposableEffect(Unit) {
        onDispose {
            playerA.release()
            playerB.release()
        }
    }

    // Load active media item jika berubah
    LaunchedEffect(activeUri) {
        if (currentLoadedUri != activeUri) {
            val targetPlayer = if (activePlayerIndex == 0) playerA else playerB
            val parsedUri = if (activeUri.startsWith("/")) Uri.fromFile(File(activeUri)) else Uri.parse(activeUri)
            targetPlayer.setMediaItem(MediaItem.fromUri(parsedUri))
            targetPlayer.prepare()
            targetPlayer.playWhenReady = !isPaused
            currentLoadedUri = activeUri
        }
    }

    // Update play/pause state
    LaunchedEffect(isPaused) {
        val targetPlayer = if (activePlayerIndex == 0) playerA else playerB
        targetPlayer.playWhenReady = !isPaused
    }

    // PRELOAD buffer pada 3 detik terakhir (PRD 4.2.1)
    LaunchedEffect(remainingSeconds, nextUri) {
        if (remainingSeconds <= 3 && !nextUri.isNullOrBlank() && preloadedUri != nextUri) {
            val secondaryPlayer = if (activePlayerIndex == 0) playerB else playerA
            val parsedNextUri = if (nextUri.startsWith("/")) Uri.fromFile(File(nextUri)) else Uri.parse(nextUri)
            secondaryPlayer.setMediaItem(MediaItem.fromUri(parsedNextUri))
            secondaryPlayer.prepare()
            secondaryPlayer.playWhenReady = false
            preloadedUri = nextUri
        }
    }

    // Crossfade 200ms antara playerA dan playerB
    Crossfade(
        targetState = activePlayerIndex,
        animationSpec = tween(durationMillis = 200),
        modifier = modifier,
        label = "DualBufferCrossfade"
    ) { index ->
        val player = if (index == 0) playerA else playerB
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Komponen pemutar video asset looping ringan untuk background sinematik (Opener & Closer).
 * Menjamin pembebasan resource ExoPlayer secara bersih saat keluar dari layar.
 */
@Composable
private fun LoopingAssetVideoPlayer(
    assetUri: String,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
) {
    val context = LocalContext.current
    val player = remember(assetUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            setMediaItem(MediaItem.fromUri(Uri.parse(assetUri)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            try {
                player.stop()
                player.release()
            } catch (_: Exception) { }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.resizeMode = resizeMode
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        update = { playerView ->
            playerView.player = player
        },
        modifier = modifier
    )
}

// ─── SUBCOMPONENTS: HEADER, RING CONTENT, UP NEXT, CONTROLS ──────────────────

@Composable
private fun TopHeaderBar(
    routineName: String,
    exerciseIndex: Int,
    totalExercises: Int,
    remainingSeconds: Int,
    totalElapsedSeconds: Int,
    intervalType: IntervalType,
    accentColor: Color,
    onStop: () -> Unit,
    isGlassMode: Boolean = false,
    isBgmEnabled: Boolean = true,
    onToggleBgm: (() -> Unit)? = null
) {
    val overallProgress = if (totalExercises > 0) {
        ((exerciseIndex + 1).toFloat() / totalExercises.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val elapsedMin = totalElapsedSeconds / 60
    val elapsedSec = totalElapsedSeconds % 60
    val elapsedStr = String.format("%02d:%02d", elapsedMin, elapsedSec)
    val textShadow = if (isGlassMode) {
        Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(0f, 2f),
            blurRadius = 6f
        )
    } else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Overall Session Progress Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${exerciseIndex + 1} / $totalExercises GERAKAN",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isGlassMode) Color.White else accentColor,
                    letterSpacing = 1.sp,
                    shadow = textShadow
                )
            )
            Text(
                text = "Waktu Latihan: $elapsedStr",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (isGlassMode) Color.White.copy(alpha = 0.85f) else TextSecondary,
                    shadow = textShadow
                )
            )
        }

        LinearProgressIndicator(
            progress = { overallProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isGlassMode) 2.dp else 4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (isGlassMode) Color.White else accentColor,
            trackColor = if (isGlassMode) Color.White.copy(alpha = 0.25f) else SurfaceElevated
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flame_streak),
                        contentDescription = null,
                        tint = if (isGlassMode) Color.White else AccentWarmup,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = routineName.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            shadow = textShadow
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Live status tag with pulsing dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isGlassMode) Color.White else accentColor,
                        modifier = Modifier.size(7.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (intervalType == IntervalType.REST) "PEMULIHAN / RECOVERY" else "LATIHAN AKTIF",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = if (isGlassMode) Color.White.copy(alpha = 0.85f) else TextSecondary,
                            shadow = textShadow
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onToggleBgm != null) {
                    IconButton(
                        onClick = onToggleBgm,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isGlassMode) Color.Black.copy(alpha = 0.25f) else SurfaceCard,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isBgmEnabled) (if (isGlassMode) Color.White.copy(alpha = 0.40f) else accentColor.copy(alpha = 0.5f))
                                        else Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isBgmEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = if (isBgmEnabled) "Matikan Musik" else "Nyalakan Musik",
                            tint = if (isBgmEnabled) (if (isGlassMode) Color.White else accentColor) else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HoldToStopButton(onStopConfirmed = onStop, isGlassMode = isGlassMode)
            }
        }
    }
}

@Composable
private fun RingCenterContent(
    intervalType: IntervalType,
    accentColor: Color,
    remainingSeconds: Int,
    currentExercise: FlatExerciseItem,
    isPaused: Boolean,
    isCompact: Boolean = false,
    isGlassMode: Boolean = false
) {
    val textShadow = if (isGlassMode) {
        Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = Offset(0f, 2f),
            blurRadius = 8f
        )
    } else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
            color = if (isGlassMode) Color.White.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, if (isGlassMode) Color.White.copy(alpha = 0.25f) else accentColor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isCompact) 8.dp else 14.dp,
                    vertical = if (isCompact) 2.dp else 5.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        if (intervalType == IntervalType.REST) R.drawable.ic_pose_meditation
                        else R.drawable.ic_intensity_lightning
                    ),
                    contentDescription = null,
                    tint = if (isGlassMode) Color.White else accentColor,
                    modifier = Modifier.size(if (isCompact) 10.dp else 14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = when (intervalType) {
                        IntervalType.WORK -> "KERJA"
                        IntervalType.REST -> "ISTIRAHAT"
                        IntervalType.WARMUP -> "PEMANASAN"
                        IntervalType.COOLDOWN -> "PENDINGINAN"
                    },
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isCompact) 9.sp else 12.sp,
                        color = if (isGlassMode) Color.White else accentColor,
                        letterSpacing = if (isCompact) 1.sp else 2.sp,
                        shadow = textShadow
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 6.dp))

        val mm = remainingSeconds / 60
        val ss = remainingSeconds % 60
        val timeString = String.format("%02d:%02d", mm, ss)

        Text(
            text = timeString,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompact) (if (isGlassMode) 26.sp else 42.sp) else 62.sp,
                color = Color.White,
                fontFeatureSettings = "tnum",
                shadow = textShadow
            )
        )

        Text(
            text = currentExercise.title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompact) (if (isGlassMode) 11.sp else 14.sp) else 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                shadow = textShadow
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = if (isCompact) 6.dp else 24.dp)
        )

        if (currentExercise.currentSet != null) {
            Spacer(modifier = Modifier.height(if (isCompact) 1.dp else 4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isGlassMode) Color.White.copy(alpha = 0.15f) else SurfaceElevated
            ) {
                Text(
                    text = "SET ${currentExercise.currentSet} DARI ${currentExercise.totalSets}",
                    modifier = Modifier.padding(
                        horizontal = if (isCompact) 6.dp else 10.dp,
                        vertical = if (isCompact) 1.dp else 3.dp
                    ),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 8.sp else 11.sp,
                        color = if (isGlassMode) Color.White else accentColor,
                        letterSpacing = 0.8.sp,
                        shadow = textShadow
                    )
                )
            }
        }

        if (isPaused) {
            Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isGlassMode) Color.Black.copy(alpha = 0.45f) else AccentHighIntensity.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isGlassMode) Color.White.copy(alpha = 0.35f) else AccentHighIntensity.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "DIJEDA",
                    modifier = Modifier.padding(
                        horizontal = if (isCompact) 6.dp else 10.dp,
                        vertical = if (isCompact) 1.dp else 3.dp
                    ),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isCompact) 8.sp else 11.sp,
                        color = if (isGlassMode) Color.White else AccentHighIntensity,
                        letterSpacing = 1.sp,
                        shadow = textShadow
                    )
                )
            }
        }
    }
}

@Composable
private fun UpNextBanner(
    next: FlatExerciseItem,
    isGlassMode: Boolean = false
) {
    val guide = getExerciseGuide(next.title, next.type)
    val textShadow = if (isGlassMode) {
        Shadow(
            color = Color.Black.copy(alpha = 0.85f),
            offset = Offset(0f, 1f),
            blurRadius = 4f
        )
    } else null

    Surface(
        shape = RoundedCornerShape(if (isGlassMode) 20.dp else 16.dp),
        color = if (isGlassMode) Color.Black.copy(alpha = 0.25f) else SurfaceCard,
        border = BorderStroke(1.dp, if (isGlassMode) Color.White.copy(alpha = 0.18f) else SurfaceBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isGlassMode) Color.White.copy(alpha = 0.15f) else AccentWork.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(guide.iconRes),
                            contentDescription = null,
                            tint = if (isGlassMode) Color.White else Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "GERAKAN BERIKUTNYA",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isGlassMode) Color.White.copy(alpha = 0.75f) else TextSecondary,
                            letterSpacing = 0.8.sp,
                            shadow = textShadow
                        )
                    )
                    Text(
                        text = next.title,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            shadow = textShadow
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGlassMode) Color.White.copy(alpha = 0.20f) else SurfaceElevated
            ) {
                Text(
                    text = "${next.durationSeconds}s",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        shadow = textShadow
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomPlaybackControls(
    intervalType: IntervalType,
    accentColor: Color,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onAddRest: (() -> Unit)? = null,
    isGlassMode: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tombol khusus fase REST: +15 Detik dan Skip Rest (Large Ergonomic Pills)
        if (intervalType == IntervalType.REST && onAddRest != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddRest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGlassMode) Color.Black.copy(alpha = 0.35f) else Color(0xFF1E1E24),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.2.dp, if (isGlassMode) Color.White.copy(alpha = 0.25f) else Color(0xFF383844))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = if (isGlassMode) Color.White else AccentRest, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+15 Detik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkip()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGlassMode) Color.Black.copy(alpha = 0.40f) else Color(0xFF22222A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.2.dp, if (isGlassMode) Color.White.copy(alpha = 0.35f) else AccentRest.copy(alpha = 0.65f))
                ) {
                    Text("Mulai Sekarang", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = if (isGlassMode) Color.White else AccentRest, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Floating Control Bar (Compact & Sleek Obsidian Glass Dock)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isGlassMode) Color.Black.copy(alpha = 0.28f) else Color(0xEE161618),
            border = BorderStroke(1.dp, if (isGlassMode) Color.White.copy(alpha = 0.20f) else Color(0x403A3A42)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPrevious()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isGlassMode) Color.Black.copy(alpha = 0.30f) else Color(0xFF202026))
                        .then(
                            if (isGlassMode) Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Primary Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isGlassMode) Color.White else Color(0xFF262630))
                        .border(BorderStroke(if (isGlassMode) 1.dp else 2.dp, if (isGlassMode) Color.White else accentColor), CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Lanjutkan" else "Jeda",
                        tint = if (isGlassMode) Color.Black else accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next / Skip Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkip()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isGlassMode) Color.Black.copy(alpha = 0.30f) else Color(0xFF202026))
                        .then(
                            if (isGlassMode) Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Lewati",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─── 60FPS CIRCULAR PROGRESS RING (CANVAS WITH NEON PLAYHEAD BEAD) ───────────

@Composable
private fun CircularProgressRing(
    progress: Float,
    strokeWidth: Dp,
    accentColor: Color,
    trackColor: Color,
    remainingSeconds: Int,
    isGlassMode: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "RingProgressAnimation"
    )

    // Dynamic glow pulse in final 3 seconds (3, 2, 1 urgency)
    val isFinalCountdown = remainingSeconds in 1..3
    val countdownPulse by animateFloatAsState(
        targetValue = if (isFinalCountdown) 1.25f else 1.0f,
        animationSpec = if (isFinalCountdown) {
            infiniteRepeatable(
                animation = tween(250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(200)
        },
        label = "countdown_pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokePx = strokeWidth.toPx()
        val diameter = size.minDimension - strokePx
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(diameter, diameter)

        // 1. Ambient blurred halo glow behind the ring (only in non-glass mode, to keep video fully transparent)
        if (!isGlassMode) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isFinalCountdown) 0.35f else 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (radius + strokePx * 2f) * countdownPulse
                ),
                radius = (radius + strokePx * 2f) * countdownPulse,
                center = center
            )
        }

        // 2. Background Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // 3. Subtle Clock Pips (12 dots around the clock)
        for (i in 0 until 12) {
            val pipAngle = Math.toRadians((i * 30 - 90).toDouble())
            val pipRadius = radius
            val pipX = center.x + pipRadius * cos(pipAngle).toFloat()
            val pipY = center.y + pipRadius * sin(pipAngle).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = if (isGlassMode) 0.18f else 0.12f),
                radius = if (isGlassMode) 1.5.dp.toPx() else 2.dp.toPx(),
                center = Offset(pipX, pipY)
            )
        }

        // 4. Dynamic Active Progress Arc
        val sweepAngle = animatedProgress * 360f
        if (sweepAngle > 0f) {
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 5. Glowing Head Bead at the tip of progress
            val tipAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
            val tipX = center.x + radius * cos(tipAngleRad).toFloat()
            val tipY = center.y + radius * sin(tipAngleRad).toFloat()
            val tipOffset = Offset(tipX, tipY)

            // Outer diffused glow
            drawCircle(
                color = if (isGlassMode) Color.White.copy(alpha = 0.5f) else accentColor.copy(alpha = 0.5f),
                radius = strokePx * 0.75f,
                center = tipOffset
            )
            // Inner solid white core
            drawCircle(
                color = Color.White,
                radius = strokePx * 0.35f,
                center = tipOffset
            )
        }
    }
}

// ─── HOLD TO STOP BUTTON (1.5 SECONDS) ───────────────────────────────────────

@Composable
private fun HoldToStopButton(
    onStopConfirmed: () -> Unit,
    isGlassMode: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val startTime = System.currentTimeMillis()
            val holdDuration = 1500L
            while (isHolding) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / holdDuration).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopConfirmed()
                    break
                }
                delay(30)
            }
        } else {
            holdProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isGlassMode) Color.Black.copy(alpha = 0.35f) else Color(0xFF222228))
            .border(
                BorderStroke(
                    1.dp,
                    if (isHolding) AccentHighIntensity.copy(alpha = 0.8f)
                    else if (isGlassMode) Color.White.copy(alpha = 0.22f)
                    else Color(0xFF32323A)
                ),
                CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (holdProgress > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                drawArc(
                    color = AccentHighIntensity,
                    startAngle = -90f,
                    sweepAngle = holdProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Icon(
            Icons.Default.Stop,
            contentDescription = "Tahan untuk berhenti",
            tint = if (isHolding) AccentHighIntensity else if (isGlassMode) Color.White.copy(alpha = 0.9f) else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── PREPARING & COMPLETED VIEWS ─────────────────────────────────────────────

@Composable
private fun PreparingView(
    state: WorkoutTimerState.Preparing,
    onCancel: () -> Unit
) {
    val firstEx = state.firstExercise
    val guide = remember(firstEx?.title) {
        if (firstEx != null) getExerciseGuide(firstEx.title, firstEx.type) else null
    }

    // Warna aksen spesifik kategori untuk tag badge
    val categoryColor = remember(state.category) {
        when (state.category.lowercase().trim()) {
            "cardio", "sprint" -> Color(0xFFFF9500) // Energetic Orange
            "hiit", "tabata" -> AccentHighIntensity // Neon Crimson
            "core", "abs" -> Color(0xFFFF375F) // Hot Coral / Core Red
            "mobility", "stretch", "yoga" -> AccentCooldown // Electric Violet
            "strength", "calisthenics" -> AccentWork // Volt Green
            else -> AccentWork
        }
    }

    // Animasi sweeping progress arc untuk hitung mundur 3, 2, 1
    val countdownProgress = (state.countdownSeconds / 3f).coerceIn(0f, 1f)
    val animatedCountdownProgress by animateFloatAsState(
        targetValue = countdownProgress,
        animationSpec = tween(300, easing = LinearEasing),
        label = "countdown_ring_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. FULL-BLEED OPENER AI VIDEO BACKGROUND (Wanita mengedipkan mata & melambaikan tangan)
        LoopingAssetVideoPlayer(
            assetUri = com.workout.timer.data.catalog.AssetVideoResolver.WORKOUT_INTRO_URI,
            modifier = Modifier.fillMaxSize()
        )

        // 2. CRYSTALLINE LIQUID GLASS GRADIENT SCRIM (Tanpa sapuan warna pekat agar video coach natural)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.45f),
                        0.18f to Color.Black.copy(alpha = 0.15f),
                        0.55f to Color.Transparent,
                        0.78f to Color.Black.copy(alpha = 0.50f),
                        1.0f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )

        // 3. MAIN CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Category Tag Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.60f),
                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(categoryColor, CircleShape)
                            )
                            Text(
                                text = "${state.category.uppercase()} • GET READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = state.routineName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Batalkan",
                        tint = Color.White
                    )
                }
            }

            // Center: Floating Frosted Liquid Glass Countdown Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer countdown arc and track
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 4.dp.toPx()
                        val radius = (size.minDimension - strokePx) / 2f
                        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        val topLeft = androidx.compose.ui.geometry.Offset(
                            (size.width - arcSize.width) / 2f,
                            (size.height - arcSize.height) / 2f
                        )

                        // Base subtle glass track
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = radius,
                            style = Stroke(width = strokePx)
                        )

                        // Active sweep countdown arc
                        drawArc(
                            color = AccentWork,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedCountdownProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }

                    // Inner frosted glass orb
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .background(
                                Color.White.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.70f),
                                            Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = state.countdownSeconds,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.3f, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(180))) togetherWith
                                        (scaleOut(targetScale = 1.4f, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(180)))
                            },
                            label = "PreparingCountdown"
                        ) { count ->
                            Text(
                                text = count.toString(),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 96.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Subtitle tag under countdown
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.60f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = "SIAPKAN POSISI",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom: First Exercise Preview Card (Frosted Glass)
            if (firstEx != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E1E24).copy(alpha = 0.60f),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Exercise icon / thumbnail container
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (guide != null) {
                                Icon(
                                    painter = painterResource(id = guide.iconRes),
                                    contentDescription = null,
                                    tint = AccentWork,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flame_streak),
                                    contentDescription = null,
                                    tint = AccentWork,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GERAKAN PERTAMA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentWork,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${firstEx.durationSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = firstEx.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val subtitleText = when {
                                !firstEx.targetMuscle.isNullOrBlank() -> firstEx.targetMuscle
                                guide != null -> guide.targetMuscles
                                else -> "Fokus pada teknik & postur tubuh"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CompletedWorkoutView(
    state: WorkoutTimerState.Completed,
    onFinish: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. FULL-BLEED SELEBRASI AI VIDEO BACKGROUND (Wanita tersenyum bangga & dua jempol 100% utuh tanpa kepotong)
        LoopingAssetVideoPlayer(
            assetUri = com.workout.timer.data.catalog.AssetVideoResolver.WORKOUT_COMPLETE_URI,
            modifier = Modifier.fillMaxSize(),
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )

        // 2. MULTI-LAYER CINEMATIC GRADIENT SCRIM (AMOLED CONTRAST)
        // Area tengah (wajah & kedua jempol coach) bersih dan jelas, area bawah kontras tinggi untuk teks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.50f),
                        0.18f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.70f),
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        // 3. Ambient Volt Green Glow di bagian bawah
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentWork.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.85f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width / 2f, size.height * 0.85f)
            )
        }

        // 4. FLOATING LIQUID GLASS CONTENT (Scroll-safe, tidak akan pernah kepotong)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, AccentWork.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentWork, CircleShape)
                        )
                        Text(
                            text = "LUAR BIASA! • LATIHAN SELESAI",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentWork,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // Tengah: Area bebas visual agar gestur coach dua jempol terlihat penuh!
            Spacer(modifier = Modifier.height(260.dp))

            // Bawah: Floating Liquid Glass Card berisi ucapan, statistik, dan tombol aksi
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF141418).copy(alpha = 0.75f),
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            AccentWork.copy(alpha = 0.40f)
                        )
                    )
                ),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Headline & Nama Rutinitas
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LATIHAN SELESAI!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AccentWork,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.8.sp
                        )
                        Text(
                            text = state.routineName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Stat Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val mm = state.totalDurationSeconds / 60
                            val ss = state.totalDurationSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mm, ss),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "TOTAL WAKTU",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.totalExercisesCompleted}",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    color = AccentWork
                                )
                            )
                            Text(
                                text = "GERAKAN SELESAI",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Button Selesai
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentWork,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = "Kembali ke Beranda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPlayerView(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .background(SurfaceElevated, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = TextPrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = AccentWork,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Menyiapkan sesi latihan...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text("Batal", color = TextTertiary, fontSize = 13.sp)
            }
        }
    }
}
