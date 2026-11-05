package com.workout.timer.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.ui.theme.*

/**
 * OnboardingScreen — Pengalaman Masuk Pengguna Pertama (Apple Fitness+ Luxury Aesthetic).
 *
 * Menggabungkan desain OLED Black, Pure White Frosted Glass, dan aksen neon dinamis.
 *
 * 3 Langkah Personalisasi:
 * 1. The Hook & Core Philosophy (Nol Iklan, 100% Offline, Presisi Milidetik)
 * 2. Kurasi Target & Level (HIIT, Strength, Mobility, Core)
 * 3. Identitas & Aksen Visual (Nama Panggilan & Pilihan Aksen Warna Energi)
 */
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val selectedGoal by viewModel.selectedGoal.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()

    val currentAccentColor = remember(selectedAccent, selectedGoal, currentStep) {
        if (currentStep == 1) Color(selectedGoal.accentHex)
        else Color(selectedAccent.colorHex)
    }

    // Ambient radial pulse animation (Subtle Pure White Frosted Depth)
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_ambient")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── 1. Living Dynamic Radial Glow Canvas ────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = pulseAlpha),
                        Color.White.copy(alpha = pulseAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.5f, canvasH * 0.28f),
                    radius = canvasW * 0.85f
                ),
                radius = canvasW * 0.85f,
                center = Offset(canvasW * 0.5f, canvasH * 0.28f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = pulseAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.8f, canvasH * 0.85f),
                    radius = canvasW * 0.70f
                ),
                radius = canvasW * 0.70f,
                center = Offset(canvasW * 0.8f, canvasH * 0.85f)
            )
        }

        // ── 2. Main Content Flow ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar (Back, Progress Segments, Skip)
            OnboardingTopBar(
                currentStep = currentStep,
                accentColor = currentAccentColor,
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.prevStep()
                },
                onSkip = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.skipOnboarding(onOnboardingFinished)
                }
            )

            // Animated Screen Step Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width / 2 } + fadeIn(tween(350)))
                                .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut(tween(250)))
                        } else {
                            (slideInHorizontally { width -> -width / 2 } + fadeIn(tween(350)))
                                .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut(tween(250)))
                        }
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        0 -> OnboardingStep1Welcome(
                            accentColor = currentAccentColor,
                            onNext = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.nextStep()
                            }
                        )
                        1 -> OnboardingStep2Goals(
                            selectedGoal = selectedGoal,
                            selectedLevel = selectedLevel,
                            onSelectGoal = { goal ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectGoal(goal)
                            },
                            onSelectLevel = { level ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectLevel(level)
                            },
                            onNext = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.nextStep()
                            }
                        )
                        2 -> OnboardingStep3Identity(
                            nickname = nickname,
                            selectedAccent = selectedAccent,
                            onNicknameChange = viewModel::setNickname,
                            onSelectAccent = { accent ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectAccent(accent)
                            },
                            onFinish = {
                                focusManager.clearFocus()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.completeOnboarding(onOnboardingFinished)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── TOP BAR WITH PROGRESS INDICATORS ────────────────────────────────────────

@Composable
private fun OnboardingTopBar(
    currentStep: Int,
    accentColor: Color,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Button
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (currentStep > 0) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3 Segmented Progress Bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val isActive = index <= currentStep
                val isCurrent = index == currentStep
                val animatedWidth by animateDpAsState(
                    targetValue = if (isCurrent) 28.dp else 14.dp,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    label = "progress_segment_w"
                )

                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(animatedWidth)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) accentColor else Color.White.copy(alpha = 0.15f)
                        )
                )
            }
        }

        // Skip Button
        TextButton(
            onClick = onSkip,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Lewati",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            )
        }
    }
}

// ─── STEP 1: WELCOME & CORE PHILOSOPHY ──────────────────────────────────────

@Composable
private fun OnboardingStep1Welcome(
    accentColor: Color,
    onNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step1_emblem")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Center Hero Emblem in Glowing Frosted Glass Disc
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
                .border(BorderStroke(1.2.dp, accentColor.copy(alpha = 0.40f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_emblem),
                contentDescription = "Emblem",
                modifier = Modifier
                    .size(95.dp)
                    .graphicsLayer {
                        translationY = floatY.dp.toPx()
                    }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Headlines
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "INTERVAL TIMER GENERASI BARU",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = accentColor,
                        letterSpacing = 1.2.sp
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Latihan Mandiri Tanpa Hambatan",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Presisi milidetik. Nol gangguan iklan. Dirancang untuk fokus latihan fisik maksimal tanpa perlu koneksi internet.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.70f),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3 Frosted Feature Badges
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeaturePill(
                iconRes = R.drawable.ic_intensity_lightning,
                title = "40+ Program Terkurasi",
                desc = "Mulai dari HIIT kilat, pembentukan otot, hingga yoga",
                accentColor = accentColor
            )
            FeaturePill(
                iconRes = R.drawable.ic_cloud_backup_sleek,
                title = "100% Privat & Offline",
                desc = "Data tersimpan di perangkat Anda tanpa wajib login",
                accentColor = accentColor
            )
            FeaturePill(
                iconRes = R.drawable.ic_heart_bpm,
                title = "Player 60fps & Audio Ducking",
                desc = "Hitungan timer akurat tanpa mematikan musik Anda",
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CTA Button
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Mulai Personalisasi →",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FeaturePill(
    iconRes: Int,
    title: String,
    desc: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White,
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
                    text = desc,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                )
            }
        }
    }
}

// ─── STEP 2: GOALS & EXPERIENCE LEVEL ────────────────────────────────────────

@Composable
private fun OnboardingStep2Goals(
    selectedGoal: OnboardingGoal,
    selectedLevel: ExperienceLevel,
    onSelectGoal: (OnboardingGoal) -> Unit,
    onSelectLevel: (ExperienceLevel) -> Unit,
    onNext: () -> Unit
) {
    val activeAccent = Color(selectedGoal.accentHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Pilih Fokus Latihan Anda",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rekomendasi 40 program latihan akan otomatis diprioritaskan sesuai target ini.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4 Focus Target Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OnboardingGoal.entries.forEach { goal ->
                    val isSelected = goal == selectedGoal

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(
                            if (isSelected) 1.2.dp else 0.8.dp,
                            if (isSelected) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGoal(goal) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = goal.iconRes),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = goal.subtitle,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF07080B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Experience Level Selector
            Text(
                text = "LEVEL PENGALAMAN SAAT INI",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.50f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExperienceLevel.entries.forEach { level ->
                    val isSelected = level == selectedLevel
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(
                            if (isSelected) 1.2.dp else 0.8.dp,
                            if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectLevel(level) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = level.label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next Button
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activeAccent,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Lanjut: Identitas Visual →",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── STEP 3: NICKNAME & VISUAL IDENTITY ─────────────────────────────────────

@Composable
private fun OnboardingStep3Identity(
    nickname: String,
    selectedAccent: ThemeAccent,
    onNicknameChange: (String) -> Unit,
    onSelectAccent: (ThemeAccent) -> Unit,
    onFinish: () -> Unit
) {
    val activeAccent = Color(selectedAccent.colorHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Sentuhan Identitas",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Untuk menyapa dan memacu semangat Anda di layar utama setiap hari.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Nickname Input Box
            Text(
                text = "NAMA PANGGILAN ANDA",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.50f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                placeholder = {
                    Text(
                        text = "Contoh: Alex, Ryan, Sarah...",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    focusedBorderColor = Color.White.copy(alpha = 0.50f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onFinish() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Accent Color Theme Selector
            Text(
                text = "WARNA ENERGI TEMA",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.50f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Pilih aksen cahaya neon yang paling memicu motivasi latihan Anda.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeAccent.entries.forEach { accent ->
                    val isSelected = accent == selectedAccent
                    val swatchColor = Color(accent.colorHex)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(
                            if (isSelected) 1.2.dp else 0.8.dp,
                            if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectAccent(accent) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        BorderStroke(2.dp, if (isSelected) Color.White else Color.Transparent),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = accent.label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Motivational Frosted Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🔥", fontSize = 24.sp)
                    Text(
                        text = "Kunci konsistensi bukan motivasi instan, melainkan kebiasaan kecil yang dilakukan setiap hari.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.80f),
                            lineHeight = 17.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Complete Button
        Button(
            onClick = onFinish,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activeAccent,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Masuk ke Arena Latihan 🚀",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

