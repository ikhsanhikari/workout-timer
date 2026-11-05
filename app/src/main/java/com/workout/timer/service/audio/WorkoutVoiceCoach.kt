package com.workout.timer.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.workout.timer.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkoutVoiceCoach — Pelatih Suara Lokal (Text-To-Speech) 100% Offline (Prioritas 2).
 *
 * Fitur:
 * 1. 100% Lokal & Beban Kuota Nol: Menggunakan mesin Android native TextToSpeech.
 * 2. Cerdas & Tanggap Konteks: Mengumumkan gerakan yang akan datang saat fase istirahat,
 *    aba-aba mulai, checkpoint setengah jalan, dan ucapan selamat saat selesai.
 * 3. Smart Audio Ducking: Musik latar otomatis meredup halus saat pelatih berbicara.
 * 4. Dukungan Bahasa Indonesia dengan fallback cerdas ke Locale sistem.
 * 5. Hemat Daya & Thread-Safe: Eksekusi async tanpa membebani rendering 60fps.
 */
@Singleton
class WorkoutVoiceCoach @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val workoutMusicManager: WorkoutMusicManager
) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tts: TextToSpeech? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    var isVoiceCoachEnabled: Boolean = true
        private set

    companion object {
        private const val TAG = "WorkoutVoiceCoach"
    }

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menginisialisasi TextToSpeech: ${e.message}")
        }

        // Observasi toggle Voice Coach dari preferensi pengguna
        scope.launch {
            preferencesRepository.voiceCoachEnabledFlow.collectLatest { enabled ->
                isVoiceCoachEnabled = enabled
                if (!enabled) {
                    stop()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            try {
                // Konfigurasi AudioAttributes untuk speech guidance
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)

                // Coba Bahasa Indonesia terlebih dahulu
                val idLocale = Locale("id", "ID")
                val langResult = engine.setLanguage(idLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Bahasa Indonesia tidak tersedia di mesin TTS bawaan. Menggunakan locale default.")
                    engine.setLanguage(Locale.getDefault())
                }

                // Pengaturan artikulasi suara atletik & energetik
                engine.setSpeechRate(1.05f)
                engine.setPitch(1.02f)

                // Listener untuk Smart Audio Ducking otomatis saat suara bicara
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        workoutMusicManager.duck(1800L)
                    }

                    override fun onDone(utteranceId: String?) { }

                    @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                    override fun onError(utteranceId: String?) { }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.w(TAG, "TTS Utterance error [$errorCode] on utterance: $utteranceId")
                    }
                })

                isInitialized = true
                Log.d(TAG, "WorkoutVoiceCoach berhasil diinisialisasi dan siap digunakan.")
            } catch (e: Exception) {
                Log.e(TAG, "Error saat konfigurasi TTS: ${e.message}")
            }
        } else {
            Log.e(TAG, "Inisialisasi TTS gagal dengan kode status: $status")
        }
    }

    /**
     * Ucapkan teks tertentu secara aman & asinkron.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isVoiceCoachEnabled || !isInitialized) return
        val engine = tts ?: return

        try {
            val utteranceId = UUID.randomUUID().toString()
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            engine.speak(text, queueMode, params, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengucapkan audio cue: ${e.message}")
        }
    }

    /**
     * Hentikan suara yang sedang berbicara saat ini.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) { }
    }

    // ─── ABA-ABA SUARA KHUSUS FASE LATIHAN ─────────────────────────────────────

    /**
     * Pengumuman gerakan pertama saat fase hitung mundur persiapan dimulai.
     */
    fun announcePreparation(firstExerciseTitle: String) {
        val clean = cleanTitle(firstExerciseTitle)
        speak("Gerakan pertama: $clean. Bersiap.")
    }

    /**
     * Pengumuman gerakan yang akan datang di detik ke-5 fase istirahat.
     */
    fun announceNextExercise(nextExerciseTitle: String) {
        val clean = cleanTitle(nextExerciseTitle)
        speak("Bersiap: $clean.")
    }

    /**
     * Aba-aba saat fase latihan aktif (WORK) dimulai.
     */
    fun announceStart() {
        speak("Mulai!")
    }

    /**
     * Aba-aba saat masuk ke fase istirahat (REST).
     */
    fun announceRest() {
        speak("Istirahat.")
    }

    /**
     * Pengingat di pertengahan waktu (50% durasi gerakan).
     */
    fun announceHalfway() {
        speak("Setengah jalan, pertahankan!")
    }

    /**
     * Pengumuman saat seluruh rutinitas latihan selesai.
     */
    fun announceCompleted() {
        speak("Latihan selesai! Kerja bagus!")
    }

    /**
     * Aba-aba saat timer di-pause.
     */
    fun announcePaused() {
        speak("Latihan dijeda.")
    }

    /**
     * Aba-aba saat timer dilanjutkan (resume).
     */
    fun announceResumed() {
        speak("Lanjut!")
    }

    /**
     * Membersihkan simbol, tanda strip, atau karakter teknis agar pengucapan TTS terdengar alami.
     */
    private fun cleanTitle(title: String): String {
        return title
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("\\(.*?\\)"), "") // Hilangkan teks dalam kurung jika ada
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Lepaskan resource TTS saat service dihancurkan.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) { }
    }
}

