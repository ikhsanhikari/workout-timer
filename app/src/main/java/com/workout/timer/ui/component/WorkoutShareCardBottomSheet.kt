package com.workout.timer.ui.component

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.workout.timer.data.catalog.AssetPhotoResolver
import com.workout.timer.data.media.WorkoutCardGenerator
import com.workout.timer.ui.theme.AccentWork
import com.workout.timer.ui.theme.SurfaceBorder
import com.workout.timer.ui.theme.SurfaceCard
import com.workout.timer.ui.theme.TextSecondary
import com.workout.timer.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bottom Sheet interaktif untuk membuat, melihat preview, serta membagikan atau menyimpan
 * Kartu Pencapaian Latihan (Workout Share Card) dengan background sinematik dari photo_ai.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutShareCardBottomSheet(
    routineName: String,
    totalDurationSeconds: Int,
    totalExercisesCompleted: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundOptions = remember { AssetPhotoResolver.SHARE_CARD_BACKGROUNDS }
    var selectedOption by remember { mutableStateOf(backgroundOptions.first()) }
    var isGenerating by remember { mutableStateOf(false) }

    val dateFormatted = remember {
        SimpleDateFormat("d MMM yyyy  •  HH:mm", Locale("id", "ID")).format(Date())
    }

    val mm = totalDurationSeconds / 60
    val ss = totalDurationSeconds % 60
    val durationFormatted = remember(totalDurationSeconds) { String.format("%02d:%02d", mm, ss) }
    val estCalories = remember(totalDurationSeconds) {
        max(20, (totalDurationSeconds * 7.5 / 60.0).roundToInt())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101014),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kartu Pencapaian Latihan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Bagikan ke Story Instagram, WhatsApp, atau simpan ke galeri",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── LIVE CARD PREVIEW (Rasio 9:16 Vertikal Estetik) ─────────────────
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(462.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(22.dp))
                    .background(Color(0xFF141418))
            ) {
                // Background Photo dari photo_ai
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(android.net.Uri.parse(selectedOption.uri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Latar Kartu",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                )

                // Bottom Gradient Scrim (Kontras tinggi untuk metrik)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.75f),
                                1f to Color.Black.copy(alpha = 0.95f)
                            )
                        )
                )

                // Card Overlay Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Branding & Tanggal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.60f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.30f))
                        ) {
                            Text(
                                text = "GASCOUNT  •  SELESAI",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Bottom Content: Headline, Nama Rutinitas, Stats & Slogan
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Kicker
                        Text(
                            text = "PENCAPAIAN LATIHAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            ),
                            color = AccentWork,
                            fontWeight = FontWeight.Bold
                        )

                        // Routine Name
                        Text(
                            text = routineName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 21.sp
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Divider Halus
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(Color.White.copy(alpha = 0.20f))
                        )

                        // 3 Kolom Metrik Esensial
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Waktu
                            Column {
                                Text(
                                    text = durationFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "WAKTU",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Gerakan
                            Column {
                                Text(
                                    text = "$totalExercisesCompleted",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    ),
                                    color = AccentWork
                                )
                                Text(
                                    text = "GERAKAN",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Kalori
                            Column {
                                Text(
                                    text = "~$estCalories",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "EST. KCAL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Divider Halus Bawah
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Footer Slogan
                        Text(
                            text = "Setiap detik berarti  •  Tracked with GasCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── PHOTO PICKER CAROUSEL ───────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PILIH FOTO LATAR (PHOTO AI)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(backgroundOptions) { option ->
                        val isSelected = option == selectedOption
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedOption = option
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) AccentWork else SurfaceBorder.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(android.net.Uri.parse(option.uri))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = option.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) Color.White else TextTertiary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── ACTION BUTTONS: BAGIKAN & SIMPAN ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Simpan ke Galeri
                OutlinedButton(
                    onClick = {
                        if (isGenerating) return@OutlinedButton
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isGenerating = true
                        coroutineScope.launch {
                            val success = withContext(Dispatchers.IO) {
                                val bitmap = WorkoutCardGenerator.generateCardBitmap(
                                    context = context,
                                    routineName = routineName,
                                    totalDurationSeconds = totalDurationSeconds,
                                    totalExercisesCompleted = totalExercisesCompleted,
                                    assetRelativePath = selectedOption.assetRelativePath
                                )
                                WorkoutCardGenerator.saveCardToGallery(context, bitmap, routineName)
                            }
                            isGenerating = false
                            if (success) {
                                Toast.makeText(context, "✅ Kartu latihan berhasil disimpan ke galeri!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan gambar ke galeri.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tombol Bagikan (Share Sheet)
                Button(
                    onClick = {
                        if (isGenerating) return@Button
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isGenerating = true
                        coroutineScope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                val bitmap = WorkoutCardGenerator.generateCardBitmap(
                                    context = context,
                                    routineName = routineName,
                                    totalDurationSeconds = totalDurationSeconds,
                                    totalExercisesCompleted = totalExercisesCompleted,
                                    assetRelativePath = selectedOption.assetRelativePath
                                )
                                WorkoutCardGenerator.saveCardToCache(context, bitmap)
                            }
                            isGenerating = false
                            if (uri != null) {
                                WorkoutCardGenerator.shareCard(context, uri, routineName)
                            } else {
                                Toast.makeText(context, "Gagal memproses kartu untuk dibagikan.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentWork,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bagikan Kartu",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

