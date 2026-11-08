package com.workout.timer.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.model.WorkoutSessionRecord
import com.workout.timer.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class HistoryTab {
    COMPLETED,
    ALL
}

data class DayActivityUiModel(
    val dayOfWeek: Int,
    val dayNameShort: String,
    val dateNumber: Int,
    val totalMinutes: Int,
    val sessionCount: Int,
    val isToday: Boolean
)

data class CategoryShareUiModel(
    val category: String,
    val sessionCount: Int,
    val totalMinutes: Int,
    val percentage: Int
)

data class WorkoutHistoryUiState(
    val sessionRecords: List<WorkoutSessionRecord> = emptyList(),
    val filteredRecords: List<WorkoutSessionRecord> = emptyList(),
    val allRoutines: List<WorkoutRoutine> = emptyList(),
    val filteredRoutines: List<WorkoutRoutine> = emptyList(),
    val selectedTab: HistoryTab = HistoryTab.COMPLETED,
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val weeklyTotalMinutes: Int = 0,
    val weeklySessionCount: Int = 0,
    val allTimeTotalMinutes: Int = 0,
    val allTimeSessionCount: Int = 0,
    val weeklyDaysActivity: List<DayActivityUiModel> = emptyList(),
    val categoryBreakdown: List<CategoryShareUiModel> = emptyList(),
    val activeDaysCountThisWeek: Int = 0,
    val selectedRecordForDetail: WorkoutSessionRecord? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    init {
        val weekStartMs = getStartOfWeekTimestamp()

        val (weeklySecondsFlow, weeklySessionsFlow) = repository.observeWeeklyStats(weekStartMs)
        val (allTimeSecondsFlow, allTimeSessionsFlow) = repository.observeAllTimeStats()

        val weeklyStatsFlow = combine(weeklySecondsFlow, weeklySessionsFlow) { sec, count ->
            (sec / 60) to count
        }
        val allTimeStatsFlow = combine(allTimeSecondsFlow, allTimeSessionsFlow) { sec, count ->
            (sec / 60) to count
        }

        viewModelScope.launch {
            combine(
                repository.observeSessionRecords(),
                repository.observeAllRoutinesForHistory(),
                weeklyStatsFlow,
                allTimeStatsFlow
            ) { records, all, weeklyStats, allTimeStats ->
                val currentTab = _uiState.value.selectedTab
                val query = _uiState.value.searchQuery
                val catFilter = _uiState.value.selectedCategoryFilter

                val weeklyActivity = calculateWeeklyActivity(records)
                val activeDaysCount = weeklyActivity.count { it.sessionCount > 0 }
                val categoryBreakdown = calculateCategoryBreakdown(records)

                WorkoutHistoryUiState(
                    sessionRecords = records,
                    filteredRecords = filterRecords(records, query, catFilter),
                    allRoutines = all,
                    filteredRoutines = filterRoutines(all, query, catFilter),
                    selectedTab = currentTab,
                    searchQuery = query,
                    selectedCategoryFilter = catFilter,
                    weeklyTotalMinutes = weeklyStats.first,
                    weeklySessionCount = weeklyStats.second,
                    allTimeTotalMinutes = allTimeStats.first,
                    allTimeSessionCount = allTimeStats.second,
                    weeklyDaysActivity = weeklyActivity,
                    categoryBreakdown = categoryBreakdown,
                    activeDaysCountThisWeek = activeDaysCount,
                    selectedRecordForDetail = _uiState.value.selectedRecordForDetail,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun selectTab(tab: HistoryTab) {
        _uiState.update { current ->
            current.copy(selectedTab = tab)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredRecords = filterRecords(current.sessionRecords, query, current.selectedCategoryFilter),
                filteredRoutines = filterRoutines(current.allRoutines, query, current.selectedCategoryFilter)
            )
        }
    }

    fun onCategoryFilterSelected(category: String?) {
        _uiState.update { current ->
            val newFilter = if (current.selectedCategoryFilter == category) null else category
            current.copy(
                selectedCategoryFilter = newFilter,
                filteredRecords = filterRecords(current.sessionRecords, current.searchQuery, newFilter),
                filteredRoutines = filterRoutines(current.allRoutines, current.searchQuery, newFilter)
            )
        }
    }

    fun selectRecordForDetail(record: WorkoutSessionRecord?) {
        _uiState.update { it.copy(selectedRecordForDetail = record) }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiState.update { it.copy(selectedRecordForDetail = null) }
        }
    }

    fun deleteSessionRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteSessionRecord(recordId)
            _uiState.update {
                if (it.selectedRecordForDetail?.id == recordId) {
                    it.copy(selectedRecordForDetail = null)
                } else it
            }
        }
    }

    private fun filterRecords(
        list: List<WorkoutSessionRecord>,
        query: String,
        categoryFilter: String?
    ): List<WorkoutSessionRecord> {
        return list.filter { record ->
            val matchesQuery = query.isBlank() ||
                record.routineName.contains(query, ignoreCase = true) ||
                record.category.contains(query, ignoreCase = true)
            val matchesCategory = categoryFilter == null ||
                record.category.equals(categoryFilter, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    private fun filterRoutines(
        list: List<WorkoutRoutine>,
        query: String,
        categoryFilter: String?
    ): List<WorkoutRoutine> {
        return list.filter { routine ->
            val matchesQuery = query.isBlank() ||
                routine.name.contains(query, ignoreCase = true) ||
                routine.category.contains(query, ignoreCase = true)
            val matchesCategory = categoryFilter == null ||
                routine.category.equals(categoryFilter, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    private fun calculateWeeklyActivity(records: List<WorkoutSessionRecord>): List<DayActivityUiModel> {
        val nowCal = Calendar.getInstance()
        val todayDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)

        // Cari Senin minggu ini jam 00:00:00.000
        val mondayCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStartMs = mondayCal.timeInMillis

        // Minggu minggu ini jam 23:59:59.999
        val weekEndMs = weekStartMs + (7L * 24 * 60 * 60 * 1000L)

        val thisWeekRecords = records.filter { it.completedAt in weekStartMs until weekEndMs }

        // Urutan standar Indonesia: Senin s/d Minggu
        val orderedDays = listOf(
            Calendar.MONDAY to "Sen",
            Calendar.TUESDAY to "Sel",
            Calendar.WEDNESDAY to "Rab",
            Calendar.THURSDAY to "Kam",
            Calendar.FRIDAY to "Jum",
            Calendar.SATURDAY to "Sab",
            Calendar.SUNDAY to "Min"
        )

        return orderedDays.mapIndexed { index, (dayOfWeek, shortName) ->
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = weekStartMs + (index * 24L * 60 * 60 * 1000L)
            }
            val dateNumber = dayCal.get(Calendar.DAY_OF_MONTH)

            val dayRecords = thisWeekRecords.filter { record ->
                val rCal = Calendar.getInstance().apply { timeInMillis = record.completedAt }
                rCal.get(Calendar.DAY_OF_WEEK) == dayOfWeek
            }

            val totalSec = dayRecords.sumOf { it.durationSeconds }
            val mins = (totalSec / 60).coerceAtLeast(if (dayRecords.isNotEmpty() && totalSec > 0) 1 else 0)

            DayActivityUiModel(
                dayOfWeek = dayOfWeek,
                dayNameShort = shortName,
                dateNumber = dateNumber,
                totalMinutes = mins,
                sessionCount = dayRecords.size,
                isToday = dayOfWeek == todayDayOfWeek
            )
        }
    }

    private fun calculateCategoryBreakdown(records: List<WorkoutSessionRecord>): List<CategoryShareUiModel> {
        if (records.isEmpty()) return emptyList()
        val totalSec = records.sumOf { it.durationSeconds }.coerceAtLeast(1)
        val grouped = records.groupBy { it.category }

        return grouped.map { (category, catRecords) ->
            val catSec = catRecords.sumOf { it.durationSeconds }
            val catMins = (catSec / 60).coerceAtLeast(1)
            val percent = (((catSec.toDouble() / totalSec) * 100) + 0.5).toInt()
            CategoryShareUiModel(
                category = category,
                sessionCount = catRecords.size,
                totalMinutes = catMins,
                percentage = percent
            )
        }.sortedByDescending { it.totalMinutes }
    }

    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
