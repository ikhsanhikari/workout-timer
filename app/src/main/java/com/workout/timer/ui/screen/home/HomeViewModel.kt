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

/**
 * State UI untuk layar Home Dashboard.
 */
data class HomeUiState(
    val routines: List<WorkoutRoutine> = emptyList(),
    val categories: List<String> = emptyList(),
    val weeklyTotalMinutes: Int = 0,
    val weeklySessionCount: Int = 0,
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
    private val syncManager: CatalogSyncManager
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

    /** Ambil waktu awal minggu ini (Senin 00:00:00) sebagai Unix timestamp. */
    private val weekStartMs: Long get() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery.debounce(200), // Debounce 200ms untuk real-time search
        _selectedCategory,
        _selectedDuration,
        _selectedSort
    ) { query, category, duration, sort ->
        Triple(query to category, duration, sort)
    }.flatMapLatest { (queryCat, duration, sort) ->
        val (query, category) = queryCat
        repository.searchRoutines(
            keyword = query.ifBlank { null },
            category = category,
            minDuration = duration.minSec,
            maxDuration = duration.maxSec,
            sortBy = sort
        )
    }.combine(repository.observeAllCategories()) { routines, categories ->
        routines to categories
    }.combine(_snackbarMessage) { (routines, categories), snackbar ->
        Triple(routines, categories, snackbar)
    }.combine(_isSyncing) { (routines, categories, snackbar), isSyncing ->
        HomeUiState(
            routines = routines,
            categories = categories,
            isLoading = false,
            isSyncing = isSyncing,
            searchQuery = _searchQuery.value,
            selectedCategory = _selectedCategory.value,
            selectedDurationFilter = _selectedDuration.value,
            selectedSort = _selectedSort.value,
            snackbarMessage = snackbar
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

    fun restoreStandardRoutines() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreStandardRoutines()
            _snackbarMessage.value = "15 Preset latihan standar ilmiah berhasil dimuat!"
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
