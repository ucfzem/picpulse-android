package com.picpulse.app.core

import android.graphics.Bitmap

class NoopOcrEngine : OcrEngine {
    override suspend fun recognize(bitmap: Bitmap): OcrEngine.Result? = null
    override fun close() {}
}
