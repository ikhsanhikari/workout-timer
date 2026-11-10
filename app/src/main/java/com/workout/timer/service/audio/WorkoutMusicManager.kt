package com.workout.timer.service.audio

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkoutMusicManager — Pengelola Background Music (BGM) Adaptif Latihan.
 *
 * Fitur:
 * 1. Pemutaran seamless loop musik berenergi tinggi dari assets.
 * 2. Adaptasi Volume Fase: Volume penuh (80%) saat WORK, meredup halus (22%) saat REST.
 * 3. Smart Audio Ducking: Otomatis meredup sejenak saat hitung mundur 3-2-1 / peluit GO.
 * 4. Smooth Fade In / Fade Out pada saat Pause / Resume / Stop.
 * 5. Deteksi aplikasi musik eksternal (Spotify / YouTube Music).
 */
@Singleton
class WorkoutMusicManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var player: ExoPlayer? = null
    private var fadeJob: Job? = null
    private var duckJob: Job? = null

    var isBgmEnabled: Boolean = true
        private set

    private var currentPhase: IntervalType = IntervalType.WORK
    private var isWorkoutActive: Boolean = false
    private var isPaused: Boolean = false
    private var isDucked: Boolean = false

    companion object {
        private const val TAG = "WorkoutMusicManager"
        private const val BGM_ASSET_URI = "file:///android_asset/music/workout_bgm_energy.mp3"

        const val VOLUME_WORK = 0.80f
        const val VOLUME_REST = 0.22f
        const val VOLUME_WARMUP = 0.50f
        const val VOLUME_DUCK = 0.10f
    }

    init {
        scope.launch {
            preferencesRepository.bgmEnabledFlow.collectLatest { enabled ->
                isBgmEnabled = enabled
                if (!enabled) {
                    fadeToVolume(0f, 250L) {
                        player?.pause()
                    }
                } else if (isWorkoutActive && !isPaused) {
                    val p = ensurePlayerReady()
                    if (p.playbackState == Player.STATE_IDLE) {
                        p.prepare()
                    }
                    p.play()
                    fadeToVolume(getTargetVolumeForCurrentPhase(), 350L)
                }
            }
        }
    }

    private fun getTargetVolumeForCurrentPhase(): Float {
        return when (currentPhase) {
            IntervalType.WORK -> VOLUME_WORK
            IntervalType.REST -> VOLUME_REST
            IntervalType.WARMUP, IntervalType.COOLDOWN -> VOLUME_WARMUP
        }
    }

    @Synchronized
    private fun ensurePlayerReady(): ExoPlayer {
        val existing = player
        if (existing == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            player = ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(audioAttributes, false) // Mengelola audio ducking secara internal
                val mediaItem = MediaItem.fromUri(Uri.parse(BGM_ASSET_URI))
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                prepare()
            }
        } else {
            if (existing.mediaItemCount == 0) {
                val mediaItem = MediaItem.fromUri(Uri.parse(BGM_ASSET_URI))
                existing.setMediaItem(mediaItem)
                existing.prepare()
            } else if (existing.playbackState == Player.STATE_IDLE) {
                existing.prepare()
            }
        }
        return player!!
    }

    /**
     * Mulai memutar musik saat rutinitas latihan dimulai.
     */
    fun startMusic() {
        isWorkoutActive = true
        isPaused = false
        isDucked = false
        currentPhase = IntervalType.WORK

        if (!isBgmEnabled) {
            Log.d(TAG, "BGM dinonaktifkan oleh pengguna, melewati start.")
            return
        }

        scope.launch {
            try {
                fadeJob?.cancel()
                duckJob?.cancel()
                val p = ensurePlayerReady()
                if (p.playbackState == Player.STATE_IDLE) {
                    p.prepare()
                }
                p.seekTo(0)
                p.play()
                fadeToVolume(VOLUME_WORK, 400L)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memulai BGM: ${e.message}", e)
            }
        }
    }

    /**
     * Menyesuaikan volume berdasarkan fase latihan (WORK vs REST vs WARMUP).
     */
    fun setPhase(phase: IntervalType) {
        currentPhase = phase
        if (!isWorkoutActive || !isBgmEnabled || isPaused || isDucked) return

        val targetVol = getTargetVolumeForCurrentPhase()
        fadeToVolume(targetVol, 400L)
    }

    /**
     * Pause musik saat timer di-pause.
     */
    fun pause() {
        if (!isWorkoutActive) return
        isPaused = true

        fadeToVolume(0f, 200L) {
            player?.pause()
        }
    }

    /**
     * Resume musik saat timer dilanjutkan.
     */
    fun resume() {
        if (!isWorkoutActive) return
        isPaused = false

        if (!isBgmEnabled) return

        scope.launch {
            val p = ensurePlayerReady()
            if (p.playbackState == Player.STATE_IDLE) {
                p.prepare()
            }
            p.play()
            val target = getTargetVolumeForCurrentPhase()
            fadeToVolume(target, 300L)
        }
    }

    /**
     * Smart Audio Ducking: Meredupkan musik sejenak saat suara Beep 3-2-1 atau peluit GO berbunyi.
     */
    fun duck(durationMs: Long = 400L) {
        if (!isWorkoutActive || !isBgmEnabled || isPaused) return

        duckJob?.cancel()
        duckJob = scope.launch {
            isDucked = true
            fadeToVolume(VOLUME_DUCK, 100L)
            delay(durationMs)
            isDucked = false
            if (isWorkoutActive && !isPaused && isBgmEnabled) {
                fadeToVolume(getTargetVolumeForCurrentPhase(), 250L)
            }
        }
    }

    /**
     * Hentikan musik saat latihan selesai atau dihentikan.
     * Melepaskan resource player sepenuhnya agar sesi berikutnya selalu bersih dari nol.
     */
    fun stop() {
        isWorkoutActive = false
        isPaused = false
        isDucked = false

        fadeJob?.cancel()
        duckJob?.cancel()

        scope.launch {
            try {
                val p = player
                player = null
                if (p != null) {
                    p.volume = 0f
                    p.pause()
                    p.stop()
                    p.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error membersihkan player di stop(): ${e.message}")
            }
        }
    }

    /**
     * Toggle musik aktif / mati (dari tombol UI Player).
     */
    fun toggleBgm() {
        scope.launch {
            preferencesRepository.setBgmEnabled(!isBgmEnabled)
        }
    }

    private fun fadeToVolume(targetVolume: Float, durationMs: Long, onComplete: (() -> Unit)? = null) {
        fadeJob?.cancel()
        val p = player ?: return

        fadeJob = scope.launch {
            val startVolume = p.volume
            val steps = 15
            val stepDelay = durationMs / steps
            val volumeDelta = (targetVolume - startVolume) / steps

            for (i in 1..steps) {
                p.volume = (startVolume + (volumeDelta * i)).coerceIn(0f, 1f)
                delay(stepDelay)
            }
            p.volume = targetVolume.coerceIn(0f, 1f)
            onComplete?.invoke()
        }
    }

    fun release() {
        stop()
    }
}

