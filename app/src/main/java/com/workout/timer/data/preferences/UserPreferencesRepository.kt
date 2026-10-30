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
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val KEY_BEEP_ENABLED = booleanPreferencesKey("beep_enabled")
        val KEY_COUNTDOWN_ENABLED = booleanPreferencesKey("countdown_enabled")
        val KEY_CATALOG_VERSION = intPreferencesKey("catalog_version")
        val KEY_CATALOG_URL = stringPreferencesKey("catalog_url")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val catalogVersionFlow: Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_CATALOG_VERSION] ?: 1 }

    val catalogUrlFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_CATALOG_URL] }

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

    suspend fun recordSyncTimestamp() {
        dataStore.edit { it[KEY_LAST_SYNC_TIMESTAMP] = System.currentTimeMillis() }
    }

    data class UserPreferences(
        val nickname: String = "",
        val isOnboardingDone: Boolean = false,
        val hapticEnabled: Boolean = true,
        val beepEnabled: Boolean = true,
        val countdownEnabled: Boolean = true
    )

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { preferences ->
            UserPreferences(
                nickname = preferences[KEY_USER_NICKNAME] ?: "",
                isOnboardingDone = preferences[KEY_ONBOARDING_DONE] ?: false,
                hapticEnabled = preferences[KEY_HAPTIC_ENABLED] ?: true,
                beepEnabled = preferences[KEY_BEEP_ENABLED] ?: true,
                countdownEnabled = preferences[KEY_COUNTDOWN_ENABLED] ?: true
            )
        }

    suspend fun saveNickname(nickname: String) {
        dataStore.edit { it[KEY_USER_NICKNAME] = nickname }
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
}
