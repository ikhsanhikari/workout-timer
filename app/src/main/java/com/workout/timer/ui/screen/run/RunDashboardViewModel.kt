package com.workout.timer.ui.screen.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.timer.data.model.RunSession
import com.workout.timer.data.repository.RunRepository
import com.workout.timer.service.RunTrackingService
import com.workout.timer.service.RunTrackingState
import com.workout.timer.service.location.OfflineRegion
import com.workout.timer.service.location.OfflineTileManager
import com.workout.timer.service.location.TileDownloadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import javax.inject.Inject

data class RunDashboardUiState(
    val totalDistanceKm: Double = 0.0,
    val totalCompletedRuns: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val recentSessions: List<RunSession> = emptyList(),
    val activeTrackingState: RunTrackingState = RunTrackingState(),
    val offlineCacheSizeMb: Float = 0f,
    val downloadProgress: TileDownloadProgress = TileDownloadProgress(),
    val showDownloadSheet: Boolean = false,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val sortedRegions: List<OfflineRegion> = emptyList()
)

@HiltViewModel
class RunDashboardViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val offlineTileManager: OfflineTileManager,
    private val locationTracker: com.workout.timer.service.location.LocationTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunDashboardUiState())
    val uiState: StateFlow<RunDashboardUiState> = _uiState.asStateFlow()

    init {
        observeRunData()
        observeActiveTracking()
        refreshCacheSize()
        loadInitialRegions()
    }

    private fun loadInitialRegions() {
        _uiState.update {
            it.copy(sortedRegions = offlineTileManager.presetRegions)
        }
    }

    private fun observeRunData() {
        viewModelScope.launch {
            combine(
                runRepository.getTotalDistanceMeters(),
                runRepository.getTotalCompletedRuns(),
                runRepository.getTotalCaloriesBurned(),
                runRepository.getAllSessions()
            ) { distanceMeters, runsCount, calories, allSessions ->
                val distanceKm = (distanceMeters ?: 0.0) / 1000.0
                _uiState.update { current ->
                    current.copy(
                        totalDistanceKm = distanceKm,
                        totalCompletedRuns = runsCount,
                        totalCaloriesBurned = calories ?: 0,
                        recentSessions = allSessions.take(5)
                    )
                }
            }.collect()
        }
    }

    private fun observeActiveTracking() {
        viewModelScope.launch {
            RunTrackingService.trackingState.collect { state ->
                _uiState.update { it.copy(activeTrackingState = state) }
            }
        }
        viewModelScope.launch {
            offlineTileManager.downloadState.collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
                if (!progress.isDownloading && progress.progress == 1f) {
                    refreshCacheSize()
                }
            }
        }
    }

    fun openDownloadSheet() {
        locationTracker.getLastLocation { loc ->
            val lat = loc?.latitude ?: _uiState.value.activeTrackingState.currentLat
            val lng = loc?.longitude ?: _uiState.value.activeTrackingState.currentLng
            val sorted = offlineTileManager.getRegionsSortedByProximity(lat, lng)
            _uiState.update {
                it.copy(
                    showDownloadSheet = true,
                    userLat = lat,
                    userLng = lng,
                    sortedRegions = sorted
                )
            }
        }
        refreshCacheSize()
    }

    fun closeDownloadSheet() {
        _uiState.update { it.copy(showDownloadSheet = false) }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val size = offlineTileManager.getCacheSizeMb()
            _uiState.update { it.copy(offlineCacheSizeMb = size) }
        }
    }

    fun downloadRegion(region: OfflineRegion) {
        offlineTileManager.downloadRegion(region)
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            offlineTileManager.clearCache()
            refreshCacheSize()
        }
    }
}
