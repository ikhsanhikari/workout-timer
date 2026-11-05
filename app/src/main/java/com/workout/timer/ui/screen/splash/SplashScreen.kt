package com.workout.timer.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.workout.timer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Screen (F-01 Onboarding & Brand Hook).
 *
 * Menggabungkan desain OLED Black, Pure White Frosted Glass,
 * dan aksen athletic neon (Electric Cyan #00F5EA & Volt Lime #A4FF00).
 *
 * Fitur:
 * - Ambient radial glow canvas.
 * - Glowing sonar pulse ripple di belakang logo Kinetic 'W'.
 * - Hook copywriting motivasi yang membangkitkan semangat latihan.
 * - Micro-badges keunggulan aplikasi (40 Rutinitas, Timer Presisi, Audio Pelatih).
 * - Cybernetic neon energy loading bar dengan status dinamis.
 * - Tap to skip kapan saja untuk kenyamanan pengguna setia.
 */
@Composable
fun SplashScreen(
    onSplashFinished: (isOnboardingDone: Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // ── Animation Controllers ──────────────────────────────────────────────
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(30f) }
    val progress = remember { Animatable(0f) }

    // ── Infinite Ambient Pulsing Glow ──────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

    val pulseRing1Scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_1_scale"
    )

    val pulseRing1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_1_alpha"
    )

    val pulseRing2Scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_2_scale"
    )

    val pulseRing2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_2_alpha"
    )

    val emblemBreathe by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_breathe"
    )

    // ── Sequence Orchestration ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // Step 1: Logo entrance animation
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        }

        // Step 2: Content (Title, Hook, Badges) fades in with slight upward slide
        delay(220)
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            contentOffsetY.animateTo(0f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }

        // Step 3: Neon charging bar smoothly fills up over 1.7s
        progress.animateTo(1f, animationSpec = tween(1700, easing = LinearEasing))

        // Step 4: Finish & transition to Home or Onboarding
        delay(120)
        val isDone = viewModel.isOnboardingDone()
        onSplashFinished(isDone)
    }

    // ── Root UI Container ──────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080B))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Instantly skip on tap
                coroutineScope.launch {
                    val isDone = viewModel.isOnboardingDone()
                    onSplashFinished(isDone)
                }
            }
    ) {
        // ── 1. Ambient Canvas Radial Gradients ──────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Top-Center Electric Cyan Ambient Energy Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2E00F5EA),
                        Color(0x1200F5EA),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.5f, canvasH * 0.32f),
                    radius = canvasW * 0.85f
                ),
                radius = canvasW * 0.85f,
                center = Offset(canvasW * 0.5f, canvasH * 0.32f)
            )

            // Bottom Volt Lime Accent Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x22A4FF00),
                        Color(0x0BA4FF00),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.5f, canvasH * 0.78f),
                    radius = canvasW * 0.65f
                ),
                radius = canvasW * 0.65f,
                center = Offset(canvasW * 0.5f, canvasH * 0.78f)
            )
        }

        // ── 2. Center Content Column ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Main Hero Section (Emblem + Sonar + Title + Hook) ───────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // ── Hero Kinetic Emblem with Sonar Ripples ──────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    // Sonar Ripple 2 (Outer Cyan)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseRing2Scale)
                            .alpha(pulseRing2Alpha)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x0000F5EA),
                                    Color(0x4D00F5EA)
                                )
                            ),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // Sonar Ripple 1 (Inner Volt Lime)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseRing1Scale)
                            .alpha(pulseRing1Alpha)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x00A4FF00),
                                    Color(0x66A4FF00)
                                )
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Frosted Glass Halo Backing
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .clip(CircleShape)
                            .background(Color(0x0FFFFFFF))
                            .border(
                                BorderStroke(1.5.dp, Color(0x2BFFFFFF)),
                                CircleShape
                            )
                    )

                    // Hero Concept 3 Kinetic Pulse 'W' Emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_emblem),
                        contentDescription = "Pulse Workout Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(175.dp)
                            .scale(logoScale.value * emblemBreathe)
                            .alpha(logoAlpha.value)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Animated Brand Typography & Hook Section ────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(contentAlpha.value)
                        .padding(top = contentOffsetY.value.dp)
                ) {
                    // Brand Title with Gradient Shader
                    Text(
                        text = "PULSE",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp,
                            letterSpacing = 6.sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE0FFFF),
                                    Color(0xFF00F5EA)
                                )
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Athletic Category Tracker Subtitle
                    Text(
                        text = "INTERVAL & CIRCUIT TIMER",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 3.5.sp,
                            color = Color(0xB3FFFFFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── The "Hook" Motivational Card (Glassmorphic Pill) ────
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x14FFFFFF))
                            .border(
                                BorderStroke(1.dp, Color(0x33FFFFFF)),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "Setiap Detik Berharga. Lampaui Batasmu.",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Micro Feature Badges Row ────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SplashFeatureBadge(
                            icon = "🔥",
                            label = "40 Rutinitas"
                        )
                        SplashFeatureBadge(
                            icon = "⏱",
                            label = "Timer Presisi"
                        )
                        SplashFeatureBadge(
                            icon = "🔊",
                            label = "Audio Pelatih"
                        )
                    }
                }
            }

            // ── 3. Cybernetic Neon Progress Charging Section ────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .alpha(contentAlpha.value)
            ) {
                // Dynamic Status Text
                val statusText = when {
                    progress.value < 0.30f -> "Menyiapkan sistem latihan..."
                    progress.value < 0.65f -> "Sinkronisasi 40+ katalog rutinitas..."
                    progress.value < 0.95f -> "Memuat audio cue & panduan video..."
                    else -> "Siap tempur! ⚡"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        color = Color(0x99FFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "${(progress.value * 100).toInt()}%",
                        color = Color(0xFF00F5EA),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x1AFFFFFF))
                ) {
                    // Animated Electric Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00F5EA),
                                        Color(0xFFA4FF00)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tap to Skip Prompt
                Text(
                    text = "Ketuk di mana saja untuk lewati",
                    color = Color(0x4DFFFFFF),
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Micro glassmorphism badge untuk fitur utama aplikasi.
 */
@Composable
private fun SplashFeatureBadge(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(
                BorderStroke(0.8.dp, Color(0x24FFFFFF)),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = label,
                color = Color(0xCCFFFFFF),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

