package com.workout.timer.data.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Generator kartu pencapaian latihan (Workout Share / Completion Card).
 * Menghasilkan gambar beresolusi tinggi 1080 x 1920 (9:16 Story format) dengan
 * latar belakang foto AI estetik, tipografi bersih modern, dan metrik esensial.
 */
object WorkoutCardGenerator {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1920

    /**
     * Menghasilkan Bitmap kartu latihan siap dibagikan/disimpan.
     */
    fun generateCardBitmap(
        context: Context,
        routineName: String,
        totalDurationSeconds: Int,
        totalExercisesCompleted: Int,
        assetRelativePath: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Gambar Background Foto dari Assets
        val cleanPath = assetRelativePath
            .removePrefix("file:///android_asset/")
            .removePrefix("/")

        try {
            context.assets.open(cleanPath).use { stream ->
                val bgBitmap = BitmapFactory.decodeStream(stream)
                if (bgBitmap != null) {
                    val srcRect = calculateCenterCropRect(bgBitmap.width, bgBitmap.height, CARD_WIDTH, CARD_HEIGHT)
                    val destRect = Rect(0, 0, CARD_WIDTH, CARD_HEIGHT)
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(bgBitmap, srcRect, destRect, paint)
                    bgBitmap.recycle()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkoutCardGenerator", "Gagal memuat asset $cleanPath: ${e.message}")
            // Fallback dark color jika asset gagal dimuat
            canvas.drawColor(android.graphics.Color.rgb(18, 18, 22))
        }

        // 2. Dual Gradient Scrim (Vignette gelap elegan untuk kontras teks tajam)
        // Top Scrim
        val topScrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, 500f,
                intArrayOf(android.graphics.Color.argb(180, 0, 0, 0), android.graphics.Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), 500f, topScrimPaint)

        // Bottom Scrim
        val bottomScrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, 850f, 0f, CARD_HEIGHT.toFloat(),
                intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.argb(200, 10, 10, 14),
                    android.graphics.Color.argb(250, 6, 6, 8)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 850f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bottomScrimPaint)

        // 3. Header Atas: Branding Badge & Tanggal
        val marginHorizontal = 75f
        val headerY = 110f

        // Brand Pill
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val brandBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(70, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val pillRect = RectF(marginHorizontal, headerY - 45f, marginHorizontal + 410f, headerY + 25f)
        canvas.drawRoundRect(pillRect, 22f, 22f, brandPaint)
        canvas.drawRoundRect(pillRect, 22f, 22f, brandBorderPaint)

        val brandTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("GASCOUNT  •  SELESAI", marginHorizontal + 32f, headerY + 3f, brandTextPaint)

        // Tanggal & Waktu di kanan
        val dateStr = SimpleDateFormat("d MMM yyyy  •  HH:mm", Locale.forLanguageTag("id-ID")).format(Date())
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(dateStr, CARD_WIDTH - marginHorizontal, headerY + 3f, datePaint)

        // 4. Area Konten Utama (Bawah)
        val contentStartY = 1180f

        // Small Kicker
        val kickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        canvas.drawText("PENCAPAIAN LATIHAN", marginHorizontal, contentStartY, kickerPaint)

        // Routine Name (Besar, Tebal, Estetik)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(120, 0, 0, 0))
        }
        val formattedTitle = if (routineName.length > 28) routineName.take(26) + "..." else routineName
        canvas.drawText(formattedTitle, marginHorizontal, contentStartY + 75f, titlePaint)

        // Garis Pembatas Halus
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(40, 255, 255, 255)
            strokeWidth = 2f
        }
        val dividerY = contentStartY + 125f
        canvas.drawLine(marginHorizontal, dividerY, CARD_WIDTH - marginHorizontal, dividerY, dividerPaint)

        // 5. Grid Statistik Bersih (3 Kolom: Waktu, Gerakan, Estimasi Kalori - All Monochrome White)
        val statsY = dividerY + 50f
        val colWidth = (CARD_WIDTH - (marginHorizontal * 2)) / 3f

        val valPaintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 66f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(170, 161, 161, 170)
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }

        // Kolom 1: Durasi
        val mm = totalDurationSeconds / 60
        val ss = totalDurationSeconds % 60
        val durationStr = String.format("%02d:%02d", mm, ss)
        val col1X = marginHorizontal
        canvas.drawText(durationStr, col1X, statsY + 65f, valPaintWhite)
        canvas.drawText("TOTAL WAKTU", col1X, statsY + 115f, labelPaint)

        // Kolom 2: Gerakan (Pure White)
        val col2X = marginHorizontal + colWidth
        canvas.drawText("$totalExercisesCompleted", col2X, statsY + 65f, valPaintWhite)
        canvas.drawText("GERAKAN SELESAI", col2X, statsY + 115f, labelPaint)

        // Kolom 3: Estimasi Kalori (~7.5 kcal/menit aktif)
        val col3X = marginHorizontal + (colWidth * 2)
        val estCalories = max(20, (totalDurationSeconds * 7.5 / 60.0).roundToInt())
        canvas.drawText("~$estCalories", col3X, statsY + 65f, valPaintWhite)
        canvas.drawText("EST. KALORI", col3X, statsY + 115f, labelPaint)

        // 6. Garis Pembatas Bawah
        val bottomDividerY = statsY + 175f
        canvas.drawLine(marginHorizontal, bottomDividerY, CARD_WIDTH - marginHorizontal, bottomDividerY, dividerPaint)

        // 7. Footer Minimalis
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(150, 255, 255, 255)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.05f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Setiap detik berarti  •  Tracked with GasCount", CARD_WIDTH / 2f, bottomDividerY + 65f, footerPaint)

        return bitmap
    }

    /**
     * Menyimpan bitmap kartu ke file cache untuk di-share via FileProvider.
     */
    fun saveCardToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cacheFolder = File(context.cacheDir, "share_cards").apply { mkdirs() }
            val file = File(cacheFolder, "gascount_card_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("WorkoutCardGenerator", "Gagal simpan kartu ke cache: ${e.message}", e)
            null
        }
    }

    /**
     * Menyimpan kartu ke Galeri Foto HP (MediaStore Pictures/GasCount).
     */
    fun saveCardToGallery(context: Context, bitmap: Bitmap, routineName: String): Boolean {
        val cleanRoutineName = routineName.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)
        val fileName = "GasCount_${cleanRoutineName}_${System.currentTimeMillis()}.jpg"

        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GasCount")
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    out.flush()
                }
                try {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(uri.toString()),
                        arrayOf("image/jpeg"),
                        null
                    )
                } catch (_: Exception) {}
                true
            } else {
                saveToPicturesLegacy(context, bitmap, fileName)
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkoutCardGenerator", "Gagal simpan ke MediaStore: ${e.message}", e)
            saveToPicturesLegacy(context, bitmap, fileName)
        }
    }

    private fun saveToPicturesLegacy(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "GasCount").apply { mkdirs() }
            val file = File(appDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
            }
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("WorkoutCardGenerator", "Gagal simpan ke legacy Pictures: ${e.message}", e)
            false
        }
    }

    /**
     * Membuka Android Share Sheet sistem untuk mengirim gambar kartu.
     */
    fun shareCard(context: Context, imageUri: Uri, routineName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_SUBJECT, "Latihan Selesai: $routineName")
            putExtra(Intent.EXTRA_TEXT, "Baru saja menyelesaikan latihan '$routineName' dengan GasCount Workout Timer! 🔥")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Bagikan Kartu Latihan")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun calculateCenterCropRect(srcW: Int, srcH: Int, targetW: Int, targetH: Int): Rect {
        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val targetRatio = targetW.toFloat() / targetH.toFloat()

        return if (srcRatio > targetRatio) {
            val newW = (srcH * targetRatio).toInt()
            val left = (srcW - newW) / 2
            Rect(left, 0, left + newW, srcH)
        } else {
            val newH = (srcW / targetRatio).toInt()
            val top = (srcH - newH) / 2
            Rect(0, top, srcW, top + newH)
        }
    }
}

