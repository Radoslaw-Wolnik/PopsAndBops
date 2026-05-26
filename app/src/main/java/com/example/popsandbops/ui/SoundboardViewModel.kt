package com.example.popsandbops.ui

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.SoundBlobRepository

class SoundboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundBlobRepository(application)
    private val _uiState = mutableStateOf(
        SoundboardUiState(blobs = repository.loadBlobs()),
    )

    val uiState: State<SoundboardUiState> = _uiState

    fun showSection(section: SoundboardSection) {
        _uiState.value = _uiState.value.copy(activeSection = section)
    }

    fun selectBlob(blobId: String?) {
        _uiState.value = _uiState.value.copy(selectedBlobId = blobId)
    }

    fun previewBlob(blob: SoundBlob) {
        _uiState.value = _uiState.value.copy(playingBlobId = blob.id, selectedBlobId = blob.id)
    }

    fun clearPreview(blobId: String) {
        if (_uiState.value.playingBlobId == blobId) {
            _uiState.value = _uiState.value.copy(playingBlobId = null)
        }
    }

    fun upsertBlob(blob: SoundBlob) {
        val current = _uiState.value.blobs
        val updated = if (current.any { it.id == blob.id }) {
            current.map { if (it.id == blob.id) blob else it }
        } else {
            current + blob
        }
        persist(updated)
    }

    private fun persist(blobs: List<SoundBlob>) {
        repository.saveBlobs(blobs)
        _uiState.value = _uiState.value.copy(blobs = blobs)
    }
}
