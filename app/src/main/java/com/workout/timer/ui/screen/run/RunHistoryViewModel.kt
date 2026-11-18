package com.workout.timer.ui.screen.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.model.RunSession
import com.workout.timer.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunHistoryUiState(
    val sessions: List<RunSession> = emptyList(),
    val totalDistanceKm: Double = 0.0,
    val totalCompletedRuns: Int = 0,
    val totalDurationSeconds: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class RunHistoryViewModel @Inject constructor(
    private val runRepository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunHistoryUiState())
    val uiState: StateFlow<RunHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            combine(
                runRepository.getAllSessions(),
                runRepository.getTotalDistanceMeters(),
                runRepository.getTotalCompletedRuns(),
                runRepository.getTotalDurationSeconds(),
                runRepository.getTotalCaloriesBurned()
            ) { sessions, distanceMeters, runsCount, durationSec, calories ->
                val distanceKm = (distanceMeters ?: 0.0) / 1000.0
                RunHistoryUiState(
                    sessions = sessions,
                    totalDistanceKm = distanceKm,
                    totalCompletedRuns = runsCount,
                    totalDurationSeconds = durationSec ?: 0,
                    totalCaloriesBurned = calories ?: 0,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            runRepository.deleteSession(sessionId)
        }
    }
}
