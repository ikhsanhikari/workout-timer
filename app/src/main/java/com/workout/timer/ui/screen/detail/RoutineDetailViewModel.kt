package com.workout.timer.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.model.BlockWithExercises
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val routine: WorkoutRoutine? = null,
    val blocks: List<BlockWithExercises> = emptyList(),
    val isLoading: Boolean = true,
    val totalDurationSec: Int = 0,
    val totalExercises: Int = 0
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routineId: Long = checkNotNull(savedStateHandle["routineId"])

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeRoutine(routineId),
        repository.observeBlocksWithExercises(routineId)
    ) { routine, blocksWithEx ->
        val blocks = blocksWithEx.sortedBy { it.block.orderIndex }
        val totalDuration = blocks.sumOf { bwe ->
            bwe.exercises.sumOf { it.durationSeconds } * bwe.block.repeatCount
        }
        DetailUiState(
            routine = routine,
            blocks = blocks,
            isLoading = false,
            totalDurationSec = routine?.totalDurationSec ?: totalDuration,
            totalExercises = routine?.totalExercises ?: blocks.sumOf { it.exercises.size }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(routineId)
        }
    }
}
