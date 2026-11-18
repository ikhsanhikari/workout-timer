package com.workout.timer.ui.component

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.workout.timer.data.model.WorkoutReminderItem
import com.workout.timer.util.WorkoutReminderManager
import java.util.Calendar
import java.util.Locale

/**
 * Kartu baris pengingat compact pure white frosted glass (~62dp).
 * Menampilkan jam digital, label latihan, ringkasan hari aktif, indikator edit, dan saklar ON/OFF.
 * Menggantikan kartu 3-baris yang ramai dengan 7 tombol lingkaran.
 */
@Composable
fun FrostedReminderCard(
    reminder: WorkoutReminderItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            1.dp,
            if (reminder.isEnabled) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Squircle Notification Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = if (reminder.isEnabled) 0.12f else 0.05f))
                    .border(
                        BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = if (reminder.isEnabled) 0.24f else 0.08f)
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reminder.isEnabled) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = if (reminder.isEnabled) Color.White else Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Info (Time & Subtitle: Label • Hari)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d WIB", reminder.hour, reminder.minute)
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = if (reminder.isEnabled) Color.White else Color.White.copy(alpha = 0.45f)
                )

                val daysSummary = WorkoutReminderManager.formatDaysSummary(reminder.activeDays)
                Text(
                    text = "${reminder.label} • $daysSummary",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = if (reminder.isEnabled) Color(0xFFA0A5B2) else Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Subtle Edit Pen Icon
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit Pengingat",
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )

            // Minimalist Monochrome Switch
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.40f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

/**
 * Dialog Edit Pengingat Tunggal / Multi Frosted Glass.
 * Tampil saat kartu pengingat diketuk, menyatukan ubah jam, label/kategori, pemilihan hari, dan tombol hapus.
 */
@Composable
fun EditReminderDialog(
    reminder: WorkoutReminderItem,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (label: String, hour: Int, minute: Int, days: List<Int>) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(reminder.label) }
    var selectedHour by remember { mutableIntStateOf(reminder.hour) }
    var selectedMinute by remember { mutableIntStateOf(reminder.minute) }
    var selectedDays by remember { mutableStateOf(reminder.activeDays.ifEmpty { listOf(Calendar.MONDAY) }) }

    val presetLabels = listOf("Latihan Pagi", "Latihan Sore", "HIIT Malam", "Mobilitas", "Cardio")

    val daysList = listOf(
        Calendar.MONDAY to "Sen",
        Calendar.TUESDAY to "Sel",
        Calendar.WEDNESDAY to "Rab",
        Calendar.THURSDAY to "Kam",
        Calendar.FRIDAY to "Jum",
        Calendar.SATURDAY to "Sab",
        Calendar.SUNDAY to "Min"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF13161D),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Title, Delete if canDelete, Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Pengingat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canDelete) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Hapus Pengingat",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Section 1: Jam Pengingat (Clickable Glass Tile)
                Surface(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                selectedHour = h
                                selectedMinute = m
                            },
                            selectedHour,
                            selectedMinute,
                            true
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d WIB", selectedHour, selectedMinute)
                                Text(
                                    text = timeFormatted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ketuk untuk ganti jam",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0A5B2)
                                )
                            }
                        }

                        Text(
                            text = "Ubah >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Section 2: Preset Chips & Text Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Label Pengingat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA0A5B2)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetLabels) { preset ->
                            val isSelected = label == preset
                            Surface(
                                onClick = { label = preset },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.12f))
                            ) {
                                Text(
                                    text = preset,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        placeholder = { Text("Nama Pengingat", fontSize = 12.5.sp, color = Color(0xFF6B7280)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 3: Hari Aktif (Senin - Minggu)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hari Pengingat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA0A5B2)
                        )
                        Text(
                            text = WorkoutReminderManager.formatDaysSummary(selectedDays),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        daysList.forEach { (calDay, shortName) ->
                            val isActive = selectedDays.contains(calDay)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.07f))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isActive) Color.White else Color.White.copy(alpha = 0.12f)
                                        ),
                                        CircleShape
                                    )
                                    .clickable {
                                        val mutableDays = selectedDays.toMutableList()
                                        if (isActive) {
                                            if (mutableDays.size > 1) mutableDays.remove(calDay)
                                        } else {
                                            mutableDays.add(calDay)
                                        }
                                        selectedDays = mutableDays.sorted()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isActive) Color.Black else Color.White.copy(alpha = 0.60f)
                                )
                            }
                        }
                    }
                }

                // Actions: Batal & Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color(0xFFA0A5B2))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onConfirm(label.ifBlank { "Latihan" }, selectedHour, selectedMinute, selectedDays)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

/**
 * Dialog Tambah Pengingat Baru Pure White Frosted Glass.
 */
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, hour: Int, minute: Int, days: List<Int>) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Latihan Sore") }
    var selectedHour by remember { mutableIntStateOf(17) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedDays by remember {
        mutableStateOf(
            listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY
            )
        )
    }

    val presetLabels = listOf("Latihan Pagi", "Latihan Sore", "HIIT Malam", "Mobilitas", "Cardio")

    val daysList = listOf(
        Calendar.MONDAY to "Sen",
        Calendar.TUESDAY to "Sel",
        Calendar.WEDNESDAY to "Rab",
        Calendar.THURSDAY to "Kam",
        Calendar.FRIDAY to "Jum",
        Calendar.SATURDAY to "Sab",
        Calendar.SUNDAY to "Min"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF13161D),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah Pengingat Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Section 1: Jam Pengingat
                Surface(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                selectedHour = h
                                selectedMinute = m
                            },
                            selectedHour,
                            selectedMinute,
                            true
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d WIB", selectedHour, selectedMinute)
                                Text(
                                    text = timeFormatted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ketuk untuk pilih jam",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0A5B2)
                                )
                            }
                        }

                        Text(
                            text = "Pilih >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Section 2: Preset Chips & Text Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Label Pengingat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA0A5B2)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetLabels) { preset ->
                            val isSelected = label == preset
                            Surface(
                                onClick = { label = preset },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.12f))
                            ) {
                                Text(
                                    text = preset,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        placeholder = { Text("Nama Pengingat", fontSize = 12.5.sp, color = Color(0xFF6B7280)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 3: Hari Aktif (Senin - Minggu)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hari Pengingat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFA0A5B2)
                        )
                        Text(
                            text = WorkoutReminderManager.formatDaysSummary(selectedDays),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        daysList.forEach { (calDay, shortName) ->
                            val isActive = selectedDays.contains(calDay)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.07f))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isActive) Color.White else Color.White.copy(alpha = 0.12f)
                                        ),
                                        CircleShape
                                    )
                                    .clickable {
                                        val mutableDays = selectedDays.toMutableList()
                                        if (isActive) {
                                            if (mutableDays.size > 1) mutableDays.remove(calDay)
                                        } else {
                                            mutableDays.add(calDay)
                                        }
                                        selectedDays = mutableDays.sorted()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isActive) Color.Black else Color.White.copy(alpha = 0.60f)
                                )
                            }
                        }
                    }
                }

                // Actions: Batal & Tambah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color(0xFFA0A5B2))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onConfirm(label.ifBlank { "Latihan" }, selectedHour, selectedMinute, selectedDays)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Tambah", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

