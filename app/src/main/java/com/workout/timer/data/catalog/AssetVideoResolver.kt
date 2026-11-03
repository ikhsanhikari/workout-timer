package com.workout.timer.data.catalog

import android.util.Log
import com.workout.timer.data.database.dao.ExerciseTemplateDao
import com.workout.timer.data.model.IntervalType
import com.workout.timer.data.model.MediaType

/**
 * Single source of truth untuk pemetaan video panduan AI (MP4) beresolusi tinggi di assets.
 * Memastikan aplikasi SELALU memprioritaskan dan memutar video MP4 AI daripada animasi GIF.
 */
object AssetVideoResolver {

    private const val TAG = "AssetVideoResolver"
    const val ASSET_VIDEO_DIR = "file:///android_asset/exercise_ai/"

    const val WORKOUT_INTRO_URI = "file:///android_asset/exercise_ai/workout_intro.mp4"
    const val WORKOUT_COMPLETE_URI = "file:///android_asset/exercise_ai/workout_complete.mp4"

    // 84 File Video MP4 yang tersedia di folder assets/exercise_ai/
    private val AVAILABLE_VIDEOS = setOf(
        "active-recovery-jog.mp4",
        "arm-shake-recovery.mp4",
        "back-extension-row.mp4",
        "bear-crawl-alt.mp4",
        "bear-crawl.mp4",
        "bicycle-crunches.mp4",
        "bird-dog-hold.mp4",
        "bird-dog.mp4",
        "boat-pose.mp4",
        "bodyweight-squat.mp4",
        "broad-jump.mp4",
        "bulgarian-split-squat.mp4",
        "burpee-tuck-jumps.mp4",
        "burpees.mp4",
        "butt-kicks.mp4",
        "calf-raises.mp4",
        "cat-cow-stretch.mp4",
        "chest-opener-stretch.mp4",
        "childs-pose-alt.mp4",
        "childs-pose.mp4",
        "chin-tucks-neck-rolls.mp4",
        "clamshell.mp4",
        "cossack-squat.mp4",
        "crunches.mp4",
        "curtsy-lunges.mp4",
        "dead-bug.mp4",
        "deep-breathing.mp4",
        "diamond-push-up.mp4",
        "downward-dog-to-cobra.mp4",
        "duck-and-weave.mp4",
        "duck-walk.mp4",
        "flutter-kicks.mp4",
        "forearm-plank-reach.mp4",
        "frog-pumps.mp4",
        "glute-bridge.mp4",
        "hand-release-push-up.mp4",
        "hanging-leg-raises.mp4",
        "high-rows.mp4",
        "hindu-push-up.mp4",
        "hollow-body-hold.mp4",
        "jump-lunge.mp4",
        "jump-rope-hops.mp4",
        "jump-squats.mp4",
        "jumping-jacks.mp4",
        "lateral-lunges.mp4",
        "leg-raise.mp4",
        "lunge-with-twist.mp4",
        "lunges.mp4",
        "modified-curl-up.mp4",
        "mountain-climbers.mp4",
        "pike-push-up.mp4",
        "plank-hip-dips.mp4",
        "plank-shoulder-taps.mp4",
        "plank-to-push-up.mp4",
        "plank.mp4",
        "pull-up.mp4",
        "push-up-and-rotation.mp4",
        "push-up-male.mp4",
        "push-up.mp4",
        "rest.mp4",
        "russian-twist.mp4",
        "scissors.mp4",
        "seated-spinal-twist-alt.mp4",
        "seated-spinal-twist.mp4",
        "shadow-boxing.mp4",
        "shoulder-rolls-squeeze.mp4",
        "side-leg-lift.mp4",
        "side-plank.mp4",
        "skater-jumps.mp4",
        "sphinx-push-up.mp4",
        "spiderman-push-up.mp4",
        "split-squat.mp4",
        "standing-crunch.mp4",
        "standing-quad-stretch.mp4",
        "stationary-sprint.mp4",
        "step-ups.mp4",
        "sumo-squat.mp4",
        "swimmer.mp4",
        "thoracic-spine-twist.mp4",
        "triceps-dip.mp4",
        "tuck-jumps.mp4",
        "wall-sit.mp4",
        "wide-arm-push-up.mp4",
        "yoga-flow.mp4"
    )

    // Pemetaan nama/judul variasi gerakan ke nama file video di atas
    private val TITLE_TO_VIDEO_MAP = mapOf(
        // Push-ups & Upper
        "push-up" to "push-up.mp4",
        "push up" to "push-up.mp4",
        "push-ups" to "push-up.mp4",
        "pushups" to "push-up.mp4",
        "push up standar" to "push-up.mp4",
        "push-up-ringan" to "push-up.mp4",
        "push up ringan" to "push-up.mp4",
        "diamond-push-up" to "diamond-push-up.mp4",
        "diamond push-up" to "diamond-push-up.mp4",
        "diamond push up" to "diamond-push-up.mp4",
        "close grip push up" to "diamond-push-up.mp4",
        "close-grip-push-up" to "diamond-push-up.mp4",
        "pike-push-up" to "pike-push-up.mp4",
        "pike push up" to "pike-push-up.mp4",
        "pike push-up" to "pike-push-up.mp4",
        "hindu-push-up" to "hindu-push-up.mp4",
        "hindu push up" to "hindu-push-up.mp4",
        "sphinx-push-up" to "sphinx-push-up.mp4",
        "sphinx push up" to "sphinx-push-up.mp4",
        "spiderman-push-up" to "spiderman-push-up.mp4",
        "spiderman push up" to "spiderman-push-up.mp4",
        "hand-release-push-up" to "hand-release-push-up.mp4",
        "hand release push up" to "hand-release-push-up.mp4",
        "triceps-dip" to "triceps-dip.mp4",
        "triceps dip" to "triceps-dip.mp4",
        "tricep dips" to "triceps-dip.mp4",
        "pull-up" to "pull-up.mp4",
        "pull up" to "pull-up.mp4",
        "pull-ups" to "pull-up.mp4",
        "shadow-boxing" to "shadow-boxing.mp4",
        "shadow boxing" to "shadow-boxing.mp4",
        "shadow boxing combo" to "shadow-boxing.mp4",
        "swimmer" to "swimmer.mp4",
        "prone swimmers" to "swimmer.mp4",
        "back-extension-row" to "back-extension-row.mp4",
        "back extension row" to "back-extension-row.mp4",
        "superman pull" to "back-extension-row.mp4",
        "back up" to "back-extension-row.mp4",
        "back-up" to "back-extension-row.mp4",

        // Squats & Legs
        "bodyweight-squat" to "bodyweight-squat.mp4",
        "bodyweight squat" to "bodyweight-squat.mp4",
        "squat" to "bodyweight-squat.mp4",
        "squats" to "bodyweight-squat.mp4",
        "squat-jump" to "jump-squats.mp4",
        "squat jump" to "jump-squats.mp4",
        "sumo-squat" to "sumo-squat.mp4",
        "sumo squat" to "sumo-squat.mp4",
        "jump-squats" to "jump-squats.mp4",
        "jump squats" to "jump-squats.mp4",
        "jump squat" to "jump-squats.mp4",
        "split-squat" to "split-squat.mp4",
        "split squat" to "split-squat.mp4",
        "cossack-squat" to "cossack-squat.mp4",
        "cossack squat" to "cossack-squat.mp4",
        "bulgarian-split-squat" to "bulgarian-split-squat.mp4",
        "bulgarian split squat" to "bulgarian-split-squat.mp4",
        "lunges" to "lunges.mp4",
        "lunge" to "lunges.mp4",
        "forward lunges" to "lunges.mp4",
        "alternating-lunges" to "lunges.mp4",
        "alternating lunges" to "lunges.mp4",
        "lateral-lunges" to "lateral-lunges.mp4",
        "lateral lunges" to "lateral-lunges.mp4",
        "jump-lunge" to "jump-lunge.mp4",
        "jump lunge" to "jump-lunge.mp4",
        "lunge-with-twist" to "lunge-with-twist.mp4",
        "lunge with twist" to "lunge-with-twist.mp4",
        "step-ups" to "step-ups.mp4",
        "step-up onto chair" to "step-ups.mp4",
        "step up onto chair" to "step-ups.mp4",
        "bench-step-ups" to "step-ups.mp4",
        "bench step-ups" to "step-ups.mp4",
        "calf-raises" to "calf-raises.mp4",
        "calf raises" to "calf-raises.mp4",
        "calf raise" to "calf-raises.mp4",
        "glute-bridge" to "glute-bridge.mp4",
        "glute bridge" to "glute-bridge.mp4",
        "single-leg-glute-bridge" to "glute-bridge.mp4",
        "single leg glute bridge" to "glute-bridge.mp4",
        "frog-pumps" to "frog-pumps.mp4",
        "frog pumps" to "frog-pumps.mp4",
        "clamshell" to "clamshell.mp4",
        "clamshells" to "clamshell.mp4",
        "side-leg-lift" to "side-leg-lift.mp4",
        "side leg lift" to "side-leg-lift.mp4",
        "side-lying leg lift" to "side-leg-lift.mp4",
        "duck-walk" to "duck-walk.mp4",
        "duck walk" to "duck-walk.mp4",
        "wall-sit" to "wall-sit.mp4",
        "wall sit" to "wall-sit.mp4",
        "wall sit hold" to "wall-sit.mp4",

        // Core & Abs
        "plank" to "plank.mp4",
        "plank hold" to "plank.mp4",
        "standard plank" to "plank.mp4",
        "forearm plank" to "plank.mp4",
        "side-plank" to "side-plank.mp4",
        "side plank" to "side-plank.mp4",
        "side-bridge-side-plank" to "side-plank.mp4",
        "side bridge / side plank" to "side-plank.mp4",
        "side-plank-kanan" to "side-plank.mp4",
        "side plank kanan" to "side-plank.mp4",
        "side plank (kanan)" to "side-plank.mp4",
        "side-plank-kiri" to "side-plank.mp4",
        "side plank kiri" to "side-plank.mp4",
        "side plank (kiri)" to "side-plank.mp4",
        "plank-hip-dips" to "plank-hip-dips.mp4",
        "plank hip dips" to "plank-hip-dips.mp4",
        "forearm-plank-reach" to "forearm-plank-reach.mp4",
        "forearm plank reach" to "forearm-plank-reach.mp4",
        "crunches" to "crunches.mp4",
        "crunch" to "crunches.mp4",
        "abdominal crunch" to "crunches.mp4",
        "abdominal crunches" to "crunches.mp4",
        "abdominal-crunch" to "crunches.mp4",
        "bicycle-crunches" to "bicycle-crunches.mp4",
        "bicycle crunches" to "bicycle-crunches.mp4",
        "bicycle crunch" to "bicycle-crunches.mp4",
        "russian-twist" to "russian-twist.mp4",
        "russian twist" to "russian-twist.mp4",
        "russian twists" to "russian-twist.mp4",
        "mountain-climbers" to "mountain-climbers.mp4",
        "mountain climbers" to "mountain-climbers.mp4",
        "dead-bug" to "dead-bug.mp4",
        "dead bug" to "dead-bug.mp4",
        "boat-pose" to "boat-pose.mp4",
        "boat pose" to "boat-pose.mp4",
        "boat pose (navasana)" to "boat-pose.mp4",
        "scissors" to "scissors.mp4",
        "scissor kicks" to "scissors.mp4",
        "hanging-leg-raises" to "hanging-leg-raises.mp4",
        "hanging leg raises" to "hanging-leg-raises.mp4",

        // Cardio & Agility
        "jumping-jacks" to "jumping-jacks.mp4",
        "jumping jacks" to "jumping-jacks.mp4",
        "jumping jack" to "jumping-jacks.mp4",
        "jumping-jacks-ringan" to "jumping-jacks.mp4",
        "jumping jacks ringan" to "jumping-jacks.mp4",
        "burpees" to "burpees.mp4",
        "burpee" to "burpees.mp4",
        "butt-kicks" to "butt-kicks.mp4",
        "butt kicks" to "butt-kicks.mp4",
        "skater-jumps" to "skater-jumps.mp4",
        "skater jumps" to "skater-jumps.mp4",
        "broad-jump" to "broad-jump.mp4",
        "broad jump" to "broad-jump.mp4",
        "tuck-jumps" to "tuck-jumps.mp4",
        "tuck jumps" to "tuck-jumps.mp4",
        "bear-crawl" to "bear-crawl.mp4",
        "bear crawl" to "bear-crawl.mp4",
        "stationary-sprint" to "stationary-sprint.mp4",
        "high-knees" to "stationary-sprint.mp4",
        "high knees" to "stationary-sprint.mp4",
        "knees up" to "stationary-sprint.mp4",
        "knees-up" to "stationary-sprint.mp4",
        "sprint in place" to "stationary-sprint.mp4",
        "sprint-in-place" to "stationary-sprint.mp4",
        "sprint-all-out" to "stationary-sprint.mp4",
        "sprint all-out" to "stationary-sprint.mp4",
        "sprint all out" to "stationary-sprint.mp4",
        "sprint-vo2-max" to "stationary-sprint.mp4",
        "sprint vo2 max" to "stationary-sprint.mp4",
        "high knee against wall" to "stationary-sprint.mp4",

        // Recovery & Yoga
        "child-s-pose" to "childs-pose.mp4",
        "child's pose" to "childs-pose.mp4",
        "childs-pose" to "childs-pose.mp4",
        "child pose" to "childs-pose.mp4",
        "cat-cow-stretch" to "cat-cow-stretch.mp4",
        "cat-cow stretch" to "cat-cow-stretch.mp4",
        "cat cow stretch" to "cat-cow-stretch.mp4",
        "yoga-flow" to "yoga-flow.mp4",
        "yoga flow" to "yoga-flow.mp4",
        "yoga flow (sun salutation)" to "yoga-flow.mp4",
        "istirahat" to "rest.mp4",
        "rest" to "rest.mp4",
        "istirahat aktif" to "rest.mp4",
        "active rest" to "rest.mp4",
        "deep-breathing" to "deep-breathing.mp4",
        "deep breathing" to "deep-breathing.mp4",
        "tarik napas" to "deep-breathing.mp4",
        "napas dalam" to "deep-breathing.mp4",
        "pemulihan-ringan" to "arm-shake-recovery.mp4",
        "pemulihan ringan" to "arm-shake-recovery.mp4",
        "pendinginan aktif" to "arm-shake-recovery.mp4",
        "active recovery" to "arm-shake-recovery.mp4",
        "arm shake" to "arm-shake-recovery.mp4",

        // Core & Back Stabilizer Tambahan
        "bird-dog" to "bird-dog.mp4",
        "bird dog" to "bird-dog.mp4",
        "bird-dog-hold" to "bird-dog-hold.mp4",
        "bird dog hold" to "bird-dog-hold.mp4",
        "flutter-kicks" to "flutter-kicks.mp4",
        "flutter kicks" to "flutter-kicks.mp4",
        "hollow-body-hold" to "hollow-body-hold.mp4",
        "hollow body hold" to "hollow-body-hold.mp4",
        "hollow body position" to "hollow-body-hold.mp4",
        "leg-raise" to "leg-raise.mp4",
        "leg raise" to "leg-raise.mp4",
        "lying leg raises" to "leg-raise.mp4",
        "modified-curl-up" to "modified-curl-up.mp4",
        "modified curl-up" to "modified-curl-up.mp4",
        "modified curl up" to "modified-curl-up.mp4",
        "plank-shoulder-taps" to "plank-shoulder-taps.mp4",
        "plank shoulder taps" to "plank-shoulder-taps.mp4",
        "shoulder-taps" to "plank-shoulder-taps.mp4",
        "shoulder taps" to "plank-shoulder-taps.mp4",
        "standing-crunch" to "standing-crunch.mp4",
        "standing crunch" to "standing-crunch.mp4",
        "standing crunches" to "standing-crunch.mp4",

        // Cardio & HIIT Tambahan
        "active-recovery-jog" to "active-recovery-jog.mp4",
        "active recovery jog" to "active-recovery-jog.mp4",
        "jogging in place" to "active-recovery-jog.mp4",
        "burpee-tuck-jumps" to "burpee-tuck-jumps.mp4",
        "burpee tuck jumps" to "burpee-tuck-jumps.mp4",
        "duck-and-weave" to "duck-and-weave.mp4",
        "duck and weave" to "duck-and-weave.mp4",
        "ducking in boxing guard" to "duck-and-weave.mp4",
        "jump-rope-hops" to "jump-rope-hops.mp4",
        "jump rope hops" to "jump-rope-hops.mp4",
        "jump rope" to "jump-rope-hops.mp4",
        "plank-to-push-up" to "plank-to-push-up.mp4",
        "plank to push-up" to "plank-to-push-up.mp4",
        "plank to push up" to "plank-to-push-up.mp4",
        "push-up-and-rotation" to "push-up-and-rotation.mp4",
        "push-up and rotation" to "push-up-and-rotation.mp4",
        "push up and rotation" to "push-up-and-rotation.mp4",

        // Strength Tambahan
        "curtsy-lunges" to "curtsy-lunges.mp4",
        "curtsy lunges" to "curtsy-lunges.mp4",
        "curtsy lunge" to "curtsy-lunges.mp4",
        "high-rows" to "high-rows.mp4",
        "high rows" to "high-rows.mp4",
        "wide-arm-push-up" to "wide-arm-push-up.mp4",
        "wide arm push up" to "wide-arm-push-up.mp4",
        "wide push-up" to "wide-arm-push-up.mp4",
        "wide push up" to "wide-arm-push-up.mp4",

        // Mobility & Recovery Tambahan
        "chest-opener-stretch" to "chest-opener-stretch.mp4",
        "chest opener stretch" to "chest-opener-stretch.mp4",
        "chin-tucks-neck-rolls" to "chin-tucks-neck-rolls.mp4",
        "chin tucks & neck rolls" to "chin-tucks-neck-rolls.mp4",
        "chin tucks and neck rolls" to "chin-tucks-neck-rolls.mp4",
        "downward-dog-to-cobra" to "downward-dog-to-cobra.mp4",
        "downward dog to cobra" to "downward-dog-to-cobra.mp4",
        "seated-spinal-twist" to "seated-spinal-twist.mp4",
        "seated spinal twist" to "seated-spinal-twist.mp4",
        "shoulder-rolls-squeeze" to "shoulder-rolls-squeeze.mp4",
        "shoulder rolls & squeeze" to "shoulder-rolls-squeeze.mp4",
        "shoulder rolls and squeeze" to "shoulder-rolls-squeeze.mp4",
        "standing-quad-stretch" to "standing-quad-stretch.mp4",
        "standing quad stretch" to "standing-quad-stretch.mp4",
        "thoracic-spine-twist" to "thoracic-spine-twist.mp4",
        "thoracic spine twist" to "thoracic-spine-twist.mp4"
    )

    /**
     * Memeriksa apakah suatu URI adalah file Video MP4.
     */
    fun isVideo(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        return uri.endsWith(".mp4", ignoreCase = true) || uri.startsWith(ASSET_VIDEO_DIR)
    }

    /**
     * Mengembalikan MediaType yang akurat berdasarkan URI.
     */
    fun getEffectiveMediaType(uri: String?, fallback: MediaType = MediaType.NONE): MediaType {
        return when {
            isVideo(uri) -> MediaType.VIDEO
            uri?.endsWith(".gif", ignoreCase = true) == true -> MediaType.GIF
            !uri.isNullOrBlank() -> if (fallback != MediaType.NONE) fallback else MediaType.GIF
            else -> fallback
        }
    }

    /**
     * Mencari path video MP4 yang paling cocok untuk judul gerakan tertentu.
     * Prioritas:
     * 1. Jika [currentUri] sudah merupakan .mp4, gunakan [currentUri].
     * 2. Jika intervalType adalah REST dan title kosong / generik istirahat, default ke rest.mp4.
     * 3. Cari di daftar 58 video AI yang cocok berdasarkan nama/slug.
     * 4. Jika tidak ada video yang cocok, kembalikan [currentUri] lama (misal GIF).
     */
    fun resolveVideo(title: String?, currentUri: String? = null, intervalType: IntervalType? = null): String? {
        // Jika sudah video MP4, langsung pakai
        if (isVideo(currentUri)) {
            return currentUri
        }

        // Jika fase REST dan title tidak ada/kosong atau rest/istirahat biasa, langsung berikan rest.mp4
        if (intervalType == IntervalType.REST && (title.isNullOrBlank() || title.equals("istirahat", ignoreCase = true) || title.equals("rest", ignoreCase = true))) {
            return "${ASSET_VIDEO_DIR}rest.mp4"
        }

        if (title.isNullOrBlank()) {
            return if (intervalType == IntervalType.REST) "${ASSET_VIDEO_DIR}rest.mp4" else currentUri
        }

        val cleanTitle = title.lowercase().trim()

        // 1. Cek langsung di kamus alias judul
        TITLE_TO_VIDEO_MAP[cleanTitle]?.let { fileName ->
            return "$ASSET_VIDEO_DIR$fileName"
        }

        // 2. Cek berdasarkan slug
        val slug = cleanTitle.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        TITLE_TO_VIDEO_MAP[slug]?.let { fileName ->
            return "$ASSET_VIDEO_DIR$fileName"
        }

        val directFile = "$slug.mp4"
        if (AVAILABLE_VIDEOS.contains(directFile)) {
            return "$ASSET_VIDEO_DIR$directFile"
        }

        // 3. Cek pencarian parsial (contain)
        for ((key, fileName) in TITLE_TO_VIDEO_MAP) {
            if (cleanTitle.contains(key) || key.contains(cleanTitle)) {
                return "$ASSET_VIDEO_DIR$fileName"
            }
        }

        // 4. Jika masih REST tapi judul tidak match, fallback ke rest.mp4
        if (intervalType == IntervalType.REST) {
            return "${ASSET_VIDEO_DIR}rest.mp4"
        }

        return currentUri
    }

    /**
     * Memperbarui seluruh template di SQLite Database yang masih menggunakan file GIF
     * atau belum punya media, ke video MP4 AI yang sesuai.
     */
    suspend fun upgradeAllTemplatesInDb(templateDao: ExerciseTemplateDao) {
        try {
            val allTemplates = templateDao.getAll()
            var upgradedCount = 0

            for (template in allTemplates) {
                // Jangan timpa file rekaman pribadi pengguna
                val isUserPersonalFile = template.mediaUri != null && !template.mediaUri.startsWith("file:///android_asset/")
                if (isUserPersonalFile) continue

                val resolved = resolveVideo(template.name, template.mediaUri)
                if (resolved != null && isVideo(resolved) && (template.mediaUri != resolved || template.mediaType != MediaType.VIDEO)) {
                    templateDao.update(
                        template.copy(
                            mediaUri = resolved,
                            mediaType = MediaType.VIDEO
                        )
                    )
                    upgradedCount++
                }
            }

            if (upgradedCount > 0) {
                Log.i(TAG, "Berhasil meng-upgrade $upgradedCount template di DB dari GIF ke Video AI MP4.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal meng-upgrade template di DB: ${e.message}", e)
        }
    }
}

