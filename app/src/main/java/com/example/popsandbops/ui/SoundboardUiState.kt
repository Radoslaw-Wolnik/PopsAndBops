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
    val isRecording: Boolean = false,
    val message: String? = null,
) {
    val pinnedBlobs: List<SoundBlob>
        get() = blobs.filter { it.isPinned }
}
