package com.example.popsandbops.ui.recording

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.popsandbops.audio.AudioRecorder
import com.example.popsandbops.data.sanitizeTrimRange
import com.example.popsandbops.ui.PendingRecording
import com.example.popsandbops.ui.components.WaveformView
import kotlin.math.roundToInt

@Composable
fun RecordingBlobOverlay(
    elapsedMs: Int,
    waveform: List<Float>,
    modifier: Modifier = Modifier,
    onStop: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "recording pulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recording scale",
    )
    val progress = (elapsedMs / AudioRecorder.MAX_RECORDING_MS.toFloat()).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .offset(y = (-118).dp)
            .scale(pulse),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.12f),
                    )
                    drawArc(
                        color = primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx()),
                    )
                }
                Icon(
                    imageVector = Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    tint = primary,
                )
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = formatMs(elapsedMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                WaveformView(
                    waveform = waveform.takeLast(28),
                    modifier = Modifier.fillMaxWidth(),
                    activeColor = primary,
                )
            }
            FilledIconButton(onClick = onStop, shape = CircleShape) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop recording")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimRecordingSheet(
    pendingRecording: PendingRecording,
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit,
    onTrimChange: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val durationMs = pendingRecording.safeDurationMs
    val trimRange = pendingRecording.trimRange

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Trim recording",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                FilledIconButton(onClick = onDiscard) {
                    Icon(Icons.Filled.Close, contentDescription = "Discard recording")
                }
            }

            OutlinedTextField(
                value = pendingRecording.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Name") },
            )

            WaveformView(
                waveform = pendingRecording.waveform,
                durationMs = durationMs,
                trimStartMs = trimRange.startMs,
                trimEndMs = trimRange.endMs,
                activeColor = MaterialTheme.colorScheme.primary,
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
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatMs(trimRange.startMs)} - ${formatMs(trimRange.endMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDiscard) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Discard")
                    }
                    Button(onClick = onSave) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val seconds = ms / 1000
    val tenths = (ms % 1000) / 100
    return "$seconds.$tenths s"
}
