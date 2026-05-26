package com.example.popsandbops.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun WaveformView(
    waveform: List<Float>,
    modifier: Modifier = Modifier,
    durationMs: Int = 1_000,
    trimStartMs: Int = 0,
    trimEndMs: Int = durationMs,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp),
    ) {
        val safeDurationMs = durationMs.coerceAtLeast(1)
        val bars = waveform.ifEmpty { List(48) { 0.14f } }
        val barGap = size.width / bars.size
        val startFraction = (trimStartMs / safeDurationMs.toFloat()).coerceIn(0f, 1f)
        val endFraction = (trimEndMs / safeDurationMs.toFloat()).coerceIn(0f, 1f)
        bars.forEachIndexed { index, value ->
            val x = index * barGap + barGap / 2f
            val fraction = index / bars.lastIndex.coerceAtLeast(1).toFloat()
            val color = if (fraction in startFraction..endFraction) activeColor else inactiveColor.copy(alpha = 0.38f)
            val barHeight = (size.height * value.coerceIn(0.08f, 1f)).coerceAtLeast(6.dp.toPx())
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = (barGap * 0.58f).coerceIn(3.dp.toPx(), 10.dp.toPx()),
                cap = StrokeCap.Round,
            )
        }
    }
}
