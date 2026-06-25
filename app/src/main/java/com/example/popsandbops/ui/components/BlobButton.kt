package com.example.popsandbops.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.BlobShapeNode
import com.example.popsandbops.data.DEFAULT_BLOB_CURVE_TENSION
import com.example.popsandbops.data.isValidBlobShapeNodes
import com.example.popsandbops.data.toBlobShapeNodes

@Composable
fun BlobButton(
    name: String,
    color: Color,
    points: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    showName: Boolean = true,
    curveTension: Float = DEFAULT_BLOB_CURVE_TENSION,
    nodes: List<BlobShapeNode> = emptyList(),
    onClick: () -> Unit,
) {
    val press = rememberPressFeedback(pressedScale = 0.88f)
    val outlineColor = blobOutlineColor(color)
    val playingTransition = rememberInfiniteTransition(label = "blob playing")
    val pulse by playingTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(420),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob scale",
    )
    val ringProgress by playingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blob play ring",
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isPlaying) pulse else 1f)
            .scale(press.scale)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val blobPath = smoothBlobPath(
                size = this.size,
                points = points,
                curveTension = curveTension,
                nodes = nodes,
            )
            if (isPlaying) {
                withTransform({
                    val ringScale = 1f + ringProgress * 0.18f
                    scale(scaleX = ringScale, scaleY = ringScale, pivot = center)
                }) {
                    drawPath(
                        path = blobPath,
                        color = outlineColor.copy(alpha = 0.22f * (1f - ringProgress)),
                        style = Stroke(width = 12.dp.toPx()),
                    )
                }
            }
            drawPath(path = blobPath, color = color)
            if (press.isPressed) {
                drawPath(
                    path = blobPath,
                    color = Color.White.copy(alpha = 0.20f),
                )
            }
            drawPath(
                path = blobPath,
                color = outlineColor.copy(alpha = if (isPlaying) 0.98f else 0.74f),
                style = Stroke(width = if (isPlaying) 4.dp.toPx() else 3.dp.toPx()),
            )
            if (isPlaying) {
                drawPath(
                    path = blobPath,
                    color = Color.White.copy(alpha = 0.22f),
                    style = Stroke(width = 7.dp.toPx()),
                )
            }
        }
        if (showName) {
            Text(
                text = name,
                modifier = Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BlobPreview(
    color: Color,
    points: List<Float>,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    curveTension: Float = DEFAULT_BLOB_CURVE_TENSION,
    nodes: List<BlobShapeNode> = emptyList(),
) {
    val outlineColor = blobOutlineColor(color)
    var pulse = 1f
    var ringProgress = 0f
    if (isSelected) {
        val selectedTransition = rememberInfiniteTransition(label = "selected blob preview")
        val animatedPulse by selectedTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(520),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "selected preview pulse",
        )
        val animatedRing by selectedTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 840, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "selected preview ring",
        )
        pulse = animatedPulse
        ringProgress = animatedRing
    }

    Canvas(modifier = modifier.scale(pulse)) {
        val blobPath = smoothBlobPath(
            size = size,
            points = points,
            curveTension = curveTension,
            nodes = nodes,
        )
        if (isSelected) {
            withTransform({
                val ringScale = 1f + ringProgress * 0.16f
                scale(scaleX = ringScale, scaleY = ringScale, pivot = center)
            }) {
                drawPath(
                    path = blobPath,
                    color = outlineColor.copy(alpha = 0.22f * (1f - ringProgress)),
                    style = Stroke(width = 8.dp.toPx()),
                )
            }
        }
        drawPath(path = blobPath, color = color)
        drawPath(
            path = blobPath,
            color = outlineColor.copy(alpha = if (isSelected) 0.98f else 0.68f),
            style = Stroke(width = if (isSelected) 4.dp.toPx() else 2.dp.toPx()),
        )
    }
}

fun smoothBlobPath(
    size: Size,
    points: List<Float>,
    curveTension: Float = DEFAULT_BLOB_CURVE_TENSION,
    nodes: List<BlobShapeNode> = emptyList(),
): Path {
    val safeNodes = nodes.takeIf { it.isValidBlobShapeNodes() }
        ?: points.toBlobShapeNodes(curveTension)
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.50f

    val path = Path()
    if (safeNodes.isEmpty()) return path
    val first = safeNodes.first().anchor.toCanvasOffset(center, radius)
    path.moveTo(first.x, first.y)
    safeNodes.forEachIndexed { index, node ->
        val next = safeNodes[(index + 1) % safeNodes.size]
        val controlOne = node.outHandle.toCanvasOffset(center, radius)
        val controlTwo = next.inHandle.toCanvasOffset(center, radius)
        val end = next.anchor.toCanvasOffset(center, radius)
        path.cubicTo(
            controlOne.x,
            controlOne.y,
            controlTwo.x,
            controlTwo.y,
            end.x,
            end.y,
        )
    }
    path.close()
    return path
}

private fun com.example.popsandbops.data.MapPoint.toCanvasOffset(center: Offset, radius: Float): Offset {
    return Offset(
        x = center.x + x * radius,
        y = center.y + y * radius,
    )
}

private fun blobOutlineColor(color: Color): Color {
    return Color(
        red = (color.red * 0.68f).coerceIn(0f, 1f),
        green = (color.green * 0.68f).coerceIn(0f, 1f),
        blue = (color.blue * 0.68f).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}
