package com.workout.timer.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.backup.LocalBackupManager
import com.workout.timer.data.catalog.CatalogSyncManager
import com.workout.timer.data.catalog.SyncResult
import com.workout.timer.data.model.WorkoutReminderItem
import com.workout.timer.data.preferences.UserPreferencesRepository
import com.workout.timer.data.repository.WorkoutRepository
import com.workout.timer.util.WorkoutReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupManager: LocalBackupManager,
    private val syncManager: CatalogSyncManager,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val userPreferences = preferencesRepository.userPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPreferencesRepository.UserPreferences()
        )

    val customReminders: StateFlow<List<WorkoutReminderItem>> = preferencesRepository.customRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAnyReminderActive: StateFlow<Boolean> = customReminders
        .map { list -> list.any { it.isEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            preferencesRepository.saveNickname(nickname.trim())
        }
    }

    fun updatePrimaryGoal(goal: String) {
        viewModelScope.launch {
            preferencesRepository.savePrimaryGoal(goal)
        }
    }

    fun updateExperienceLevel(level: String) {
        viewModelScope.launch {
            preferencesRepository.saveExperienceLevel(level)
        }
    }

    fun updateThemeAccent(accent: String) {
        viewModelScope.launch {
            preferencesRepository.saveThemeAccent(accent)
        }
    }

    fun setVoiceCoachEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVoiceCoachEnabled(enabled)
        }
    }

    fun setBgmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBgmEnabled(enabled)
        }
    }

    fun setBeepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBeepEnabled(enabled)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticEnabled(enabled)
        }
    }

    fun setVideoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVideoEnabled(enabled)
        }
    }

    fun toggleReminder(context: Context, reminderId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map {
                if (it.id == reminderId) it.copy(isEnabled = isEnabled) else it
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null) {
                if (isEnabled) {
                    WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
                    _uiState.update { it.copy(snackbarMessage = "Pengingat '${item.label}' diaktifkan") }
                } else {
                    WorkoutReminderManager.cancelReminderItem(context.applicationContext, item.id)
                    _uiState.update { it.copy(snackbarMessage = "Pengingat '${item.label}' dinonaktifkan") }
                }
            }
        }
    }

    fun updateReminderTime(context: Context, reminderId: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map {
                if (it.id == reminderId) it.copy(hour = hour, minute = minute, isEnabled = true) else it
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null) {
                WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
                _uiState.update { it.copy(snackbarMessage = "Jam '${item.label}' diatur ke ${String.format(Locale.getDefault(), "%02d:%02d WIB", hour, minute)}") }
            }
        }
    }

    fun toggleReminderDay(context: Context, reminderId: String, dayOfWeek: Int) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map { item ->
                if (item.id == reminderId) {
                    val days = item.activeDays.toMutableList()
                    if (days.contains(dayOfWeek)) {
                        if (days.size > 1) days.remove(dayOfWeek)
                    } else {
                        days.add(dayOfWeek)
                    }
                    item.copy(activeDays = days.sorted())
                } else item
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null && item.isEnabled) {
                WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
            }
        }
    }

    fun addCustomReminder(context: Context, label: String, hour: Int, minute: Int, days: List<Int> = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)) {
        viewModelScope.launch {
            val newItem = WorkoutReminderItem(
                label = label.ifBlank { "Latihan" },
                hour = hour,
                minute = minute,
                activeDays = if (days.isEmpty()) listOf(Calendar.MONDAY) else days.sorted(),
                isEnabled = true
            )
            val currentList = customReminders.value
            val updated = currentList + newItem
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.scheduleReminderItem(context.applicationContext, newItem)
            _uiState.update { it.copy(snackbarMessage = "Pengingat '${newItem.label}' (${String.format(Locale.getDefault(), "%02d:%02d WIB", hour, minute)}) ditambahkan!") }
        }
    }

    fun updateFullReminder(context: Context, reminderId: String, label: String, hour: Int, minute: Int, days: List<Int>) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map { item ->
                if (item.id == reminderId) {
                    item.copy(
                        label = label.ifBlank { "Latihan" },
                        hour = hour,
                        minute = minute,
                        activeDays = if (days.isEmpty()) listOf(Calendar.MONDAY) else days.sorted(),
                        isEnabled = true
                    )
                } else item
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null) {
                WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
                _uiState.update { it.copy(snackbarMessage = "Pengingat '${item.label}' diperbarui!") }
            }
        }
    }

    fun deleteCustomReminder(context: Context, reminderId: String) {
        viewModelScope.launch {
            val currentList = customReminders.value
            if (currentList.size <= 1) {
                _uiState.update { it.copy(snackbarMessage = "Minimal harus ada 1 pengingat") }
                return@launch
            }
            val itemToDelete = currentList.find { it.id == reminderId }
            val updated = currentList.filterNot { it.id == reminderId }
            preferencesRepository.saveCustomReminders(updated)
            if (itemToDelete != null) {
                WorkoutReminderManager.cancelReminderItem(context.applicationContext, itemToDelete.id)
                _uiState.update { it.copy(snackbarMessage = "Pengingat '${itemToDelete.label}' dihapus") }
            }
        }
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReminderEnabled(enabled)
            val hour = preferencesRepository.reminderHourFlow.first()
            val minute = preferencesRepository.reminderMinuteFlow.first()
            if (enabled) {
                com.workout.timer.util.WorkoutReminderManager.scheduleDailyReminder(
                    context = context.applicationContext,
                    hour = hour,
                    minute = minute
                )
            } else {
                com.workout.timer.util.WorkoutReminderManager.cancelReminder(context.applicationContext)
            }
        }
    }

    fun setReminderTime(context: android.content.Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setReminderTime(hour, minute)
            preferencesRepository.setReminderEnabled(true)
            com.workout.timer.util.WorkoutReminderManager.scheduleDailyReminder(
                context = context.applicationContext,
                hour = hour,
                minute = minute
            )
        }
    }

    fun testReminderNotification(context: android.content.Context) {
        com.workout.timer.util.WorkoutReminderManager.testNotificationNow(context.applicationContext)
    }

    fun testReminderIn10Seconds(context: android.content.Context) {
        com.workout.timer.util.WorkoutReminderManager.scheduleTestReminderInSeconds(context.applicationContext, 10)
    }

    fun syncCatalog() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            when (val result = syncManager.sync()) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        snackbarMessage = "Katalog berhasil disinkronkan (${result.updatedCount} diperbarui)."
                    )
                }
                is SyncResult.AlreadyUpToDate -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        snackbarMessage = "Katalog sudah versi terbaru (v${result.currentVersion})."
                    )
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        snackbarMessage = "Gagal menyinkronkan: ${result.message}"
                    )
                }
                SyncResult.NoNetwork -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        snackbarMessage = "Tidak ada koneksi internet untuk sinkronisasi."
                    )
                }
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isExporting = true)
            when (val result = backupManager.exportBackup(uri)) {
                is LocalBackupManager.BackupResult.Success -> {
                    val sizeKb = result.fileSizeBytes / 1024
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        snackbarMessage = "Backup berhasil diekspor ($sizeKb KB)."
                    )
                }
                is LocalBackupManager.BackupResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        snackbarMessage = "Gagal mengekspor backup: ${result.message}"
                    )
                }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isImporting = true)
            when (val result = backupManager.importBackup(uri)) {
                is LocalBackupManager.RestoreResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        snackbarMessage = "Backup berhasil dipulihkan (${result.routineCount} rutinitas)!"
                    )
                }
                is LocalBackupManager.RestoreResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        snackbarMessage = "Gagal memulihkan backup: ${result.message}"
                    )
                }
            }
        }
    }

    fun reloadDefaultCatalog() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                workoutRepository.ensureStandardRoutinesLoaded()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    snackbarMessage = "40 rutinitas bawaan berhasil dimuat ulang."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    snackbarMessage = "Gagal memuat ulang: ${e.message}"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

