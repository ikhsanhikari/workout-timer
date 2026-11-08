package com.workout.timer.ui.screen.plan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.workout.timer.data.database.dao.WorkoutSessionRecordDao
import com.workout.timer.data.model.WorkoutReminderItem
import com.workout.timer.data.model.WorkoutRoutine
import com.workout.timer.data.model.WorkoutSessionRecord
import com.workout.timer.data.preferences.UserPreferencesRepository
import com.workout.timer.data.repository.WorkoutRepository
import com.workout.timer.util.WorkoutReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DayPlanUiModel(
    val dayOfWeek: Int, // Calendar.SUNDAY=1..Calendar.SATURDAY=7
    val dayNameShort: String, // "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"
    val dayNameFull: String, // "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    val dateDayNumber: Int, // e.g. 7
    val isToday: Boolean,
    val isSelected: Boolean,
    val routine: WorkoutRoutine?,
    val isRestDay: Boolean,
    val isCompletedThisWeek: Boolean
)

data class WorkoutPlanUiState(
    val days: List<DayPlanUiModel> = emptyList(),
    val selectedDayPlan: DayPlanUiModel? = null,
    val allRoutines: List<WorkoutRoutine> = emptyList(),
    val filteredRoutines: List<WorkoutRoutine> = emptyList(),
    val spotlightRoutines: List<WorkoutRoutine> = emptyList(),
    val recentRoutines: List<WorkoutRoutine> = emptyList(),
    val activeProgramName: String = "Athletic Split 4 Hari",
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedMaxDurationMinutes: Int? = null,
    val isReminderEnabled: Boolean = true,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val customReminders: List<WorkoutReminderItem> = emptyList(),
    val activeRemindersCount: Int = 0,
    val reminderSummary: String = "",
    val weeklyCompletedCount: Int = 0,
    val weeklyTargetCount: Int = 0,
    val weeklyTotalMinutes: Int = 0,
    val streakDays: Int = 0,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class WorkoutPlanViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val sessionRecordDao: WorkoutSessionRecordDao,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val gson = Gson()
    private val _selectedDayOfWeek = MutableStateFlow(currentDayOfWeek)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedMaxDurationMinutes = MutableStateFlow<Int?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    companion object {
        val currentDayOfWeek: Int
            get() = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    // Days in Indonesian standard order: Senin (2), Selasa (3), Rabu (4), Kamis (5), Jumat (6), Sabtu (7), Minggu (1)
    private val orderedDays = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )

    private val dayNames = mapOf(
        Calendar.MONDAY to Pair("Sen", "Senin"),
        Calendar.TUESDAY to Pair("Sel", "Selasa"),
        Calendar.WEDNESDAY to Pair("Rab", "Rabu"),
        Calendar.THURSDAY to Pair("Kam", "Kamis"),
        Calendar.FRIDAY to Pair("Jum", "Jumat"),
        Calendar.SATURDAY to Pair("Sab", "Sabtu"),
        Calendar.SUNDAY to Pair("Min", "Minggu")
    )

    val allRoutinesFlow = workoutRepository.observeAllRoutinesForHistory()

    private val weekStartMs: Long
        get() {
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

    // Map of dayOfWeek to routineId (null if Rest Day)
    private val scheduleMapFlow: Flow<Map<Int, Long?>> = combine(
        preferencesRepository.weeklyScheduleJsonFlow,
        allRoutinesFlow
    ) { json, routines ->
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, Long?>>() {}.type
                val rawMap: Map<String, Long?> = gson.fromJson(json, type) ?: emptyMap()
                return@combine rawMap.mapKeys { it.key.toInt() }
            } catch (e: Exception) {
                // Parse fallback
            }
        }
        createDefaultScheduleMap(routines)
    }

    private fun createDefaultScheduleMap(routines: List<WorkoutRoutine>): Map<Int, Long?> {
        val fullBody = routines.find { it.category.equals("Full Body", ignoreCase = true) }?.id ?: routines.firstOrNull()?.id
        val hiit = routines.find { it.category.equals("HIIT", ignoreCase = true) }?.id ?: routines.getOrNull(1)?.id
        val core = routines.find { it.category.equals("Core", ignoreCase = true) }?.id ?: routines.getOrNull(2)?.id
        val strength = routines.find { it.category.equals("Strength", ignoreCase = true) }?.id ?: routines.getOrNull(3)?.id
        val mobility = routines.find { it.category.equals("Mobility", ignoreCase = true) }?.id ?: routines.getOrNull(4)?.id

        return mapOf(
            Calendar.MONDAY to fullBody,
            Calendar.TUESDAY to null, // Rest Day
            Calendar.WEDNESDAY to hiit,
            Calendar.THURSDAY to core,
            Calendar.FRIDAY to strength,
            Calendar.SATURDAY to mobility,
            Calendar.SUNDAY to null // Rest Day
        )
    }

    private data class WeeklyStats(
        val completedDays: Set<Int>,
        val completedCount: Int,
        val totalMinutes: Int,
        val streak: Int
    )

    // Flow for weekly session records stats
    private val weeklyStatsFlow: Flow<WeeklyStats> = sessionRecordDao.observeAll().map { records ->
        val startMs = weekStartMs
        val completedDays = mutableSetOf<Int>()
        var totalSec = 0
        var count = 0

        records.filter { it.completedAt >= startMs }.forEach { record ->
            val recordCal = Calendar.getInstance().apply { timeInMillis = record.completedAt }
            completedDays.add(recordCal.get(Calendar.DAY_OF_WEEK))
            totalSec += record.durationSeconds
            count++
        }

        val streak = calculateStreak(records)
        WeeklyStats(
            completedDays = completedDays,
            completedCount = count,
            totalMinutes = totalSec / 60,
            streak = streak
        )
    }

    private fun calculateStreak(records: List<WorkoutSessionRecord>): Int {
        if (records.isEmpty()) return 0
        val daysWithWorkout = records.map { record ->
            val c = Calendar.getInstance().apply { timeInMillis = record.completedAt }
            c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }.toSet()

        val checkCal = Calendar.getInstance()
        var streak = 0
        val todayKey = checkCal.get(Calendar.YEAR) * 1000 + checkCal.get(Calendar.DAY_OF_YEAR)

        if (daysWithWorkout.contains(todayKey)) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = checkCal.get(Calendar.YEAR) * 1000 + checkCal.get(Calendar.DAY_OF_YEAR)
            if (!daysWithWorkout.contains(yesterdayKey)) return 0
        }

        while (true) {
            val key = checkCal.get(Calendar.YEAR) * 1000 + checkCal.get(Calendar.DAY_OF_YEAR)
            if (daysWithWorkout.contains(key)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    private val baseScheduleFlow = combine(
        scheduleMapFlow,
        allRoutinesFlow,
        weeklyStatsFlow
    ) { scheduleMap, routines, stats ->
        Triple(scheduleMap, routines, stats)
    }

    private data class FilterParams(
        val query: String,
        val category: String?,
        val maxDuration: Int?,
        val selectedDay: Int
    )

    private val filterInputsFlow = combine(
        _searchQuery,
        _selectedCategory,
        _selectedMaxDurationMinutes,
        _selectedDayOfWeek
    ) { query, category, maxDuration, selectedDay ->
        FilterParams(query, category, maxDuration, selectedDay)
    }

    val customRemindersFlow: Flow<List<WorkoutReminderItem>> = preferencesRepository.customRemindersFlow

    val uiState: StateFlow<WorkoutPlanUiState> = combine(
        baseScheduleFlow,
        filterInputsFlow,
        preferencesRepository.activeProgramNameFlow,
        customRemindersFlow,
        _snackbarMessage
    ) { base, filterParams, programName, customRemindersList, snackbar ->
        val (scheduleMap, routines, stats) = base
        val activeReminders = customRemindersList.filter { it.isEnabled }
        val remEnabled = activeReminders.isNotEmpty()
        val firstActive = activeReminders.firstOrNull()
        val remHour = firstActive?.hour ?: 7
        val remMin = firstActive?.minute ?: 0
        val remSummary = if (firstActive != null) {
            val daysStr = WorkoutReminderManager.formatDaysSummary(firstActive.activeDays)
            "%02d:%02d WIB • %s".format(remHour, remMin, daysStr)
        } else "Belum aktif"
        val today = currentDayOfWeek

        val dayUiModels = orderedDays.mapIndexed { index, dayOfWeek ->
            val (shortName, fullName) = dayNames[dayOfWeek] ?: Pair("Hari", "Hari")
            val routineId = scheduleMap[dayOfWeek]
            val routine = routines.find { it.id == routineId }
            val isRest = routineId == null || routine == null

            val dayCal = Calendar.getInstance().apply {
                timeInMillis = weekStartMs + (index * 24L * 60 * 60 * 1000L)
            }
            val dateNumber = dayCal.get(Calendar.DAY_OF_MONTH)

            DayPlanUiModel(
                dayOfWeek = dayOfWeek,
                dayNameShort = shortName,
                dayNameFull = fullName,
                dateDayNumber = dateNumber,
                isToday = dayOfWeek == today,
                isSelected = dayOfWeek == filterParams.selectedDay,
                routine = routine,
                isRestDay = isRest,
                isCompletedThisWeek = stats.completedDays.contains(dayOfWeek)
            )
        }

        val selectedPlan = dayUiModels.find { it.dayOfWeek == filterParams.selectedDay } ?: dayUiModels.firstOrNull()

        // Filtered routines based on category, duration & search
        val filtered = routines.filter { routine ->
            val matchQuery = filterParams.query.isBlank() || routine.name.contains(filterParams.query, ignoreCase = true) || routine.category.contains(filterParams.query, ignoreCase = true)
            val matchCategory = when (filterParams.category?.lowercase()) {
                null, "semua" -> true
                "fat loss" -> routine.category.contains("hiit", ignoreCase = true) || routine.category.contains("fat", ignoreCase = true) || routine.category.contains("cardio", ignoreCase = true)
                "strength" -> routine.category.contains("strength", ignoreCase = true) || routine.category.contains("body", ignoreCase = true) || routine.category.contains("upper", ignoreCase = true)
                "cardio" -> routine.category.contains("cardio", ignoreCase = true) || routine.category.contains("hiit", ignoreCase = true)
                else -> routine.category.contains(filterParams.category, ignoreCase = true)
            }
            val matchDuration = filterParams.maxDuration == null || (routine.totalDurationSec / 60) <= filterParams.maxDuration
            matchQuery && matchCategory && matchDuration
        }

        // Spotlight list (for carousel: 4 routines)
        val spotlights = if (routines.isNotEmpty()) {
            val list = mutableListOf<WorkoutRoutine>()
            routines.find { it.name.contains("Full Body", ignoreCase = true) }?.let { list.add(it) }
            routines.find { it.category.equals("HIIT", ignoreCase = true) }?.let { list.add(it) }
            routines.find { it.category.equals("Strength", ignoreCase = true) }?.let { list.add(it) }
            routines.find { it.category.equals("Core", ignoreCase = true) }?.let { list.add(it) }
            routines.forEach { if (!list.contains(it) && list.size < 4) list.add(it) }
            list.take(4)
        } else emptyList()

        // Recent routines (sorted by lastPerformed or default)
        val recents = if (routines.isNotEmpty()) {
            routines.sortedByDescending { it.lastPerformedAt ?: 0L }.take(6)
        } else emptyList()

        val activeWorkoutsTarget = scheduleMap.values.count { it != null }.coerceAtLeast(3)

        WorkoutPlanUiState(
            days = dayUiModels,
            selectedDayPlan = selectedPlan,
            allRoutines = routines,
            filteredRoutines = filtered,
            spotlightRoutines = spotlights,
            recentRoutines = recents,
            activeProgramName = programName,
            searchQuery = filterParams.query,
            selectedCategory = filterParams.category,
            selectedMaxDurationMinutes = filterParams.maxDuration,
            isReminderEnabled = remEnabled,
            reminderHour = remHour,
            reminderMinute = remMin,
            customReminders = customRemindersList,
            activeRemindersCount = activeReminders.size,
            reminderSummary = remSummary,
            weeklyCompletedCount = stats.completedCount,
            weeklyTargetCount = activeWorkoutsTarget,
            weeklyTotalMinutes = stats.totalMinutes,
            streakDays = stats.streak,
            isLoading = false,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutPlanUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onQuickDurationSelected(minutes: Int) {
        _selectedMaxDurationMinutes.value = if (_selectedMaxDurationMinutes.value == minutes) null else minutes
        if (_selectedMaxDurationMinutes.value != null) {
            _snackbarMessage.value = "Menampilkan latihan berdurasi ≤ $minutes menit"
        }
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedMaxDurationMinutes.value = null
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDayOfWeek.value = dayOfWeek
    }

    fun toggleReminder(context: Context, reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = current.map {
                if (it.id == reminderId) it.copy(isEnabled = enabled) else it
            }
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = if (enabled) "Pengingat latihan diaktifkan" else "Pengingat latihan dinonaktifkan"
        }
    }

    fun updateFullReminder(context: Context, updatedItem: WorkoutReminderItem) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = current.map {
                if (it.id == updatedItem.id) updatedItem else it
            }
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = "Pengingat '${updatedItem.label}' diperbarui"
        }
    }

    fun updateFullReminder(
        context: Context,
        reminderId: String,
        label: String,
        hour: Int,
        minute: Int,
        activeDays: List<Int>
    ) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = current.map { item ->
                if (item.id == reminderId) {
                    item.copy(
                        label = label,
                        hour = hour,
                        minute = minute,
                        activeDays = activeDays,
                        isEnabled = true
                    )
                } else item
            }
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = "Pengingat '$label' diperbarui"
        }
    }

    fun addCustomReminder(context: Context, newItem: WorkoutReminderItem) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = current + newItem
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = "Pengingat '${newItem.label}' berhasil ditambahkan"
        }
    }

    fun addCustomReminder(
        context: Context,
        label: String,
        hour: Int,
        minute: Int,
        activeDays: List<Int>
    ) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val newItem = WorkoutReminderItem(
                id = "reminder_" + System.currentTimeMillis(),
                label = label,
                hour = hour,
                minute = minute,
                activeDays = activeDays,
                isEnabled = true
            )
            val updated = current + newItem
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = "Pengingat '$label' berhasil ditambahkan"
        }
    }

    fun deleteCustomReminder(context: Context, reminderId: String) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = current.filterNot { it.id == reminderId }
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.cancelReminderItem(context, reminderId)
            _snackbarMessage.value = "Pengingat berhasil dihapus"
        }
    }

    fun toggleReminder(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = if (current.isEmpty()) {
                listOf(WorkoutReminderItem(id = "default_reminder", label = "Latihan Harian", hour = 7, minute = 0, isEnabled = enabled))
            } else {
                current.map { it.copy(isEnabled = enabled) }
            }
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = if (enabled) "Pengingat latihan diaktifkan" else "Pengingat latihan dinonaktifkan"
        }
    }

    fun updateReminderTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = preferencesRepository.customRemindersFlow.firstOrNull() ?: emptyList()
            val updated = if (current.isEmpty()) {
                listOf(WorkoutReminderItem(id = "default_reminder", label = "Latihan Harian", hour = hour, minute = minute, isEnabled = true))
            } else {
                current.mapIndexed { idx, item ->
                    if (idx == 0) item.copy(hour = hour, minute = minute, isEnabled = true) else item
                }
            }
            preferencesRepository.saveCustomReminders(updated)
            WorkoutReminderManager.rescheduleAll(context)
            _snackbarMessage.value = "Waktu pengingat diatur ke %02d:%02d WIB".format(hour, minute)
        }
    }

    fun testReminderNotification(context: Context) {
        WorkoutReminderManager.testNotificationNow(context)
        _snackbarMessage.value = "🔔 Mengirim notifikasi sesuai jadwal latihan hari ini!"
    }

    fun assignRoutine(dayOfWeek: Int, routineId: Long?, context: Context? = null) {
        viewModelScope.launch {
            val routines = allRoutinesFlow.firstOrNull() ?: emptyList()
            val currentMap = scheduleMapFlow.firstOrNull() ?: createDefaultScheduleMap(routines)
            val updated = currentMap.toMutableMap()
            updated[dayOfWeek] = routineId
            val jsonMap = updated.mapKeys { it.key.toString() }
            preferencesRepository.saveWeeklySchedule(gson.toJson(jsonMap), "Jadwal Kustom")
            val dayName = dayNames[dayOfWeek]?.second ?: "Hari"
            if (routineId != null) {
                val routineName = routines.find { it.id == routineId }?.name ?: "Latihan"
                _snackbarMessage.value = "$dayName diatur ke: $routineName (Notifikasi otomatis tersinkron)"
            } else {
                _snackbarMessage.value = "$dayName diatur sebagai Hari Istirahat (Rest Day) 🛌"
            }
            if (context != null) {
                WorkoutReminderManager.rescheduleAll(context)
            }
        }
    }

    fun applyPreset(programId: String) {
        viewModelScope.launch {
            val routines = allRoutinesFlow.firstOrNull() ?: emptyList()
            val fullBody = routines.find { it.category.equals("Full Body", ignoreCase = true) }?.id ?: routines.firstOrNull()?.id
            val hiit = routines.find { it.category.equals("HIIT", ignoreCase = true) }?.id ?: routines.getOrNull(1)?.id
            val core = routines.find { it.category.equals("Core", ignoreCase = true) }?.id ?: routines.getOrNull(2)?.id
            val strength = routines.find { it.category.equals("Strength", ignoreCase = true) }?.id ?: routines.getOrNull(3)?.id
            val mobility = routines.find { it.category.equals("Mobility", ignoreCase = true) }?.id ?: routines.getOrNull(4)?.id

            val (newMap, name) = when (programId) {
                "3day" -> {
                    Pair(
                        mapOf(
                            Calendar.MONDAY to fullBody,
                            Calendar.TUESDAY to null,
                            Calendar.WEDNESDAY to strength,
                            Calendar.THURSDAY to null,
                            Calendar.FRIDAY to core,
                            Calendar.SATURDAY to null,
                            Calendar.SUNDAY to null
                        ),
                        "Full Body 3 Hari"
                    )
                }
                "daily" -> {
                    Pair(
                        mapOf(
                            Calendar.MONDAY to fullBody,
                            Calendar.TUESDAY to hiit,
                            Calendar.WEDNESDAY to core,
                            Calendar.THURSDAY to strength,
                            Calendar.FRIDAY to mobility,
                            Calendar.SATURDAY to hiit,
                            Calendar.SUNDAY to mobility
                        ),
                        "Daily Athlete 7 Hari"
                    )
                }
                else -> {
                    Pair(
                        mapOf(
                            Calendar.MONDAY to fullBody,
                            Calendar.TUESDAY to null,
                            Calendar.WEDNESDAY to hiit,
                            Calendar.THURSDAY to core,
                            Calendar.FRIDAY to strength,
                            Calendar.SATURDAY to mobility,
                            Calendar.SUNDAY to null
                        ),
                        "Athletic Split 4 Hari"
                    )
                }
            }

            val jsonMap = newMap.mapKeys { it.key.toString() }
            preferencesRepository.saveWeeklySchedule(gson.toJson(jsonMap), name)
            _snackbarMessage.value = "Program $name berhasil diterapkan!"
        }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }
}
