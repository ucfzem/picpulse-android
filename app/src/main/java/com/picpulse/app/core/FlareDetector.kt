package com.picpulse.app.core

import android.graphics.Bitmap

class FlareDetector(private val threshold: Float = 0.3f) {

    data class Result(
        val hasFlare: Boolean,
        val score: Float,
        val brightRatio: Float,
        val brightRegionCount: Int
    )

    private data class Region(
        val area: Int, val meanHue: Float, val isMagenta: Boolean
    )

    fun analyze(bitmap: Bitmap): Result {
        val w = bitmap.width
        val h = bitmap.height
        val totalPixels = w * h
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hue = FloatArray(w * h)
        val value = FloatArray(w * h)
        val saturation = FloatArray(w * h)

        val brightMask = BooleanArray(w * h)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val rf = r / 255f
            val gf = g / 255f
            val bf = b / 255f

            val cmax = maxOf(rf, gf, bf)
            val cmin = minOf(rf, gf, bf)
            val delta = cmax - cmin

            // Value
            value[i] = cmax * 360f
            // Saturation
            saturation[i] = if (cmax == 0f) 0f else (delta / cmax) * 360f
            // Hue
            hue[i] = when {
                delta == 0f -> 0f
                cmax == rf -> 60f * (((gf - bf) / delta) % 6f)
                cmax == gf -> 60f * (((bf - rf) / delta) + 2f)
                else -> 60f * (((rf - gf) / delta) + 4f)
            }
            if (hue[i] < 0f) hue[i] += 360f

            brightMask[i] = value[i] > 240f
        }

        val brightCount = brightMask.count { it }
        val brightRatio = brightCount.toFloat() / totalPixels

        val visited = BooleanArray(w * h)
        val regions = mutableListOf<Region>()

        fun floodFill(start: Int): Pair<Int, Float> {
            val stack = mutableListOf(start)
            var count = 0
            var totalHue = 0f
            while (stack.isNotEmpty()) {
                val idx = stack.removeLast()
                if (idx < 0 || idx >= w * h || visited[idx] || !brightMask[idx]) continue
                visited[idx] = true
                count++
                totalHue += hue[idx]
                val x = idx % w
                val y = idx / w
                if (x > 0) stack.add(idx - 1)
                if (x < w - 1) stack.add(idx + 1)
                if (y > 0) stack.add(idx - w)
                if (y < h - 1) stack.add(idx + w)
            }
            return Pair(count, totalHue)
        }

        for (i in brightMask.indices) {
            if (!visited[i] && brightMask[i]) {
                val (area, totalHue) = floodFill(i)
                if (area >= 50) {
                    val meanHue = totalHue / area
                    val isMagenta = meanHue in 280f..330f
                    regions.add(Region(area, meanHue, isMagenta))
                }
            }
        }

        val largeRegions = regions.count { it.area > 500 }
        val magentaRegions = regions.count { it.isMagenta }

        var score = 0f
        if (brightRatio > 0.15f) score += 0.3f
        if (largeRegions >= 2) score += 0.3f
        if (largeRegions >= 3) score += 0.2f
        if (magentaRegions >= 1) score += 0.2f
        if (brightRatio > 0.05f && largeRegions >= 1) score += 0.2f
        score = score.coerceAtMost(1f)

        return Result(
            hasFlare = score >= threshold,
            score = score,
            brightRatio = brightRatio,
            brightRegionCount = largeRegions
        )
    }
}
