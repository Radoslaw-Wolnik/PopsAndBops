package com.example.popsandbops.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.popsandbops.audio.AudioRecorder
import com.example.popsandbops.audio.SoundPlayer
import com.example.popsandbops.data.BlobMapLayout
import com.example.popsandbops.data.BlobShapePreset
import com.example.popsandbops.data.MapPoint
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.SoundBlobRepository
import com.example.popsandbops.data.sanitizeTrimRange

class SoundboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundBlobRepository(application)
    private val recorder = AudioRecorder(application)
    private val player = SoundPlayer(application)
    private val handler = Handler(Looper.getMainLooper())
    private var recordingTicker: Runnable? = null
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

    fun startEditing(blobId: String) {
        _uiState.value = _uiState.value.copy(
            editingBlobId = blobId,
            selectedBlobId = blobId,
            activeSection = SoundboardSection.Library,
        )
    }

    fun closeEditor() {
        _uiState.value = _uiState.value.copy(editingBlobId = null)
    }

    fun previewBlob(blob: SoundBlob) {
        val didStart = player.play(blob) {
            clearPreview(blob.id)
        }
        _uiState.value = if (didStart) {
            _uiState.value.copy(playingBlobId = blob.id, selectedBlobId = blob.id)
        } else {
            _uiState.value.copy(
                playingBlobId = null,
                selectedBlobId = blob.id,
                message = "Could not play sound",
            )
        }
    }

    fun showRecordingPermissionDenied() {
        _uiState.value = _uiState.value.copy(message = "Microphone permission is needed to record")
    }

    fun clearMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }

    fun clearPreview(blobId: String) {
        if (_uiState.value.playingBlobId == blobId) {
            _uiState.value = _uiState.value.copy(playingBlobId = null)
        }
    }

    fun openRecordPanel() {
        _uiState.value = _uiState.value.copy(
            isRecordPanelOpen = true,
            activeSection = SoundboardSection.Map,
        )
    }

    fun closeRecordPanel() {
        if (_uiState.value.isRecording) return
        _uiState.value = _uiState.value.copy(isRecordPanelOpen = false)
    }

    fun cancelRecordingFlow() {
        recordingTicker?.let(handler::removeCallbacks)
        recordingTicker = null
        recorder.cancel()
        _uiState.value.pendingRecording?.path?.let { java.io.File(it).delete() }
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordingElapsedMs = 0,
            recordingWaveform = emptyList(),
            pendingRecording = null,
            isRecordPanelOpen = false,
        )
    }

    fun setBlobNamesVisible(isVisible: Boolean) {
        _uiState.value = _uiState.value.copy(showBlobNames = isVisible)
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

    fun startRecording() {
        if (_uiState.value.isRecording) return
        player.stop()
        val outputFile = repository.createRecordingFile()
        runCatching {
            recorder.start(outputFile)
        }.onSuccess {
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                recordingElapsedMs = 0,
                recordingWaveform = emptyList(),
                pendingRecording = null,
                playingBlobId = null,
                isRecordPanelOpen = true,
                activeSection = SoundboardSection.Map,
            )
            tickRecording()
        }.onFailure {
            _uiState.value = _uiState.value.copy(message = "Could not start recording")
        }
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) return
        recordingTicker?.let(handler::removeCallbacks)
        recordingTicker = null
        val result = recorder.stop()
        if (result == null) {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                recordingElapsedMs = 0,
                recordingWaveform = emptyList(),
                message = "Recording was too short",
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordingElapsedMs = 0,
            recordingWaveform = emptyList(),
            pendingRecording = PendingRecording(
                path = result.file.absolutePath,
                durationMs = result.durationMs,
                waveform = result.waveform,
                name = "",
            ),
            isRecordPanelOpen = true,
        )
    }

    fun updatePendingName(name: String) {
        val pending = _uiState.value.pendingRecording ?: return
        _uiState.value = _uiState.value.copy(pendingRecording = pending.copy(name = name))
    }

    fun updatePendingTrim(startMs: Int, endMs: Int) {
        val pending = _uiState.value.pendingRecording ?: return
        val trim = sanitizeTrimRange(startMs, endMs, pending.safeDurationMs)
        _uiState.value = _uiState.value.copy(
            pendingRecording = pending.copy(trimStartMs = trim.startMs, trimEndMs = trim.endMs),
        )
    }

    fun savePendingRecording() {
        val pending = _uiState.value.pendingRecording ?: return
        val name = pending.name.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Name the sound before saving")
            return
        }
        val trim = pending.trimRange
        val createdBlob = repository.createRecordingBlob(
            audioPath = pending.path,
            durationMs = pending.safeDurationMs,
            waveform = pending.waveform,
            existingCount = _uiState.value.blobs.count(),
        )
        val occupiedPositions = _uiState.value.blobs
            .filter { it.isPinned }
            .map { it.position }
        val newBlob = createdBlob.copy(
            name = name,
            trimStartMs = trim.startMs,
            trimEndMs = trim.endMs,
            sourceDurationMs = pending.safeDurationMs,
            position = BlobMapLayout.resolveOverlaps(createdBlob.position, occupiedPositions),
        )
        val updated = _uiState.value.blobs + newBlob
        repository.saveBlobs(updated)
        _uiState.value = _uiState.value.copy(
            blobs = updated,
            pendingRecording = null,
            selectedBlobId = newBlob.id,
            isRecordPanelOpen = false,
            activeSection = SoundboardSection.Map,
        )
    }

    fun discardPendingRecording() {
        val pending = _uiState.value.pendingRecording ?: return
        java.io.File(pending.path).delete()
        _uiState.value = _uiState.value.copy(pendingRecording = null, isRecordPanelOpen = false)
    }

    fun updateBlobName(blobId: String, name: String) {
        updateBlob(blobId) { it.copy(name = name) }
    }

    fun toggleBlobPinned(blobId: String, isPinned: Boolean) {
        updateBlob(blobId) { it.copy(isPinned = isPinned) }
    }

    fun updateBlobColor(blobId: String, colorArgb: Long) {
        updateBlob(blobId) { it.copy(colorArgb = colorArgb) }
    }

    fun updateBlobShape(blobId: String, shapePreset: BlobShapePreset, points: List<Float>) {
        updateBlob(blobId) { it.copy(shapePreset = shapePreset, shapePoints = points) }
    }

    fun updateBlobShapePoints(blobId: String, points: List<Float>) {
        updateBlob(blobId) { it.copy(shapePoints = points) }
    }

    fun updateBlobTrim(blobId: String, startMs: Int, endMs: Int) {
        updateBlob(blobId) {
            val trim = sanitizeTrimRange(startMs, endMs, it.safeSourceDurationMs)
            it.copy(trimStartMs = trim.startMs, trimEndMs = trim.endMs)
        }
    }

    fun saveBlobEdits(editedBlob: SoundBlob) {
        val name = editedBlob.name.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Name the sound before saving")
            return
        }
        updateBlob(editedBlob.id) {
            val trim = sanitizeTrimRange(
                startMs = editedBlob.trimStartMs,
                endMs = editedBlob.trimEndMs,
                sourceDurationMs = editedBlob.safeSourceDurationMs,
            )
            editedBlob.copy(
                name = name,
                trimStartMs = trim.startMs,
                trimEndMs = trim.endMs,
            )
        }
        closeEditor()
    }

    fun updateBlobPosition(blobId: String, position: MapPoint) {
        val occupiedPositions = _uiState.value.blobs
            .filter { it.id != blobId && it.isPinned }
            .map { it.position }
        val resolvedPosition = BlobMapLayout.resolveOverlaps(position, occupiedPositions)
        updateBlob(blobId) { it.copy(position = resolvedPosition) }
    }

    fun autoArrangePinnedBlobs() {
        val arrangedPositions = BlobMapLayout.arrangedPositions(_uiState.value.blobs.count { it.isPinned })
        var pinnedIndex = 0
        val updated = _uiState.value.blobs.map { blob ->
            if (blob.isPinned) {
                blob.copy(position = arrangedPositions[pinnedIndex++])
            } else {
                blob
            }
        }
        persist(updated)
    }

    private fun persist(blobs: List<SoundBlob>) {
        repository.saveBlobs(blobs)
        _uiState.value = _uiState.value.copy(blobs = blobs)
    }

    private fun updateBlob(blobId: String, transform: (SoundBlob) -> SoundBlob) {
        val updated = _uiState.value.blobs.map { blob ->
            if (blob.id == blobId) transform(blob) else blob
        }
        persist(updated)
    }

    private fun tickRecording() {
        recorder.recordSample()
        val elapsed = recorder.elapsedMs().coerceAtMost(AudioRecorder.MAX_RECORDING_MS)
        _uiState.value = _uiState.value.copy(
            recordingElapsedMs = elapsed,
            recordingWaveform = recorder.liveWaveform(),
        )
        if (elapsed >= AudioRecorder.MAX_RECORDING_MS) {
            stopRecording()
            return
        }
        recordingTicker = Runnable { tickRecording() }
        handler.postDelayed(recordingTicker!!, 90L)
    }

    override fun onCleared() {
        recordingTicker?.let(handler::removeCallbacks)
        recorder.cancel()
        player.stop()
        super.onCleared()
    }
}
