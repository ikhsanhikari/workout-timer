package com.workout.timer.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioHapticManager — Manajemen Audio Cue, Smart Audio Ducking, dan Haptic Feedback (PRD 4.2.5 & 4.2.1).
 *
 * Mengatur:
 * 1. Sound Cue dengan latensi ultra-rendah (ToneGenerator STREAM_ALARM / MUSIC).
 * 2. Smart Audio Ducking: Menggunakan AudioManager dengan flag AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
 *    agar musik latar (Spotify, YouTube Music) hanya mengecil sementara selama ~400ms dan
 *    otomatis kembali normal tanpa mematikan pemutaran musik.
 * 3. Haptic Feedback presisi (Light Tick untuk detik 3, 2, 1 dan Heavy Vibration untuk transisi fase).
 */
@Singleton
class AudioHapticManager @Inject constructor(
    @ApplicationContext private val context: Context
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

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    private fun getToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            toneGenerator = try {
                ToneGenerator(AudioManager.STREAM_ALARM, 85)
            } catch (_: Exception) {
                null
            }
        }
        return toneGenerator
    }

    private var focusRequest: AudioFocusRequest? = null

    init {
        initAudioFocusRequest()
    }

    private fun initAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.requestAudioFocus(focusRequest!!)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_ALARM,
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
     * Beep pendek untuk hitung mundur detik ke 3, 2, 1.
     */
    fun playCountdownBeep() {
        requestDucking(250L)
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_CDMA_PIP, 120)
        } catch (_: Exception) { }
        vibrateLightTick()
    }

    /**
     * Beep tegas bernada tinggi saat fase WORK dimulai (GO!).
     */
    fun playStartTone() {
        requestDucking(500L)
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        } catch (_: Exception) { }
        vibrateHeavy()
    }

    /**
     * Nada penanda masuk ke fase REST (Istirahat).
     */
    fun playRestTone() {
        requestDucking(400L)
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
        } catch (_: Exception) { }
        vibrateDoublePulse()
    }

    /**
     * Nada perayaan saat seluruh sesi latihan selesai (COMPLETED).
     */
    fun playWorkoutCompleteTone() {
        requestDucking(800L)
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 600)
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
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) { }
    }
}
