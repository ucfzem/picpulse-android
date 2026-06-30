package com.picpulse.app.core

data class RenamePlan(
    val originalUri: String,
    val originalName: String,
    val extension: String,
    val keyword: String,
    val hasFlare: Boolean,
    val newName: String,
    val willRename: Boolean
)

class Renamer(var addFlareTag: Boolean = true) {
    private val counters = mutableMapOf<String, Int>()
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp")

    fun isImage(name: String): Boolean {
        return name.substringAfterLast('.', "").lowercase() in imageExtensions
    }

    fun plan(
        originalUri: String,
        originalName: String,
        keyword: String,
        hasFlare: Boolean
    ): RenamePlan {
        val ext = originalName.substringAfterLast('.', "").lowercase()
        var baseKeyword = keyword.ifEmpty { "unknown" }

        val count = counters.getOrDefault(baseKeyword, 0) + 1
        counters[baseKeyword] = count

        var newName = "${baseKeyword}_${"%03d".format(count)}.$ext"
        if (hasFlare && addFlareTag) {
            newName = "flare_$newName"
        }

        return RenamePlan(
            originalUri = originalUri,
            originalName = originalName,
            extension = ext,
            keyword = baseKeyword,
            hasFlare = hasFlare,
            newName = newName,
            willRename = originalName != newName
        )
    }

    fun reset() {
        counters.clear()
    }
}
