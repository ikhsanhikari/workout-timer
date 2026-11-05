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
        title = "HIIT & Fat Burning",
        subtitle = "Maksimalkan denyut jantung & bakar kalori tinggi",
        iconRes = com.workout.timer.R.drawable.ic_cat_hiit,
        accentHex = 0xFFA4FF00 // Volt Lime
    ),
    STRENGTH(
        id = "Strength",
        title = "Strength & Muscle",
        subtitle = "Bentuk massa otot & tingkatkan daya tahan fisik",
        iconRes = com.workout.timer.R.drawable.ic_cat_strength,
        accentHex = 0xFFFFB800 // Amber
    ),
    MOBILITY(
        id = "Mobility",
        title = "Mobility & Yoga",
        subtitle = "Lepaskan ketegangan sendi & perbaiki postur",
        iconRes = com.workout.timer.R.drawable.ic_cat_mobility,
        accentHex = 0xFF00F5EA // Electric Cyan
    ),
    CORE(
        id = "Core",
        title = "Core & Abs Definition",
        subtitle = "Perkuat stabilitas otot inti & perut kencang",
        iconRes = com.workout.timer.R.drawable.ic_cat_core,
        accentHex = 0xFFFF3366 // Neon Rose
    )
}

enum class ExperienceLevel(
    val id: String,
    val label: String,
    val description: String
) {
    BEGINNER("Beginner", "Pemula", "Fokus pembentukan kebiasaan & tempo rileks"),
    INTERMEDIATE("Intermediate", "Menengah", "Intensitas seimbang untuk hasil konsisten"),
    ADVANCED("Advanced", "Lanjutan", "Daya tahan maksimal dengan jeda istirahat singkat")
}

enum class ThemeAccent(
    val id: String,
    val label: String,
    val colorHex: Long
) {
    VOLT_LIME("VoltLime", "Volt Lime", 0xFFA4FF00),
    ELECTRIC_CYAN("ElectricCyan", "Electric Cyan", 0xFF00F5EA),
    AMBER("Amber", "Cyber Amber", 0xFFFFB800),
    NEON_ROSE("NeonRose", "Neon Rose", 0xFFFF3366)
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

