package com.example.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrManager {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    suspend fun recognizeTextFromBitmaps(bitmaps: List<Bitmap>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        bitmaps.forEachIndexed { index, bitmap ->
            if (index > 0) sb.append("\n--- PAGE ${index + 1} ---\n")
            try {
                val pageText = recognizeTextFromBitmap(bitmap)
                sb.append(pageText)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        sb.toString()
    }
}
