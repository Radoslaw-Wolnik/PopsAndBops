package com.example.popsandbops.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.popsandbops.ui.editor.SoundEditorScreen
import com.example.popsandbops.ui.library.SoundLibraryScreen
import com.example.popsandbops.ui.map.SoundMapScreen
import com.example.popsandbops.ui.recording.RecordingFlowScreen

@Composable
fun PopsAndBopsApp(
    viewModel: SoundboardViewModel = viewModel(),
) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            viewModel.showRecordingPermissionDenied()
        }
    }

    fun startHoldRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopHoldRecording() {
        if (state.isRecording) {
            viewModel.stopRecording()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            val editingBlob = state.editingBlob
            if (editingBlob != null) {
                SoundEditorScreen(
                    blob = editingBlob,
                    modifier = Modifier.fillMaxSize(),
                    onClose = viewModel::closeEditor,
                    onPlay = viewModel::previewBlob,
                    onNameChange = { viewModel.updateBlobName(editingBlob.id, it) },
                    onPinnedChange = { viewModel.toggleBlobPinned(editingBlob.id, it) },
                    onColorChange = { viewModel.updateBlobColor(editingBlob.id, it) },
                    onShapePresetChange = { preset, points ->
                        viewModel.updateBlobShape(editingBlob.id, preset, points)
                    },
                    onShapePointsChange = { viewModel.updateBlobShapePoints(editingBlob.id, it) },
                    onTrimChange = { start, end -> viewModel.updateBlobTrim(editingBlob.id, start, end) },
                )
            } else {
                when (state.activeSection) {
                    SoundboardSection.Map -> SoundMapScreen(
                        blobs = state.pinnedBlobs,
                        playingBlobId = state.playingBlobId,
                        isRecording = state.isRecording,
                        showBlobNames = state.showBlobNames,
                        modifier = Modifier.fillMaxSize(),
                        onBlobClick = viewModel::previewBlob,
                        onLibraryClick = {
                            viewModel.selectBlob(null)
                            viewModel.showSection(SoundboardSection.Library)
                        },
                        onBlobMoved = { blob, position -> viewModel.updateBlobPosition(blob.id, position) },
                        onAutoArrange = viewModel::autoArrangePinnedBlobs,
                        onRecordClick = viewModel::openRecordPanel,
                        onBlobNamesVisibleChange = viewModel::setBlobNamesVisible,
                    )

                    SoundboardSection.Library -> SoundLibraryScreen(
                        blobs = state.blobs,
                        selectedBlob = state.selectedBlob,
                        playingBlobId = state.playingBlobId,
                        modifier = Modifier.fillMaxSize(),
                        onSelect = { viewModel.selectBlob(it.id) },
                        onBackToMap = {
                            viewModel.selectBlob(null)
                            viewModel.showSection(SoundboardSection.Map)
                        },
                        onBackToGrid = { viewModel.selectBlob(null) },
                        onPlay = viewModel::previewBlob,
                        onEdit = { viewModel.startEditing(it.id) },
                    )
                }
            }

            if (state.isRecordPanelOpen || state.isRecording || state.pendingRecording != null) {
                RecordingFlowScreen(
                    isRecording = state.isRecording,
                    elapsedMs = state.recordingElapsedMs,
                    waveform = state.recordingWaveform,
                    pendingRecording = state.pendingRecording,
                    modifier = Modifier.fillMaxSize(),
                    onClose = viewModel::cancelRecordingFlow,
                    onHoldStart = ::startHoldRecording,
                    onHoldEnd = ::stopHoldRecording,
                    onNameChange = viewModel::updatePendingName,
                    onTrimChange = viewModel::updatePendingTrim,
                    onSave = viewModel::savePendingRecording,
                    onDiscard = viewModel::discardPendingRecording,
                )
            }
        }
    }
}
