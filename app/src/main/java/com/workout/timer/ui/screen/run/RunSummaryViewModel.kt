package com.workout.timer.ui.screen.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.model.RunSession
import com.workout.timer.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunSummaryUiState(
    val session: RunSession? = null,
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RunSummaryViewModel @Inject constructor(
    private val runRepository: RunRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _uiState = MutableStateFlow(RunSummaryUiState())
    val uiState: StateFlow<RunSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSessionData()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val session = if (sessionId > 0L) {
                runRepository.getSessionById(sessionId)
            } else {
                // Ambil sesi terakhir jika id = 0
                runRepository.getActiveSession()
            }

            val targetId = session?.id ?: sessionId
            val dbPoints = if (targetId > 0L) {
                runRepository.getPointsForSession(targetId).map { Pair(it.latitude, it.longitude) }
            } else emptyList()

            // Jika DB masih dalam proses sinkronisasi, ambil dari in-memory cache
            val points = if (dbPoints.isNotEmpty()) {
                dbPoints
            } else {
                com.workout.timer.service.RunTrackingService.lastCompletedRoute
            }

            _uiState.update {
                it.copy(
                    session = session,
                    routePoints = points,
                    isLoading = false
                )
            }
        }
    }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.session?.id ?: sessionId
            if (id > 0L) {
                runRepository.deleteSession(id)
            }
            onDeleted()
        }
    }
}
