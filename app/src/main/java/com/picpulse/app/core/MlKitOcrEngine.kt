package com.picpulse.app.core

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): OcrEngine.Result? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            } ?: return null

            val fullText = visionText.text?.trim() ?: return null
            if (fullText.isEmpty()) return null

            OcrEngine.Result(detectedText = fullText.take(100), confidence = 1.0f)
        } catch (e: Exception) {
            null
        }
    }

    override fun close() {
        recognizer.close()
    }
}
