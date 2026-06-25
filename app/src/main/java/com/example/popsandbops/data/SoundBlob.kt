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
    private const val SHAPE_PRESET_COUNT = 100

    val palette = listOf(
        0xFFFF5A7A, 0xFFFF3366, 0xFFE84855, 0xFFFF6B6B,
        0xFFFF8A3D, 0xFFFF9F1C, 0xFFFFC857, 0xFFFFE066,
        0xFFB8F35F, 0xFF90BE6D, 0xFF6A994E, 0xFF2DD36F,
        0xFF28C7B7, 0xFF00A6A6, 0xFF4ECDC4, 0xFF00BBF9,
        0xFF5BC0EB, 0xFF4D96FF, 0xFF2B59C3, 0xFF7C5CFF,
        0xFF9B5DE5, 0xFFF15BB5, 0xFFB565A7, 0xFFFF77B7,
        0xFF8AC926, 0xFF1982C4, 0xFF6A4C93, 0xFFFFCA3A,
    )

    val shapeLibrary: List<Pair<BlobShapePreset, List<Float>>> =
        List(SHAPE_PRESET_COUNT) { index -> generatedShapePreset(index) }

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

    private fun generatedShapePreset(index: Int): Pair<BlobShapePreset, List<Float>> {
        val preset = BlobShapePreset.entries[index % BlobShapePreset.entries.size]
        return preset to generatedShapePoints(index, preset)
    }

    private fun generatedShapePoints(index: Int, preset: BlobShapePreset): List<Float> {
        val seed = index + 1
        val count = when (preset) {
            BlobShapePreset.Splash -> 12 + (seed * 3).floorMod(7)
            BlobShapePreset.Pebble -> 10 + (seed * 5).floorMod(5)
            BlobShapePreset.Starburst -> 14 + (seed * 7).floorMod(7)
            BlobShapePreset.Pillow -> 10 + (seed * 2).floorMod(6)
            BlobShapePreset.Wobble -> 11 + (seed * 4).floorMod(8)
        }
        val slowFrequency = 2 + seed.floorMod(3)
        val mediumFrequency = 3 + (seed * 2).floorMod(5)
        val fineFrequency = 5 + (seed * 3).floorMod(5)
        val phaseOne = seed * 0.71f
        val phaseTwo = seed * 1.37f
        val phaseThree = seed * 2.11f

        return List(count) { point ->
            val angle = point / count.toFloat() * PI.toFloat() * 2f
            val slow = sin(angle * slowFrequency + phaseOne)
            val medium = sin(angle * mediumFrequency + phaseTwo)
            val fine = sin(angle * fineFrequency + phaseThree)
            val lobe = cos(angle - phaseTwo).coerceAtLeast(0f)
            val value = when (preset) {
                BlobShapePreset.Splash -> 1f + slow * 0.13f + medium * 0.16f + fine * 0.06f + lobe * 0.10f
                BlobShapePreset.Pebble -> 1f + slow * 0.07f + medium * 0.045f + fine * 0.025f
                BlobShapePreset.Starburst -> {
                    val alternating = if (point % 2 == 0) 0.18f else -0.15f
                    1f + alternating + slow * 0.07f + medium * 0.09f + fine * 0.05f
                }
                BlobShapePreset.Pillow -> 1f + slow * 0.11f + cos(angle * 4f + phaseThree) * 0.055f
                BlobShapePreset.Wobble -> 1f + slow * 0.15f + medium * 0.10f + fine * 0.04f - lobe * 0.06f
            }
            value.coerceIn(0.62f, 1.34f)
        }
    }

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
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
