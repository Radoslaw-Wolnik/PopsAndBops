package com.example.popsandbops.data

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class BlobShapePreset {
    Splash,
    Pebble,
    Starburst,
    Pillow,
    Wobble,
}

enum class BuiltInTone {
    Pop,
    Bleep,
    Bounce,
    Pulse,
    Spark,
}

data class MapPoint(
    val x: Float,
    val y: Float,
)

data class SoundBlob(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
    val position: MapPoint,
    val colorArgb: Long,
    val shapePreset: BlobShapePreset,
    val shapePoints: List<Float>,
    val waveform: List<Float>,
    val trimStartMs: Int,
    val trimEndMs: Int,
    val isPinned: Boolean = true,
    val audioPath: String? = null,
    val builtInTone: BuiltInTone? = null,
) {
    val color: Color
        get() = Color(colorArgb)

    val durationMs: Int
        get() = max(250, trimEndMs - trimStartMs)

    val isRecorded: Boolean
        get() = audioPath != null
}

object BlobDefaults {
    val palette = listOf(
        0xFFFF5A7A,
        0xFF28C7B7,
        0xFFFFC857,
        0xFF7C5CFF,
        0xFF4ECDC4,
        0xFFFF8A3D,
        0xFF5BC0EB,
        0xFFB8F35F,
    )

    val shapeLibrary = listOf(
        BlobShapePreset.Splash to listOf(1.00f, 0.78f, 1.18f, 0.86f, 1.08f, 0.74f, 1.14f, 0.92f),
        BlobShapePreset.Pebble to listOf(1.00f, 0.96f, 1.04f, 1.08f, 0.98f, 0.90f, 0.94f, 1.02f),
        BlobShapePreset.Starburst to listOf(1.20f, 0.70f, 1.16f, 0.76f, 1.26f, 0.68f, 1.12f, 0.80f),
        BlobShapePreset.Pillow to listOf(1.02f, 1.10f, 1.02f, 0.92f, 1.02f, 1.10f, 1.02f, 0.92f),
        BlobShapePreset.Wobble to listOf(0.94f, 1.18f, 0.82f, 1.05f, 1.14f, 0.88f, 1.08f, 0.96f),
    )

    fun defaultSoundBlobs(now: Long = System.currentTimeMillis()): List<SoundBlob> {
        val names = listOf("Pop prism", "Bubble zap", "Velvet boing", "Tiny orbit", "Lemon pulse")
        return names.mapIndexed { index, name ->
            val shape = shapeLibrary[index % shapeLibrary.size]
            SoundBlob(
                id = "preset-$index",
                name = name,
                createdAtMillis = now - ((names.size - index) * 60_000L),
                position = edgePosition(index),
                colorArgb = palette[index % palette.size],
                shapePreset = shape.first,
                shapePoints = shape.second,
                waveform = generatedWaveform(index + 7, 52),
                trimStartMs = 0,
                trimEndMs = 900 + (index * 140),
                builtInTone = BuiltInTone.entries[index % BuiltInTone.entries.size],
            )
        }
    }

    fun edgePosition(index: Int): MapPoint {
        val ring = index / 8
        val angle = index * 137.5f * (PI.toFloat() / 180f)
        val radius = 180f + ring * 130f + (index % 3) * 34f
        return MapPoint(
            x = cos(angle) * radius,
            y = sin(angle) * radius,
        )
    }

    fun generatedWaveform(seed: Int, size: Int): List<Float> {
        return List(size) { index ->
            val slow = sin((index + seed) * 0.34f)
            val fast = sin((index * seed + 11) * 0.91f)
            val shaped = 0.54f + (slow * 0.28f) + (fast * 0.18f)
            shaped.coerceIn(0.12f, 1f)
        }
    }
}
