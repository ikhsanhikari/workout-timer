package com.workout.timer.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingGoal(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val accentHex: Long
) {
    HIIT(
        id = "HIIT",
        title = "HIIT & Pembakaran Lemak",
        subtitle = "Tingkatkan denyut jantung & efisiensi metabolisme tubuh",
        iconRes = com.workout.timer.R.drawable.ic_cat_hiit,
        accentHex = 0xFFFFFFFF
    ),
    STRENGTH(
        id = "Strength",
        title = "Kekuatan & Pembentukan Otot",
        subtitle = "Bentuk massa otot & perkuat ketahanan fisik bertahap",
        iconRes = com.workout.timer.R.drawable.ic_cat_strength,
        accentHex = 0xFFFFFFFF
    ),
    MOBILITY(
        id = "Mobility",
        title = "Mobilitas & Fleksibilitas",
        subtitle = "Pelepasan sendi kaku, elastisitas otot & perbaikan postur",
        iconRes = com.workout.timer.R.drawable.ic_cat_mobility,
        accentHex = 0xFFFFFFFF
    ),
    CORE(
        id = "Core",
        title = "Penguatan Otot Inti (Core)",
        subtitle = "Stabilitas postur, kekuatan pinggang & definisi perut",
        iconRes = com.workout.timer.R.drawable.ic_cat_core,
        accentHex = 0xFFFFFFFF
    )
}

enum class ExperienceLevel(
    val id: String,
    val label: String,
    val description: String
) {
    BEGINNER("Beginner", "Pemula", "Fokus pembentukan kebiasaan dasar & tempo terkontrol"),
    INTERMEDIATE("Intermediate", "Menengah", "Intensitas seimbang untuk progres bertahap"),
    ADVANCED("Advanced", "Mahir", "Tantangan fisik optimal dengan tempo dinamis")
}

enum class ThemeAccent(
    val id: String,
    val label: String,
    val colorHex: Long
) {
    VOLT_LIME("VoltLime", "Pure Minimalist", 0xFFFFFFFF),
    ELECTRIC_CYAN("ElectricCyan", "Silver Slate", 0xFF94A3B8),
    AMBER("Amber", "Muted Amber", 0xFFF59E0B),
    NEON_ROSE("NeonRose", "Crimson Frost", 0xFFF43F5E)
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _selectedGoal = MutableStateFlow(OnboardingGoal.HIIT)
    val selectedGoal: StateFlow<OnboardingGoal> = _selectedGoal.asStateFlow()

    private val _selectedLevel = MutableStateFlow(ExperienceLevel.INTERMEDIATE)
    val selectedLevel: StateFlow<ExperienceLevel> = _selectedLevel.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _selectedAccent = MutableStateFlow(ThemeAccent.VOLT_LIME)
    val selectedAccent: StateFlow<ThemeAccent> = _selectedAccent.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < 2) {
            _currentStep.value += 1
        }
    }

    fun prevStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun selectGoal(goal: OnboardingGoal) {
        _selectedGoal.value = goal
    }

    fun selectLevel(level: ExperienceLevel) {
        _selectedLevel.value = level
    }

    fun setNickname(name: String) {
        _nickname.value = name
    }

    fun selectAccent(accent: ThemeAccent) {
        _selectedAccent.value = accent
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.completeOnboarding(
                nickname = _nickname.value.ifBlank { "Atlet" },
                goal = _selectedGoal.value.id,
                level = _selectedLevel.value.id,
                accent = _selectedAccent.value.id
            )
            onSuccess()
        }
    }

    fun skipOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.completeOnboarding(
                nickname = "Atlet",
                goal = "All",
                level = "Intermediate",
                accent = "VoltLime"
            )
            onSuccess()
        }
    }
}

