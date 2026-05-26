package com.example.popsandbops.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobDefaults
import com.example.popsandbops.data.BlobShapePreset
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.ui.components.BlobPreview
import com.example.popsandbops.ui.components.WaveformView
import com.example.popsandbops.ui.components.smoothBlobPath
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundEditorScreen(
    blob: SoundBlob,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onPlay: (SoundBlob) -> Unit,
    onNameChange: (String) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onColorChange: (Long) -> Unit,
    onShapePresetChange: (BlobShapePreset, List<Float>) -> Unit,
    onShapePointsChange: (List<Float>) -> Unit,
    onTrimChange: (Int, Int) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Edit blob",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (blob.isPinned) "Pinned to map" else "Library only",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(onClick = { onPlay(blob) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${blob.name}")
                }
                FilledIconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close editor")
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = blob.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Pin on map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Switch(checked = blob.isPinned, onCheckedChange = onPinnedChange)
                }
            }
        }

        EditorSection(title = "Colour") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BlobDefaults.palette.forEach { colorArgb ->
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onColorChange(colorArgb) },
                        shape = CircleShape,
                        color = Color(colorArgb),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (blob.colorArgb == colorArgb) 4.dp else 1.dp,
                            color = if (blob.colorArgb == colorArgb) MaterialTheme.colorScheme.onSurface else Color.White,
                        ),
                    ) {}
                }
            }
        }

        EditorSection(title = "Shape") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BlobDefaults.shapeLibrary.forEach { (preset, points) ->
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onShapePresetChange(preset, points) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (blob.shapePreset == preset) 3.dp else 1.dp,
                            color = if (blob.shapePreset == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        BlobPreview(
                            color = blob.color,
                            points = points,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            ShapeEditorCanvas(
                points = blob.shapePoints,
                color = blob.color,
                onPointsChange = onShapePointsChange,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (blob.shapePoints.size < 16) {
                            val next = blob.shapePoints.toMutableList()
                            next.add(1f)
                            onShapePointsChange(next)
                        }
                    },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Point")
                }
                IconButton(
                    enabled = blob.shapePoints.size > 5,
                    onClick = { onShapePointsChange(blob.shapePoints.dropLast(1)) },
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Remove point")
                }
            }
        }

        EditorSection(title = "Trim") {
            WaveformView(
                waveform = blob.waveform,
                durationMs = blob.sourceDurationMs,
                trimStartMs = blob.trimStartMs,
                trimEndMs = blob.trimEndMs,
                activeColor = blob.color,
            )
            RangeSlider(
                value = blob.trimStartMs.toFloat()..blob.trimEndMs.toFloat(),
                onValueChange = { range ->
                    val start = range.start.roundToInt().coerceIn(0, blob.sourceDurationMs - 200)
                    val end = range.endInclusive.roundToInt().coerceIn(start + 200, blob.sourceDurationMs)
                    onTrimChange(start, end)
                },
                valueRange = 0f..blob.sourceDurationMs.toFloat(),
            )
        }
    }
}

@Composable
private fun EditorSection(
    title: String,
    content: @Composable Column.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            content()
        }
    }
}

@Composable
private fun ShapeEditorCanvas(
    points: List<Float>,
    color: Color,
    onPointsChange: (List<Float>) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activePoint by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(points, canvasSize) {
                detectTapGestures { offset ->
                    val anchors = handleOffsets(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), points)
                    if (nearestHandle(offset, anchors) != null || points.size >= 16) return@detectTapGestures
                    val insertAt = insertionIndex(offset, canvasSize, points.size)
                    val next = points.toMutableList()
                    next.add(insertAt, multiplierFor(offset, canvasSize))
                    onPointsChange(next)
                }
            }
            .pointerInput(points, canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val anchors = handleOffsets(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), points)
                        activePoint = nearestHandle(offset, anchors) ?: -1
                    },
                    onDragEnd = { activePoint = -1 },
                    onDragCancel = { activePoint = -1 },
                ) { change, _ ->
                    val index = activePoint
                    if (index >= 0) {
                        val next = points.toMutableList()
                        next[index] = multiplierFor(change.position, canvasSize)
                        onPointsChange(next)
                    }
                }
            },
    ) {
        val safePoints = points.ifEmpty { List(8) { 1f } }
        val pathSize = Size(size.minDimension * 0.78f, size.minDimension * 0.78f)
        val path = smoothBlobPath(pathSize, safePoints)
        val pathOffset = Offset((size.width - pathSize.width) / 2f, (size.height - pathSize.height) / 2f)
        translate(left = pathOffset.x, top = pathOffset.y) {
            drawPath(path = path, color = color)
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.72f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }

        handleOffsets(size, safePoints).forEachIndexed { index, offset ->
            drawCircle(
                color = if (index == activePoint) MaterialTheme.colorScheme.primary else Color.White,
                radius = 9.dp.toPx(),
                center = offset,
            )
            drawCircle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                radius = 9.dp.toPx(),
                center = offset,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

private fun handleOffsets(size: Size, points: List<Float>): List<Offset> {
    val safePoints = points.ifEmpty { List(8) { 1f } }
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.31f
    return safePoints.mapIndexed { index, multiplier ->
        val angle = (index.toFloat() / safePoints.size) * PI.toFloat() * 2f - PI.toFloat() / 2f
        Offset(
            x = center.x + cos(angle) * radius * multiplier,
            y = center.y + sin(angle) * radius * multiplier,
        )
    }
}

private fun nearestHandle(offset: Offset, anchors: List<Offset>): Int? {
    return anchors
        .mapIndexed { index, anchor -> index to hypot(offset.x - anchor.x, offset.y - anchor.y) }
        .minByOrNull { it.second }
        ?.takeIf { it.second < 42f }
        ?.first
}

private fun multiplierFor(offset: Offset, size: IntSize): Float {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.31f
    return (hypot(offset.x - center.x, offset.y - center.y) / radius).coerceIn(0.58f, 1.36f)
}

private fun insertionIndex(offset: Offset, size: IntSize, count: Int): Int {
    val center = Offset(size.width / 2f, size.height / 2f)
    val angle = atan2(offset.y - center.y, offset.x - center.x)
    val normalized = (((angle + PI.toFloat() / 2f) + PI.toFloat() * 2f) % (PI.toFloat() * 2f)) / (PI.toFloat() * 2f)
    return (normalized * count).roundToInt().coerceIn(0, count)
}
