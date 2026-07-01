package com.picpulse.app.core

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels

class Classifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    data class Result(val label: String, val cleanLabel: String, val confidence: Float)

    val isLoaded: Boolean get() = interpreter != null

    fun load(): Boolean {
        return try {
            // Try bundled assets first, then fall back to downloaded file
            if (!loadFromAssets()) {
                val modelFile = getModelFile()
                if (!modelFile.exists()) return false
                interpreter = Interpreter(modelFile)
                labels = loadLabels()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun loadFromAssets(): Boolean {
        return try {
            val modelStream = context.assets.open("models/mobilenet_v2_1.0_224.tflite")
            val modelBytes = modelStream.readBytes()
            modelStream.close()
            interpreter = Interpreter(ByteBuffer.wrap(modelBytes))

            val labelsStream = context.assets.open("models/imagenet_labels.txt")
            labels = labelsStream.bufferedReader().readLines()
            labelsStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadModel(onProgress: (Int) -> Unit = {}): Boolean {
        return try {
            val modelUrl = "https://raw.githubusercontent.com/google-ai-edge/LiteRT/main/litert/test/testdata/mobilenet_v2_1.0_224.tflite"
            val labelsUrl = "https://storage.googleapis.com/download.tensorflow.org/data/ImageNetLabels.txt"

            downloadFile(modelUrl, getModelFile(), onProgress)
            downloadFile(labelsUrl, getLabelsFile(), onProgress)

            labels = loadLabels()
            val modelFile = getModelFile()
            interpreter?.close()
            interpreter = Interpreter(modelFile)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getModelDir(): File {
        val dir = File(context.filesDir, "models")
        dir.mkdirs()
        return dir
    }

    private fun getModelFile(): File = File(getModelDir(), "mobilenet_v2_1.0_224.tflite")
    private fun getLabelsFile(): File = File(getModelDir(), "imagenet_labels.txt")

    private fun downloadFile(url: String, dest: File, onProgress: (Int) -> Unit) {
        URL(url).openConnection().apply {
            setRequestProperty("User-Agent", "PicPulse")
            connect()
            val inputStream = getInputStream()
            val totalSize = contentLengthLong
            val outputStream = FileOutputStream(dest)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalSize > 0) {
                    onProgress(((totalRead * 100) / totalSize).toInt())
                }
            }
            outputStream.close()
            inputStream.close()
        }
    }

    private fun loadLabels(): List<String> {
        return try {
            getLabelsFile().readLines()
        } catch (e: Exception) {
            (0 until 1001).map { "class_$it" }
        }
    }

    fun classify(bitmap: Bitmap): Result? {
        val interpreter = interpreter ?: return null
        if (labels.isEmpty()) return null

        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        input.order(ByteOrder.nativeOrder())
        input.rewind()

        val pixels = IntArray(224 * 224)
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            input.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1.0f)
            input.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1.0f)
            input.putFloat((pixel and 0xFF) / 127.5f - 1.0f)
        }

        val output = Array(1) { FloatArray(1001) }
        interpreter.run(input, output)

        val probs = output[0]
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: return null
        val confidence = probs[maxIdx]

        val rawLabel = labels.getOrElse(maxIdx) { "unknown" }
        val cleanLabel = rawLabel
            .split(",").first().trim()
            .lowercase().replace(" ", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
            .ifEmpty { "unknown" }

        return Result(rawLabel, cleanLabel, confidence)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
