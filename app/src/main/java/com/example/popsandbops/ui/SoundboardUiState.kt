package com.example.popsandbops.ui

import com.example.popsandbops.data.SoundBlob

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
}

data class PendingRecording(
    val path: String,
    val durationMs: Int,
    val waveform: List<Float>,
    val name: String,
    val trimStartMs: Int = 0,
    val trimEndMs: Int = durationMs,
)
