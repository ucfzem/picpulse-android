package com.picpulse.app.core

import android.graphics.Bitmap

interface OcrEngine {
    data class Result(val detectedText: String, val confidence: Float)
    suspend fun recognize(bitmap: Bitmap): Result?
    fun close()
}
