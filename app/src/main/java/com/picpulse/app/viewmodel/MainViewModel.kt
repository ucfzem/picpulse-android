package com.picpulse.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.picpulse.app.core.*
import com.picpulse.app.data.ImageRepository
import com.picpulse.app.data.model.ImageResult
import com.picpulse.app.data.model.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImageRepository(application)
    private val classifier = Classifier(application)
    private val flareDetector = FlareDetector()
    private val ocrEngine = MlKitOcrEngine()
    private val renamer = Renamer()

    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private val _results = MutableStateFlow<List<ImageResult>>(emptyList())
    val results: StateFlow<List<ImageResult>> = _results.asStateFlow()

    private val _modelDownloadProgress = MutableStateFlow(-1)
    val modelDownloadProgress: StateFlow<Int> = _modelDownloadProgress.asStateFlow()

    var addFlareTag = true
    var flareThreshold = 0.3f
    var useOcr = true
    var previewMode = true

    fun downloadModel() {
        viewModelScope.launch {
            _modelDownloadProgress.value = 0
            val success = classifier.downloadModel { progress ->
                _modelDownloadProgress.value = progress
            }
            if (!success) {
                _state.value = ProcessingState.Error("Failed to download AI model")
            }
            _modelDownloadProgress.value = 100
        }
    }

    fun loadModel(): Boolean {
        return classifier.load()
    }

    fun startProcessing(folderUri: Uri) {
        if (!classifier.isLoaded) {
            _state.value = ProcessingState.Error("AI model not loaded. Download it first.")
            return
        }

        viewModelScope.launch {
            _results.value = emptyList()
            _state.value = ProcessingState.Scanning(0)

            renamer.addFlareTag = addFlareTag
            flareDetector.let {
                // threshold is set in constructor, we use default
            }

            val files = repository.scanFolder(folderUri)
            _state.value = ProcessingState.Scanning(files.size)

            val results = mutableListOf<ImageResult>()

            repository.processFolder(
                folderUri = folderUri,
                classifier = classifier,
                flareDetector = flareDetector,
                ocrEngine = if (useOcr) ocrEngine else NoopOcrEngine(),
                renamer = renamer,
                onProgress = { progress ->
                    _state.value = ProcessingState.Processing(
                        current = progress.current,
                        total = progress.total,
                        fileName = progress.fileName,
                        keyword = progress.keyword,
                        confidence = progress.confidence,
                        hasFlare = progress.hasFlare
                    )
                },
                onResult = { result ->
                    results.add(result)
                    _results.value = results.toList()
                }
            )

            val renamed = results.count { it.renamed }
            val flared = results.count { it.hasFlare }
            val errors = results.count { it.error != null }

            _state.value = ProcessingState.Complete(
                total = files.size,
                renamed = renamed,
                flared = flared,
                errors = errors,
                results = results
            )
        }
    }

    fun reset() {
        _state.value = ProcessingState.Idle
        _results.value = emptyList()
    }

    override fun onCleared() {
        classifier.close()
        ocrEngine.close()
        super.onCleared()
    }
}
