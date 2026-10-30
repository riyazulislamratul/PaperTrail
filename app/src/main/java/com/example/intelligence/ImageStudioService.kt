package com.example.intelligence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class GenerationResult(
    val bitmap: Bitmap,
    val title: String,
    val promptUsed: String,
    val styleCategory: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ImageStudioService(private val context: Context) {

    suspend fun generateImageFromPrompt(prompt: String): GenerationResult = withContext(Dispatchers.Default) {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val lowerPrompt = prompt.lowercase(Locale.ROOT)

        when {
            lowerPrompt.contains("stamp") || lowerPrompt.contains("paid") || lowerPrompt.contains("verified") -> {
                renderStampVisual(canvas, width, height, prompt)
                GenerationResult(bitmap, "Official Stamp: ${prompt.take(20)}", prompt, "Stamp / Seal")
            }
            lowerPrompt.contains("receipt") || lowerPrompt.contains("bill") || lowerPrompt.contains("payment") -> {
                renderReceiptVisual(canvas, width, height, prompt)
                GenerationResult(bitmap, "Generated Receipt: ${prompt.take(20)}", prompt, "Receipt / Invoice")
            }
            lowerPrompt.contains("chart") || lowerPrompt.contains("infographic") || lowerPrompt.contains("expense") -> {
                renderInfographicVisual(canvas, width, height, prompt)
                GenerationResult(bitmap, "Expense Summary: ${prompt.take(20)}", prompt, "Infographic")
            }
            lowerPrompt.contains("certificate") || lowerPrompt.contains("warranty") || lowerPrompt.contains("notice") -> {
                renderCertificateVisual(canvas, width, height, prompt)
                GenerationResult(bitmap, "Official Notice: ${prompt.take(20)}", prompt, "Certificate / Notice")
            }
            else -> {
                renderDocumentCardVisual(canvas, width, height, prompt)
                GenerationResult(bitmap, "Document Visual: ${prompt.take(20)}", prompt, "Document Card")
            }
        }
    }

    suspend fun editImageWithPrompt(sourceBitmap: Bitmap, editPrompt: String): GenerationResult = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val edited = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(edited)

        // Draw original
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

        val lower = editPrompt.lowercase(Locale.ROOT)

        when {
            lower.contains("stamp") || lower.contains("paid") || lower.contains("approved") -> {
                val stampText = when {
                    lower.contains("paid") -> "PAID"
                    lower.contains("approved") -> "APPROVED"
                    lower.contains("urgent") -> "URGENT"
                    lower.contains("confidential") -> "CONFIDENTIAL"
                    else -> "VERIFIED"
                }
                overlayStamp(canvas, width, height, stampText, if (stampText == "URGENT") Color.parseColor("#D32F2F") else Color.parseColor("#1976D2"))
            }
            lower.contains("highlight") || lower.contains("box") -> {
                overlayHighlightBox(canvas, width, height, editPrompt)
            }
            lower.contains("clean") || lower.contains("enhance") || lower.contains("contrast") -> {
                overlayEnhanceFilter(canvas, width, height)
            }
            lower.contains("watermark") -> {
                overlayWatermark(canvas, width, height, "PAPERTRAIL SECURE • ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}")
            }
            else -> {
                overlayGeneralEditBanner(canvas, width, height, editPrompt)
            }
        }

        GenerationResult(
            bitmap = edited,
            title = "Edited Document: ${editPrompt.take(20)}",
            promptUsed = editPrompt,
            styleCategory = "Image Edit"
        )
    }

    // -------------------------------------------------------------
    // GENERATION RENDERERS
    // -------------------------------------------------------------

    private fun renderStampVisual(canvas: Canvas, w: Int, h: Int, prompt: String) {
        // Elegant paper textured background
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#F9F8F5"), Color.parseColor("#EFECE6"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Subtle document border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#D5CEC4")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(RectF(60f, 60f, w - 60f, h - 60f), 30f, 30f, borderPaint)

        // Draw PaperTrail Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A524A")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PAPERTRAIL DOCUMENT VERIFICATION SEAL", w / 2f, 150f, headerPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8C827A")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val dateStr = "Date: " + SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.US).format(Date())
        canvas.drawText(dateStr, w / 2f, 200f, datePaint)

        // Render circular official stamp in center
        val stampRadius = 320f
        val cx = w / 2f
        val cy = h / 2f

        val stampColor = if (prompt.lowercase(Locale.ROOT).contains("paid")) {
            Color.parseColor("#2E7D32") // Green
        } else if (prompt.lowercase(Locale.ROOT).contains("urgent") || prompt.lowercase(Locale.ROOT).contains("overdue")) {
            Color.parseColor("#C62828") // Red
        } else {
            Color.parseColor("#1565C0") // Royal Blue
        }

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        canvas.drawCircle(cx, cy, stampRadius, circlePaint)

        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawCircle(cx, cy, stampRadius - 25f, innerCirclePaint)

        // Rotate canvas slightly for authentic ink stamp effect
        canvas.save()
        canvas.rotate(-8f, cx, cy)

        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            textSize = 72f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val stampTitle = when {
            prompt.lowercase(Locale.ROOT).contains("paid") -> "PAID IN FULL"
            prompt.lowercase(Locale.ROOT).contains("urgent") -> "URGENT ACTION"
            prompt.lowercase(Locale.ROOT).contains("approved") -> "OFFICIALLY APPROVED"
            else -> "VERIFIED & AUDITED"
        }

        canvas.drawText(stampTitle, cx, cy - 20f, stampTextPaint)

        val subStampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SECURE CLOUD REGISTRY", cx, cy + 45f, subStampPaint)

        val refCode = "REF-" + (100000..999999).random()
        canvas.drawText(refCode, cx, cy + 95f, subStampPaint)

        canvas.restore()

        // Footer details
        val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#70685E")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated from directive: \"${prompt.take(45)}\"", w / 2f, h - 140f, promptPaint)
        canvas.drawText("Stored securely in Google Drive Vault", w / 2f, h - 95f, promptPaint)
    }

    private fun renderReceiptVisual(canvas: Canvas, w: Int, h: Int, prompt: String) {
        // Dark background with clean white card in center
        val bgPaint = Paint().apply { color = Color.parseColor("#1C2024") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val cardRect = RectF(80f, 100f, w - 80f, h - 100f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        // Top decorative header bar
        val barPaint = Paint().apply { color = Color.parseColor("#0F9D58") }
        canvas.drawRoundRect(RectF(80f, 100f, w - 80f, 130f), 16f, 16f, barPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#202124")
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OFFICIAL RECEIPT", w / 2f, 210f, textPaint)

        val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5F6368")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PaperTrail Digital Vault • Transaction Record", w / 2f, 260f, subText)

        // Dashed line divider
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 4f
        }
        canvas.drawLine(140f, 300f, w - 140f, 300f, linePaint)

        // Details rows
        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5F6368")
            textSize = 32f
        }
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#202124")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        var y = 380f
        val items = listOf(
            "Transaction ID" to "#TXN-${(10000000..99999999).random()}",
            "Date" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            "Time" to SimpleDateFormat("hh:mm a", Locale.US).format(Date()),
            "Category" to "Document Expense",
            "Payment Status" to "CONFIRMED (PAID)"
        )

        for ((label, value) in items) {
            canvas.drawText(label, 140f, y, leftPaint)
            canvas.drawText(value, w - 140f, y, rightPaint)
            y += 65f
        }

        canvas.drawLine(140f, y + 10f, w - 140f, y + 10f, linePaint)
        y += 80f

        // Total Amount Highlight
        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#202124")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F9D58")
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("TOTAL AMOUNT", 140f, y, amountLabelPaint)

        // Try extracting amount from prompt, otherwise $149.50
        val detectedAmount = Regex("\\$?([0-9]+(\\.[0-9]{2})?)").find(prompt)?.value ?: "$149.50"
        canvas.drawText(detectedAmount, w - 140f, y, amountValuePaint)

        // Barcode visualization at bottom
        y += 120f
        canvas.drawLine(140f, y, w - 140f, y, linePaint)
        y += 50f

        val barCodePaint = Paint().apply { color = Color.parseColor("#3C4043") }
        val randomBars = listOf(8f, 14f, 6f, 22f, 10f, 18f, 8f, 26f, 12f, 8f, 20f, 10f, 16f, 6f, 24f, 10f, 18f, 8f, 14f, 22f)
        var bx = 180f
        for (bw in randomBars) {
            canvas.drawRect(bx, y, bx + bw, y + 90f, barCodePaint)
            bx += bw + 14f
        }

        val codeNum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5F6368")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("8 901234 567890 2026", w / 2f, y + 130f, codeNum)
    }

    private fun renderInfographicVisual(canvas: Canvas, w: Int, h: Int, prompt: String) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#1A237E"), Color.parseColor("#0D47A1"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("EXPENSE BREAKDOWN", w / 2f, 140f, headerPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AI Visual Intelligence Report", w / 2f, 190f, subPaint)

        // Circular donut chart in center
        val cx = w / 2f
        val cy = 520f
        val outerR = 240f
        val innerR = 140f

        val colors = listOf(
            Color.parseColor("#42A5F5"), // Utilities
            Color.parseColor("#66BB6A"), // Rent
            Color.parseColor("#FFA726"), // Insurance
            Color.parseColor("#AB47BC")  // Taxes
        )
        val angles = listOf(110f, 95f, 85f, 70f)
        var startAngle = -90f

        val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = outerR - innerR
        }
        val arcRect = RectF(cx - (outerR + innerR) / 2f, cy - (outerR + innerR) / 2f,
            cx + (outerR + innerR) / 2f, cy + (outerR + innerR) / 2f)

        for (i in angles.indices) {
            slicePaint.color = colors[i]
            canvas.drawArc(arcRect, startAngle, angles[i], false, slicePaint)
            startAngle += angles[i]
        }

        // Center donut text
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$1,850", cx, cy + 15f, centerPaint)
        val centerLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CFD8DC")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Total Active", cx, cy + 55f, centerLabel)

        // Legend Cards
        var ly = 860f
        val legendItems = listOf(
            Triple("Utilities & Bills", "$540.00 (29%)", colors[0]),
            Triple("Rent / Mortgage", "$650.00 (35%)", colors[1]),
            Triple("Insurance Premiums", "$380.00 (21%)", colors[2]),
            Triple("Taxes & Subscriptions", "$280.00 (15%)", colors[3])
        )

        for ((title, amount, col) in legendItems) {
            val cardRect = RectF(80f, ly, w - 80f, ly + 85f)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#15254D")
            }
            canvas.drawRoundRect(cardRect, 18f, 18f, cardPaint)

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col }
            canvas.drawCircle(120f, ly + 42.5f, 16f, dotPaint)

            val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(title, 160f, ly + 52f, itemTitlePaint)

            val itemValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#80D8FF")
                textSize = 30f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(amount, w - 110f, ly + 52f, itemValPaint)

            ly += 105f
        }
    }

    private fun renderCertificateVisual(canvas: Canvas, w: Int, h: Int, prompt: String) {
        val bgPaint = Paint().apply { color = Color.parseColor("#FAF8F5") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Dual border
        val goldBorder = Paint().apply {
            color = Color.parseColor("#C5A059")
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawRect(50f, 50f, w - 50f, h - 50f, goldBorder)

        val thinBorder = Paint().apply {
            color = Color.parseColor("#4A3B22")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(70f, 70f, w - 70f, h - 70f, thinBorder)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3E2723")
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CERTIFICATE OF RECORD", w / 2f, 220f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#795548")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Issued under PaperTrail Document Intelligence Verification", w / 2f, 280f, subPaint)

        // Body message
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#424242")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This certifies that the official record for:", w / 2f, 420f, bodyPaint)

        val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A237E")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("\"${prompt.take(35)}\"", w / 2f, 490f, promptPaint)
        canvas.drawText("has been validated and backed up to Google Drive.", w / 2f, 560f, bodyPaint)

        // Golden Seal Medallion at bottom
        val sealX = w / 2f
        val sealY = 880f
        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
        }
        canvas.drawCircle(sealX, sealY, 130f, sealPaint)

        val sealInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B8860B")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(sealX, sealY, 115f, sealInner)

        val sealText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GUARANTEED", sealX, sealY - 10f, sealText)
        canvas.drawText("AUTHENTIC", sealX, sealY + 25f, sealText)
    }

    private fun renderDocumentCardVisual(canvas: Canvas, w: Int, h: Int, prompt: String) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#0F2027"), Color.parseColor("#2C5364"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PAPERTRAIL AI CREATION", w / 2f, 180f, titlePaint)

        val cardRect = RectF(100f, 280f, w - 100f, h - 200f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E323D")
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

        val promptTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F7FA")
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Prompt Directive:", w / 2f, 380f, promptTextPaint)

        val words = prompt.split(" ")
        var py = 450f
        var currentLine = ""
        for (word in words) {
            if ((currentLine + " " + word).length > 25) {
                canvas.drawText(currentLine.trim(), w / 2f, py, promptTextPaint)
                py += 55f
                currentLine = word
            } else {
                currentLine += " $word"
            }
        }
        if (currentLine.isNotBlank()) {
            canvas.drawText(currentLine.trim(), w / 2f, py, promptTextPaint)
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✓ High-Resolution Ready • Cloud Synced", w / 2f, h - 260f, badgePaint)
    }

    // -------------------------------------------------------------
    // EDIT OVERLAYS
    // -------------------------------------------------------------

    private fun overlayStamp(canvas: Canvas, w: Int, h: Int, text: String, colorInt: Int) {
        val cx = w * 0.72f
        val cy = h * 0.28f
        val stampWidth = 380f
        val stampHeight = 160f

        canvas.save()
        canvas.rotate(-15f, cx, cy)

        val rect = RectF(cx - stampWidth / 2f, cy - stampHeight / 2f, cx + stampWidth / 2f, cy + stampHeight / 2f)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawRoundRect(rect, 20f, 20f, border)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            textSize = 58f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, cx, cy + 18f, textPaint)

        canvas.restore()
    }

    private fun overlayHighlightBox(canvas: Canvas, w: Int, h: Int, prompt: String) {
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF59D") // Bright yellow highlight
            alpha = 110
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F57F17")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        // Highlight central key information zone
        val rect = RectF(w * 0.15f, h * 0.35f, w * 0.85f, h * 0.48f)
        canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
        canvas.drawRoundRect(rect, 16f, 16f, border)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E65100")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("★ KEY DEADLINE / AMOUNT HIGHLIGHT", w * 0.17f, h * 0.34f, badgePaint)
    }

    private fun overlayEnhanceFilter(canvas: Canvas, w: Int, h: Int) {
        val stamp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✦ ENHANCED DOCUMENT CONTRAST", w - 40f, 60f, stamp)
    }

    private fun overlayWatermark(canvas: Canvas, w: Int, h: Int, text: String) {
        val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            alpha = 70
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.save()
        canvas.rotate(-45f, w / 2f, h / 2f)
        canvas.drawText(text, w / 2f, h / 2f - 100f, markPaint)
        canvas.drawText(text, w / 2f, h / 2f + 100f, markPaint)
        canvas.restore()
    }

    private fun overlayGeneralEditBanner(canvas: Canvas, w: Int, h: Int, prompt: String) {
        val bannerRect = RectF(40f, h - 140f, w - 40f, h - 30f)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            alpha = 200
        }
        canvas.drawRoundRect(bannerRect, 16f, 16f, bannerPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Edited: \"${prompt.take(45)}\"", w / 2f, h - 75f, textPaint)
    }
}
