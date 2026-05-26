package com.example.popsandbops.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.ui.components.BlobPreview
import com.example.popsandbops.ui.components.rememberPressFeedback
import com.example.popsandbops.ui.components.WaveformView

@Composable
fun SoundLibraryScreen(
    blobs: List<SoundBlob>,
    selectedBlob: SoundBlob?,
    playingBlobId: String?,
    modifier: Modifier = Modifier,
    onSelect: (SoundBlob) -> Unit,
    onBackToMap: () -> Unit,
    onBackToGrid: () -> Unit,
    onPlay: (SoundBlob) -> Unit,
    onEdit: (SoundBlob) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (selectedBlob == null) {
            SoundLibraryGrid(
                blobs = blobs,
                playingBlobId = playingBlobId,
                onSelect = onSelect,
                onBackToMap = onBackToMap,
            )
        } else {
            SoundLibraryDetail(
                blob = selectedBlob,
                isPlaying = playingBlobId == selectedBlob.id,
                onBack = onBackToGrid,
                onPlay = { onPlay(selectedBlob) },
                onEdit = { onEdit(selectedBlob) },
            )
        }
    }
}

@Composable
private fun SoundLibraryGrid(
    blobs: List<SoundBlob>,
    playingBlobId: String?,
    onSelect: (SoundBlob) -> Unit,
    onBackToMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 42.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackToMap) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to map")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sound library",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${blobs.size} saved sounds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 116.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = blobs, key = { it.id }) { blob ->
                SoundLibraryTile(
                    blob = blob,
                    isPlaying = playingBlobId == blob.id,
                    onClick = { onSelect(blob) },
                )
            }
        }
    }
}

@Composable
private fun SoundLibraryTile(
    blob: SoundBlob,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val press = rememberPressFeedback(pressedScale = 0.95f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .scale(press.scale)
            .clickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = when {
            isPlaying -> 7.dp
            press.isPressed -> 5.dp
            else -> 2.dp
        },
        border = BorderStroke(
            width = if (isPlaying || press.isPressed) 2.dp else 1.dp,
            color = when {
                isPlaying -> blob.color
                press.isPressed -> blob.color.copy(alpha = 0.72f)
                else -> MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlobPreview(
                color = blob.color,
                points = blob.shapePoints,
                modifier = Modifier.size(72.dp),
                isSelected = isPlaying,
            )
            Text(
                text = blob.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (blob.isRecorded) "Recording" else "Preset",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SoundLibraryDetail(
    blob: SoundBlob,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
) {
    val backPress = rememberPressFeedback(pressedScale = 0.90f)
    val editPress = rememberPressFeedback(pressedScale = 0.90f)
    val playPress = rememberPressFeedback(pressedScale = 0.94f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 42.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .scale(backPress.scale),
            interactionSource = backPress.interactionSource,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library")
        }

        FilledIconButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .scale(editPress.scale),
            interactionSource = editPress.interactionSource,
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit ${blob.name}")
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BlobPreview(
                color = blob.color,
                points = blob.shapePoints,
                modifier = Modifier.size(190.dp),
                isSelected = isPlaying,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = blob.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${if (blob.isRecorded) "Recording" else "Preset tone"} - ${formatMs(blob.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = blob.color,
                            )
                            Text(
                                text = if (blob.isPinned) "On map" else "Library only",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        FilledTonalButton(
                            onClick = onPlay,
                            modifier = Modifier.scale(playPress.scale),
                            interactionSource = playPress.interactionSource,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(text = if (isPlaying) "Playing" else "Play")
                        }
                    }

                    WaveformView(
                        waveform = blob.waveform,
                        durationMs = blob.sourceDurationMs,
                        trimStartMs = blob.trimStartMs,
                        trimEndMs = blob.trimEndMs,
                        activeColor = blob.color,
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1_000f).coerceAtLeast(0f)
    return "%.1fs".format(totalSeconds)
}
