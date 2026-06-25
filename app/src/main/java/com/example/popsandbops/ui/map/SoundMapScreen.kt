package com.example.popsandbops.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobDefaults
import com.example.popsandbops.data.BlobMapLayout
import com.example.popsandbops.data.MapPoint
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.effectiveShapeNodes
import com.example.popsandbops.ui.components.BlobButton
import com.example.popsandbops.ui.components.rememberPressFeedback
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SoundMapScreen(
    blobs: List<SoundBlob>,
    playingBlobId: String?,
    showBlobNames: Boolean,
    modifier: Modifier = Modifier,
    onBlobClick: (SoundBlob) -> Unit,
    onLibraryClick: () -> Unit,
    onBlobMoved: (SoundBlob, MapPoint) -> Unit,
    onAutoArrange: () -> Unit,
    onRecordClick: () -> Unit,
    onBlobNamesVisibleChange: (Boolean) -> Unit,
) {
    val panState = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomState = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var isArranging by remember { mutableStateOf(false) }
    var draggingBlobId by remember { mutableStateOf<String?>(null) }
    val dragOffsets = remember { mutableStateMapOf<String, Offset>() }
    val density = LocalDensity.current
    val background = MaterialTheme.colorScheme.background
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.76f)
    val fieldColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val recordColor = MaterialTheme.colorScheme.primary
    val pan = panState.value
    val zoom = zoomState.value

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(Unit) {
                detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                    scope.launch {
                        panState.snapTo(panState.value + gesturePan)
                        zoomState.snapTo((zoomState.value * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM))
                    }
                }
            },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx / 2f)
        val worldToPx = with(density) { 1.dp.toPx() } * zoom

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDottedMap(
                pan = pan,
                zoom = zoom,
                dotColor = gridColor,
                fieldColor = fieldColor,
                isArranging = isArranging,
            )
        }

        blobs.forEach { blob ->
            key(blob.id) {
                val scaledSize = with(density) { (BlobMapLayout.BlobButtonDiameter.dp.toPx() * zoom).toDp() }
                val dragOffset = dragOffsets[blob.id] ?: Offset.Zero
                val candidatePosition = MapPoint(
                    x = blob.position.x + dragOffset.x,
                    y = blob.position.y + dragOffset.y,
                )
                val occupiedPositions = blobs
                    .filter { it.id != blob.id }
                    .map { it.position }
                val isDragging = draggingBlobId == blob.id
                val resolvedPosition = if (isDragging) {
                    BlobMapLayout.resolveOverlaps(candidatePosition, occupiedPositions)
                } else {
                    candidatePosition
                }
                val worldPosition = Offset(resolvedPosition.x, resolvedPosition.y)
                val targetScreen = center + pan + Offset(worldPosition.x * worldToPx, worldPosition.y * worldToPx)
                val animatedScreen by animateOffsetAsState(
                    targetValue = targetScreen,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "blob map position",
                )
                BlobButton(
                    name = blob.name,
                    color = blob.color,
                    points = blob.shapePoints,
                    isPlaying = playingBlobId == blob.id || isDragging,
                    size = scaledSize,
                    showName = showBlobNames,
                    curveTension = blob.curveTension,
                    nodes = blob.effectiveShapeNodes(),
                    modifier = Modifier
                        .offset {
                            val sizePx = with(density) { scaledSize.toPx() }
                            IntOffset(
                                x = (animatedScreen.x - sizePx / 2f).roundToInt(),
                                y = (animatedScreen.y - sizePx / 2f).roundToInt(),
                            )
                        }
                        .then(
                            if (isArranging) {
                                Modifier.pointerInput(blob.id, zoom) {
                                    detectDragGestures(
                                        onDragStart = { draggingBlobId = blob.id },
                                        onDragEnd = {
                                            val finalOffset = dragOffsets[blob.id] ?: Offset.Zero
                                            val finalPosition = BlobMapLayout.resolveOverlaps(
                                                position = MapPoint(
                                                    x = blob.position.x + finalOffset.x,
                                                    y = blob.position.y + finalOffset.y,
                                                ),
                                                occupiedPositions = occupiedPositions,
                                            )
                                            onBlobMoved(
                                                blob,
                                                finalPosition,
                                            )
                                            dragOffsets.remove(blob.id)
                                            draggingBlobId = null
                                        },
                                        onDragCancel = {
                                            dragOffsets.remove(blob.id)
                                            draggingBlobId = null
                                        },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dragOffsets[blob.id] = (dragOffsets[blob.id] ?: Offset.Zero) +
                                            (dragAmount / worldToPx)
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                    onClick = {
                        if (!isArranging) {
                            onBlobClick(blob)
                        }
                    },
                )
            }
        }

        if (!isArranging) {
            val recordSize = 96.dp
            val recordScreen = center + pan
            MapRecordBlob(
                color = recordColor,
                modifier = Modifier.offset {
                    val sizePx = with(density) { recordSize.toPx() }
                    IntOffset(
                        x = (recordScreen.x - sizePx / 2f).roundToInt(),
                        y = (recordScreen.y - sizePx / 2f).roundToInt(),
                    )
                },
                size = recordSize,
                onClick = onRecordClick,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (isArranging) {
                MapActionButton(
                    label = "Done",
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Finish arranging",
                    emphasized = true,
                    onClick = {
                        dragOffsets.clear()
                        draggingBlobId = null
                        isArranging = false
                    },
                )
                MapActionButton(
                    label = "Auto",
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Auto arrange sounds",
                    onClick = {
                        dragOffsets.clear()
                        draggingBlobId = null
                        onAutoArrange()
                    },
                )
            } else {
                MapActionButton(
                    label = "Library",
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Open library",
                    onClick = onLibraryClick,
                )
                MapActionButton(
                    label = "Arrange",
                    imageVector = Icons.Filled.OpenWith,
                    contentDescription = "Arrange sounds",
                    onClick = { isArranging = true },
                )
                MapActionButton(
                    label = if (showBlobNames) "Hide names" else "Names",
                    imageVector = if (showBlobNames) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showBlobNames) "Hide sound names" else "Show sound names",
                    onClick = { onBlobNamesVisibleChange(!showBlobNames) },
                )
            }
            MapToolButton(icon = { Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in") }) {
                scope.launch {
                    zoomState.animateTo(
                        targetValue = (zoomState.value + 0.14f).coerceAtMost(MAX_ZOOM),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                }
            }
            MapToolButton(icon = { Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out") }) {
                scope.launch {
                    zoomState.animateTo(
                        targetValue = (zoomState.value - 0.14f).coerceAtLeast(MIN_ZOOM),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    )
                }
            }
            MapToolButton(icon = { Icon(Icons.Filled.MyLocation, contentDescription = "Center map") }) {
                scope.launch {
                    launch {
                        panState.animateTo(
                            targetValue = Offset.Zero,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                    launch {
                        zoomState.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 54.dp, start = 18.dp),
            shape = RoundedCornerShape(18.dp),
            color = glassContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, glassBorderColor()),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isArranging) "Arrange map" else "Pops & Bops",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${blobs.size} sounds",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
        }
    }
}

@Composable
private fun MapRecordBlob(
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = 0.10f),
                radius = this.size.minDimension * 0.52f,
                center = center,
            )
            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = this.size.minDimension * 0.44f,
                center = center,
            )
        }
        BlobButton(
            name = "",
            color = color,
            points = BlobDefaults.shapeLibrary.first().second,
            isPlaying = false,
            size = size,
            showName = false,
            curveTension = BlobDefaults.shapeLibrary.first().curveTension,
            nodes = BlobDefaults.shapeLibrary.first().nodes,
            modifier = Modifier.fillMaxSize(),
            onClick = onClick,
        )
        Surface(
            modifier = Modifier.size(size * 0.56f),
            shape = CircleShape,
            color = contentColor.copy(alpha = 0.14f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.FiberManualRecord,
                    contentDescription = "Open recorder",
                    tint = contentColor,
                    modifier = Modifier.size(size * 0.32f),
                )
            }
        }
    }
}

@Composable
private fun MapActionButton(
    label: String,
    imageVector: ImageVector,
    contentDescription: String,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    val press = rememberPressFeedback(pressedScale = 0.94f)
    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    }
    Surface(
        modifier = Modifier
            .scale(press.scale)
            .clickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(24.dp),
        color = glassContainerColor(emphasized = emphasized),
        contentColor = contentColor,
        tonalElevation = if (press.isPressed) 7.dp else 4.dp,
        border = BorderStroke(1.dp, glassBorderColor(emphasized = emphasized)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MapToolButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val press = rememberPressFeedback(pressedScale = 0.90f)
    Surface(
        modifier = Modifier
            .size(46.dp)
            .scale(press.scale)
            .clickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = glassContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        tonalElevation = if (press.isPressed) 7.dp else 4.dp,
        border = BorderStroke(1.dp, glassBorderColor()),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
private fun glassContainerColor(emphasized: Boolean = false): Color {
    val isDark = isSystemInDarkTheme()
    return if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.92f else 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.78f else 0.72f)
    }
}

@Composable
private fun glassBorderColor(emphasized: Boolean = false): Color {
    val isDark = isSystemInDarkTheme()
    return if (emphasized) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = if (isDark) 0.20f else 0.26f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.22f else 0.15f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDottedMap(
    pan: Offset,
    zoom: Float,
    dotColor: Color,
    fieldColor: Color,
    isArranging: Boolean,
) {
    val spacing = 34.dp.toPx() * zoom
    val origin = center + pan
    val startX = ((origin.x % spacing) + spacing) % spacing
    val startY = ((origin.y % spacing) + spacing) % spacing
    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(
                color = dotColor,
                radius = 1.45f * zoom.coerceIn(0.82f, 1.18f),
                center = Offset(x, y),
            )
            y += spacing
        }
        x += spacing
    }

    if (isArranging) {
        drawCircle(
            color = fieldColor.copy(alpha = 0.40f),
            radius = 6.5f,
            center = origin,
        )
    }
}

private const val MIN_ZOOM = 0.64f
private const val MAX_ZOOM = 1.56f
