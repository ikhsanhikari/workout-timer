package com.workout.timer.ui.screen.builder

import android.net.Uri
import java.io.File
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.media.MediaFileManager
import com.workout.timer.data.model.*
import com.workout.timer.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

/**
 * Representasi blok di UI Builder.
 * Berisi [WorkoutBlock] + daftar [WorkoutExercise] yang bisa di-edit secara lokal
 * sebelum di-save ke database.
 */
data class UiBlock(
    val block: WorkoutBlock,
    val exercises: List<UiExercise>,
    val isExpanded: Boolean = true
)

/**
 * Representasi gerakan di UI Builder.
 * Berisi [WorkoutExercise] + state tambahan seperti isEditing.
 */
data class UiExercise(
    val exercise: WorkoutExercise,
    val resolvedMediaUri: String? = null,
    val resolvedMediaType: MediaType = MediaType.NONE,
    val thumbnailUri: String? = null
)

data class BuilderUiState(
    val routineName: String = "",
    val category: String = "HIIT",
    val accentColor: String = "#A4FF00",
    val blocks: List<UiBlock> = emptyList(),
    val totalDurationSec: Int = 0,
    val totalExercises: Int = 0,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null,
    val navigateToPlayer: Long? = null,
    val autocompleteSuggestions: List<ExerciseTemplate> = emptyList(),
    val autocompleteQuery: String = ""
)

val PRESET_CATEGORIES = listOf("HIIT", "Strength", "Core", "Mobility", "Cardio", "Flexibility")
val PRESET_ACCENT_COLORS = listOf(
    "#A4FF00", // Volt Green (default)
    "#FA114F", // Neon Crimson
    "#00F5EA", // Electric Cyan
    "#FFD60A", // Amber Yellow
    "#BF5AF2", // Purple
    "#FF9F0A"  // Orange
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkoutBuilderViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val mediaFileManager: MediaFileManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.get<Long>("routineId")
        ?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(BuilderUiState())
    val uiState: StateFlow<BuilderUiState> = _uiState.asStateFlow()

    val allTemplates: StateFlow<List<ExerciseTemplate>> = repository.observeAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (routineId != null) {
            loadExistingRoutine(routineId)
        } else {
            // New routine — add default empty block
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    blocks = listOf(
                        UiBlock(
                            block = WorkoutBlock(routineId = 0, orderIndex = 0, repeatCount = 1, label = null),
                            exercises = emptyList()
                        )
                    )
                )
            }
        }
    }

    private fun loadExistingRoutine(id: Long) {
        viewModelScope.launch {
            val routine = repository.getRoutineById(id) ?: return@launch
            val blocksFlow = repository.observeBlocksWithExercises(id).first()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    routineName = routine.name,
                    category = routine.category,
                    accentColor = routine.accentColor,
                    blocks = blocksFlow.sortedBy { it.block.orderIndex }.map { blockWithEx ->
                        UiBlock(
                            block = blockWithEx.block,
                            exercises = blockWithEx.exercises.sortedBy { it.orderIndex }.map { ex ->
                                UiExercise(
                                    exercise = ex,
                                    resolvedMediaUri = ex.customMediaUri,
                                    resolvedMediaType = ex.customMediaType
                                )
                            }
                        )
                    }
                )
            }
            recalculateStats()
        }
    }

    // ── Routine Metadata ────────────────────────────────────────────────────

    fun onNameChanged(name: String) = _uiState.update { it.copy(routineName = name) }

    fun onCategoryChanged(category: String) = _uiState.update { it.copy(category = category) }

    fun onAccentColorChanged(color: String) = _uiState.update { it.copy(accentColor = color) }

    // ── Block Operations ────────────────────────────────────────────────────

    fun addBlock() {
        val currentBlocks = _uiState.value.blocks
        val newBlock = UiBlock(
            block = WorkoutBlock(
                routineId = routineId ?: 0,
                orderIndex = currentBlocks.size,
                repeatCount = 1
            ),
            exercises = emptyList()
        )
        _uiState.update { it.copy(blocks = currentBlocks + newBlock) }
    }

    fun removeBlock(blockIndex: Int) {
        val updated = _uiState.value.blocks.toMutableList()
        val removed = updated.removeAt(blockIndex)
        // Re-index remaining blocks
        val reindexed = updated.mapIndexed { i, b ->
            b.copy(block = b.block.copy(orderIndex = i))
        }
        _uiState.update { it.copy(blocks = reindexed) }
        recalculateStats()
        showSnackbar("Blok dihapus")
    }

    fun duplicateBlock(blockIndex: Int) {
        val blocks = _uiState.value.blocks.toMutableList()
        val source = blocks[blockIndex]
        val duplicate = source.copy(
            block = source.block.copy(id = 0, orderIndex = blockIndex + 1),
            exercises = source.exercises.map { ex ->
                ex.copy(exercise = ex.exercise.copy(id = 0))
            }
        )
        blocks.add(blockIndex + 1, duplicate)
        val reindexed = blocks.mapIndexed { i, b ->
            b.copy(block = b.block.copy(orderIndex = i))
        }
        _uiState.update { it.copy(blocks = reindexed) }
        recalculateStats()
    }

    fun updateBlockRepeatCount(blockIndex: Int, count: Int) {
        val updated = _uiState.value.blocks.mapIndexed { i, block ->
            if (i == blockIndex) block.copy(block = block.block.copy(repeatCount = count.coerceIn(1, 20)))
            else block
        }
        _uiState.update { it.copy(blocks = updated) }
        recalculateStats()
    }

    fun updateBlockLabel(blockIndex: Int, label: String) {
        val updated = _uiState.value.blocks.mapIndexed { i, block ->
            if (i == blockIndex) block.copy(block = block.block.copy(label = label.ifBlank { null }))
            else block
        }
        _uiState.update { it.copy(blocks = updated) }
    }

    fun toggleBlockExpanded(blockIndex: Int) {
        val updated = _uiState.value.blocks.mapIndexed { i, block ->
            if (i == blockIndex) block.copy(isExpanded = !block.isExpanded)
            else block
        }
        _uiState.update { it.copy(blocks = updated) }
    }

    fun moveBlockUp(blockIndex: Int) {
        if (blockIndex <= 0) return
        val blocks = _uiState.value.blocks.toMutableList()
        val temp = blocks[blockIndex]
        blocks[blockIndex] = blocks[blockIndex - 1].copy(block = blocks[blockIndex - 1].block.copy(orderIndex = blockIndex))
        blocks[blockIndex - 1] = temp.copy(block = temp.block.copy(orderIndex = blockIndex - 1))
        _uiState.update { it.copy(blocks = blocks) }
    }

    fun moveBlockDown(blockIndex: Int) {
        val blocks = _uiState.value.blocks.toMutableList()
        if (blockIndex >= blocks.size - 1) return
        val temp = blocks[blockIndex]
        blocks[blockIndex] = blocks[blockIndex + 1].copy(block = blocks[blockIndex + 1].block.copy(orderIndex = blockIndex))
        blocks[blockIndex + 1] = temp.copy(block = temp.block.copy(orderIndex = blockIndex + 1))
        _uiState.update { it.copy(blocks = blocks) }
    }

    // ── Exercise Operations ─────────────────────────────────────────────────

    fun addExercise(blockIndex: Int, type: IntervalType = IntervalType.WORK) {
        val blocks = _uiState.value.blocks.toMutableList()
        val block = blocks[blockIndex]
        val newExercise = UiExercise(
            exercise = WorkoutExercise(
                blockId = block.block.id,
                orderIndex = block.exercises.size,
                title = when (type) {
                    IntervalType.WARMUP -> "Pemanasan"
                    IntervalType.WORK -> "Gerakan Baru"
                    IntervalType.REST -> "Istirahat"
                    IntervalType.COOLDOWN -> "Pendinginan"
                },
                durationSeconds = when (type) {
                    IntervalType.REST -> 15
                    IntervalType.WARMUP, IntervalType.COOLDOWN -> 30
                    IntervalType.WORK -> 30
                },
                type = type
            )
        )
        blocks[blockIndex] = block.copy(exercises = block.exercises + newExercise)
        _uiState.update { it.copy(blocks = blocks) }
        recalculateStats()
    }

    fun addExerciseFromTemplate(blockIndex: Int, template: ExerciseTemplate, type: IntervalType = IntervalType.WORK) {
        val blocks = _uiState.value.blocks.toMutableList()
        val block = blocks[blockIndex]
        val newExercise = UiExercise(
            exercise = WorkoutExercise(
                blockId = block.block.id,
                orderIndex = block.exercises.size,
                title = template.name,
                durationSeconds = template.defaultDuration,
                type = type,
                templateId = template.id
            ),
            resolvedMediaUri = template.mediaUri,
            resolvedMediaType = template.mediaType,
            thumbnailUri = template.thumbnailUri
        )
        blocks[blockIndex] = block.copy(exercises = block.exercises + newExercise)
        _uiState.update { it.copy(blocks = blocks) }
        recalculateStats()
        viewModelScope.launch {
            repository.incrementTemplateUsage(template.id)
        }
    }

    fun removeExercise(blockIndex: Int, exerciseIndex: Int) {
        val blocks = _uiState.value.blocks.toMutableList()
        val block = blocks[blockIndex]
        val exercises = block.exercises.toMutableList()
        exercises.removeAt(exerciseIndex)
        val reindexed = exercises.mapIndexed { i, ex ->
            ex.copy(exercise = ex.exercise.copy(orderIndex = i))
        }
        blocks[blockIndex] = block.copy(exercises = reindexed)
        _uiState.update { it.copy(blocks = blocks) }
        recalculateStats()
        showSnackbar("Gerakan dihapus")
    }

    fun duplicateExercise(blockIndex: Int, exerciseIndex: Int) {
        val blocks = _uiState.value.blocks.toMutableList()
        val block = blocks[blockIndex]
        val exercises = block.exercises.toMutableList()
        val source = exercises[exerciseIndex]
        val duplicate = source.copy(
            exercise = source.exercise.copy(id = 0, orderIndex = exerciseIndex + 1)
        )
        exercises.add(exerciseIndex + 1, duplicate)
        val reindexed = exercises.mapIndexed { i, ex ->
            ex.copy(exercise = ex.exercise.copy(orderIndex = i))
        }
        blocks[blockIndex] = block.copy(exercises = reindexed)
        _uiState.update { it.copy(blocks = blocks) }
        recalculateStats()
    }

    fun onExerciseFocused(blockIndex: Int, exerciseIndex: Int) {
        val currentTitle = _uiState.value.blocks.getOrNull(blockIndex)
            ?.exercises?.getOrNull(exerciseIndex)?.exercise?.title.orEmpty()
        val query = if (currentTitle == "Gerakan Baru") "" else currentTitle
        searchAutocomplete(query)
    }

    fun updateExerciseTitle(blockIndex: Int, exerciseIndex: Int, title: String) {
        updateExercise(blockIndex, exerciseIndex) { ex ->
            ex.copy(exercise = ex.exercise.copy(title = title))
        }
        searchAutocomplete(title)
    }

    fun updateExerciseDuration(blockIndex: Int, exerciseIndex: Int, seconds: Int) {
        updateExercise(blockIndex, exerciseIndex) { ex ->
            ex.copy(exercise = ex.exercise.copy(durationSeconds = seconds.coerceIn(1, 3600)))
        }
        recalculateStats()
    }

    fun addDurationPreset(blockIndex: Int, exerciseIndex: Int, addSeconds: Int) {
        val current = _uiState.value.blocks
            .getOrNull(blockIndex)?.exercises?.getOrNull(exerciseIndex)?.exercise?.durationSeconds ?: 0
        updateExerciseDuration(blockIndex, exerciseIndex, current + addSeconds)
    }

    fun setDurationPreset(blockIndex: Int, exerciseIndex: Int, seconds: Int) {
        updateExerciseDuration(blockIndex, exerciseIndex, seconds)
    }

    fun updateExerciseType(blockIndex: Int, exerciseIndex: Int, type: IntervalType) {
        updateExercise(blockIndex, exerciseIndex) { ex ->
            ex.copy(exercise = ex.exercise.copy(type = type))
        }
    }

    fun applyTemplate(blockIndex: Int, exerciseIndex: Int, template: ExerciseTemplate) {
        updateExercise(blockIndex, exerciseIndex) { ex ->
            ex.copy(
                exercise = ex.exercise.copy(
                    title = template.name,
                    durationSeconds = template.defaultDuration,
                    templateId = template.id
                ),
                resolvedMediaUri = template.mediaUri,
                resolvedMediaType = template.mediaType,
                thumbnailUri = template.thumbnailUri
            )
        }
        recalculateStats()
        clearAutocomplete()
        viewModelScope.launch {
            repository.incrementTemplateUsage(template.id)
        }
    }

    fun moveExercise(blockIndex: Int, from: Int, to: Int) {
        val blocks = _uiState.value.blocks.toMutableList()
        val block = blocks[blockIndex]
        val exercises = block.exercises.toMutableList()
        val item = exercises.removeAt(from)
        exercises.add(to, item)
        val reindexed = exercises.mapIndexed { i, ex ->
            ex.copy(exercise = ex.exercise.copy(orderIndex = i))
        }
        blocks[blockIndex] = block.copy(exercises = reindexed)
        _uiState.update { it.copy(blocks = blocks) }
        recalculateStats()
    }

    // ── Media Management (F-05) ─────────────────────────────────────────────

    fun createTempCameraVideoUri(): Pair<File, Uri> = mediaFileManager.createTempCameraVideoUri()

    fun attachMediaToExercise(blockIndex: Int, exerciseIndex: Int, uri: Uri) {
        viewModelScope.launch {
            when (val result = mediaFileManager.importMedia(uri)) {
                is MediaFileManager.ImportResult.Success -> {
                    updateExercise(blockIndex, exerciseIndex) { ex ->
                        ex.copy(
                            exercise = ex.exercise.copy(
                                customMediaUri = result.savedPath,
                                customMediaType = result.mediaType
                            ),
                            resolvedMediaUri = result.savedPath,
                            resolvedMediaType = result.mediaType,
                            thumbnailUri = result.thumbnailPath
                        )
                    }
                    val typeLabel = if (result.mediaType == MediaType.GIF) "GIF" else "Video"
                    showSnackbar("Berhasil menambahkan $typeLabel!")
                }
                is MediaFileManager.ImportResult.ValidationError -> {
                    showSnackbar(result.message)
                }
                is MediaFileManager.ImportResult.Error -> {
                    showSnackbar("Gagal memproses media: ${result.message}")
                }
            }
        }
    }

    fun removeMediaFromExercise(blockIndex: Int, exerciseIndex: Int) {
        updateExercise(blockIndex, exerciseIndex) { ex ->
            ex.copy(
                exercise = ex.exercise.copy(
                    customMediaUri = null,
                    customMediaType = MediaType.NONE
                ),
                resolvedMediaUri = null,
                resolvedMediaType = MediaType.NONE,
                thumbnailUri = null
            )
        }
        showSnackbar("Media dilepas dari gerakan")
    }

    // ── Autocomplete ────────────────────────────────────────────────────────

    private var searchJob: Job? = null

    fun searchAutocomplete(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cleanQuery = query.trim().let { if (it == "Gerakan Baru") "" else it }
            repository.searchTemplates(cleanQuery).collect { templates ->
                _uiState.update { it.copy(autocompleteSuggestions = templates, autocompleteQuery = query) }
            }
        }
    }

    fun clearAutocomplete() {
        searchJob?.cancel()
        _uiState.update { it.copy(autocompleteSuggestions = emptyList(), autocompleteQuery = "") }
    }

    // ── Save ────────────────────────────────────────────────────────────────

    fun saveRoutine(startImmediately: Boolean = false) {
        val state = _uiState.value
        if (state.routineName.isBlank()) {
            showSnackbar("Nama rutinitas tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // 1. Save/update routine header
                val savedRoutineId = if (routineId != null) {
                    val existing = repository.getRoutineById(routineId) ?: return@launch
                    repository.updateRoutine(
                        existing.copy(
                            name = state.routineName,
                            category = state.category,
                            accentColor = state.accentColor,
                            totalDurationSec = state.totalDurationSec,
                            totalExercises = state.totalExercises
                        )
                    )
                    routineId
                } else {
                    repository.saveRoutine(
                        WorkoutRoutine(
                            name = state.routineName,
                            category = state.category,
                            accentColor = state.accentColor,
                            totalDurationSec = state.totalDurationSec,
                            totalExercises = state.totalExercises
                        )
                    )
                }

                // 2. Delete existing blocks (untuk edit mode) — CASCADE hapus exercises juga
                if (routineId != null) {
                    repository.deleteAllBlocksByRoutineId(routineId)
                }


                // 3. Insert semua blok dan exercise baru
                state.blocks.forEachIndexed { blockIdx, uiBlock ->
                    val savedBlockId = repository.insertBlock(
                        uiBlock.block.copy(routineId = savedRoutineId, orderIndex = blockIdx)
                    )
                    uiBlock.exercises.forEachIndexed { exIdx, uiEx ->
                        repository.insertExercise(
                            uiEx.exercise.copy(blockId = savedBlockId, orderIndex = exIdx)
                        )
                    }
                }

                _uiState.update { it.copy(
                    isSaving = false,
                    navigateToPlayer = if (startImmediately) savedRoutineId else null
                )}
                if (!startImmediately) showSnackbar("Rutinitas disimpan!")
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                showSnackbar("Gagal menyimpan: ${e.message}")
            }
        }
    }

    fun clearNavigation() = _uiState.update { it.copy(navigateToPlayer = null) }

    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private fun updateExercise(blockIndex: Int, exerciseIndex: Int, transform: (UiExercise) -> UiExercise) {
        val blocks = _uiState.value.blocks.mapIndexed { bi, block ->
            if (bi == blockIndex) {
                block.copy(
                    exercises = block.exercises.mapIndexed { ei, ex ->
                        if (ei == exerciseIndex) transform(ex) else ex
                    }
                )
            } else block
        }
        _uiState.update { it.copy(blocks = blocks) }
    }

    private fun recalculateStats() {
        val blocks = _uiState.value.blocks
        val totalDuration = blocks.sumOf { block ->
            block.exercises.sumOf { it.exercise.durationSeconds } * block.block.repeatCount
        }
        val totalExercises = blocks.sumOf { it.exercises.size }
        _uiState.update { it.copy(totalDurationSec = totalDuration, totalExercises = totalExercises) }
    }

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }
}
