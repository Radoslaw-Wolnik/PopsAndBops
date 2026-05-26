package com.example.popsandbops.ui

import com.example.popsandbops.data.MIN_SOUND_DURATION_MS
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.TrimRange
import com.example.popsandbops.data.sanitizeTrimRange

enum class SoundboardSection {
    Map,
    Library,
}

data class SoundboardUiState(
    val blobs: List<SoundBlob> = emptyList(),
    val activeSection: SoundboardSection = SoundboardSection.Map,
    val playingBlobId: String? = null,
    val selectedBlobId: String? = null,
    val editingBlobId: String? = null,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Int = 0,
    val recordingWaveform: List<Float> = emptyList(),
    val pendingRecording: PendingRecording? = null,
    val message: String? = null,
) {
    val pinnedBlobs: List<SoundBlob>
        get() = blobs.filter { it.isPinned }

    val editingBlob: SoundBlob?
        get() = blobs.firstOrNull { it.id == editingBlobId }

    val selectedBlob: SoundBlob?
        get() = blobs.firstOrNull { it.id == selectedBlobId }
}

data class PendingRecording(
    val path: String,
    val durationMs: Int,
    val waveform: List<Float>,
    val name: String,
    val trimStartMs: Int = 0,
    val trimEndMs: Int = durationMs,
) {
    val safeDurationMs: Int
        get() = durationMs.coerceAtLeast(MIN_SOUND_DURATION_MS)

    val trimRange: TrimRange
        get() = sanitizeTrimRange(trimStartMs, trimEndMs, safeDurationMs)
}
