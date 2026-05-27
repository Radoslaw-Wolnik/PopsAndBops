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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BlobButton(
    name: String,
    color: Color,
    points: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    showName: Boolean = true,
    onClick: () -> Unit,
) {
    val press = rememberPressFeedback(pressedScale = 0.88f)
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
            val blobPath = smoothBlobPath(size = this.size, points = points)
            if (isPlaying) {
                withTransform({
                    val ringScale = 1f + ringProgress * 0.18f
                    scale(scaleX = ringScale, scaleY = ringScale, pivot = center)
                }) {
                    drawPath(
                        path = blobPath,
                        color = color.copy(alpha = 0.28f * (1f - ringProgress)),
                        style = Stroke(width = 10.dp.toPx()),
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
                color = Color.White.copy(alpha = if (isPlaying) 0.92f else 0.68f),
                style = Stroke(width = if (isPlaying) 4.dp.toPx() else 3.dp.toPx()),
            )
            if (isPlaying) {
                drawPath(
                    path = blobPath,
                    color = Color.Black.copy(alpha = 0.13f),
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
) {
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
        val blobPath = smoothBlobPath(size = size, points = points)
        if (isSelected) {
            withTransform({
                val ringScale = 1f + ringProgress * 0.16f
                scale(scaleX = ringScale, scaleY = ringScale, pivot = center)
            }) {
                drawPath(
                    path = blobPath,
                    color = color.copy(alpha = 0.24f * (1f - ringProgress)),
                    style = Stroke(width = 8.dp.toPx()),
                )
            }
        }
        drawPath(path = blobPath, color = color)
        drawPath(
            path = blobPath,
            color = Color.White.copy(alpha = if (isSelected) 0.95f else 0.58f),
            style = Stroke(width = if (isSelected) 4.dp.toPx() else 2.dp.toPx()),
        )
    }
}

fun smoothBlobPath(size: Size, points: List<Float>): Path {
    val safePoints = points.ifEmpty { List(8) { 1f } }
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.42f
    val anchors = safePoints.mapIndexed { index, multiplier ->
        val angle = (index.toFloat() / safePoints.size) * PI.toFloat() * 2f - PI.toFloat() / 2f
        Offset(
            x = center.x + cos(angle) * radius * multiplier,
            y = center.y + sin(angle) * radius * multiplier,
        )
    }

    val path = Path()
    val first = anchors.first()
    val second = anchors[1 % anchors.size]
    path.moveTo((first.x + second.x) / 2f, (first.y + second.y) / 2f)
    anchors.forEachIndexed { index, anchor ->
        val next = anchors[(index + 1) % anchors.size]
        path.quadraticTo(
            anchor.x,
            anchor.y,
            (anchor.x + next.x) / 2f,
            (anchor.y + next.y) / 2f,
        )
    }
    path.close()
    return path
}
