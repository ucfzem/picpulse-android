package com.picpulse.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.picpulse.app.core.*
import com.picpulse.app.data.model.ImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageRepository(private val context: Context) {

    data class Progress(
        val current: Int, val total: Int, val fileName: String,
        val keyword: String = "", val confidence: Float = 0f,
        val hasFlare: Boolean = false
    )

    suspend fun scanFolder(folderUri: Uri): List<DocumentFile> = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        scanRecursive(folder)
    }

    private fun scanRecursive(folder: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        val files = folder.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                result.addAll(scanRecursive(file))
            } else if (file.isFile && isImage(file.name ?: "")) {
                result.add(file)
            }
        }
        return result
    }

    private fun isImage(name: String): Boolean {
        return name.substringAfterLast('.', "").lowercase() in
                setOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp")
    }

    suspend fun processFolder(
        folderUri: Uri,
        classifier: Classifier,
        flareDetector: FlareDetector,
        ocrEngine: OcrEngine,
        renamer: Renamer,
        onProgress: (Progress) -> Unit,
        onResult: (ImageResult) -> Unit
    ) = withContext(Dispatchers.IO) {
        renamer.reset()
        val files = scanFolder(folderUri)
        val total = files.size

        for ((idx, file) in files.withIndex()) {
            val fileName = file.name ?: "unknown"

            onProgress(Progress(idx + 1, total, fileName))

            val bitmap = try {
                val uri = file.uri
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            } catch (e: Exception) {
                null
            }

            if (bitmap == null) {
                onResult(ImageResult(
                    originalUri = file.uri.toString(),
                    originalName = fileName,
                    keyword = "",
                    confidence = 0f,
                    hasFlare = false,
                    flareScore = 0f,
                    detectedText = null,
                    newName = null,
                    error = "Cannot decode image"
                ))
                continue
            }

            // Step 1: Flare detection
            val flareResult = flareDetector.analyze(bitmap)

            // Step 2: AI classification
            val classResult = classifier.classify(bitmap)

            // Step 3: OCR
            val ocrResult = ocrEngine.recognize(bitmap)

            // Step 4: Determine keyword
            val keyword = when {
                ocrResult != null && ocrResult.confidence > 0.5f -> "document"
                classResult != null -> classResult.cleanLabel
                else -> "unknown"
            }

            onProgress(Progress(
                idx + 1, total, fileName,
                keyword = keyword,
                confidence = classResult?.confidence ?: ocrResult?.confidence ?: 0f,
                hasFlare = flareResult.hasFlare
            ))

            // Step 5: Plan rename
            val plan = renamer.plan(
                originalUri = file.uri.toString(),
                originalName = fileName,
                keyword = keyword,
                hasFlare = flareResult.hasFlare
            )

            // Step 6: Execute rename (copy to new name in same folder)
            var renamed = false
            if (plan.willRename) {
                try {
                    val parent = file.parentFile
                    if (parent != null) {
                        val newFile = parent.findFile(plan.newName)
                        if (newFile == null) {
                            renameFile(file, plan.newName)
                            renamed = true
                        } else {
                            // Conflict: skip
                        }
                    }
                } catch (e: Exception) {
                    // Rename failed, skip
                }
            }

            onResult(ImageResult(
                originalUri = file.uri.toString(),
                originalName = fileName,
                keyword = plan.keyword,
                confidence = classResult?.confidence ?: 0f,
                hasFlare = flareResult.hasFlare,
                flareScore = flareResult.score,
                detectedText = ocrResult?.detectedText,
                newName = plan.newName,
                renamed = renamed
            ))
        }
    }

    private fun renameFile(file: DocumentFile, newName: String): Boolean {
        return try {
            val uri = file.uri
            val parentUri = uri.buildUpon()
                .appendEncodedPath("..")
                .build()
            val from = DocumentsContract.renameDocument(
                context.contentResolver, uri, newName
            )
            from != null
        } catch (e: Exception) {
            false
        }
    }
}
