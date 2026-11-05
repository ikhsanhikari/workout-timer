package com.workout.timer.ui.screen.player

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.preferences.UserPreferencesRepository
import com.workout.timer.data.repository.WorkoutRepository
import com.workout.timer.service.WorkoutTimerService
import com.workout.timer.service.audio.WorkoutMusicManager
import com.workout.timer.service.timer.WorkoutTimerEngine
import com.workout.timer.service.timer.WorkoutTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WorkoutPlayerViewModel — ViewModel untuk Player Layar Latihan (F-06).
 *
 * Menghubungkan Player UI dengan [WorkoutTimerEngine] dan [WorkoutTimerService].
 */
@HiltViewModel
class WorkoutPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timerEngine: WorkoutTimerEngine,
    private val workoutMusicManager: WorkoutMusicManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val timerState: StateFlow<WorkoutTimerState> = timerEngine.timerState

    val isBgmEnabled: StateFlow<Boolean> = preferencesRepository.bgmEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVideoEnabled: StateFlow<Boolean> = preferencesRepository.videoEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVoiceCoachEnabled: StateFlow<Boolean> = preferencesRepository.voiceCoachEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleVideo() {
        viewModelScope.launch {
            preferencesRepository.setVideoEnabled(!isVideoEnabled.value)
        }
    }

    fun toggleVoiceCoach() {
        viewModelScope.launch {
            preferencesRepository.setVoiceCoachEnabled(!isVoiceCoachEnabled.value)
        }
    }

    /**
     * Memulai rutinitas latihan jika belum aktif.
     */
    fun startRoutine(routineId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            workoutRepository.markRoutinePerformed(routineId)
        }

        val currentState = timerEngine.timerState.value
        val isAlreadyRunningThis = when (currentState) {
            is WorkoutTimerState.Preparing -> currentState.routineId == routineId
            is WorkoutTimerState.Running -> currentState.routineId == routineId
            is WorkoutTimerState.Resting -> currentState.routineId == routineId
            else -> false
        }

        if (!isAlreadyRunningThis) {
            val intent = WorkoutTimerService.startIntent(context, routineId)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    fun togglePause() {
        timerEngine.togglePause()
    }

    fun skip() {
        timerEngine.skip()
    }

    fun previous() {
        timerEngine.previous()
    }

    fun addRestTime(seconds: Int = 15) {
        timerEngine.addRestTime(seconds)
    }

    fun addWorkTime(seconds: Int = 15) {
        timerEngine.addWorkTime(seconds)
    }

    fun toggleBgm() {
        workoutMusicManager.toggleBgm()
    }

    fun stopWorkout(saveProgress: Boolean = false) {
        if (saveProgress) {
            val snapshot = timerEngine.getCurrentSessionSnapshot(isCompletedFully = false)
            if (snapshot != null && snapshot.durationSeconds >= 5) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        workoutRepository.recordWorkoutSession(snapshot)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        val intent = WorkoutTimerService.stopIntent(context, saveProgress = false)
        try {
            context.startService(intent)
        } catch (_: Exception) { }
        timerEngine.stop()
    }
}
