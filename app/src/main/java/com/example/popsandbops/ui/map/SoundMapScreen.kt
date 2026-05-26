package com.example.popsandbops.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.ui.components.BlobButton
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun SoundMapScreen(
    blobs: List<SoundBlob>,
    playingBlobId: String?,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onBlobClick: (SoundBlob) -> Unit,
    onLibraryClick: () -> Unit,
    onRecordClick: () -> Unit,
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val background = MaterialTheme.colorScheme.background
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.76f)
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(Unit) {
                detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                    pan += gesturePan
                    zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                }
            },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx / 2f)
        val maxDistance = blobs.maxOfOrNull { hypot(it.position.x, it.position.y) } ?: 220f

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDottedMap(
                pan = pan,
                zoom = zoom,
                dotColor = gridColor,
                ringColor = ringColor,
                worldRadius = maxDistance + 150f,
            )
        }

        blobs.forEach { blob ->
            val scaledSize = with(density) { (84.dp.toPx() * zoom.coerceIn(0.82f, 1.22f)).toDp() }
            val screen = center + pan + Offset(blob.position.x * zoom, blob.position.y * zoom)
            BlobButton(
                name = blob.name,
                color = blob.color,
                points = blob.shapePoints,
                isPlaying = playingBlobId == blob.id,
                size = scaledSize,
                modifier = Modifier.offset {
                    val sizePx = with(density) { scaledSize.toPx() }
                    IntOffset(
                        x = (screen.x - sizePx / 2f).roundToInt(),
                        y = (screen.y - sizePx / 2f).roundToInt(),
                    )
                },
                onClick = { onBlobClick(blob) },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            LibraryButton(onClick = onLibraryClick)
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
                    text = "Pops & Bops",
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

        FloatingActionButton(
            onClick = onRecordClick,
            modifier = Modifier
                .align(Alignment.Center)
                .size(86.dp),
            shape = CircleShape,
            containerColor = if (isRecording) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                contentDescription = if (isRecording) "Stop recording" else "Record sound",
                modifier = Modifier.size(40.dp),
            )
        }

    }
}

@Composable
private fun LibraryButton(
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = "Library",
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
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(46.dp)) {
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

    repeat(3) { index ->
        drawCircle(
            color = ringColor,
            radius = (worldRadius + index * 120f) * zoom,
            center = origin,
            style = Stroke(width = 2f),
        )
    }
}

private const val MIN_ZOOM = 0.64f
private const val MAX_ZOOM = 1.56f
