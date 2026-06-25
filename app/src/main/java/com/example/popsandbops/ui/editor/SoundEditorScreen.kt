package com.example.popsandbops.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobDefaults
import com.example.popsandbops.data.BlobShapePreset
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.sanitizeTrimRange
import com.example.popsandbops.ui.components.BlobPreview
import com.example.popsandbops.ui.components.rememberPressFeedback
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
    val backPress = rememberPressFeedback(pressedScale = 0.90f)
    val playPress = rememberPressFeedback(pressedScale = 0.90f)
    val addPointPress = rememberPressFeedback(pressedScale = 0.95f)
    val removePointPress = rememberPressFeedback(pressedScale = 0.90f)
    val sourceDurationMs = blob.safeSourceDurationMs
    val trimRange = blob.trimRange
    var selectedShapePoint by remember(blob.id) { mutableIntStateOf(-1) }

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
            IconButton(
                onClick = onClose,
                modifier = Modifier.scale(backPress.scale),
                interactionSource = backPress.interactionSource,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to sound")
            }
            Column(modifier = Modifier.weight(1f)) {
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
            FilledIconButton(
                onClick = { onPlay(blob) },
                modifier = Modifier.scale(playPress.scale),
                interactionSource = playPress.interactionSource,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${blob.name}")
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BlobPreview(
                    color = blob.color,
                    points = blob.shapePoints,
                    modifier = Modifier.size(136.dp),
                )
                Text(
                    text = blob.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${if (blob.isRecorded) "Recording" else "Preset tone"} - ${formatMs(blob.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EditorNameField(
                    value = blob.name,
                    onValueChange = onNameChange,
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
                    val press = rememberPressFeedback(pressedScale = 0.88f)
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(press.scale)
                            .clickable(
                                interactionSource = press.interactionSource,
                                indication = LocalIndication.current,
                            ) { onColorChange(colorArgb) },
                        shape = CircleShape,
                        color = Color(colorArgb),
                        border = BorderStroke(
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
                    val press = rememberPressFeedback(pressedScale = 0.92f)
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(press.scale)
                            .clickable(
                                interactionSource = press.interactionSource,
                                indication = LocalIndication.current,
                            ) { onShapePresetChange(preset, points) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (press.isPressed) 5.dp else 0.dp,
                        border = BorderStroke(
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
                selectedPoint = selectedShapePoint,
                onSelectedPointChange = { selectedShapePoint = it },
                onPointsChange = onShapePointsChange,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (blob.shapePoints.size < MAX_SHAPE_POINTS) {
                            val insertAt = if (selectedShapePoint in blob.shapePoints.indices) {
                                selectedShapePoint + 1
                            } else {
                                blob.shapePoints.size
                            }
                            val next = blob.shapePoints.toMutableList()
                            val value = if (selectedShapePoint in blob.shapePoints.indices) {
                                val following = blob.shapePoints[(selectedShapePoint + 1) % blob.shapePoints.size]
                                (blob.shapePoints[selectedShapePoint] + following) / 2f
                            } else {
                                1f
                            }
                            next.add(insertAt, value)
                            selectedShapePoint = insertAt
                            onShapePointsChange(next)
                        }
                    },
                    enabled = blob.shapePoints.size < MAX_SHAPE_POINTS,
                    modifier = Modifier.scale(addPointPress.scale),
                    interactionSource = addPointPress.interactionSource,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add point")
                }
                Button(
                    enabled = blob.shapePoints.size > MIN_SHAPE_POINTS && selectedShapePoint in blob.shapePoints.indices,
                    onClick = {
                        val next = blob.shapePoints.toMutableList().also { it.removeAt(selectedShapePoint) }
                        selectedShapePoint = selectedShapePoint.coerceAtMost(next.lastIndex)
                        onShapePointsChange(next)
                    },
                    modifier = Modifier.scale(removePointPress.scale),
                    interactionSource = removePointPress.interactionSource,
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Remove")
                }
            }
        }

        EditorSection(title = "Trim") {
            WaveformView(
                waveform = blob.waveform,
                durationMs = sourceDurationMs,
                trimStartMs = trimRange.startMs,
                trimEndMs = trimRange.endMs,
                activeColor = blob.color,
            )
            RangeSlider(
                value = trimRange.startMs.toFloat()..trimRange.endMs.toFloat(),
                onValueChange = { range ->
                    val trim = sanitizeTrimRange(
                        startMs = range.start.roundToInt(),
                        endMs = range.endInclusive.roundToInt(),
                        sourceDurationMs = sourceDurationMs,
                    )
                    onTrimChange(trim.startMs, trim.endMs)
                },
                valueRange = 0f..sourceDurationMs.toFloat(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatMs(trimRange.startMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Text(
                    text = formatMs(trimRange.endMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
            }
        }
    }
}

@Composable
private fun EditorSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
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
private fun EditorNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isBlank()) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(36)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShapeEditorCanvas(
    points: List<Float>,
    color: Color,
    selectedPoint: Int,
    onSelectedPointChange: (Int) -> Unit,
    onPointsChange: (List<Float>) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activePoint by remember { mutableIntStateOf(-1) }
    val activeHandleColor = MaterialTheme.colorScheme.primary
    val handleBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val editorBackground = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = darker(color)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(editorBackground, RoundedCornerShape(8.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(points, canvasSize) {
                detectTapGestures { offset ->
                    val anchors = handleOffsets(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), points)
                    val handle = nearestHandle(offset, anchors)
                    if (handle != null) {
                        onSelectedPointChange(handle)
                    } else {
                        onSelectedPointChange(-1)
                    }
                }
            }
            .pointerInput(points, canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val anchors = handleOffsets(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()), points)
                        activePoint = nearestHandle(offset, anchors) ?: -1
                        if (activePoint >= 0) {
                            onSelectedPointChange(activePoint)
                        }
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
        val handles = handleOffsets(size, safePoints)
        translate(left = pathOffset.x, top = pathOffset.y) {
            drawPath(path = path, color = color)
            drawPath(
                path = path,
                color = outlineColor.copy(alpha = 0.78f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }

        handles.forEachIndexed { index, offset ->
            drawCircle(
                color = if (index == activePoint || index == selectedPoint) activeHandleColor else Color.White,
                radius = if (index == selectedPoint) 13.dp.toPx() else 10.dp.toPx(),
                center = offset,
            )
            drawCircle(
                color = handleBorderColor,
                radius = if (index == selectedPoint) 13.dp.toPx() else 10.dp.toPx(),
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
        ?.takeIf { it.second < 54f }
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

private fun darker(color: Color): Color {
    return Color(
        red = color.red * 0.68f,
        green = color.green * 0.68f,
        blue = color.blue * 0.68f,
        alpha = color.alpha,
    )
}

private const val MIN_SHAPE_POINTS = 5
private const val MAX_SHAPE_POINTS = 24

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1_000f).coerceAtLeast(0f)
    return "%.1fs".format(totalSeconds)
}
