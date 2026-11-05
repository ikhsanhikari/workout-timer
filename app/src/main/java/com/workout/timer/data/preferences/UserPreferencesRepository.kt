package com.workout.timer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository untuk preferensi pengguna (F-01 Onboarding):
 * - Nama panggilan pengguna
 * - Apakah onboarding sudah selesai
 * - Pengaturan haptic feedback
 * - Pengaturan audio beep
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_NICKNAME = stringPreferencesKey("user_nickname")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_PRIMARY_GOAL = stringPreferencesKey("primary_goal")
        val KEY_EXPERIENCE_LEVEL = stringPreferencesKey("experience_level")
        val KEY_THEME_ACCENT = stringPreferencesKey("theme_accent")
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val KEY_BEEP_ENABLED = booleanPreferencesKey("beep_enabled")
        val KEY_COUNTDOWN_ENABLED = booleanPreferencesKey("countdown_enabled")
        val KEY_BGM_ENABLED = booleanPreferencesKey("bgm_enabled")
        val KEY_VIDEO_ENABLED = booleanPreferencesKey("video_enabled")
        val KEY_VOICE_COACH_ENABLED = booleanPreferencesKey("voice_coach_enabled")
        val KEY_CATALOG_VERSION = intPreferencesKey("catalog_version")
        val KEY_CATALOG_URL = stringPreferencesKey("catalog_url")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_EXERCISE_LIBRARY_VERSION = intPreferencesKey("exercise_library_version")
        val KEY_EXERCISE_LIBRARY_URL = stringPreferencesKey("exercise_library_url")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val catalogVersionFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_CATALOG_VERSION] ?: 1 }

    val catalogUrlFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_CATALOG_URL] }

    val exerciseLibraryVersionFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_EXERCISE_LIBRARY_VERSION] ?: 1 }

    val exerciseLibraryUrlFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_EXERCISE_LIBRARY_URL] }

    suspend fun getCatalogVersion(): Int {
        return dataStore.data.firstOrNull()?.get(KEY_CATALOG_VERSION) ?: 1
    }

    suspend fun saveCatalogVersion(version: Int) {
        dataStore.edit { it[KEY_CATALOG_VERSION] = version }
    }

    suspend fun getCatalogUrl(): String? {
        return dataStore.data.firstOrNull()?.get(KEY_CATALOG_URL)
    }

    suspend fun saveCatalogUrl(url: String) {
        dataStore.edit { it[KEY_CATALOG_URL] = url }
    }

    suspend fun getExerciseLibraryVersion(): Int {
        return dataStore.data.firstOrNull()?.get(KEY_EXERCISE_LIBRARY_VERSION) ?: 1
    }

    suspend fun saveExerciseLibraryVersion(version: Int) {
        dataStore.edit { it[KEY_EXERCISE_LIBRARY_VERSION] = version }
    }

    suspend fun getExerciseLibraryUrl(): String? {
        return dataStore.data.firstOrNull()?.get(KEY_EXERCISE_LIBRARY_URL)
    }

    suspend fun saveExerciseLibraryUrl(url: String) {
        dataStore.edit { it[KEY_EXERCISE_LIBRARY_URL] = url }
    }

    suspend fun recordSyncTimestamp() {
        dataStore.edit { it[KEY_LAST_SYNC_TIMESTAMP] = System.currentTimeMillis() }
    }

    data class UserPreferences(
        val nickname: String = "",
        val isOnboardingDone: Boolean = false,
        val primaryGoal: String = "All",
        val experienceLevel: String = "Intermediate",
        val themeAccent: String = "VoltLime",
        val hapticEnabled: Boolean = true,
        val beepEnabled: Boolean = true,
        val countdownEnabled: Boolean = true,
        val bgmEnabled: Boolean = true,
        val videoEnabled: Boolean = true,
        val voiceCoachEnabled: Boolean = true,
        val reminderEnabled: Boolean = false,
        val reminderHour: Int = 7,
        val reminderMinute: Int = 0
    )

    val bgmEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_BGM_ENABLED] ?: true }

    val videoEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_VIDEO_ENABLED] ?: true }

    val voiceCoachEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_VOICE_COACH_ENABLED] ?: true }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { preferences ->
            UserPreferences(
                nickname = preferences[KEY_USER_NICKNAME] ?: "",
                isOnboardingDone = preferences[KEY_ONBOARDING_DONE] ?: false,
                primaryGoal = preferences[KEY_PRIMARY_GOAL] ?: "All",
                experienceLevel = preferences[KEY_EXPERIENCE_LEVEL] ?: "Intermediate",
                themeAccent = preferences[KEY_THEME_ACCENT] ?: "VoltLime",
                hapticEnabled = preferences[KEY_HAPTIC_ENABLED] ?: true,
                beepEnabled = preferences[KEY_BEEP_ENABLED] ?: true,
                countdownEnabled = preferences[KEY_COUNTDOWN_ENABLED] ?: true,
                bgmEnabled = preferences[KEY_BGM_ENABLED] ?: true,
                videoEnabled = preferences[KEY_VIDEO_ENABLED] ?: true,
                voiceCoachEnabled = preferences[KEY_VOICE_COACH_ENABLED] ?: true,
                reminderEnabled = preferences[KEY_REMINDER_ENABLED] ?: false,
                reminderHour = preferences[KEY_REMINDER_HOUR] ?: 7,
                reminderMinute = preferences[KEY_REMINDER_MINUTE] ?: 0
            )
        }

    suspend fun saveNickname(nickname: String) {
        dataStore.edit { it[KEY_USER_NICKNAME] = nickname }
    }

    suspend fun savePrimaryGoal(goal: String) {
        dataStore.edit { it[KEY_PRIMARY_GOAL] = goal }
    }

    suspend fun saveExperienceLevel(level: String) {
        dataStore.edit { it[KEY_EXPERIENCE_LEVEL] = level }
    }

    suspend fun saveThemeAccent(accent: String) {
        dataStore.edit { it[KEY_THEME_ACCENT] = accent }
    }

    suspend fun completeOnboarding(
        nickname: String,
        goal: String,
        level: String,
        accent: String
    ) {
        dataStore.edit {
            if (nickname.isNotBlank()) it[KEY_USER_NICKNAME] = nickname.trim()
            it[KEY_PRIMARY_GOAL] = goal
            it[KEY_EXPERIENCE_LEVEL] = level
            it[KEY_THEME_ACCENT] = accent
            it[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun markOnboardingDone() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setBeepEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BEEP_ENABLED] = enabled }
    }

    suspend fun setCountdownEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_COUNTDOWN_ENABLED] = enabled }
    }

    suspend fun setBgmEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BGM_ENABLED] = enabled }
    }

    suspend fun setVideoEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VIDEO_ENABLED] = enabled }
    }

    suspend fun setVoiceCoachEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VOICE_COACH_ENABLED] = enabled }
    }

    val reminderEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_REMINDER_ENABLED] ?: false }

    val reminderHourFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_REMINDER_HOUR] ?: 7 }

    val reminderMinuteFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_REMINDER_MINUTE] ?: 0 }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[KEY_REMINDER_HOUR] = hour
            it[KEY_REMINDER_MINUTE] = minute
        }
    }
}
