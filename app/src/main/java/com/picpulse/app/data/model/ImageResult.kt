package com.picpulse.app.data.model

data class ImageResult(
    val originalUri: String,
    val originalName: String,
    val keyword: String,
    val confidence: Float,
    val hasFlare: Boolean,
    val flareScore: Float,
    val detectedText: String?,
    val newName: String?,
    val renamed: Boolean = false,
    val error: String? = null
)
