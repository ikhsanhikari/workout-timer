package com.workout.timer.ui.screen.run

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.service.RunTrackingService
import com.workout.timer.service.RunTrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveRunViewModel @Inject constructor() : ViewModel() {

    val trackingState: StateFlow<RunTrackingState> = RunTrackingService.trackingState

    private val _isFollowUser = MutableStateFlow(true)
    val isFollowUser: StateFlow<Boolean> = _isFollowUser.asStateFlow()

    fun toggleFollowUser() {
        _isFollowUser.update { !it }
    }

    fun setFollowUser(follow: Boolean) {
        _isFollowUser.value = follow
    }

    fun pause(context: Context) {
        context.startService(RunTrackingService.pauseIntent(context))
    }

    fun resume(context: Context) {
        context.startService(RunTrackingService.resumeIntent(context))
    }

    fun stop(context: Context, onStopped: (Long) -> Unit) {
        val currentSessionId = trackingState.value.sessionId
        context.startService(RunTrackingService.stopIntent(context))
        onStopped(currentSessionId)
    }
}
