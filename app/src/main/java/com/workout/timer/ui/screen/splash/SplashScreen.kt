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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workout.timer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Screen (Cinematic Visual Luxury Opening).
 *
 * Mengusung estetika AMOLED Black, Pure White Frosted Glass,
 * dan animasi visual kinetic emblem tanpa teks berlebih.
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
    val contentOffsetY = remember { Animatable(20f) }
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

        // Step 2: Content (Title & Subtitle) fades in smoothly
        delay(200)
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            contentOffsetY.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }

        // Step 3: Pure white progress bar fills smoothly over 1.6s
        progress.animateTo(1f, animationSpec = tween(1600, easing = LinearEasing))

        // Step 4: Finish & transition to Home or Onboarding
        delay(100)
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

            // Pure White Frosted Ambient Glow (Top & Center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.5f, canvasH * 0.35f),
                    radius = canvasW * 0.85f
                ),
                radius = canvasW * 0.85f,
                center = Offset(canvasW * 0.5f, canvasH * 0.35f)
            )

            // Pure White Subtle Ground Glow (Bottom)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(canvasW * 0.5f, canvasH * 0.85f),
                    radius = canvasW * 0.60f
                ),
                radius = canvasW * 0.60f,
                center = Offset(canvasW * 0.5f, canvasH * 0.85f)
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

            // ── Main Hero Section (Emblem + Sonar + Minimal Brand Title) ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // ── Hero Kinetic Emblem with Frosted Sonar Ripples ──────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    // Sonar Ripple 2 (Outer Pure White Frosted Ring)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseRing2Scale)
                            .alpha(pulseRing2Alpha)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.12f)
                                )
                            ),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }

                    // Sonar Ripple 1 (Inner Pure White Frosted Ring)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseRing1Scale)
                            .alpha(pulseRing1Alpha)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.22f)
                                )
                            ),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Frosted Glass Halo Backing
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                CircleShape
                            )
                    )

                    // GasCount Hero Emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_emblem),
                        contentDescription = "GasCount Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(175.dp)
                            .scale(logoScale.value * emblemBreathe)
                            .alpha(logoAlpha.value)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Pure Metallic Brand Typography ──────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(contentAlpha.value)
                        .padding(top = contentOffsetY.value.dp)
                ) {
                    Text(
                        text = "GASCOUNT",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 38.sp,
                            letterSpacing = 6.sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFF3F4F6),
                                    Color(0xFFD1D5DB)
                                )
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "INTERVAL & CIRCUIT TIMER",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 4.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    )
                }
            }

            // ── 3. Sleek Pure White Frosted Progress Line ───────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .padding(bottom = 36.dp)
                    .alpha(contentAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
