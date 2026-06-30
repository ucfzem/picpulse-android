package com.picpulse.app.data.model

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data class Scanning(val discovered: Int) : ProcessingState()
    data class Processing(
        val current: Int,
        val total: Int,
        val fileName: String,
        val keyword: String = "",
        val confidence: Float = 0f,
        val hasFlare: Boolean = false
    ) : ProcessingState()
    data class Complete(
        val total: Int,
        val renamed: Int,
        val flared: Int,
        val errors: Int,
        val results: List<ImageResult>
    ) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
