package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileStorageHelper {

    private const val DOCUMENTS_DIR = "documents"

    fun getDocumentsDirectory(context: Context): File {
        val dir = File(context.filesDir, DOCUMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "doc"): File = withContext(Dispatchers.IO) {
        val dir = getDocumentsDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "${prefix}_${timeStamp}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        file
    }

    suspend fun savePdfStream(context: Context, inputStream: InputStream): File = withContext(Dispatchers.IO) {
        val dir = getDocumentsDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "pdf_${timeStamp}.pdf")
        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        file
    }

    fun getShareableUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun createPlaceholderDocumentBitmap(title: String, category: String, organization: String?): Bitmap {
        val width = 600
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean subtle document background
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header band
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, paint)

        // Header Category text
        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(category.uppercase(), 40f, 65f, paint)

        // Header Organization
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val orgText = organization ?: title
        canvas.drawText(if (orgText.length > 24) orgText.take(24) + "..." else orgText, 40f, 110f, paint)

        // Document mock lines
        paint.color = Color.parseColor("#E2E8F0")
        val startY = 200f
        for (i in 0..12) {
            val y = startY + i * 36f
            val lineWidth = if (i % 3 == 0) (width - 240).toFloat() else (width - 80).toFloat()
            canvas.drawRoundRect(40f, y, lineWidth, y + 16f, 8f, 8f, paint)
        }

        // Watermark / Seal
        paint.color = Color.parseColor("#CBD5E1")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("PAPERTRAIL VERIFIED RECORD", 40f, height - 60f, paint)

        return bitmap
    }

    fun createSampleDocumentForScan(sampleIndex: Int = 0): Bitmap {
        val width = 800
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White paper background with slight texture
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Top decorative header bar
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, width.toFloat(), 120f, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("METRO POWER & UTILITIES", 50f, 75f, paint)

        // Document text content
        paint.color = Color.parseColor("#1E293B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText("MONTHLY RESIDENTIAL BILLING STATEMENT", 50f, 190f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 24f
        canvas.drawText("Account Number: ACC-7892104", 50f, 250f, paint)
        canvas.drawText("Invoice Number: INV-2026-9912", 50f, 290f, paint)
        canvas.drawText("Statement Date: Oct 30, 2026", 50f, 330f, paint)

        // Highlight Due Date Box
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRoundRect(50f, 380f, (width - 50).toFloat(), 530f, 16f, 16f, paint)

        paint.color = Color.parseColor("#92400E")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 26f
        canvas.drawText("PAYMENT DUE DATE: NOV 20, 2026", 75f, 435f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 38f
        canvas.drawText("TOTAL AMOUNT DUE: $128.45", 75f, 495f, paint)

        // Line Items
        paint.color = Color.parseColor("#334155")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 22f
        canvas.drawText("Electricity Base Service: $45.00", 50f, 600f, paint)
        canvas.drawText("Usage (420 kWh @ $0.18/kWh): $75.60", 50f, 640f, paint)
        canvas.drawText("State Regulatory Surcharge: $4.25", 50f, 680f, paint)
        canvas.drawText("Local City Tax: $3.60", 50f, 720f, paint)

        // Remittance notice
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        canvas.drawText("Please remit payment before the due date to avoid a late fee.", 50f, 820f, paint)
        canvas.drawText("Pay online at www.metropower.example or call 1-800-555-0199.", 50f, 860f, paint)

        // Security barcode simulation
        paint.color = Color.parseColor("#0F172A")
        var barX = 50f
        while (barX < width - 50f) {
            val barWidth = if ((barX.toInt() % 7) == 0) 6f else 2.5f
            canvas.drawRect(barX, 930f, barX + barWidth, 1000f, paint)
            barX += barWidth + 4f
        }

        return bitmap
    }
}
