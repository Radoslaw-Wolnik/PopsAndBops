package com.example.popsandbops.ui.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobDefaults
import com.example.popsandbops.data.BlobMapLayout
import com.example.popsandbops.data.MapPoint
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.ui.components.BlobButton
import com.example.popsandbops.ui.components.rememberPressFeedback
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SoundMapScreen(
    blobs: List<SoundBlob>,
    playingBlobId: String?,
    isRecording: Boolean,
    showBlobNames: Boolean,
    modifier: Modifier = Modifier,
    onBlobClick: (SoundBlob) -> Unit,
    onLibraryClick: () -> Unit,
    onBlobMoved: (SoundBlob, MapPoint) -> Unit,
    onAutoArrange: () -> Unit,
    onRecordClick: () -> Unit,
    onBlobNamesVisibleChange: (Boolean) -> Unit,
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var isArranging by remember { mutableStateOf(false) }
    var draggingBlobId by remember { mutableStateOf<String?>(null) }
    val dragOffsets = remember { mutableStateMapOf<String, Offset>() }
    val density = LocalDensity.current
    val background = MaterialTheme.colorScheme.background
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.76f)
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val recordColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .then(
                if (isArranging) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                            pan += gesturePan
                            zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        }
                    }
                }
            ),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx / 2f)
        val maxDistance = blobs.maxOfOrNull { hypot(it.position.x, it.position.y) } ?: 220f
        val guideRadii = BlobMapLayout.guideRadii(maxDistance)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDottedMap(
                pan = pan,
                zoom = zoom,
                dotColor = gridColor,
                ringColor = ringColor,
                worldRadius = maxDistance + 150f,
                isArranging = isArranging,
                guideRadii = guideRadii,
            )
        }

        blobs.forEach { blob ->
            val scaledSize = with(density) { (84.dp.toPx() * zoom.coerceIn(0.82f, 1.22f)).toDp() }
            val dragOffset = dragOffsets[blob.id] ?: Offset.Zero
            val worldPosition = Offset(blob.position.x, blob.position.y) + dragOffset
            val screen = center + pan + Offset(worldPosition.x * zoom, worldPosition.y * zoom)
            val isDragging = draggingBlobId == blob.id
            BlobButton(
                name = blob.name,
                color = blob.color,
                points = blob.shapePoints,
                isPlaying = playingBlobId == blob.id || isDragging,
                size = scaledSize,
                showName = showBlobNames,
                modifier = Modifier
                    .offset {
                        val sizePx = with(density) { scaledSize.toPx() }
                        IntOffset(
                            x = (screen.x - sizePx / 2f).roundToInt(),
                            y = (screen.y - sizePx / 2f).roundToInt(),
                        )
                    }
                    .then(
                        if (isArranging) {
                            Modifier.pointerInput(blob.id, zoom) {
                                detectDragGestures(
                                    onDragStart = { draggingBlobId = blob.id },
                                    onDragEnd = {
                                        val finalOffset = dragOffsets[blob.id] ?: Offset.Zero
                                        onBlobMoved(
                                            blob,
                                            MapPoint(
                                                x = blob.position.x + finalOffset.x,
                                                y = blob.position.y + finalOffset.y,
                                            ),
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
                                    dragOffsets[blob.id] = (dragOffsets[blob.id] ?: Offset.Zero) + (dragAmount / zoom)
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

        if (!isArranging) {
            val recordSize = with(density) { (72.dp.toPx() * zoom.coerceIn(0.86f, 1.18f)).toDp() }
            val recordScreen = center + pan
            MapRecordBlob(
                color = recordColor,
                isRecording = isRecording,
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
                zoom = (zoom + 0.14f).coerceAtMost(MAX_ZOOM)
            }
            MapToolButton(icon = { Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out") }) {
                zoom = (zoom - 0.14f).coerceAtLeast(MIN_ZOOM)
            }
            MapToolButton(icon = { Icon(Icons.Filled.MyLocation, contentDescription = "Center map") }) {
                pan = Offset.Zero
                zoom = 1f
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 54.dp, start = 18.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 4.dp,
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
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "map record blob")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "map record pulse",
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isRecording) pulse else 1f),
        contentAlignment = Alignment.Center,
    ) {
        BlobButton(
            name = "",
            color = color,
            points = BlobDefaults.shapeLibrary.first().second,
            isPlaying = isRecording,
            size = size,
            showName = false,
            modifier = Modifier.fillMaxSize(),
            onClick = onClick,
        )
        Icon(
            imageVector = Icons.Filled.FiberManualRecord,
            contentDescription = "Open recorder",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.38f),
        )
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
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    }
    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
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
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (press.isPressed) 7.dp else 4.dp,
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = if (press.isPressed) 7.dp else 4.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDottedMap(
    pan: Offset,
    zoom: Float,
    dotColor: Color,
    ringColor: Color,
    worldRadius: Float,
    isArranging: Boolean,
    guideRadii: List<Float>,
) {
    val spacing = 34f * zoom
    val origin = center + pan
    val startX = ((-origin.x % spacing) + spacing) % spacing
    val startY = ((-origin.y % spacing) + spacing) % spacing
    var x = startX
    while (x < size.width) {
        var y = startY
        while (y < size.height) {
            drawCircle(
                color = dotColor,
                radius = 1.7f,
                center = Offset(x, y),
            )
            y += spacing
        }
        x += spacing
    }

    if (isArranging) {
        guideRadii.forEachIndexed { ring, radius ->
            drawCircle(
                color = ringColor.copy(alpha = 0.58f),
                radius = radius * zoom,
                center = origin,
                style = Stroke(width = 2.8f),
            )
            repeat(BlobMapLayout.slotsForRing(ring)) { slot ->
                val slots = BlobMapLayout.slotsForRing(ring)
                val angle = (slot / slots.toFloat()) * PI.toFloat() * 2f - PI.toFloat() / 2f
                drawCircle(
                    color = ringColor.copy(alpha = 0.76f),
                    radius = 3.4f,
                    center = origin + Offset(
                        x = cos(angle) * radius * zoom,
                        y = sin(angle) * radius * zoom,
                    ),
                )
            }
        }
    } else {
        repeat(3) { index ->
            drawCircle(
                color = ringColor,
                radius = (worldRadius + index * 120f) * zoom,
                center = origin,
                style = Stroke(width = 2f),
            )
        }
    }
}

private const val MIN_ZOOM = 0.64f
private const val MAX_ZOOM = 1.56f
