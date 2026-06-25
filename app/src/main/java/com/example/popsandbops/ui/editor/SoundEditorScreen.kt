package com.example.popsandbops.ui.editor

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobDefaults
import com.example.popsandbops.data.BlobShapeNode
import com.example.popsandbops.data.BlobShapeTemplate
import com.example.popsandbops.data.DEFAULT_BLOB_CURVE_TENSION
import com.example.popsandbops.data.MAX_BLOB_NODE_COORDINATE
import com.example.popsandbops.data.MAX_BLOB_SHAPE_NODES
import com.example.popsandbops.data.MIN_BLOB_NODE_COORDINATE
import com.example.popsandbops.data.MIN_BLOB_SHAPE_NODES
import com.example.popsandbops.data.MapPoint
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.autoHandleNodes
import com.example.popsandbops.data.effectiveShapeNodes
import com.example.popsandbops.data.isValidBlobShapeNodes
import com.example.popsandbops.data.sanitizeTrimRange
import com.example.popsandbops.data.toBlobShapeNodes
import com.example.popsandbops.data.toShapePointMultipliers
import com.example.popsandbops.ui.components.BlobPreview
import com.example.popsandbops.ui.components.rememberPressFeedback
import com.example.popsandbops.ui.components.WaveformView
import kotlin.math.PI
import kotlin.math.abs
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
    val draftShapeNodes = draft.effectiveShapeNodes()

    BackHandler(enabled = isEditingShape) {
        isEditingShape = false
    }

    if (isEditingShape) {
        ShapeVectorEditorScreen(
            nodes = draftShapeNodes,
            curveTension = draft.curveTension,
            color = draft.color,
            selectedPoint = selectedShapePoint,
            modifier = modifier.fillMaxSize(),
            onSelectedPointChange = { selectedShapePoint = it },
            onNodesChange = { nodes ->
                draft = draft.copy(
                    shapeNodes = nodes,
                    shapePoints = nodes.toShapePointMultipliers(),
                )
            },
            onCurveTensionChange = { draft = draft.copy(curveTension = it) },
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
                    curveTension = draft.curveTension,
                    nodes = draftShapeNodes,
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
            ColorPaletteGrid(
                selectedColorArgb = draft.colorArgb,
                onColorChange = { draft = draft.copy(colorArgb = it) },
            )
            CustomColorMixer(
                colorArgb = draft.colorArgb,
                shapePoints = draft.shapePoints,
                shapeNodes = draftShapeNodes,
                curveTension = draft.curveTension,
                onColorChange = { draft = draft.copy(colorArgb = it) },
            )
        }

        EditorSection(title = "Shape") {
            ShapePresetGrid(
                selectedNodes = draftShapeNodes,
                color = draft.color,
                onPresetSelected = { shape ->
                    draft = draft.copy(
                        shapePreset = shape.preset,
                        shapePoints = shape.points,
                        curveTension = shape.curveTension,
                        shapeNodes = shape.nodes,
                    )
                    selectedShapePoint = -1
                },
            )

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
                        curveTension = draft.curveTension,
                        nodes = draftShapeNodes,
                    )
                    Text(
                        text = "${draftShapeNodes.size} points",
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
private fun ShapePresetGrid(
    selectedNodes: List<BlobShapeNode>,
    color: Color,
    onPresetSelected: (BlobShapeTemplate) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BlobDefaults.shapeLibrary.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { shape ->
                    val press = rememberPressFeedback(pressedScale = 0.92f)
                    val isSelected = shapeNodesMatch(selectedNodes, shape.nodes)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .scale(press.scale)
                            .clickable(
                                interactionSource = press.interactionSource,
                                indication = LocalIndication.current,
                            ) { onPresetSelected(shape) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (press.isPressed) 5.dp else 0.dp,
                        border = BorderStroke(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                    ) {
                        BlobPreview(
                            color = color,
                            points = shape.points,
                            modifier = Modifier.padding(7.dp),
                            curveTension = shape.curveTension,
                            nodes = shape.nodes,
                        )
                    }
                }
                repeat(5 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteGrid(
    selectedColorArgb: Long,
    onColorChange: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BlobDefaults.palette.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { colorArgb ->
                    ColorSwatch(
                        colorArgb = colorArgb,
                        isSelected = selectedColorArgb == colorArgb,
                        modifier = Modifier.weight(1f),
                        onClick = { onColorChange(colorArgb) },
                    )
                }
                repeat(7 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    colorArgb: Long,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val press = rememberPressFeedback(pressedScale = 0.88f)
    Surface(
        modifier = modifier
            .height(42.dp)
            .scale(press.scale)
            .clickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color(colorArgb),
        border = BorderStroke(
            width = if (isSelected) 4.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            },
        ),
    ) {}
}

@Composable
private fun CustomColorMixer(
    colorArgb: Long,
    shapePoints: List<Float>,
    shapeNodes: List<BlobShapeNode>,
    curveTension: Float,
    onColorChange: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BlobPreview(
                    color = Color(colorArgb),
                    points = shapePoints,
                    modifier = Modifier.size(74.dp),
                    curveTension = curveTension,
                    nodes = shapeNodes,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = colorHex(colorArgb),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                }
            }
            ColorWheelPicker(
                colorArgb = colorArgb,
                onColorChange = onColorChange,
            )
        }
    }
}

@Composable
private fun ColorWheelPicker(
    colorArgb: Long,
    onColorChange: (Long) -> Unit,
) {
    val hsv = hsvFromArgb(colorArgb)
    val latestColorArgb by rememberUpdatedState(colorArgb)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .pointerInput(Unit) {
                fun updateFromOffset(offset: Offset, size: Size) {
                    val currentHsv = hsvFromArgb(latestColorArgb)
                    val baseGeometry = colorPickerGeometry(size, currentHsv[0])
                    val center = baseGeometry.center
                    val distance = hypot(offset.x - center.x, offset.y - center.y)
                    val hue = if (distance in baseGeometry.hueInnerRadius..baseGeometry.hueOuterRadius) {
                        angleDegrees(offset - center)
                    } else {
                        currentHsv[0]
                    }
                    val geometry = colorPickerGeometry(size, hue)
                    val sv = if (distance < geometry.hueInnerRadius) {
                        saturationValueFromTriangle(offset, geometry, currentHsv)
                    } else {
                        currentHsv[1] to currentHsv[2]
                    }
                    onColorChange(argbFromHsv(hue, sv.first, sv.second))
                }

                detectDragGestures(
                    onDragStart = { updateFromOffset(it, Size(size.width.toFloat(), size.height.toFloat())) },
                ) { change, _ ->
                    change.consume()
                    updateFromOffset(change.position, Size(size.width.toFloat(), size.height.toFloat()))
                }
            },
    ) {
        val geometry = colorPickerGeometry(size, hsv[0])
        drawHueRing(geometry)
        drawSaturationValueTriangle(geometry, hsv[0])
        drawColorPickerMarkers(geometry, hsv)
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
    nodes: List<BlobShapeNode>,
    curveTension: Float,
    color: Color,
    selectedPoint: Int,
    modifier: Modifier = Modifier,
    onSelectedPointChange: (Int) -> Unit,
    onNodesChange: (List<BlobShapeNode>) -> Unit,
    onCurveTensionChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    val backPress = rememberPressFeedback(pressedScale = 0.90f)
    val donePress = rememberPressFeedback(pressedScale = 0.94f)
    val addPointPress = rememberPressFeedback(pressedScale = 0.94f)
    val removePointPress = rememberPressFeedback(pressedScale = 0.94f)
    var mirrorCurveHandles by remember { mutableStateOf(true) }

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
                    text = "${nodes.size} points",
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
                nodes = nodes,
                curveTension = curveTension,
                color = color,
                selectedPoint = selectedPoint,
                mirrorCurveHandles = mirrorCurveHandles,
                modifier = Modifier.fillMaxSize(),
                onSelectedPointChange = onSelectedPointChange,
                onNodesChange = onNodesChange,
            )
        }

        PointCurveControl(
            nodes = nodes,
            selectedPoint = selectedPoint,
            curveTension = curveTension,
            mirrorCurveHandles = mirrorCurveHandles,
            onNodesChange = onNodesChange,
            onCurveTensionChange = onCurveTensionChange,
            onMirrorCurveHandlesChange = { enabled ->
                mirrorCurveHandles = enabled
                if (enabled) {
                    onNodesChange(mirrorSelectedHandles(nodes, selectedPoint))
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    val (nextPoints, nextSelection) = addPointPreservingOutline(nodes, selectedPoint)
                    onSelectedPointChange(nextSelection)
                    onNodesChange(nextPoints)
                },
                enabled = nodes.size < MAX_BLOB_SHAPE_NODES,
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
                    val (nextPoints, nextSelection) = removePointPreservingOutline(nodes, selectedPoint)
                    onSelectedPointChange(nextSelection)
                    onNodesChange(nextPoints)
                },
                enabled = nodes.size > MIN_BLOB_SHAPE_NODES && selectedPoint in nodes.indices,
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
    nodes: List<BlobShapeNode>,
    curveTension: Float,
    color: Color,
    selectedPoint: Int,
    mirrorCurveHandles: Boolean,
    modifier: Modifier = Modifier,
    onSelectedPointChange: (Int) -> Unit,
    onNodesChange: (List<BlobShapeNode>) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activeTarget by remember { mutableStateOf<ShapeDragTarget?>(null) }
    val latestNodes by rememberUpdatedState(nodes)
    val latestSelectedPoint by rememberUpdatedState(selectedPoint)
    val latestMirrorCurveHandles by rememberUpdatedState(mirrorCurveHandles)
    val activeHandleColor = MaterialTheme.colorScheme.primary
    val handleBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val outlineColor = darker(color)

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(canvasSize) {
                detectTapGestures { offset ->
                    val target = nearestShapeTarget(
                        offset = offset,
                        nodes = latestNodes,
                        size = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                        selectedPoint = latestSelectedPoint,
                        radius = SELECT_HANDLE_RADIUS_PX,
                    )
                    if (target != null) {
                        onSelectedPointChange(target.index)
                    } else {
                        onSelectedPointChange(-1)
                    }
                }
            }
            .pointerInput(canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeTarget = nearestShapeTarget(
                            offset = offset,
                            nodes = latestNodes,
                            size = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                            selectedPoint = latestSelectedPoint,
                            radius = DRAG_HANDLE_RADIUS_PX,
                        )
                        activeTarget?.let {
                            onSelectedPointChange(it.index)
                        }
                    },
                    onDragEnd = { activeTarget = null },
                    onDragCancel = { activeTarget = null },
                ) { change, _ ->
                    val target = activeTarget
                    if (target != null) {
                        change.consume()
                        onNodesChange(
                            moveShapeTarget(
                                nodes = latestNodes,
                                target = target,
                                position = change.position,
                                size = canvasSize,
                                mirrorCurveHandles = latestMirrorCurveHandles,
                            ),
                        )
                    }
                }
            },
    ) {
        val safeNodes = nodes.takeIf { it.isValidBlobShapeNodes() }
            ?: List(8) { 1f }.toBlobShapeNodes(curveTension)
        val canvasNodes = safeNodes.toCanvasNodes(size)
        val path = pathFromCanvasNodes(canvasNodes)

        drawVectorGrid(color = guideColor)

        canvasNodes.forEachIndexed { index, node ->
            drawLine(
                color = outlineColor.copy(alpha = 0.32f),
                start = node.anchor,
                end = canvasNodes[(index + 1) % canvasNodes.size].anchor,
                strokeWidth = 1.5.dp.toPx(),
            )
        }

        drawPath(path = path, color = color.copy(alpha = 0.28f))
        if (selectedPoint in canvasNodes.indices) {
            val selected = canvasNodes[selectedPoint]
            drawLine(
                color = guideColor.copy(alpha = 0.42f),
                start = selected.anchor,
                end = selected.inHandle,
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = guideColor.copy(alpha = 0.42f),
                start = selected.anchor,
                end = selected.outHandle,
                strokeWidth = 1.dp.toPx(),
            )
            listOf(selected.inHandle, selected.outHandle).forEach { handle ->
                drawCircle(
                    color = activeHandleColor.copy(alpha = 0.88f),
                    radius = 10.dp.toPx(),
                    center = handle,
                )
                drawCircle(
                    color = handleBorderColor,
                    radius = 10.dp.toPx(),
                    center = handle,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
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

        canvasNodes.forEachIndexed { index, node ->
            drawCircle(
                color = if (activeTarget?.index == index || index == selectedPoint) activeHandleColor else Color.White,
                radius = if (index == selectedPoint) 18.dp.toPx() else 13.dp.toPx(),
                center = node.anchor,
            )
            drawCircle(
                color = handleBorderColor,
                radius = if (index == selectedPoint) 18.dp.toPx() else 13.dp.toPx(),
                center = node.anchor,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun PointCurveControl(
    nodes: List<BlobShapeNode>,
    selectedPoint: Int,
    curveTension: Float,
    mirrorCurveHandles: Boolean,
    onNodesChange: (List<BlobShapeNode>) -> Unit,
    onCurveTensionChange: (Float) -> Unit,
    onMirrorCurveHandlesChange: (Boolean) -> Unit,
) {
    val hasSelection = selectedPoint in nodes.indices
    val curveAmount = if (hasSelection) {
        selectedHandleDistance(nodes[selectedPoint])
    } else {
        curveTension
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Point curve",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (hasSelection) "Selected" else "None",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
            }
            Slider(
                value = curveAmount.coerceIn(0f, MAX_SELECTED_HANDLE_DISTANCE),
                onValueChange = { amount ->
                    if (hasSelection) {
                        onNodesChange(
                            scaleSelectedHandles(
                                nodes = nodes,
                                selectedPoint = selectedPoint,
                                distance = amount,
                                mirrorCurveHandles = mirrorCurveHandles,
                            ),
                        )
                    } else {
                        onCurveTensionChange(amount.coerceIn(MIN_CURVE_TENSION, MAX_CURVE_TENSION))
                    }
                },
                valueRange = 0f..MAX_SELECTED_HANDLE_DISTANCE,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mirror handles",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (mirrorCurveHandles) "Symmetric" else "Free",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                }
                Switch(
                    checked = mirrorCurveHandles,
                    onCheckedChange = onMirrorCurveHandlesChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onNodesChange(smoothSelectedNode(nodes, selectedPoint, curveTension)) },
                    enabled = hasSelection,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Smooth")
                }
                Button(
                    onClick = { onNodesChange(cornerSelectedNode(nodes, selectedPoint)) },
                    enabled = hasSelection,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Corner")
                }
            }
        }
    }
}

private data class CanvasShapeNode(
    val anchor: Offset,
    val inHandle: Offset,
    val outHandle: Offset,
)

private enum class ShapeHandle {
    Anchor,
    InHandle,
    OutHandle,
}

private data class ShapeDragTarget(
    val index: Int,
    val handle: ShapeHandle,
)

private fun List<BlobShapeNode>.toCanvasNodes(size: Size): List<CanvasShapeNode> {
    return map { node ->
        CanvasShapeNode(
            anchor = node.anchor.toCanvasOffset(size),
            inHandle = node.inHandle.toCanvasOffset(size),
            outHandle = node.outHandle.toCanvasOffset(size),
        )
    }
}

private fun MapPoint.toCanvasOffset(size: Size): Offset {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = shapeEditRadius(size)
    return Offset(
        x = center.x + x * radius,
        y = center.y + y * radius,
    )
}

private fun Offset.toShapePoint(size: IntSize): MapPoint {
    val safeSize = Size(size.width.toFloat(), size.height.toFloat())
    val center = Offset(safeSize.width / 2f, safeSize.height / 2f)
    val radius = shapeEditRadius(safeSize).coerceAtLeast(1f)
    return MapPoint(
        x = ((x - center.x) / radius).coerceIn(MIN_BLOB_NODE_COORDINATE, MAX_BLOB_NODE_COORDINATE),
        y = ((y - center.y) / radius).coerceIn(MIN_BLOB_NODE_COORDINATE, MAX_BLOB_NODE_COORDINATE),
    )
}

private fun shapeEditRadius(size: Size): Float {
    return size.minDimension * 0.38f
}

private fun nearestShapeTarget(
    offset: Offset,
    nodes: List<BlobShapeNode>,
    size: Size,
    selectedPoint: Int,
    radius: Float,
): ShapeDragTarget? {
    val canvasNodes = nodes.toCanvasNodes(size)
    val targets = mutableListOf<Pair<ShapeDragTarget, Offset>>()
    if (selectedPoint in canvasNodes.indices) {
        val selected = canvasNodes[selectedPoint]
        targets += ShapeDragTarget(selectedPoint, ShapeHandle.InHandle) to selected.inHandle
        targets += ShapeDragTarget(selectedPoint, ShapeHandle.OutHandle) to selected.outHandle
    }
    canvasNodes.forEachIndexed { index, node ->
        targets += ShapeDragTarget(index, ShapeHandle.Anchor) to node.anchor
    }
    return targets
        .map { (target, targetOffset) ->
            target to hypot(offset.x - targetOffset.x, offset.y - targetOffset.y)
        }
        .minByOrNull { it.second }
        ?.takeIf { it.second < radius }
        ?.first
}

private fun moveShapeTarget(
    nodes: List<BlobShapeNode>,
    target: ShapeDragTarget,
    position: Offset,
    size: IntSize,
    mirrorCurveHandles: Boolean,
): List<BlobShapeNode> {
    if (target.index !in nodes.indices) return nodes
    val next = nodes.toMutableList()
    val node = next[target.index]
    val point = position.toShapePoint(size)
    next[target.index] = when (target.handle) {
        ShapeHandle.Anchor -> {
            val dx = point.x - node.anchor.x
            val dy = point.y - node.anchor.y
            node.copy(
                anchor = point,
                inHandle = node.inHandle.translate(dx, dy),
                outHandle = node.outHandle.translate(dx, dy),
            )
        }

        ShapeHandle.InHandle -> if (mirrorCurveHandles) {
            node.copy(
                inHandle = point,
                outHandle = mirroredHandle(node.anchor, point),
            )
        } else {
            node.copy(inHandle = point)
        }

        ShapeHandle.OutHandle -> if (mirrorCurveHandles) {
            node.copy(
                inHandle = mirroredHandle(node.anchor, point),
                outHandle = point,
            )
        } else {
            node.copy(outHandle = point)
        }
    }
    return next
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

private fun pathFromCanvasNodes(nodes: List<CanvasShapeNode>): Path {
    val path = Path()
    if (nodes.isEmpty()) return path
    path.moveTo(nodes.first().anchor.x, nodes.first().anchor.y)
    nodes.forEachIndexed { index, node ->
        val next = nodes[(index + 1) % nodes.size]
        path.cubicTo(
            node.outHandle.x,
            node.outHandle.y,
            next.inHandle.x,
            next.inHandle.y,
            next.anchor.x,
            next.anchor.y,
        )
    }
    path.close()
    return path
}

private fun addPointPreservingOutline(
    nodes: List<BlobShapeNode>,
    selectedPoint: Int,
): Pair<List<BlobShapeNode>, Int> {
    val safeNodes = nodes.takeIf { it.isValidBlobShapeNodes() } ?: List(8) { 1f }.toBlobShapeNodes()
    val startIndex = selectedPoint.takeIf { it in safeNodes.indices } ?: safeNodes.lastIndex
    val endIndex = (startIndex + 1).floorMod(safeNodes.size)
    val start = safeNodes[startIndex]
    val end = safeNodes[endIndex]

    val p01 = start.anchor.lerp(start.outHandle, 0.5f)
    val p12 = start.outHandle.lerp(end.inHandle, 0.5f)
    val p23 = end.inHandle.lerp(end.anchor, 0.5f)
    val p012 = p01.lerp(p12, 0.5f)
    val p123 = p12.lerp(p23, 0.5f)
    val midpoint = p012.lerp(p123, 0.5f)

    val next = safeNodes.toMutableList()
    next[startIndex] = start.copy(outHandle = p01)
    val insertIndex = if (endIndex == 0) next.size else endIndex
    next.add(
        insertIndex,
        BlobShapeNode(
            anchor = midpoint,
            inHandle = p012,
            outHandle = p123,
        ),
    )
    val shiftedEndIndex = if (endIndex == 0) 0 else insertIndex + 1
    next[shiftedEndIndex] = next[shiftedEndIndex].copy(inHandle = p23)
    return next to insertIndex
}

private fun removePointPreservingOutline(
    nodes: List<BlobShapeNode>,
    selectedPoint: Int,
): Pair<List<BlobShapeNode>, Int> {
    if (nodes.size <= MIN_BLOB_SHAPE_NODES || selectedPoint !in nodes.indices) {
        return nodes to selectedPoint
    }
    val next = nodes.toMutableList()
    next.removeAt(selectedPoint)
    val auto = autoHandleNodes(next.map { it.anchor }, DEFAULT_BLOB_CURVE_TENSION)
    val previousIndex = (selectedPoint - 1).floorMod(next.size)
    val nextIndex = selectedPoint.floorMod(next.size)
    next[previousIndex] = next[previousIndex].copy(outHandle = auto[previousIndex].outHandle)
    next[nextIndex] = next[nextIndex].copy(inHandle = auto[nextIndex].inHandle)
    return next to nextIndex
}

private fun smoothSelectedNode(
    nodes: List<BlobShapeNode>,
    selectedPoint: Int,
    curveTension: Float,
): List<BlobShapeNode> {
    if (selectedPoint !in nodes.indices) return nodes
    val auto = autoHandleNodes(nodes.map { it.anchor }, curveTension)
    return nodes.toMutableList().also { it[selectedPoint] = auto[selectedPoint] }
}

private fun cornerSelectedNode(nodes: List<BlobShapeNode>, selectedPoint: Int): List<BlobShapeNode> {
    if (selectedPoint !in nodes.indices) return nodes
    val next = nodes.toMutableList()
    val node = next[selectedPoint]
    next[selectedPoint] = node.copy(inHandle = node.anchor, outHandle = node.anchor)
    return next
}

private fun scaleSelectedHandles(
    nodes: List<BlobShapeNode>,
    selectedPoint: Int,
    distance: Float,
    mirrorCurveHandles: Boolean,
): List<BlobShapeNode> {
    if (selectedPoint !in nodes.indices) return nodes
    val auto = autoHandleNodes(nodes.map { it.anchor }, DEFAULT_BLOB_CURVE_TENSION)
    val next = nodes.toMutableList()
    val node = next[selectedPoint]
    val fallback = auto[selectedPoint]
    if (mirrorCurveHandles) {
        val incoming = hypot(node.inHandle.x - node.anchor.x, node.inHandle.y - node.anchor.y)
        val outgoing = hypot(node.outHandle.x - node.anchor.x, node.outHandle.y - node.anchor.y)
        val useIncoming = incoming > outgoing
        val scaledSourceHandle = if (useIncoming) {
            scaledHandle(node.anchor, node.inHandle, fallback.inHandle, distance)
        } else {
            scaledHandle(node.anchor, node.outHandle, fallback.outHandle, distance)
        }
        next[selectedPoint] = if (useIncoming) {
            node.copy(
                inHandle = scaledSourceHandle,
                outHandle = mirroredHandle(node.anchor, scaledSourceHandle),
            )
        } else {
            node.copy(
                inHandle = mirroredHandle(node.anchor, scaledSourceHandle),
                outHandle = scaledSourceHandle,
            )
        }
        return next
    }
    next[selectedPoint] = node.copy(
        inHandle = scaledHandle(node.anchor, node.inHandle, fallback.inHandle, distance),
        outHandle = scaledHandle(node.anchor, node.outHandle, fallback.outHandle, distance),
    )
    return next
}

private fun mirrorSelectedHandles(
    nodes: List<BlobShapeNode>,
    selectedPoint: Int,
): List<BlobShapeNode> {
    if (selectedPoint !in nodes.indices) return nodes
    val next = nodes.toMutableList()
    val node = next[selectedPoint]
    val incoming = hypot(node.inHandle.x - node.anchor.x, node.inHandle.y - node.anchor.y)
    val outgoing = hypot(node.outHandle.x - node.anchor.x, node.outHandle.y - node.anchor.y)
    next[selectedPoint] = if (incoming > outgoing) {
        node.copy(outHandle = mirroredHandle(node.anchor, node.inHandle))
    } else {
        node.copy(inHandle = mirroredHandle(node.anchor, node.outHandle))
    }
    return next
}

private fun mirroredHandle(anchor: MapPoint, handle: MapPoint): MapPoint {
    return MapPoint(
        x = anchor.x * 2f - handle.x,
        y = anchor.y * 2f - handle.y,
    ).coerceShapePoint()
}

private fun selectedHandleDistance(node: BlobShapeNode): Float {
    val incoming = hypot(node.inHandle.x - node.anchor.x, node.inHandle.y - node.anchor.y)
    val outgoing = hypot(node.outHandle.x - node.anchor.x, node.outHandle.y - node.anchor.y)
    return ((incoming + outgoing) / 2f).coerceIn(0f, MAX_SELECTED_HANDLE_DISTANCE)
}

private fun scaledHandle(
    anchor: MapPoint,
    handle: MapPoint,
    fallback: MapPoint,
    distance: Float,
): MapPoint {
    var dx = handle.x - anchor.x
    var dy = handle.y - anchor.y
    var length = hypot(dx, dy)
    if (length < 0.001f) {
        dx = fallback.x - anchor.x
        dy = fallback.y - anchor.y
        length = hypot(dx, dy).coerceAtLeast(0.001f)
    }
    val scale = distance / length
    return MapPoint(
        x = anchor.x + dx * scale,
        y = anchor.y + dy * scale,
    ).coerceShapePoint()
}

private fun MapPoint.translate(dx: Float, dy: Float): MapPoint {
    return MapPoint(x + dx, y + dy).coerceShapePoint()
}

private fun MapPoint.coerceShapePoint(): MapPoint {
    return MapPoint(
        x = x.coerceIn(MIN_BLOB_NODE_COORDINATE, MAX_BLOB_NODE_COORDINATE),
        y = y.coerceIn(MIN_BLOB_NODE_COORDINATE, MAX_BLOB_NODE_COORDINATE),
    )
}

private fun MapPoint.lerp(end: MapPoint, fraction: Float): MapPoint {
    return MapPoint(
        x = lerp(x, end.x, fraction),
        y = lerp(y, end.y, fraction),
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

private fun Int.floorMod(modulus: Int): Int {
    return ((this % modulus) + modulus) % modulus
}

private data class ColorPickerGeometry(
    val center: Offset,
    val hueOuterRadius: Float,
    val hueInnerRadius: Float,
    val whiteVertex: Offset,
    val hueVertex: Offset,
    val blackVertex: Offset,
)

private data class BarycentricColor(
    val white: Float,
    val hue: Float,
    val black: Float,
)

private fun colorPickerGeometry(size: Size, hue: Float): ColorPickerGeometry {
    val center = Offset(size.width / 2f, size.height / 2f)
    val hueOuterRadius = size.minDimension * 0.46f
    val hueStrokeWidth = size.minDimension * 0.09f
    val hueInnerRadius = hueOuterRadius - hueStrokeWidth
    val triangleRadius = hueInnerRadius - size.minDimension * 0.016f
    val hueAngle = Math.toRadians(hue.toDouble()).toFloat()
    val thirdTurn = PI.toFloat() * 2f / 3f

    fun vertex(angle: Float): Offset {
        return Offset(
            x = center.x + cos(angle) * triangleRadius,
            y = center.y + sin(angle) * triangleRadius,
        )
    }

    return ColorPickerGeometry(
        center = center,
        hueOuterRadius = hueOuterRadius,
        hueInnerRadius = hueInnerRadius,
        whiteVertex = vertex(hueAngle - thirdTurn),
        hueVertex = vertex(hueAngle),
        blackVertex = vertex(hueAngle + thirdTurn),
    )
}

private fun DrawScope.drawHueRing(geometry: ColorPickerGeometry) {
    val strokeWidth = geometry.hueOuterRadius - geometry.hueInnerRadius
    val arcSize = Size(geometry.hueOuterRadius * 2f, geometry.hueOuterRadius * 2f)
    val topLeft = Offset(
        geometry.center.x - geometry.hueOuterRadius,
        geometry.center.y - geometry.hueOuterRadius,
    )
    for (degree in 0 until 360 step 3) {
        drawArc(
            color = Color.hsv(degree.toFloat(), 1f, 1f),
            startAngle = degree.toFloat(),
            sweepAngle = 4f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
    }
}

private fun DrawScope.drawSaturationValueTriangle(
    geometry: ColorPickerGeometry,
    hue: Float,
) {
    val path = trianglePath(geometry)
    val whiteFadeEnd = projectionOntoLine(
        point = geometry.whiteVertex,
        lineStart = geometry.hueVertex,
        lineEnd = geometry.blackVertex,
    )
    val blackFadeEnd = projectionOntoLine(
        point = geometry.blackVertex,
        lineStart = geometry.whiteVertex,
        lineEnd = geometry.hueVertex,
    )

    clipPath(path) {
        drawPath(
            path = path,
            color = Color.hsv(hue, 1f, 1f),
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                start = geometry.whiteVertex,
                end = whiteFadeEnd,
            ),
            topLeft = Offset.Zero,
            size = size,
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Black, Color.Black.copy(alpha = 0f)),
                start = geometry.blackVertex,
                end = blackFadeEnd,
            ),
            topLeft = Offset.Zero,
            size = size,
        )
    }

    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.34f),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private fun DrawScope.drawColorPickerMarkers(
    geometry: ColorPickerGeometry,
    hsv: FloatArray,
) {
    val hueAngle = Math.toRadians(hsv[0].toDouble()).toFloat()
    val hueMarkerRadius = (geometry.hueInnerRadius + geometry.hueOuterRadius) / 2f
    val hueMarker = Offset(
        x = geometry.center.x + cos(hueAngle) * hueMarkerRadius,
        y = geometry.center.y + sin(hueAngle) * hueMarkerRadius,
    )
    val svMarker = pointFromSaturationValue(hsv[1], hsv[2], geometry)

    listOf(hueMarker, svMarker).forEach { marker ->
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = marker,
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.58f),
            radius = 7.dp.toPx(),
            center = marker,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun trianglePath(geometry: ColorPickerGeometry): Path {
    return Path().apply {
        moveTo(geometry.whiteVertex.x, geometry.whiteVertex.y)
        lineTo(geometry.hueVertex.x, geometry.hueVertex.y)
        lineTo(geometry.blackVertex.x, geometry.blackVertex.y)
        close()
    }
}

private fun projectionOntoLine(
    point: Offset,
    lineStart: Offset,
    lineEnd: Offset,
): Offset {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 0.001f) return lineStart
    val fraction = (((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared)
        .coerceIn(0f, 1f)
    return Offset(
        x = lineStart.x + dx * fraction,
        y = lineStart.y + dy * fraction,
    )
}

private fun saturationValueFromTriangle(
    offset: Offset,
    geometry: ColorPickerGeometry,
    hsv: FloatArray,
): Pair<Float, Float> {
    val distance = hypot(offset.x - geometry.center.x, offset.y - geometry.center.y)
    if (distance > geometry.hueInnerRadius) return hsv[1] to hsv[2]
    return saturationValueFromBarycentric(
        barycentricFor(offset, geometry).clamped(),
    )
}

private fun saturationValueFromBarycentric(barycentric: BarycentricColor): Pair<Float, Float> {
    val value = (barycentric.white + barycentric.hue).coerceIn(0f, 1f)
    val saturation = if (value <= 0.001f) {
        0f
    } else {
        (barycentric.hue / value).coerceIn(0f, 1f)
    }
    return saturation to value
}

private fun pointFromSaturationValue(
    saturation: Float,
    value: Float,
    geometry: ColorPickerGeometry,
): Offset {
    val hueWeight = saturation.coerceIn(0f, 1f) * value.coerceIn(0f, 1f)
    val whiteWeight = (1f - saturation.coerceIn(0f, 1f)) * value.coerceIn(0f, 1f)
    val blackWeight = 1f - value.coerceIn(0f, 1f)
    return Offset(
        x = geometry.whiteVertex.x * whiteWeight +
            geometry.hueVertex.x * hueWeight +
            geometry.blackVertex.x * blackWeight,
        y = geometry.whiteVertex.y * whiteWeight +
            geometry.hueVertex.y * hueWeight +
            geometry.blackVertex.y * blackWeight,
    )
}

private fun barycentricFor(point: Offset, geometry: ColorPickerGeometry): BarycentricColor {
    val white = geometry.whiteVertex
    val hue = geometry.hueVertex
    val black = geometry.blackVertex
    val denominator = (hue.y - black.y) * (white.x - black.x) +
        (black.x - hue.x) * (white.y - black.y)
    if (abs(denominator) < 0.001f) {
        return BarycentricColor(white = 0f, hue = 1f, black = 0f)
    }
    val whiteWeight = ((hue.y - black.y) * (point.x - black.x) +
        (black.x - hue.x) * (point.y - black.y)) / denominator
    val hueWeight = ((black.y - white.y) * (point.x - black.x) +
        (white.x - black.x) * (point.y - black.y)) / denominator
    val blackWeight = 1f - whiteWeight - hueWeight
    return BarycentricColor(
        white = whiteWeight,
        hue = hueWeight,
        black = blackWeight,
    )
}

private fun BarycentricColor.clamped(): BarycentricColor {
    val whiteWeight = white.coerceAtLeast(0f)
    val hueWeight = hue.coerceAtLeast(0f)
    val blackWeight = black.coerceAtLeast(0f)
    val total = (whiteWeight + hueWeight + blackWeight).coerceAtLeast(0.001f)
    return BarycentricColor(
        white = whiteWeight / total,
        hue = hueWeight / total,
        black = blackWeight / total,
    )
}

private fun angleDegrees(offset: Offset): Float {
    return ((atan2(offset.y, offset.x) * 180f / PI.toFloat()) + 360f) % 360f
}

private fun hsvFromArgb(colorArgb: Long): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(colorArgb.toInt(), hsv)
    return hsv
}

private fun argbFromHsv(hue: Float, saturation: Float, value: Float): Long {
    return AndroidColor.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f),
        ),
    ).toLong() and 0xFFFFFFFFL
}

private fun darker(color: Color): Color {
    return Color(
        red = color.red * 0.68f,
        green = color.green * 0.68f,
        blue = color.blue * 0.68f,
        alpha = color.alpha,
    )
}

private fun colorComponent(colorArgb: Long, shift: Int): Float {
    return ((colorArgb shr shift) and 0xFFL).toFloat()
}

private fun colorHex(colorArgb: Long): String {
    val red = colorComponent(colorArgb, 16).roundToInt()
    val green = colorComponent(colorArgb, 8).roundToInt()
    val blue = colorComponent(colorArgb, 0).roundToInt()
    return "#%02X%02X%02X".format(red, green, blue)
}

private fun shapeNodesMatch(left: List<BlobShapeNode>, right: List<BlobShapeNode>): Boolean {
    return left.size == right.size && left.zip(right).all { (leftNode, rightNode) ->
        leftNode.anchor.nearlyEquals(rightNode.anchor) &&
            leftNode.inHandle.nearlyEquals(rightNode.inHandle) &&
            leftNode.outHandle.nearlyEquals(rightNode.outHandle)
    }
}

private fun MapPoint.nearlyEquals(other: MapPoint): Boolean {
    return abs(x - other.x) < 0.001f && abs(y - other.y) < 0.001f
}

private const val MIN_CURVE_TENSION = 0.04f
private const val MAX_CURVE_TENSION = 0.38f
private const val MAX_SELECTED_HANDLE_DISTANCE = 0.80f
private const val SELECT_HANDLE_RADIUS_PX = 86f
private const val DRAG_HANDLE_RADIUS_PX = 104f

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1_000f).coerceAtLeast(0f)
    return "%.1fs".format(totalSeconds)
}
