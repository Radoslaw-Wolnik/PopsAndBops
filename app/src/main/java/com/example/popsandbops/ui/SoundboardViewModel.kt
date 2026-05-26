package com.example.popsandbops.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.popsandbops.audio.AudioRecorder
import com.example.popsandbops.audio.SoundPlayer
import com.example.popsandbops.data.SoundBlob
import com.example.popsandbops.data.SoundBlobRepository

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

    fun previewBlob(blob: SoundBlob) {
        player.play(blob) {
            clearPreview(blob.id)
        }
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

    fun startRecording() {
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
                activeSection = SoundboardSection.Map,
            )
            tickRecording()
        }.onFailure {
            _uiState.value = _uiState.value.copy(message = "Could not start recording")
        }
    }

    fun stopRecording() {
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

        val now = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordingElapsedMs = 0,
            recordingWaveform = emptyList(),
            pendingRecording = PendingRecording(
                path = result.file.absolutePath,
                durationMs = result.durationMs,
                waveform = result.waveform,
                name = repository.defaultRecordingName(now),
            ),
        )
    }

    fun updatePendingName(name: String) {
        val pending = _uiState.value.pendingRecording ?: return
        _uiState.value = _uiState.value.copy(pendingRecording = pending.copy(name = name))
    }

    fun updatePendingTrim(startMs: Int, endMs: Int) {
        val pending = _uiState.value.pendingRecording ?: return
        _uiState.value = _uiState.value.copy(
            pendingRecording = pending.copy(trimStartMs = startMs, trimEndMs = endMs),
        )
    }

    fun savePendingRecording() {
        val pending = _uiState.value.pendingRecording ?: return
        val newBlob = repository.createRecordingBlob(
            audioPath = pending.path,
            durationMs = pending.durationMs,
            waveform = pending.waveform,
            existingCount = _uiState.value.blobs.count(),
        ).copy(
            name = pending.name.ifBlank { repository.defaultRecordingName(System.currentTimeMillis()) },
            trimStartMs = pending.trimStartMs,
            trimEndMs = pending.trimEndMs,
        )
        val updated = _uiState.value.blobs + newBlob
        repository.saveBlobs(updated)
        _uiState.value = _uiState.value.copy(
            blobs = updated,
            pendingRecording = null,
            selectedBlobId = newBlob.id,
            activeSection = SoundboardSection.Map,
        )
    }

    fun discardPendingRecording() {
        val pending = _uiState.value.pendingRecording ?: return
        java.io.File(pending.path).delete()
        _uiState.value = _uiState.value.copy(pendingRecording = null)
    }

    private fun persist(blobs: List<SoundBlob>) {
        repository.saveBlobs(blobs)
        _uiState.value = _uiState.value.copy(blobs = blobs)
    }

    private fun tickRecording() {
        val sample = recorder.recordSample()
        val elapsed = recorder.elapsedMs().coerceAtMost(AudioRecorder.MAX_RECORDING_MS)
        val waveform = (recorder.currentWaveform() + sample).takeLast(64)
        _uiState.value = _uiState.value.copy(
            recordingElapsedMs = elapsed,
            recordingWaveform = waveform,
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
