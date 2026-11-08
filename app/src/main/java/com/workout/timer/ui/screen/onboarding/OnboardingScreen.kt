package com.workout.timer.ui.screen.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * OnboardingScreen — Pengalaman Masuk Pengguna Pertama (Visual Luxury Aesthetic).
 *
 * Mengutamakan kejelasan visual, minim teks esai, kartu metrik frosted glass,
 * dan tata letak modern tanpa beban kognitif berlebih.
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
                            if (isActive) Color.White else Color.White.copy(alpha = 0.15f)
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

// ─── STEP 1: WELCOME & VISUAL METRIC SHOWCASE ────────────────────────────────

@Composable
private fun OnboardingStep1Welcome(
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
        Spacer(modifier = Modifier.height(12.dp))

        // Center Hero Emblem in Glowing Frosted Glass Disc
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_emblem),
                contentDescription = "Pulse Emblem",
                modifier = Modifier
                    .size(98.dp)
                    .graphicsLayer {
                        translationY = floatY.dp.toPx()
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Concise Punchy Headlines
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Text(
                    text = "PULSE WORKOUT",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = Color.White,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Latihan Presisi",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Timer interval profesional tanpa distraksi.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3-Stat Visual Metric Frosted Card (Replaces paragraphs of text)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingStatColumn(
                    iconRes = R.drawable.ic_intensity_lightning,
                    value = "40+",
                    label = "Rutinitas"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
                OnboardingStatColumn(
                    iconRes = R.drawable.ic_cloud_backup_sleek,
                    value = "100%",
                    label = "Offline"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
                OnboardingStatColumn(
                    iconRes = R.drawable.ic_heart_bpm,
                    value = "0",
                    label = "Iklan"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CTA Button (Pure White Pill)
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Mulai →",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun OnboardingStatColumn(
    iconRes: Int,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.5.sp,
                color = Color.White
            )
        )
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.60f)
            )
        )
    }
}

// ─── STEP 2: GOALS & EXPERIENCE LEVEL (VISUAL 2x2 GRID) ─────────────────────

@Composable
private fun OnboardingStep2Goals(
    selectedGoal: OnboardingGoal,
    selectedLevel: ExperienceLevel,
    onSelectGoal: (OnboardingGoal) -> Unit,
    onSelectLevel: (ExperienceLevel) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Target Latihan",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pilih fokus utama Anda",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Visual 2x2 Grid for Focus Categories
            val row1 = listOf(
                OnboardingGoal.HIIT to ("HIIT" to "Bakar Lemak"),
                OnboardingGoal.STRENGTH to ("Kekuatan" to "Bentuk Otot")
            )
            val row2 = listOf(
                OnboardingGoal.MOBILITY to ("Mobilitas" to "Fleksibilitas"),
                OnboardingGoal.CORE to ("Core" to "Otot Inti")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row1.forEach { (goal, meta) ->
                        VisualGoalCard(
                            goal = goal,
                            title = meta.first,
                            tag = meta.second,
                            isSelected = goal == selectedGoal,
                            onSelect = { onSelectGoal(goal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row2.forEach { (goal, meta) ->
                        VisualGoalCard(
                            goal = goal,
                            title = meta.first,
                            tag = meta.second,
                            isSelected = goal == selectedGoal,
                            onSelect = { onSelectGoal(goal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Experience Level Selector
            Text(
                text = "PENGALAMAN",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.50f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExperienceLevel.entries.forEach { level ->
                    val isSelected = level == selectedLevel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { onSelectLevel(level) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level.label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.65f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next Button (Pure White Pill)
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Lanjut →",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun VisualGoalCard(
    goal: OnboardingGoal,
    title: String,
    tag: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 0.8.dp,
            if (isSelected) Color.White else Color.White.copy(alpha = 0.12f)
        ),
        modifier = modifier.clickable { onSelect() }
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isSelected) 0.16f else 0.08f))
                        .border(
                            BorderStroke(0.8.dp, Color.White.copy(alpha = if (isSelected) 0.35f else 0.16f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = goal.iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                )

                Text(
                    text = tag,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF07080B),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// ─── STEP 3: NICKNAME & VISUAL IDENTITY (ELEGANT & UNCLUTTERED) ─────────────

@Composable
private fun OnboardingStep3Identity(
    nickname: String,
    selectedAccent: ThemeAccent,
    onNicknameChange: (String) -> Unit,
    onSelectAccent: (ThemeAccent) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Profil Anda",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Personalisasi latihan harian",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Visual Center Avatar Preview with Ambient Glow
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                        .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (nickname.isNotBlank()) {
                        Text(
                            text = nickname.trim().take(1).uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = Color.White
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Compact Nickname Input Field
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    placeholder = {
                        Text(
                            text = "Nama Anda (opsional)",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 14.sp
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
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Accent Color Theme Selector (Large Color Spheres)
            Text(
                text = "TEMA AKSEN",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.50f),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val accentLabels = mapOf(
                ThemeAccent.VOLT_LIME to "Pure",
                ThemeAccent.ELECTRIC_CYAN to "Slate",
                ThemeAccent.AMBER to "Amber",
                ThemeAccent.NEON_ROSE to "Crimson"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeAccent.entries.forEach { accent ->
                    val isSelected = accent == selectedAccent
                    val swatchColor = Color(accent.colorHex)
                    val shortLabel = accentLabels[accent] ?: accent.label

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(
                            if (isSelected) 1.2.dp else 0.8.dp,
                            if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f)
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
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        BorderStroke(2.dp, if (isSelected) Color.White else Color.Transparent),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = shortLabel,
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

            // Micro Status Badge (Sleek, replaces paragraph quote)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Siap untuk sesi latihan pertama",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Complete Button (Pure White Pill)
        Button(
            onClick = onFinish,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Mulai Latihan →",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
