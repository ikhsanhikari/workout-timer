package com.workout.timer.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.workout.timer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioHapticManager — Manajemen Audio Cue Studio Modern, Smart Audio Ducking, dan Haptic Feedback.
 *
 * Fitur:
 * 1. Sound Cue dengan latensi ultra-rendah menggunakan Android SoundPool (bebas nada peluit).
 * 2. Smart Audio Ducking: Otomatis meredupkan musik latar (BGM / Spotify) saat audio cue berbunyi.
 * 3. Haptic Feedback presisi (Light Tick untuk detik 3, 2, 1, Heavy Vibration untuk Start, Double Pulse untuk Rest).
 */
@Singleton
class AudioHapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutMusicManager: WorkoutMusicManager
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var soundPool: SoundPool? = null
    private var cueCountdownId: Int = 0
    private var cueStartId: Int = 0
    private var cueRestId: Int = 0
    private var cueCompleteId: Int = 0
    private val loadedSoundIds = java.util.Collections.synchronizedSet(HashSet<Int>())

    @Volatile
    private var fallbackToneGenerator: android.media.ToneGenerator? = null

    private fun getFallbackToneGenerator(): android.media.ToneGenerator? {
        if (fallbackToneGenerator == null) {
            fallbackToneGenerator = try {
                android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            } catch (_: Exception) {
                null
            }
        }
        return fallbackToneGenerator
    }

    private var focusRequest: AudioFocusRequest? = null

    init {
        initAudioFocusRequest()
        initSoundPool()
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            loadedSoundIds.add(sampleId)
                        }
                    }
                    cueCountdownId = load(context, R.raw.cue_countdown, 1)
                    cueStartId = load(context, R.raw.cue_start, 1)
                    cueRestId = load(context, R.raw.cue_rest, 1)
                    cueCompleteId = load(context, R.raw.cue_complete, 1)
                }
        } catch (_: Exception) {
            // Fallback fail-safe
        }
    }

    private fun initAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { /* No-op, auto ducks */ }
                .build()
        }
    }

    /**
     * Meminta audio focus ducking sementara (musik latar mengecil selama duckDurationMs).
     */
    private fun requestDucking(duckDurationMs: Long = 400L) {
        workoutMusicManager.duck(duckDurationMs)
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.requestAudioFocus(focusRequest!!)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                }

                delay(duckDurationMs)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest!!)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(null)
                }
            } catch (_: Exception) {
                // Fail-safe gracefully if audio focus is rejected
            }
        }
    }

    /**
     * Nada 'ding' lembut & nyaring untuk hitung mundur detik ke 3, 2, 1 (SoundPool modern chime).
     */
    fun playCountdownBeep() {
        requestDucking(300L)
        try {
            val streamId = if (loadedSoundIds.contains(cueCountdownId)) {
                soundPool?.play(cueCountdownId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
            } else 0

            if (streamId == 0) {
                getFallbackToneGenerator()?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 120)
            }
        } catch (_: Exception) { }
        vibrateLightTick()
    }

    /**
     * Nada modern berenergi (power chime chord) saat fase WORK dimulai (GO!). Bebas suara peluit.
     */
    fun playStartTone() {
        requestDucking(550L)
        try {
            val streamId = if (loadedSoundIds.contains(cueStartId)) {
                soundPool?.play(cueStartId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
            } else 0

            if (streamId == 0) {
                getFallbackToneGenerator()?.startTone(android.media.ToneGenerator.TONE_PROP_PROMPT, 300)
            }
        } catch (_: Exception) { }
        vibrateHeavy()
    }

    /**
     * Nada relaksasi lembut (descending bowl chime) saat masuk ke fase REST (Istirahat).
     */
    fun playRestTone() {
        requestDucking(500L)
        try {
            val streamId = if (loadedSoundIds.contains(cueRestId)) {
                soundPool?.play(cueRestId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
            } else 0

            if (streamId == 0) {
                getFallbackToneGenerator()?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
            }
        } catch (_: Exception) { }
        vibrateDoublePulse()
    }

    /**
     * Nada perayaan megah (victory arpeggio fanfare) saat seluruh latihan selesai (COMPLETED).
     */
    fun playWorkoutCompleteTone() {
        requestDucking(1400L)
        try {
            val streamId = if (loadedSoundIds.contains(cueCompleteId)) {
                soundPool?.play(cueCompleteId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
            } else 0

            if (streamId == 0) {
                getFallbackToneGenerator()?.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 500)
            }
        } catch (_: Exception) { }
        vibrateCompletion()
    }

    // ─── HAPTIC FEEDBACK ─────────────────────────────────────────────────────

    fun vibrateLightTick() {
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(35)
        }
    }

    fun vibrateHeavy() {
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }

    fun vibrateDoublePulse() {
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 100), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 80, 50, 100), -1)
        }
    }

    fun vibrateCompletion() {
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120, 80, 250), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 120, 80, 120, 80, 250), -1)
        }
    }

    @Synchronized
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            loadedSoundIds.clear()
            fallbackToneGenerator?.release()
            fallbackToneGenerator = null
        } catch (_: Exception) { }
    }
}
