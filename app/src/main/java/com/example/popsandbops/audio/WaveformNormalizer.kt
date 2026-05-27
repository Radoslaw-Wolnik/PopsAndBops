package com.example.popsandbops.audio

import kotlin.math.ceil
import kotlin.math.sqrt

object WaveformNormalizer {
    fun live(samples: List<Float>, maxBars: Int): List<Float> {
        if (samples.isEmpty() || maxBars <= 0) return emptyList()
        return samples
            .takeLast(maxBars)
            .map(::displaySample)
    }

    fun summary(samples: List<Float>, maxBars: Int): List<Float> {
        if (samples.isEmpty() || maxBars <= 0) return emptyList()
        val bucketCount = minOf(samples.size, maxBars)
        val bucketSize = ceil(samples.size.toFloat() / bucketCount).toInt().coerceAtLeast(1)
        val buckets = samples.chunked(bucketSize).map { bucket -> bucket.maxOrNull() ?: 0f }
        val reference = buckets
            .sorted()
            .getOrElse((buckets.lastIndex * 0.92f).toInt()) { buckets.maxOrNull() ?: 0f }
            .coerceAtLeast(0.22f)

        return buckets.map { sample ->
            displaySample(sample / reference)
        }
    }

    private fun displaySample(sample: Float): Float {
        return sqrt(sample.coerceIn(0f, 1f)).coerceIn(MIN_VISIBLE_BAR, 1f)
    }

    private const val MIN_VISIBLE_BAR = 0.06f
}
