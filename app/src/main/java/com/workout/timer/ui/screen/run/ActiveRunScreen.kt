package com.workout.timer.ui.screen.run

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.service.location.RunStatsCalculator
import com.workout.timer.ui.component.OsmMapView

@Composable
fun ActiveRunScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    onFinishRun: (Long) -> Unit,
    viewModel: ActiveRunViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val isFollowUser by viewModel.isFollowUser.collectAsStateWithLifecycle()

    var showStopConfirmation by remember { mutableStateOf(false) }

    val currentCoordinates = remember(trackingState.currentLat, trackingState.currentLng) {
        val lat = trackingState.currentLat
        val lng = trackingState.currentLng
        if (lat != null && lng != null) Pair(lat, lng) else null
    }

    // Animasi denyut titik status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
    ) {
        // Lapisan Peta OSMDroid
        OsmMapView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f),
            currentLocation = currentCoordinates,
            routePoints = trackingState.routePoints,
            isFollowUser = isFollowUser
        )

        // Overlay Tombol Atas (Back & Re-center GPS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Minimize",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tombol Follow User / Re-center
            IconButton(
                onClick = { viewModel.toggleFollowUser() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFollowUser) Color.White else Color.Black.copy(alpha = 0.65f)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFollowUser) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    contentDescription = "Pusatkan Lokasi",
                    tint = if (isFollowUser) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Panel Bawah Frosted Glass (Statistik Live & Kontrol)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xFF101014).copy(alpha = 0.95f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag Indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Durasi Besar di Tengah
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (trackingState.isPaused) Color(0xFFFFB300)
                                else Color(0xFF00E676).copy(alpha = pulseAlpha)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (trackingState.isPaused) "DIJEDA" else "BERLANGSUNG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (trackingState.isPaused) Color(0xFFFFB300) else Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = RunStatsCalculator.formatDuration(trackingState.durationSeconds),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            }

            // Metrik 3 Kolom: Jarak, Pace, Kalori
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(
                    value = RunStatsCalculator.formatDistanceKm(trackingState.distanceMeters),
                    unit = "km",
                    label = "Jarak"
                )
                MetricDivider()
                MetricColumn(
                    value = RunStatsCalculator.formatPace(trackingState.currentPaceSecPerKm),
                    unit = "/km",
                    label = "Pace"
                )
                MetricDivider()
                MetricColumn(
                    value = "${trackingState.caloriesBurned}",
                    unit = "kkal",
                    label = "Kalori"
                )
            }

            // Tombol Aksi (Pause/Resume & Selesai)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Pause / Resume
                IconButton(
                    onClick = {
                        if (trackingState.isPaused) viewModel.resume(context)
                        else viewModel.pause(context)
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (trackingState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (trackingState.isPaused) "Lanjutkan" else "Jeda",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Tombol Stop / Selesai
                IconButton(
                    onClick = { showStopConfirmation = true },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Selesai",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Dialog Konfirmasi Selesai Lari
        if (showStopConfirmation) {
            AlertDialog(
                onDismissRequest = { showStopConfirmation = false },
                containerColor = Color(0xFF18181C),
                title = {
                    Text(
                        text = "Selesaikan Lari?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Sesi latihan ini akan disimpan ke riwayat Anda beserta peta rute yang telah ditempuh.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStopConfirmation = false
                            val currentId = trackingState.sessionId
                            viewModel.stop(context) { stoppedId ->
                                onFinishRun(if (stoppedId > 0L) stoppedId else currentId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Selesai & Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirmation = false }) {
                        Text("Batal", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@Composable
private fun MetricColumn(
    value: String,
    unit: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}
