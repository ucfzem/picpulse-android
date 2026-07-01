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
        val area: Int, val meanHue: Float, val isMagenta: Boolean, val isCircularBlob: Boolean
    )

    fun analyze(bitmap: Bitmap): Result {
        val w = bitmap.width
        val h = bitmap.height
        val totalPixels = w * h
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hue = FloatArray(w * h)
        val value = FloatArray(w * h)

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

            // Value (correct 0-255 scale, was cmax * 360f)
            value[i] = cmax * 255f
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

        // Relative-to-image-size region thresholds
        val minRegionPixels = maxOf(30, (totalPixels * 0.0005f).toInt())
        val largeRegionPixels = maxOf(300, (totalPixels * 0.003f).toInt())

        val visited = BooleanArray(w * h)
        val regions = mutableListOf<Region>()

        fun floodFill(start: Int): Region? {
            val stack = mutableListOf(start)
            var count = 0
            var totalHue = 0f
            var minX = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var minY = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE
            while (stack.isNotEmpty()) {
                val idx = stack.removeLast()
                if (idx < 0 || idx >= w * h || visited[idx] || !brightMask[idx]) continue
                visited[idx] = true
                count++
                totalHue += hue[idx]
                val x = idx % w
                val y = idx / w
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (x > 0) stack.add(idx - 1)
                if (x < w - 1) stack.add(idx + 1)
                if (y > 0) stack.add(idx - w)
                if (y < h - 1) stack.add(idx + w)
            }
            if (count < minRegionPixels) return null
            val meanHue = totalHue / count
            val isMagenta = meanHue in 280f..330f
            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            val bboxArea = bw * bh
            val fillRatio = if (bboxArea > 0) count.toFloat() / bboxArea else 0f
            val aspectRatio = maxOf(bw, bh).toFloat() / maxOf(1, minOf(bw, bh))
            val isCircularBlob = fillRatio > 0.55f && aspectRatio < 1.8f
            return Region(count, meanHue, isMagenta, isCircularBlob)
        }

        for (i in brightMask.indices) {
            if (!visited[i] && brightMask[i]) {
                val region = floodFill(i)
                if (region != null) {
                    regions.add(region)
                }
            }
        }

        val circularBlobs = regions.count { r -> r.area > largeRegionPixels && r.isCircularBlob }
        val magentaRegions = regions.count { it.isMagenta }

        var score = 0f
        if (circularBlobs >= 1) score += 0.3f
        if (circularBlobs >= 2) score += 0.3f
        if (magentaRegions >= 1) score += 0.4f
        score = score.coerceAtMost(1f)

        return Result(
            hasFlare = score >= threshold,
            score = score,
            brightRatio = brightRatio,
            brightRegionCount = circularBlobs
        )
    }
}
