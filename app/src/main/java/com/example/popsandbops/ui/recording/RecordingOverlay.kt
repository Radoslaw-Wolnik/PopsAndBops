package com.example.popsandbops.ui.recording

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.sanitizeTrimRange
import com.example.popsandbops.ui.PendingRecording
import com.example.popsandbops.ui.components.BlobPreview
import com.example.popsandbops.ui.components.WaveformView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val RecordButtonBlobShape = listOf(
    1.04f,
    1.17f,
    1.08f,
    0.96f,
    1.13f,
    1.19f,
    1.03f,
    0.95f,
    1.10f,
    1.15f,
    1.01f,
    0.97f,
    1.16f,
    1.07f,
)
private const val RecordButtonCurveTension = 0.25f

private val SaveRecordingBlobShape = listOf(
    0.95f,
    1.16f,
    1.19f,
    1.09f,
    1.04f,
    1.13f,
    1.18f,
    1.06f,
    0.99f,
    1.15f,
    1.12f,
    1.05f,
    1.18f,
    1.10f,
)
private const val SaveRecordingCurveTension = 0.27f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingFlowScreen(
    isRecording: Boolean,
    elapsedMs: Int,
    waveform: List<Float>,
    pendingRecording: PendingRecording?,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onNameChange: (String) -> Unit,
    onTrimChange: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FilledIconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close recorder")
        }

        if (pendingRecording == null) {
            HoldToRecordBlob(
                isRecording = isRecording,
                elapsedMs = elapsedMs,
                waveform = waveform,
                modifier = Modifier.align(Alignment.Center),
                onHoldStart = onHoldStart,
                onHoldEnd = onHoldEnd,
            )
        } else {
            SaveRecordingBlob(
                pendingRecording = pendingRecording,
                modifier = Modifier.align(Alignment.Center),
                onNameChange = onNameChange,
                onTrimChange = onTrimChange,
                onSave = onSave,
                onDiscard = onDiscard,
            )
        }
    }
}

@Composable
private fun HoldToRecordBlob(
    isRecording: Boolean,
    elapsedMs: Int,
    waveform: List<Float>,
    modifier: Modifier = Modifier,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "hold recorder")
    val idleScale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hold blob scale",
    )
    val ringProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "record pulse",
    )
    val color = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(336.dp)
            .scale(if (isRecording) 1.04f else idleScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHoldStart()
                        try {
                            tryAwaitRelease()
                        } finally {
                            onHoldEnd()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isRecording) {
                drawCircle(
                    color = color.copy(alpha = 0.18f * (1f - ringProgress)),
                    radius = size.minDimension * (0.38f + ringProgress * 0.18f),
                    center = center,
                    style = Stroke(width = 10.dp.toPx()),
                )
            }
        }
        BlobPreview(
            color = color,
            points = RecordButtonBlobShape,
            modifier = Modifier.fillMaxSize(),
            isSelected = isRecording,
            curveTension = RecordButtonCurveTension,
        )
        if (isRecording) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularWaveformView(
                    waveform = waveform,
                    activeColor = contentColor,
                    modifier = Modifier.size(188.dp),
                )
                Text(
                    text = "Recording ${formatMs(elapsedMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = "Lift to save",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(74.dp),
                    shape = RoundedCornerShape(37.dp),
                    color = contentColor.copy(alpha = 0.14f),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                Text(
                    text = "Hold to record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = "Short sounds work best",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.74f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveRecordingBlob(
    pendingRecording: PendingRecording,
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit,
    onTrimChange: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val durationMs = pendingRecording.safeDurationMs
    val trimRange = pendingRecording.trimRange
    val canSave = pendingRecording.name.isNotBlank()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        val blobSize = minOf(maxWidth, maxHeight, 440.dp)
        val compact = blobSize < 380.dp
        val contentWidth = minOf(blobSize * 0.70f, 306.dp)
        val waveformHeight = if (compact) 58.dp else 70.dp
        val contentGap = if (compact) 6.dp else 8.dp
        BlobPreview(
            color = MaterialTheme.colorScheme.primaryContainer,
            points = SaveRecordingBlobShape,
            modifier = Modifier.size(blobSize),
            curveTension = SaveRecordingCurveTension,
        )
        Column(
            modifier = Modifier.width(contentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(contentGap),
        ) {
            RecordingNameField(
                value = pendingRecording.name,
                onValueChange = onNameChange,
                showRequired = !canSave,
            )
            WaveformView(
                waveform = pendingRecording.waveform,
                durationMs = durationMs,
                trimStartMs = trimRange.startMs,
                trimEndMs = trimRange.endMs,
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f),
                height = waveformHeight,
            )
            RangeSlider(
                value = trimRange.startMs.toFloat()..trimRange.endMs.toFloat(),
                onValueChange = { range ->
                    val trim = sanitizeTrimRange(
                        startMs = range.start.roundToInt(),
                        endMs = range.endInclusive.roundToInt(),
                        sourceDurationMs = durationMs,
                    )
                    onTrimChange(trim.startMs, trim.endMs)
                },
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                ),
            )
            Text(
                text = "${formatMs(trimRange.startMs)} - ${formatMs(trimRange.endMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun RecordingNameField(
    value: String,
    showRequired: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(
                width = if (showRequired) 2.dp else 1.dp,
                color = if (showRequired) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text(
                        text = "Sound name",
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
        if (showRequired) {
            Text(
                text = "Required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f),
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun CircularWaveformView(
    waveform: List<Float>,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barCount = 72
        val bars = List((barCount - waveform.size).coerceAtLeast(0)) { 0.06f } +
            waveform.takeLast(barCount)
        val radius = size.minDimension * 0.25f
        val maxBarLength = size.minDimension * 0.30f
        val center = Offset(size.width / 2f, size.height / 2f)

        bars.forEachIndexed { index, value ->
            val angle = (index / barCount.toFloat()) * PI.toFloat() * 2f - PI.toFloat() / 2f
            val barLength = maxBarLength * value.coerceIn(0.06f, 1f)
            val start = center + Offset(
                x = cos(angle) * radius,
                y = sin(angle) * radius,
            )
            val end = center + Offset(
                x = cos(angle) * (radius + barLength),
                y = sin(angle) * (radius + barLength),
            )
            drawLine(
                color = activeColor.copy(alpha = 0.35f + value.coerceIn(0f, 1f) * 0.65f),
                start = start,
                end = end,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun formatMs(ms: Int): String {
    val seconds = ms / 1000
    val tenths = (ms % 1000) / 100
    return "$seconds.$tenths s"
}
