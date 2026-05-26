package com.example.popsandbops.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.ui.components.BlobPreview

@Composable
fun SoundLibraryScreen(
    blobs: List<SoundBlob>,
    playingBlobId: String?,
    modifier: Modifier = Modifier,
    onPlay: (SoundBlob) -> Unit,
    onEdit: (SoundBlob) -> Unit,
    onPinnedChange: (SoundBlob, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 48.dp, bottom = 6.dp)) {
                Text(
                    text = "Sound library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${blobs.size} saved sounds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                )
            }
        }
        items(items = blobs, key = { it.id }) { blob ->
            SoundLibraryRow(
                blob = blob,
                isPlaying = playingBlobId == blob.id,
                onPlay = { onPlay(blob) },
                onEdit = { onEdit(blob) },
                onPinnedChange = { onPinnedChange(blob, it) },
            )
        }
        item {
            Spacer(modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SoundLibraryRow(
    blob: SoundBlob,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onPinnedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPlaying) 5.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BlobPreview(
                color = blob.color,
                points = blob.shapePoints,
                modifier = Modifier.size(58.dp),
                isSelected = isPlaying,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blob.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (blob.isRecorded) "Recording" else "Preset tone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }
            Switch(
                checked = blob.isPinned,
                onCheckedChange = onPinnedChange,
            )
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${blob.name}")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${blob.name}")
            }
        }
    }
}
