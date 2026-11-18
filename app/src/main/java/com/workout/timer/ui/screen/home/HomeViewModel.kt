package com.workout.timer.ui.screen.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.backup.LocalBackupManager
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.repository.SortOption
import com.workout.timer.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

import com.workout.timer.data.catalog.CatalogSyncManager
import com.workout.timer.data.catalog.SyncResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * State UI untuk layar Home Dashboard.
 */
data class HomeUiState(
    val userNickname: String = "",
    val routines: List<WorkoutRoutine> = emptyList(),
    val favoriteRoutines: List<WorkoutRoutine> = emptyList(),
    val latestPerformedRoutine: WorkoutRoutine? = null,
    val todayScheduledRoutine: WorkoutRoutine? = null,
    val isTodayRestDay: Boolean = false,
    val categories: List<String> = emptyList(),
    val weeklyTotalMinutes: Int = 0,
    val weeklySessionCount: Int = 0,
    val weeklyTargetMinutes: Int = 60,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedDurationFilter: DurationFilter = DurationFilter.ALL,
    val selectedSort: SortOption = SortOption.CREATED_AT,
    val snackbarMessage: String? = null
)

/** Filter rentang durasi (F-03). */
enum class DurationFilter(val label: String, val minSec: Int?, val maxSec: Int?) {
    ALL("Semua", null, null),
    SHORT("< 10 Menit", null, 599),
    MEDIUM("10-20 Menit", 600, 1199),
    LONG("> 20 Menit", 1200, null)
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val backupManager: LocalBackupManager,
    private val syncManager: CatalogSyncManager,
    private val preferencesRepository: com.workout.timer.data.preferences.UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedDuration = MutableStateFlow(DurationFilter.ALL)
    private val _selectedSort = MutableStateFlow(SortOption.CREATED_AT)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureStandardRoutinesLoaded()
            // Silent background check saat aplikasi dibuka jika ada update dari server
            syncCatalog(isManual = false)
        }
    }

    private val gson = Gson()

    /** Ambil waktu awal minggu ini (Senin 00:00:00) sebagai Unix timestamp. */
    private val weekStartMs: Long get() {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    val allRoutinesFlow: Flow<List<WorkoutRoutine>> = repository.observeAllRoutinesForHistory()

    /**
     * Jadwal mingguan tersinkronisasi dinamis dari Preferences & DB.
     */
    val weeklyScheduleMapFlow: Flow<Map<Int, Long?>> = combine(
        preferencesRepository.weeklyScheduleJsonFlow,
        allRoutinesFlow
    ) { json, routines ->
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, Long?>>() {}.type
                val rawMap: Map<String, Long?>? = gson.fromJson(json, type)
                val parsed = rawMap?.mapKeys { it.key.toIntOrNull() ?: -1 }?.filterKeys { it in 1..7 }
                if (!parsed.isNullOrEmpty()) return@combine parsed
            } catch (e: Exception) {
                // fallback
            }
        }
        createDefaultScheduleMap(routines)
    }

    private fun createDefaultScheduleMap(routines: List<WorkoutRoutine>): Map<Int, Long?> {
        val fullBody = routines.find { it.category.equals("Full Body", ignoreCase = true) }?.id ?: routines.firstOrNull()?.id
        val hiit = routines.find { it.category.equals("HIIT", ignoreCase = true) }?.id ?: routines.getOrNull(1)?.id
        val core = routines.find { it.category.equals("Core", ignoreCase = true) }?.id ?: routines.getOrNull(2)?.id
        val strength = routines.find { it.category.equals("Strength", ignoreCase = true) }?.id ?: routines.getOrNull(3)?.id
        val mobility = routines.find { it.category.equals("Mobility", ignoreCase = true) }?.id ?: routines.getOrNull(4)?.id

        return mapOf(
            Calendar.MONDAY to fullBody,
            Calendar.TUESDAY to null,
            Calendar.WEDNESDAY to hiit,
            Calendar.THURSDAY to core,
            Calendar.FRIDAY to strength,
            Calendar.SATURDAY to mobility,
            Calendar.SUNDAY to null
        )
    }

    /** Target durasi mingguan (menit) dihitung dinamis dari total durasi rutinitas terjadwal */
    val weeklyTargetMinutes: StateFlow<Int> = combine(
        weeklyScheduleMapFlow,
        allRoutinesFlow
    ) { scheduleMap, routines ->
        val routinesMap = routines.associateBy { it.id }
        val targetSec = scheduleMap.values.filterNotNull().mapNotNull { routinesMap[it] }.sumOf { it.totalDurationSec }
        val minutes = targetSec / 60
        if (minutes > 0) minutes else 60
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 60)

    /** Rutinitas yang dijadwalkan khusus untuk HARI INI */
    val todayScheduledRoutine: StateFlow<WorkoutRoutine?> = combine(
        weeklyScheduleMapFlow,
        allRoutinesFlow
    ) { scheduleMap, routines ->
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val routineId = scheduleMap[today]
        if (routineId != null) routines.find { it.id == routineId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Status apakah hari ini adalah Hari Istirahat / Recovery */
    val isTodayRestDay: StateFlow<Boolean> = combine(
        weeklyScheduleMapFlow,
        todayScheduledRoutine
    ) { scheduleMap, routine ->
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        scheduleMap.containsKey(today) && scheduleMap[today] == null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val spotlightFlow = combine(
        repository.observeFavoriteRoutines(),
        repository.observePerformedRoutines()
    ) { favorites, performed ->
        favorites to performed.firstOrNull()
    }

    private data class FilterCriteria(
        val query: String = "",
        val category: String? = null,
        val duration: DurationFilter = DurationFilter.ALL,
        val sort: SortOption = SortOption.CREATED_AT
    )

    private val filterCriteriaFlow = combine(
        _searchQuery,
        _selectedCategory,
        _selectedDuration,
        _selectedSort
    ) { query, category, duration, sort ->
        FilterCriteria(query, category, duration, sort)
    }

    private val routinesFlow = filterCriteriaFlow
        .debounce { criteria ->
            if (criteria.query.isNotBlank()) 200L else 0L
        }
        .flatMapLatest { criteria ->
            repository.searchRoutines(
                keyword = criteria.query.ifBlank { null },
                category = criteria.category,
                minDuration = criteria.duration.minSec,
                maxDuration = criteria.duration.maxSec,
                sortBy = criteria.sort
            )
        }

    val uiState: StateFlow<HomeUiState> = combine(
        routinesFlow,
        repository.observeAllCategories(),
        filterCriteriaFlow,
        _isSyncing,
        _snackbarMessage
    ) { routines, categories, criteria, isSyncing, snackbar ->
        HomeUiState(
            routines = routines,
            favoriteRoutines = emptyList(),
            latestPerformedRoutine = null,
            todayScheduledRoutine = null,
            isTodayRestDay = false,
            categories = categories,
            isLoading = false,
            isSyncing = isSyncing,
            searchQuery = criteria.query,
            selectedCategory = criteria.category,
            selectedDurationFilter = criteria.duration,
            selectedSort = criteria.sort,
            snackbarMessage = snackbar
        )
    }.combine(spotlightFlow) { state, (favorites, latestPerformed) ->
        state.copy(
            favoriteRoutines = favorites,
            latestPerformedRoutine = latestPerformed
        )
    }.combine(preferencesRepository.userPreferencesFlow) { state, prefs ->
        state.copy(
            userNickname = prefs.nickname
        )
    }.combine(todayScheduledRoutine) { state, scheduled ->
        state.copy(
            todayScheduledRoutine = scheduled
        )
    }.combine(isTodayRestDay) { state, isRestDay ->
        state.copy(
            isTodayRestDay = isRestDay
        )
    }.combine(weeklyTargetMinutes) { state, targetMin ->
        state.copy(
            weeklyTargetMinutes = targetMin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun syncCatalog(isManual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            when (val result = syncManager.sync()) {
                is SyncResult.Success -> {
                    _snackbarMessage.value = "Katalog diperbarui: ${result.updatedCount} rutinitas baru/update (v${result.newVersion})"
                }
                is SyncResult.AlreadyUpToDate -> {
                    if (isManual) {
                        _snackbarMessage.value = "Katalog sudah versi terbaru (v${result.currentVersion})"
                    }
                }
                is SyncResult.Error -> {
                    if (isManual) {
                        _snackbarMessage.value = "Gagal menyinkronkan: ${result.message}"
                    }
                }
                is SyncResult.NoNetwork -> {
                    if (isManual) {
                        _snackbarMessage.value = "Perangkat offline. Memakai katalog lokal."
                    }
                }
            }
            _isSyncing.value = false
        }
    }

    // Weekly stats flow (ditangani terpisah karena butuh weekStartMs)
    val weeklyTotalMinutes: StateFlow<Int> = repository
        .observeWeeklyStats(weekStartMs).first
        .map { it / 60 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val weeklySessionCount: StateFlow<Int> = repository
        .observeWeeklyStats(weekStartMs).second
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val lastWeekStartMs: Long get() = weekStartMs - 7 * 24 * 3600 * 1000L

    val weeklyDeltaMinutes: StateFlow<Int> = combine(
        repository.observeWeeklyStats(weekStartMs).first,
        repository.observeTotalSecondsInRange(lastWeekStartMs, weekStartMs)
    ) { thisWeekSec, lastWeekSec ->
        (thisWeekSec / 60) - (lastWeekSec / 60)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Actions ────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onDurationFilterSelected(filter: DurationFilter) {
        _selectedDuration.value = filter
    }

    fun onSortSelected(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun toggleFavorite(routineId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(routineId)
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            _snackbarMessage.value = "Rutinitas dihapus"
        }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    // ── Multi & Custom Workout Reminder Flows & Controls ───────────────────

    val customReminders: StateFlow<List<com.workout.timer.data.model.WorkoutReminderItem>> = preferencesRepository.customRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isAnyReminderActive: StateFlow<Boolean> = customReminders
        .map { list -> list.any { it.isEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleReminder(context: android.content.Context, reminderId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map {
                if (it.id == reminderId) it.copy(isEnabled = isEnabled) else it
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null) {
                if (isEnabled) {
                    com.workout.timer.util.WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
                    _snackbarMessage.value = "Pengingat '${item.label}' diaktifkan"
                } else {
                    com.workout.timer.util.WorkoutReminderManager.cancelReminderItem(context.applicationContext, item.id)
                    _snackbarMessage.value = "Pengingat '${item.label}' dinonaktifkan"
                }
            }
        }
    }

    fun updateReminderTime(context: android.content.Context, reminderId: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map {
                if (it.id == reminderId) it.copy(hour = hour, minute = minute, isEnabled = true) else it
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null) {
                com.workout.timer.util.WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
                _snackbarMessage.value = "Jam '${item.label}' diatur ke ${String.format(java.util.Locale.getDefault(), "%02d:%02d WIB", hour, minute)}"
            }
        }
    }

    fun toggleReminderDay(context: android.content.Context, reminderId: String, dayOfWeek: Int) {
        viewModelScope.launch {
            val currentList = customReminders.value
            val updated = currentList.map { item ->
                if (item.id == reminderId) {
                    val days = item.activeDays.toMutableList()
                    if (days.contains(dayOfWeek)) {
                        if (days.size > 1) days.remove(dayOfWeek) // jangan kosong
                    } else {
                        days.add(dayOfWeek)
                    }
                    item.copy(activeDays = days.sorted())
                } else item
            }
            preferencesRepository.saveCustomReminders(updated)
            val item = updated.find { it.id == reminderId }
            if (item != null && item.isEnabled) {
                com.workout.timer.util.WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
            }
        }
    }

    fun addCustomReminder(
        context: android.content.Context,
        label: String,
        hour: Int,
        minute: Int,
        days: List<Int> = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
    ) {
        viewModelScope.launch {
            val newItem = com.workout.timer.data.model.WorkoutReminderItem(
                label = label.ifBlank { "Latihan" },
                hour = hour,
                minute = minute,
                activeDays = if (days.isEmpty()) listOf(Calendar.MONDAY) else days.sorted(),
                isEnabled = true
            )
            val currentList = customReminders.value
            val updated = currentList + newItem
            preferencesRepository.saveCustomReminders(updated)
            com.workout.timer.util.WorkoutReminderManager.scheduleReminderItem(context.applicationContext, newItem)
            _snackbarMessage.value = "Pengingat '${newItem.label}' (${String.format(java.util.Locale.getDefault(), "%02d:%02d WIB", hour, minute)}) berhasil ditambahkan!"
        }
    }

    fun updateFullReminder(
        context: android.content.Context,
        reminderId: String,
        label: String,
        hour: Int,
        minute: Int,
        days: List<Int>
    ) {
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
                com.workout.timer.util.WorkoutReminderManager.scheduleReminderItem(context.applicationContext, item)
                _snackbarMessage.value = "Pengingat '${item.label}' diperbarui!"
            }
        }
    }

    fun deleteCustomReminder(context: android.content.Context, reminderId: String) {
        viewModelScope.launch {
            val currentList = customReminders.value
            if (currentList.size <= 1) {
                _snackbarMessage.value = "Minimal harus ada 1 pengingat"
                return@launch
            }
            val itemToDelete = currentList.find { it.id == reminderId }
            val updated = currentList.filterNot { it.id == reminderId }
            preferencesRepository.saveCustomReminders(updated)
            if (itemToDelete != null) {
                com.workout.timer.util.WorkoutReminderManager.cancelReminderItem(context.applicationContext, itemToDelete.id)
                _snackbarMessage.value = "Pengingat '${itemToDelete.label}' dihapus"
            }
        }
    }

    fun testReminderNotification(context: android.content.Context) {
        com.workout.timer.util.WorkoutReminderManager.testNotificationNow(context.applicationContext)
        _snackbarMessage.value = "Notifikasi tes dikirim!"
    }

    fun restoreStandardRoutines() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreStandardRoutines()
            _snackbarMessage.value = "40 Preset latihan standar ilmiah berhasil dimuat!"
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            when (val result = backupManager.exportBackup(uri)) {
                is LocalBackupManager.BackupResult.Success -> {
                    val sizeKb = result.fileSizeBytes / 1024
                    _snackbarMessage.value = "Backup berhasil disimpan ($sizeKb KB)"
                }
                is LocalBackupManager.BackupResult.Error -> {
                    _snackbarMessage.value = "Gagal backup: ${result.message}"
                }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            when (val result = backupManager.importBackup(uri)) {
                is LocalBackupManager.RestoreResult.Success -> {
                    _snackbarMessage.value = "Berhasil memulihkan ${result.routineCount} rutinitas latihan!"
                }
                is LocalBackupManager.RestoreResult.Error -> {
                    _snackbarMessage.value = "Gagal restore: ${result.message}"
                }
            }
        }
    }
}
