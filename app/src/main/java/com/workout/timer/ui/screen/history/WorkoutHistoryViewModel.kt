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

data class WorkoutHistoryUiState(
    val sessionRecords: List<WorkoutSessionRecord> = emptyList(),
    val filteredRecords: List<WorkoutSessionRecord> = emptyList(),
    val allRoutines: List<WorkoutRoutine> = emptyList(),
    val filteredRoutines: List<WorkoutRoutine> = emptyList(),
    val selectedTab: HistoryTab = HistoryTab.COMPLETED,
    val searchQuery: String = "",
    val weeklyTotalMinutes: Int = 0,
    val weeklySessionCount: Int = 0,
    val allTimeTotalMinutes: Int = 0,
    val allTimeSessionCount: Int = 0,
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

                WorkoutHistoryUiState(
                    sessionRecords = records,
                    filteredRecords = filterRecords(records, query),
                    allRoutines = all,
                    filteredRoutines = filterRoutines(all, query),
                    selectedTab = currentTab,
                    searchQuery = query,
                    weeklyTotalMinutes = weeklyStats.first,
                    weeklySessionCount = weeklyStats.second,
                    allTimeTotalMinutes = allTimeStats.first,
                    allTimeSessionCount = allTimeStats.second,
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
                filteredRecords = filterRecords(current.sessionRecords, query),
                filteredRoutines = filterRoutines(current.allRoutines, query)
            )
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun deleteSessionRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteSessionRecord(recordId)
        }
    }

    private fun filterRecords(list: List<WorkoutSessionRecord>, query: String): List<WorkoutSessionRecord> {
        if (query.isBlank()) return list
        return list.filter {
            it.routineName.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }

    private fun filterRoutines(list: List<WorkoutRoutine>, query: String): List<WorkoutRoutine> {
        if (query.isBlank()) return list
        return list.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }

    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
