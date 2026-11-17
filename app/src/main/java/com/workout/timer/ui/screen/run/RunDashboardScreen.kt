package com.workout.timer.ui.screen.run

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.data.model.RunSession
import com.workout.timer.service.RunTrackingService
import com.workout.timer.service.location.RunStatsCalculator
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RunDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToActiveRun: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSummary: (Long) -> Unit,
    viewModel: RunDashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            // Langsung mulai lari
            val intent = RunTrackingService.startIntent(context)
            ContextCompat.startForegroundService(context, intent)
            onNavigateToActiveRun(0L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Lari & Jalan Santai",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = { viewModel.openDownloadSheet() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Unduh Peta Offline",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Banner jika ada sesi yang sedang berjalan di background
            AnimatedVisibility(visible = uiState.activeTrackingState.isTracking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable { onNavigateToActiveRun(uiState.activeTrackingState.sessionId) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.activeTrackingState.isPaused) "Sesi Sedang Dijeda" else "Sesi Sedang Berlangsung",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${RunStatsCalculator.formatDistanceKm(uiState.activeTrackingState.distanceMeters)} km • ${RunStatsCalculator.formatDuration(uiState.activeTrackingState.durationSeconds)}",
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Buka",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Stat Cards Grid (Total Jarak, Total Lari, Total Kalori)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    modifier = Modifier.weight(1.3f),
                    label = "Total Jarak",
                    value = String.format(Locale.US, "%.1f", uiState.totalDistanceKm),
                    unit = "km"
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    label = "Sesi Lari",
                    value = "${uiState.totalCompletedRuns}",
                    unit = "sesi"
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    label = "Kalori",
                    value = "${uiState.totalCaloriesBurned}",
                    unit = "kkal"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Utama "Mulai Lari"
            Button(
                onClick = {
                    if (hasLocationPermission) {
                        val intent = RunTrackingService.startIntent(context)
                        ContextCompat.startForegroundService(context, intent)
                        onNavigateToActiveRun(0L)
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Mulai Lari / Jalan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section Riwayat Terakhir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Terakhir",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (uiState.recentSessions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToHistory) {
                        Text(
                            text = "Lihat Semua",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.recentSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada catatan lari",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(uiState.recentSessions) { session ->
                        RecentRunCard(
                            session = session,
                            onClick = { onNavigateToSummary(session.id) }
                        )
                    }
                }
            }
        }

        // Offline Map Download Sheet
        if (uiState.showDownloadSheet) {
            OfflineMapDownloadSheet(
                onDismiss = { viewModel.closeDownloadSheet() },
                presetRegions = uiState.sortedRegions,
                currentLat = uiState.userLat ?: uiState.activeTrackingState.currentLat,
                currentLng = uiState.userLng ?: uiState.activeTrackingState.currentLng,
                downloadProgress = uiState.downloadProgress,
                cacheSizeMb = uiState.offlineCacheSizeMb,
                onDownloadRegion = { region ->
                    viewModel.downloadRegion(region)
                },
                onCancelDownload = { viewModel.cancelDownload() },
                onClearCache = { viewModel.clearOfflineCache() }
            )
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun RecentRunCard(
    session: RunSession,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }
    val dateStr = remember(session.startedAt) { dateFormat.format(Date(session.startedAt)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${RunStatsCalculator.formatDistanceKm(session.distanceMeters)} km",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${RunStatsCalculator.formatDuration(session.durationSeconds)} • Pace ${RunStatsCalculator.formatPace(session.avgPaceSecPerKm)}/km",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
