package com.workout.timer.ui.screen.run

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workout.timer.R
import com.workout.timer.data.model.LocationPoint
import com.workout.timer.data.model.RunSession
import com.workout.timer.service.RunTrackingService
import com.workout.timer.service.location.RunStatsCalculator
import com.workout.timer.ui.screen.home.HomeBottomNavigation
import com.workout.timer.ui.screen.home.HomeTab
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
            val intent = RunTrackingService.startIntent(context)
            ContextCompat.startForegroundService(context, intent)
            onNavigateToActiveRun(0L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
    ) {
        // 1. Hero Background Image Pelari di kanan atas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_run_runner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient kiri ke kanan (agar teks di sisi kiri sangat jelas & terbaca)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xFF0A0A0C).copy(alpha = 0.96f),
                            0.45f to Color(0xFF0A0A0C).copy(alpha = 0.75f),
                            0.75f to Color(0xFF0A0A0C).copy(alpha = 0.35f),
                            1f to Color.Transparent
                        )
                    )
            )

            // Gradient atas ke bawah (menyatu halus ke latar belakang gelap)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF0A0A0C).copy(alpha = 0.3f),
                            0.55f to Color(0xFF0A0A0C).copy(alpha = 0.65f),
                            1f to Color(0xFF0A0A0C)
                        )
                    )
            )
        }

        // 2. Konten Halaman & Bottom Navigation
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                HomeBottomNavigation(
                    currentTab = HomeTab.BERANDA,
                    onTabSelected = { _ -> onNavigateBack() }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Lari",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Langkah kecil, perubahan besar",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Glowing Accent Pill
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF60A5FA))
                                    )
                                )
                        )
                    }

                    // Tombol Unduh Peta Offline
                    IconButton(
                        onClick = { viewModel.openDownloadSheet() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Unduh Peta Offline",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Banner Sesi Sedang Berlangsung (Jika Ada Sesi Aktif)
                AnimatedVisibility(visible = uiState.activeTrackingState.isTracking) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1B182B).copy(alpha = 0.9f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                            .clickable { onNavigateToActiveRun(uiState.activeTrackingState.sessionId) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color(0xFF6D28D9).copy(alpha = 0.2f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (uiState.activeTrackingState.isPaused) "Sesi Sedang Dijeda" else "Sesi Sedang Berlangsung",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${RunStatsCalculator.formatDistanceKm(uiState.activeTrackingState.distanceMeters)} km • ${RunStatsCalculator.formatDuration(uiState.activeTrackingState.durationSeconds)}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
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

                // 3 Kartu Ringkasan Metrik
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1.1f),
                        icon = Icons.Default.LocationOn,
                        iconColor = Color(0xFF818CF8),
                        label = "Total Jarak",
                        value = String.format(Locale.US, "%.1f", uiState.totalDistanceKm),
                        unit = "km"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer,
                        iconColor = Color(0xFFA78BFA),
                        label = "Sesi Lari",
                        value = "${uiState.totalCompletedRuns}",
                        unit = "sesi"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = Color(0xFFFB923C),
                        label = "Kalori",
                        value = "${uiState.totalCaloriesBurned}",
                        unit = "kkal"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tombol Utama "Mulai Lari / Jalan"
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
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mulai Lari / Jalan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section Riwayat Terakhir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Riwayat Terakhir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (uiState.recentSessions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onNavigateToHistory)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Lihat Semua",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            Spacer(modifier = Modifier.height(10.dp))
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
                            val routePoints = uiState.sessionRoutes[session.id] ?: emptyList()
                            RecentRunCard(
                                session = session,
                                routePoints = routePoints,
                                onClick = { onNavigateToSummary(session.id) }
                            )
                        }
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
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    unit: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141419).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 1.5.dp)
            )
        }
    }
}

@Composable
private fun RecentRunCard(
    session: RunSession,
    routePoints: List<LocationPoint>,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }
    val dateStr = remember(session.startedAt) { dateFormat.format(Date(session.startedAt)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141419).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Mini Map Route Snapshot
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 66.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181822))
            ) {
                RouteMiniMapCanvas(
                    points = routePoints,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${RunStatsCalculator.formatDistanceKm(session.distanceMeters)} km",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = RunStatsCalculator.formatDuration(session.durationSeconds),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text = " • ",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${RunStatsCalculator.formatPace(session.avgPaceSecPerKm)}/km",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateStr,
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Canvas kustom yang merender miniatur jalur rute GPS dengan warna neon ungu glowing,
 * lengkap dengan start point mint-green dan finish point ungu, seperti pada mockup referensi.
 */
@Composable
private fun RouteMiniMapCanvas(
    points: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Gambar grid halus background peta
        val gridColor = Color.White.copy(alpha = 0.04f)
        drawLine(gridColor, Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), strokeWidth = 1f)
        drawLine(gridColor, Offset(w * 0.66f, 0f), Offset(w * 0.66f, h), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)

        val neonPurple = Color(0xFFA78BFA)
        val glowPurple = Color(0xFF8B5CF6).copy(alpha = 0.38f)
        val startColor = Color(0xFF34D399) // Mint green start dot

        if (points.size >= 2) {
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLng = points.minOf { it.longitude }
            val maxLng = points.maxOf { it.longitude }

            val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)

            val padX = w * 0.16f
            val padY = h * 0.16f
            val usableW = w - padX * 2
            val usableH = h - padY * 2

            val path = Path()
            points.forEachIndexed { idx, pt ->
                val x = padX + (((pt.longitude - minLng) / lngSpan) * usableW).toFloat()
                val y = padY + (((maxLat - pt.latitude) / latSpan) * usableH).toFloat()
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // 1. Glow shadow
            drawPath(
                path,
                glowPurple,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // 2. Main line
            drawPath(
                path,
                neonPurple,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 3. Start Point Dot
            val firstPt = points.first()
            val startX = padX + (((firstPt.longitude - minLng) / lngSpan) * usableW).toFloat()
            val startY = padY + (((maxLat - firstPt.latitude) / latSpan) * usableH).toFloat()
            drawCircle(startColor, radius = 4.5.dp.toPx(), center = Offset(startX, startY))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(startX, startY))

            // 4. End Point Dot
            val lastPt = points.last()
            val endX = padX + (((lastPt.longitude - minLng) / lngSpan) * usableW).toFloat()
            val endY = padY + (((maxLat - lastPt.latitude) / latSpan) * usableH).toFloat()
            drawCircle(neonPurple, radius = 4.5.dp.toPx(), center = Offset(endX, endY))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(endX, endY))
        } else {
            // Default decorative curve yang elegan sesuai gambar referensi
            val path = Path().apply {
                moveTo(w * 0.18f, h * 0.72f)
                cubicTo(w * 0.35f, h * 0.68f, w * 0.45f, h * 0.35f, w * 0.82f, h * 0.28f)
            }
            drawPath(
                path,
                glowPurple,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path,
                neonPurple,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Start & End Dot
            drawCircle(startColor, radius = 4.5.dp.toPx(), center = Offset(w * 0.18f, h * 0.72f))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(w * 0.18f, h * 0.72f))
            drawCircle(neonPurple, radius = 4.5.dp.toPx(), center = Offset(w * 0.82f, h * 0.28f))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(w * 0.82f, h * 0.28f))
        }
    }
}
