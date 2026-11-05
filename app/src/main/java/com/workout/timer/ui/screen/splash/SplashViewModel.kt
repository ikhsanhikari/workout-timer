package com.workout.timer.ui.screen.splash

import androidx.lifecycle.ViewModel
import com.workout.timer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ViewModel untuk SplashScreen — memeriksa status onboarding awal.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    suspend fun isOnboardingDone(): Boolean {
        return preferencesRepository.userPreferencesFlow.first().isOnboardingDone
    }
}

