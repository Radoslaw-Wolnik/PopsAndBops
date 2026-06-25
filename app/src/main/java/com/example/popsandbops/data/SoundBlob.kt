package com.example.popsandbops.data

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

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

data class BlobShapeNode(
    val anchor: MapPoint,
    val inHandle: MapPoint,
    val outHandle: MapPoint,
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
    val nodes: List<BlobShapeNode> = emptyList(),
    val assetName: String? = null,
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
    val shapeNodes: List<BlobShapeNode> = emptyList(),
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

    val shapeLibrary: List<BlobShapeTemplate> = BlobAssetLibrary.templates

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
                shapeNodes = shape.nodes,
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
}

fun SoundBlob.effectiveShapeNodes(): List<BlobShapeNode> {
    return shapeNodes.takeIf { it.isValidBlobShapeNodes() }
        ?: shapePoints.toBlobShapeNodes(curveTension)
}

fun List<BlobShapeNode>.toShapePointMultipliers(): List<Float> {
    return map { node ->
        (hypot(node.anchor.x, node.anchor.y) / BLOB_NODE_RADIAL_SCALE)
            .coerceIn(MIN_SHAPE_POINT_MULTIPLIER, MAX_SHAPE_POINT_MULTIPLIER)
    }
}

fun List<Float>.toBlobShapeNodes(
    curveTension: Float = DEFAULT_BLOB_CURVE_TENSION,
): List<BlobShapeNode> {
    val safePoints = takeIf { it.size >= MIN_BLOB_SHAPE_NODES } ?: List(8) { 1f }
    val anchors = safePoints.mapIndexed { index, multiplier ->
        val angle = (index.toFloat() / safePoints.size) * PI.toFloat() * 2f - PI.toFloat() / 2f
        MapPoint(
            x = cos(angle) * BLOB_NODE_RADIAL_SCALE * multiplier,
            y = sin(angle) * BLOB_NODE_RADIAL_SCALE * multiplier,
        )
    }
    return autoHandleNodes(anchors, curveTension)
}

fun autoHandleNodes(
    anchors: List<MapPoint>,
    curveTension: Float = DEFAULT_BLOB_CURVE_TENSION,
): List<BlobShapeNode> {
    if (anchors.size < MIN_BLOB_SHAPE_NODES) return emptyList()
    val tension = curveTension.coerceIn(MIN_BLOB_CURVE_TENSION, MAX_BLOB_CURVE_TENSION)
    return anchors.mapIndexed { index, anchor ->
        val previous = anchors[(index - 1 + anchors.size) % anchors.size]
        val next = anchors[(index + 1) % anchors.size]
        BlobShapeNode(
            anchor = anchor,
            inHandle = MapPoint(
                x = anchor.x - (next.x - previous.x) * tension,
                y = anchor.y - (next.y - previous.y) * tension,
            ),
            outHandle = MapPoint(
                x = anchor.x + (next.x - previous.x) * tension,
                y = anchor.y + (next.y - previous.y) * tension,
            ),
        )
    }
}

fun List<BlobShapeNode>.sanitizeShapeNodes(fallback: List<BlobShapeNode>): List<BlobShapeNode> {
    val sanitized = take(MAX_BLOB_SHAPE_NODES).map { node ->
        BlobShapeNode(
            anchor = node.anchor.sanitizeShapePoint(),
            inHandle = node.inHandle.sanitizeShapePoint(),
            outHandle = node.outHandle.sanitizeShapePoint(),
        )
    }
    return sanitized.takeIf { it.isValidBlobShapeNodes() } ?: fallback
}

fun List<BlobShapeNode>.isValidBlobShapeNodes(): Boolean {
    return size in MIN_BLOB_SHAPE_NODES..MAX_BLOB_SHAPE_NODES &&
        all { node ->
            node.anchor.isFiniteShapePoint() &&
                node.inHandle.isFiniteShapePoint() &&
                node.outHandle.isFiniteShapePoint()
        }
}

private fun MapPoint.sanitizeShapePoint(): MapPoint {
    return MapPoint(
        x = x.takeIf { it.isFinite() }?.coerceIn(MIN_BLOB_NODE_COORDINATE, MAX_BLOB_NODE_COORDINATE) ?: 0f,
        y = y.takeIf { it.isFinite() }?.coerceIn(MIN_BLOB_NODE_COORDINATE, MAX_BLOB_NODE_COORDINATE) ?: 0f,
    )
}

private fun MapPoint.isFiniteShapePoint(): Boolean {
    return x.isFinite() && y.isFinite() &&
        abs(x) <= MAX_BLOB_NODE_COORDINATE &&
        abs(y) <= MAX_BLOB_NODE_COORDINATE
}

const val MIN_BLOB_SHAPE_NODES = 5
const val MAX_BLOB_SHAPE_NODES = 24
const val BLOB_NODE_RADIAL_SCALE = 0.84f
const val MIN_SHAPE_POINT_MULTIPLIER = 0.48f
const val MAX_SHAPE_POINT_MULTIPLIER = 1.52f
const val MIN_BLOB_CURVE_TENSION = 0.04f
const val MAX_BLOB_CURVE_TENSION = 0.38f
const val MIN_BLOB_NODE_COORDINATE = -1.35f
const val MAX_BLOB_NODE_COORDINATE = 1.35f

object BlobMapLayout {
    const val FirstRingRadius = 142f
    const val RingSpacing = 130f
    const val MinimumBlobSpacing = 126f

    fun suggestedPosition(index: Int): MapPoint {
        val ring = ringForIndex(index)
        val slots = slotsForRing(ring)
        val slot = index - firstIndexInRing(ring)
        return pointOnRing(ring, slot, slots)
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
}
