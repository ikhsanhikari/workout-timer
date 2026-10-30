package com.workout.timer.ui.screen.player

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.service.WorkoutTimerService
import com.workout.timer.service.timer.WorkoutTimerEngine
import com.workout.timer.service.timer.WorkoutTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
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
    private val timerEngine: WorkoutTimerEngine
) : ViewModel() {

    val timerState: StateFlow<WorkoutTimerState> = timerEngine.timerState

    /**
     * Memulai rutinitas latihan jika belum aktif.
     */
    fun startRoutine(routineId: Long) {
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

    fun stopWorkout() {
        val intent = WorkoutTimerService.stopIntent(context)
        context.startService(intent)
        timerEngine.stop()
    }
}
