package com.example.popsandbops.ui.editor

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobDefaults
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.sanitizeTrimRange
import com.example.popsandbops.ui.components.BlobPreview
import com.example.popsandbops.ui.components.rememberPressFeedback
import com.example.popsandbops.ui.components.WaveformView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
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
    onSave: (SoundBlob) -> Unit,
) {
    val backPress = rememberPressFeedback(pressedScale = 0.90f)
    val playPress = rememberPressFeedback(pressedScale = 0.90f)
    val savePress = rememberPressFeedback(pressedScale = 0.94f)
    var draft by remember(blob.id) { mutableStateOf(blob) }
    var isEditingShape by remember(blob.id) { mutableStateOf(false) }
    val sourceDurationMs = draft.safeSourceDurationMs
    val trimRange = draft.trimRange
    val hasChanges = draft != blob
    val canSave = draft.name.isNotBlank() && hasChanges
    var selectedShapePoint by remember(blob.id) { mutableIntStateOf(-1) }

    BackHandler(enabled = isEditingShape) {
        isEditingShape = false
    }

    if (isEditingShape) {
        ShapeVectorEditorScreen(
            points = draft.shapePoints,
            color = draft.color,
            selectedPoint = selectedShapePoint,
            modifier = modifier.fillMaxSize(),
            onSelectedPointChange = { selectedShapePoint = it },
            onPointsChange = { draft = draft.copy(shapePoints = it) },
            onDone = { isEditingShape = false },
        )
        return
    }

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
                    text = if (draft.isPinned) "Pinned to map" else "Library only",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                )
            }
            FilledIconButton(
                onClick = { onPlay(draft) },
                modifier = Modifier.scale(playPress.scale),
                interactionSource = playPress.interactionSource,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${draft.name}")
            }
            Button(
                onClick = { onSave(draft) },
                enabled = canSave,
                modifier = Modifier.scale(savePress.scale),
                interactionSource = savePress.interactionSource,
            ) {
                Text("Save")
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
                    color = draft.color,
                    points = draft.shapePoints,
                    modifier = Modifier.size(136.dp),
                )
                Text(
                    text = draft.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${if (draft.isRecorded) "Recording" else "Preset tone"} - ${formatMs(draft.durationMs)}",
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
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
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
                    Switch(
                        checked = draft.isPinned,
                        onCheckedChange = { draft = draft.copy(isPinned = it) },
                    )
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
                            ) { draft = draft.copy(colorArgb = colorArgb) },
                        shape = CircleShape,
                        color = Color(colorArgb),
                        border = BorderStroke(
                            width = if (draft.colorArgb == colorArgb) 4.dp else 1.dp,
                            color = if (draft.colorArgb == colorArgb) MaterialTheme.colorScheme.onSurface else Color.White,
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
                            ) {
                                draft = draft.copy(shapePreset = preset, shapePoints = points)
                                selectedShapePoint = -1
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (press.isPressed) 5.dp else 0.dp,
                        border = BorderStroke(
                            width = if (draft.shapePreset == preset) 3.dp else 1.dp,
                            color = if (draft.shapePreset == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        BlobPreview(
                            color = draft.color,
                            points = points,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BlobPreview(
                        color = draft.color,
                        points = draft.shapePoints,
                        modifier = Modifier.size(86.dp),
                    )
                    Text(
                        text = "${draft.shapePoints.size} points",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                }
                Button(
                    onClick = { isEditingShape = true },
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Edit shape")
                }
            }
        }

        EditorSection(title = "Trim") {
            WaveformView(
                waveform = blob.waveform,
                durationMs = sourceDurationMs,
                trimStartMs = trimRange.startMs,
                trimEndMs = trimRange.endMs,
                activeColor = draft.color,
            )
            RangeSlider(
                value = trimRange.startMs.toFloat()..trimRange.endMs.toFloat(),
                onValueChange = { range ->
                    val trim = sanitizeTrimRange(
                        startMs = range.start.roundToInt(),
                        endMs = range.endInclusive.roundToInt(),
                        sourceDurationMs = sourceDurationMs,
                    )
                    draft = draft.copy(trimStartMs = trim.startMs, trimEndMs = trim.endMs)
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
private fun ShapeVectorEditorScreen(
    points: List<Float>,
    color: Color,
    selectedPoint: Int,
    modifier: Modifier = Modifier,
    onSelectedPointChange: (Int) -> Unit,
    onPointsChange: (List<Float>) -> Unit,
    onDone: () -> Unit,
) {
    val backPress = rememberPressFeedback(pressedScale = 0.90f)
    val donePress = rememberPressFeedback(pressedScale = 0.94f)
    val addPointPress = rememberPressFeedback(pressedScale = 0.94f)
    val removePointPress = rememberPressFeedback(pressedScale = 0.94f)

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDone,
                modifier = Modifier.scale(backPress.scale),
                interactionSource = backPress.interactionSource,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to edit blob")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shape",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${points.size} points",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                )
            }
            Button(
                onClick = onDone,
                modifier = Modifier.scale(donePress.scale),
                interactionSource = donePress.interactionSource,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Done")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            ShapeVectorCanvas(
                points = points,
                color = color,
                selectedPoint = selectedPoint,
                modifier = Modifier.fillMaxSize(),
                onSelectedPointChange = onSelectedPointChange,
                onPointsChange = onPointsChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    val (nextPoints, nextSelection) = addPointPreservingOutline(points, selectedPoint)
                    onSelectedPointChange(nextSelection)
                    onPointsChange(nextPoints)
                },
                enabled = points.size < MAX_SHAPE_POINTS,
                modifier = Modifier
                    .weight(1f)
                    .scale(addPointPress.scale),
                interactionSource = addPointPress.interactionSource,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Add")
            }
            Button(
                onClick = {
                    val (nextPoints, nextSelection) = removePointPreservingOutline(points, selectedPoint)
                    onSelectedPointChange(nextSelection)
                    onPointsChange(nextPoints)
                },
                enabled = points.size > MIN_SHAPE_POINTS && selectedPoint in points.indices,
                modifier = Modifier
                    .weight(1f)
                    .scale(removePointPress.scale),
                interactionSource = removePointPress.interactionSource,
            ) {
                Icon(Icons.Filled.Remove, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Remove")
            }
        }
    }
}

@Composable
private fun ShapeVectorCanvas(
    points: List<Float>,
    color: Color,
    selectedPoint: Int,
    modifier: Modifier = Modifier,
    onSelectedPointChange: (Int) -> Unit,
    onPointsChange: (List<Float>) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activePoint by remember { mutableIntStateOf(-1) }
    val activeHandleColor = MaterialTheme.colorScheme.primary
    val handleBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val outlineColor = darker(color)

    Canvas(
        modifier = modifier
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
        val handles = handleOffsets(size, safePoints)
        val path = smoothPathFromAnchors(handles)
        val segments = curveSegments(handles)

        drawVectorGrid(color = guideColor)

        handles.forEach { handle ->
            drawLine(
                color = guideColor.copy(alpha = 0.28f),
                start = center,
                end = handle,
                strokeWidth = 1.dp.toPx(),
            )
        }
        handles.forEachIndexed { index, handle ->
            drawLine(
                color = outlineColor.copy(alpha = 0.32f),
                start = handle,
                end = handles[(index + 1) % handles.size],
                strokeWidth = 1.5.dp.toPx(),
            )
        }

        drawPath(path = path, color = color.copy(alpha = 0.28f))
        segments.forEach { segment ->
            drawLine(
                color = guideColor.copy(alpha = 0.42f),
                start = segment.start,
                end = segment.controlOne,
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = guideColor.copy(alpha = 0.42f),
                start = segment.end,
                end = segment.controlTwo,
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = guideColor.copy(alpha = 0.50f),
                radius = 3.dp.toPx(),
                center = segment.controlOne,
            )
            drawCircle(
                color = guideColor.copy(alpha = 0.50f),
                radius = 3.dp.toPx(),
                center = segment.controlTwo,
            )
        }
        drawPath(
            path = path,
            color = outlineColor.copy(alpha = 0.92f),
            style = Stroke(width = 3.5.dp.toPx()),
        )
        drawCircle(
            color = outlineColor.copy(alpha = 0.30f),
            radius = 4.dp.toPx(),
            center = center,
        )

        handles.forEachIndexed { index, offset ->
            drawCircle(
                color = if (index == activePoint || index == selectedPoint) activeHandleColor else Color.White,
                radius = if (index == selectedPoint) 15.dp.toPx() else 11.dp.toPx(),
                center = offset,
            )
            drawCircle(
                color = handleBorderColor,
                radius = if (index == selectedPoint) 15.dp.toPx() else 11.dp.toPx(),
                center = offset,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

private fun handleOffsets(size: Size, points: List<Float>): List<Offset> {
    val safePoints = points.ifEmpty { List(8) { 1f } }
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = shapeEditRadius(size)
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
        ?.takeIf { it.second < 64f }
        ?.first
}

private fun multiplierFor(offset: Offset, size: IntSize): Float {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = shapeEditRadius(Size(size.width.toFloat(), size.height.toFloat()))
    return (hypot(offset.x - center.x, offset.y - center.y) / radius).coerceIn(0.58f, 1.36f)
}

private fun shapeEditRadius(size: Size): Float {
    return size.minDimension * 0.34f
}

private fun DrawScope.drawVectorGrid(color: Color) {
    val spacing = 32.dp.toPx()
    var x = spacing / 2f
    while (x < size.width) {
        var y = spacing / 2f
        while (y < size.height) {
            drawCircle(
                color = color.copy(alpha = 0.20f),
                radius = 1.25.dp.toPx(),
                center = Offset(x, y),
            )
            y += spacing
        }
        x += spacing
    }
}

private data class CurveSegment(
    val start: Offset,
    val controlOne: Offset,
    val controlTwo: Offset,
    val end: Offset,
)

private fun smoothPathFromAnchors(anchors: List<Offset>): Path {
    val path = Path()
    if (anchors.isEmpty()) return path
    path.moveTo(anchors.first().x, anchors.first().y)
    curveSegments(anchors).forEach { segment ->
        path.cubicTo(
            segment.controlOne.x,
            segment.controlOne.y,
            segment.controlTwo.x,
            segment.controlTwo.y,
            segment.end.x,
            segment.end.y,
        )
    }
    path.close()
    return path
}

private fun curveSegments(anchors: List<Offset>): List<CurveSegment> {
    if (anchors.isEmpty()) return emptyList()
    return anchors.mapIndexed { index, anchor ->
        val previous = anchors[(index - 1 + anchors.size) % anchors.size]
        val next = anchors[(index + 1) % anchors.size]
        val afterNext = anchors[(index + 2) % anchors.size]
        CurveSegment(
            start = anchor,
            controlOne = Offset(
                x = anchor.x + (next.x - previous.x) * CURVE_TENSION,
                y = anchor.y + (next.y - previous.y) * CURVE_TENSION,
            ),
            controlTwo = Offset(
                x = next.x - (afterNext.x - anchor.x) * CURVE_TENSION,
                y = next.y - (afterNext.y - anchor.y) * CURVE_TENSION,
            ),
            end = next,
        )
    }
}

private fun addPointPreservingOutline(points: List<Float>, selectedPoint: Int): Pair<List<Float>, Int> {
    val safePoints = points.ifEmpty { List(MIN_SHAPE_POINTS) { 1f } }
    val nextCount = (safePoints.size + 1).coerceAtMost(MAX_SHAPE_POINTS)
    val nextPoints = resampleShapePoints(safePoints, nextCount)
    val edgeStart = selectedPoint.takeIf { it in safePoints.indices } ?: safePoints.lastIndex
    val midpointTurns = (edgeStart + 0.5f) / safePoints.size.toFloat()
    val nextSelection = (midpointTurns * nextCount).roundToInt().floorMod(nextCount)
    return nextPoints to nextSelection
}

private fun removePointPreservingOutline(points: List<Float>, selectedPoint: Int): Pair<List<Float>, Int> {
    val safePoints = points.ifEmpty { List(MIN_SHAPE_POINTS) { 1f } }
    val nextCount = (safePoints.size - 1).coerceAtLeast(MIN_SHAPE_POINTS)
    val nextPoints = resampleShapePoints(safePoints, nextCount)
    val nextSelection = if (nextPoints.isEmpty()) {
        -1
    } else {
        ((selectedPoint.coerceAtLeast(0) / safePoints.size.toFloat()) * nextCount)
            .roundToInt()
            .coerceIn(0, nextPoints.lastIndex)
    }
    return nextPoints to nextSelection
}

private fun resampleShapePoints(points: List<Float>, targetCount: Int): List<Float> {
    if (points.isEmpty()) return List(targetCount) { 1f }
    if (points.size == targetCount) return points
    return List(targetCount) { index ->
        val oldPosition = index * points.size / targetCount.toFloat()
        val lowerIndex = floor(oldPosition).toInt().floorMod(points.size)
        val upperIndex = (lowerIndex + 1).floorMod(points.size)
        val fraction = oldPosition - floor(oldPosition)
        lerp(points[lowerIndex], points[upperIndex], fraction)
            .coerceIn(MIN_SHAPE_MULTIPLIER, MAX_SHAPE_MULTIPLIER)
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

private fun Int.floorMod(modulus: Int): Int {
    return ((this % modulus) + modulus) % modulus
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
private const val MIN_SHAPE_MULTIPLIER = 0.58f
private const val MAX_SHAPE_MULTIPLIER = 1.36f
private const val CURVE_TENSION = 0.24f

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1_000f).coerceAtLeast(0f)
    return "%.1fs".format(totalSeconds)
}
