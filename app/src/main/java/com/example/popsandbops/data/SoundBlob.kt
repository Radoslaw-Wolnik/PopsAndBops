package com.example.popsandbops.data

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

const val MIN_SOUND_DURATION_MS = 250
const val DEFAULT_BLOB_CURVE_TENSION = 0.18f

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
    Chime,
    Pluck,
    Flutter,
}

data class MapPoint(
    val x: Float,
    val y: Float,
)

data class TrimRange(
    val startMs: Int,
    val endMs: Int,
) {
    val durationMs: Int
        get() = endMs - startMs
}

data class BlobShapeTemplate(
    val preset: BlobShapePreset,
    val points: List<Float>,
    val curveTension: Float,
) {
    val first: BlobShapePreset
        get() = preset

    val second: List<Float>
        get() = points
}

data class SoundBlob(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
    val position: MapPoint,
    val colorArgb: Long,
    val shapePreset: BlobShapePreset,
    val shapePoints: List<Float>,
    val curveTension: Float = DEFAULT_BLOB_CURVE_TENSION,
    val waveform: List<Float>,
    val trimStartMs: Int,
    val trimEndMs: Int,
    val sourceDurationMs: Int = trimEndMs,
    val isPinned: Boolean = true,
    val audioPath: String? = null,
    val builtInTone: BuiltInTone? = null,
) {
    val color: Color
        get() = Color(colorArgb)

    val safeSourceDurationMs: Int
        get() = sourceDurationMs.coerceAtLeast(MIN_SOUND_DURATION_MS)

    val trimRange: TrimRange
        get() = sanitizeTrimRange(trimStartMs, trimEndMs, safeSourceDurationMs)

    val durationMs: Int
        get() = max(MIN_SOUND_DURATION_MS, trimRange.durationMs)

    val isRecorded: Boolean
        get() = audioPath != null
}

fun sanitizeTrimRange(
    startMs: Int,
    endMs: Int,
    sourceDurationMs: Int,
): TrimRange {
    val safeDurationMs = sourceDurationMs.coerceAtLeast(MIN_SOUND_DURATION_MS)
    val latestStartMs = (safeDurationMs - MIN_SOUND_DURATION_MS).coerceAtLeast(0)
    val safeStartMs = startMs.coerceIn(0, latestStartMs)
    val safeEndMs = endMs.coerceIn(safeStartMs + MIN_SOUND_DURATION_MS, safeDurationMs)
    return TrimRange(safeStartMs, safeEndMs)
}

object BlobDefaults {
    val palette = listOf(
        0xFFFF5A7A, 0xFFFF3366, 0xFFE84855, 0xFFFF6B6B,
        0xFFFF8A3D, 0xFFFF9F1C, 0xFFFFC857, 0xFFFFE066,
        0xFFB8F35F, 0xFF90BE6D, 0xFF6A994E, 0xFF2DD36F,
        0xFF28C7B7, 0xFF00A6A6, 0xFF4ECDC4, 0xFF00BBF9,
        0xFF5BC0EB, 0xFF4D96FF, 0xFF2B59C3, 0xFF7C5CFF,
        0xFF9B5DE5, 0xFFF15BB5, 0xFFB565A7, 0xFFFF77B7,
        0xFF8AC926, 0xFF1982C4, 0xFF6A4C93, 0xFFFFCA3A,
    )

    val shapeLibrary: List<BlobShapeTemplate> = listOf(
        shape(BlobShapePreset.Splash, 0.15f, 1.42f, 0.70f, 1.18f, 0.82f, 1.36f, 0.64f, 1.08f, 0.88f, 1.30f, 0.76f, 1.16f, 0.92f),
        shape(BlobShapePreset.Splash, 0.20f, 0.82f, 1.34f, 0.68f, 1.22f, 0.90f, 1.46f, 0.72f, 1.08f, 1.28f, 0.66f, 1.12f),
        shape(BlobShapePreset.Splash, 0.12f, 1.48f, 0.62f, 1.42f, 0.74f, 1.30f, 0.86f, 1.44f, 0.60f, 1.20f, 0.92f, 1.36f, 0.70f, 1.08f),
        shape(BlobShapePreset.Splash, 0.25f, 1.20f, 1.42f, 0.72f, 0.80f, 1.28f, 0.66f, 1.44f, 0.88f, 1.10f, 0.58f),
        shape(BlobShapePreset.Splash, 0.10f, 0.64f, 1.42f, 0.78f, 1.18f, 1.46f, 0.70f, 1.02f, 0.62f, 1.34f, 0.84f, 1.24f, 0.68f, 1.12f, 0.96f),

        shape(BlobShapePreset.Pebble, 0.28f, 1.26f, 1.20f, 1.04f, 0.84f, 0.72f, 0.92f, 1.12f, 1.30f, 1.16f),
        shape(BlobShapePreset.Pebble, 0.31f, 0.82f, 1.02f, 1.34f, 1.26f, 0.98f, 0.76f, 0.88f, 1.18f, 1.28f, 1.08f),
        shape(BlobShapePreset.Pebble, 0.22f, 1.40f, 1.14f, 0.92f, 0.68f, 0.86f, 1.04f, 1.22f, 1.34f),
        shape(BlobShapePreset.Pebble, 0.34f, 0.74f, 0.92f, 1.12f, 1.36f, 1.42f, 1.16f, 0.94f, 0.78f),
        shape(BlobShapePreset.Pebble, 0.18f, 1.18f, 0.82f, 0.74f, 1.08f, 1.38f, 1.24f, 0.88f, 0.68f, 1.02f, 1.30f),

        shape(BlobShapePreset.Starburst, 0.06f, 1.48f, 0.54f, 1.26f, 0.62f, 1.44f, 0.58f, 1.18f, 0.70f, 1.38f, 0.52f, 1.30f, 0.66f),
        shape(BlobShapePreset.Starburst, 0.08f, 0.58f, 1.46f, 0.64f, 1.18f, 0.54f, 1.40f, 0.72f, 1.24f, 0.60f, 1.34f, 0.68f, 1.12f, 0.56f, 1.44f),
        shape(BlobShapePreset.Starburst, 0.04f, 1.52f, 0.50f, 1.12f, 0.78f, 1.46f, 0.56f, 1.24f, 0.64f, 1.50f, 0.52f, 1.08f, 0.82f, 1.36f, 0.60f, 1.28f, 0.70f),
        shape(BlobShapePreset.Starburst, 0.11f, 1.18f, 0.66f, 1.52f, 0.58f, 1.12f, 0.74f, 1.46f, 0.52f, 1.20f, 0.86f),
        shape(BlobShapePreset.Starburst, 0.13f, 0.62f, 1.34f, 0.70f, 1.48f, 0.56f, 1.08f, 0.84f, 1.40f, 0.64f, 1.18f, 0.76f, 1.52f),

        shape(BlobShapePreset.Pillow, 0.30f, 1.30f, 1.46f, 1.26f, 0.76f, 0.64f, 0.86f, 1.22f, 1.38f),
        shape(BlobShapePreset.Pillow, 0.24f, 0.78f, 1.24f, 1.48f, 1.30f, 0.82f, 0.60f, 0.94f, 1.34f, 1.18f, 0.72f),
        shape(BlobShapePreset.Pillow, 0.18f, 1.46f, 1.24f, 0.70f, 0.60f, 1.22f, 1.50f, 1.16f, 0.68f, 0.82f, 1.30f),
        shape(BlobShapePreset.Pillow, 0.36f, 1.18f, 1.42f, 1.36f, 1.04f, 0.66f, 0.76f, 1.00f, 1.28f),
        shape(BlobShapePreset.Pillow, 0.14f, 1.52f, 0.82f, 0.64f, 1.16f, 1.44f, 1.10f, 0.58f, 0.72f, 1.28f, 1.36f, 0.92f, 0.68f),

        shape(BlobShapePreset.Wobble, 0.21f, 1.34f, 0.68f, 0.76f, 1.22f, 1.48f, 0.92f, 0.58f, 1.14f, 1.30f, 0.84f, 1.02f),
        shape(BlobShapePreset.Wobble, 0.16f, 0.72f, 1.18f, 1.44f, 0.66f, 1.06f, 0.54f, 1.36f, 1.22f, 0.82f, 1.50f, 0.94f, 0.60f),
        shape(BlobShapePreset.Wobble, 0.27f, 1.08f, 1.40f, 0.82f, 0.64f, 1.26f, 0.72f, 1.50f, 1.18f, 0.58f, 0.96f),
        shape(BlobShapePreset.Wobble, 0.09f, 1.50f, 0.74f, 1.04f, 1.28f, 0.56f, 1.42f, 0.88f, 0.64f, 1.20f, 1.36f, 0.78f, 1.08f, 0.52f),
        shape(BlobShapePreset.Wobble, 0.32f, 0.86f, 1.46f, 1.18f, 0.62f, 0.74f, 1.34f, 1.52f, 1.04f, 0.58f, 0.92f, 1.26f),
    )

    fun defaultSoundBlobs(now: Long = System.currentTimeMillis()): List<SoundBlob> {
        val names = listOf(
            "Pop prism",
            "Bubble zap",
            "Velvet boing",
            "Tiny orbit",
            "Lemon pulse",
            "Glass chime",
            "Soft pluck",
            "Firefly flutter",
        )
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
                curveTension = shape.curveTension,
                waveform = generatedWaveform(index + 7, 52),
                trimStartMs = 0,
                trimEndMs = presetDurationMs(index),
                sourceDurationMs = presetDurationMs(index),
                builtInTone = BuiltInTone.entries[index % BuiltInTone.entries.size],
            )
        }
    }

    private fun presetDurationMs(index: Int): Int {
        return listOf(520, 680, 780, 960, 720, 1_120, 640, 1_180)[index % 8]
    }

    fun edgePosition(index: Int): MapPoint {
        return BlobMapLayout.suggestedPosition(index)
    }

    fun generatedWaveform(seed: Int, size: Int): List<Float> {
        return List(size) { index ->
            val slow = sin((index + seed) * 0.34f)
            val fast = sin((index * seed + 11) * 0.91f)
            val shaped = 0.54f + (slow * 0.28f) + (fast * 0.18f)
            shaped.coerceIn(0.12f, 1f)
        }
    }

    private fun shape(
        preset: BlobShapePreset,
        curveTension: Float,
        vararg points: Float,
    ): BlobShapeTemplate {
        return BlobShapeTemplate(
            preset = preset,
            points = points.map { it.coerceIn(0.48f, 1.52f) },
            curveTension = curveTension.coerceIn(0.04f, 0.38f),
        )
    }
}

object BlobMapLayout {
    const val FirstRingRadius = 180f
    const val RingSpacing = 130f
    const val MinimumBlobSpacing = 168f

    fun suggestedPosition(index: Int): MapPoint {
        val angle = -PI.toFloat() / 2f + index * GOLDEN_ANGLE + ((index % 3) - 1) * 0.12f
        val radius = 220f + sqrt(index.toFloat()) * 88f + (index % 5) * 18f
        return MapPoint(
            x = cos(angle) * radius,
            y = sin(angle) * radius,
        )
    }

    fun arrangedPosition(index: Int): MapPoint {
        return suggestedPosition(index)
    }

    fun arrangedPositions(count: Int): List<MapPoint> {
        val placed = mutableListOf<MapPoint>()
        repeat(count) { index ->
            placed += resolveOverlaps(
                position = arrangedPosition(index),
                occupiedPositions = placed,
                minimumSpacing = MinimumBlobSpacing,
            )
        }
        return placed
    }

    fun snapToArrangeSlot(position: MapPoint): MapPoint {
        val distance = hypot(position.x, position.y)
        val ring = ((distance - FirstRingRadius) / RingSpacing)
            .roundToInt()
            .coerceAtLeast(0)
        val slots = slotsForRing(ring)
        val angle = atan2(position.y, position.x)
        val normalized = (((angle + PI.toFloat() / 2f) + PI.toFloat() * 2f) % (PI.toFloat() * 2f)) /
            (PI.toFloat() * 2f)
        val slot = (normalized * slots).roundToInt().floorMod(slots)
        return pointOnRing(ring, slot, slots)
    }

    fun resolveOverlaps(
        position: MapPoint,
        occupiedPositions: List<MapPoint>,
        minimumSpacing: Float = MinimumBlobSpacing,
    ): MapPoint {
        if (occupiedPositions.isEmpty()) return position
        var resolved = position
        repeat(10) { pass ->
            occupiedPositions.forEachIndexed { index, occupied ->
                val dx = resolved.x - occupied.x
                val dy = resolved.y - occupied.y
                val distance = hypot(dx, dy)
                if (distance < minimumSpacing) {
                    val angle = if (distance == 0f) {
                        ((index + pass + 1) * 51f) * (PI.toFloat() / 180f)
                    } else {
                        atan2(dy, dx)
                    }
                    val push = (minimumSpacing - distance).coerceAtMost(minimumSpacing * 0.45f)
                    resolved = MapPoint(
                        x = resolved.x + cos(angle) * push,
                        y = resolved.y + sin(angle) * push,
                    )
                }
            }
        }
        return resolved
    }

    fun guideRadii(maxDistance: Float): List<Float> {
        val ringCount = (((maxDistance - FirstRingRadius) / RingSpacing).roundToInt() + 2).coerceAtLeast(3)
        return List(ringCount) { ring -> FirstRingRadius + ring * RingSpacing }
    }

    private fun ringForIndex(index: Int): Int {
        var remaining = index
        var ring = 0
        while (remaining >= slotsForRing(ring)) {
            remaining -= slotsForRing(ring)
            ring += 1
        }
        return ring
    }

    private fun firstIndexInRing(ring: Int): Int {
        var first = 0
        repeat(ring) { first += slotsForRing(it) }
        return first
    }

    fun slotsForRing(ring: Int): Int = 6 + ring * 4

    private fun pointOnRing(ring: Int, slot: Int, slots: Int): MapPoint {
        val radius = FirstRingRadius + ring * RingSpacing
        val angle = (slot / slots.toFloat()) * PI.toFloat() * 2f - PI.toFloat() / 2f
        return MapPoint(
            x = cos(angle) * radius,
            y = sin(angle) * radius,
        )
    }

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

    private const val GOLDEN_ANGLE = 2.3999631f
}
