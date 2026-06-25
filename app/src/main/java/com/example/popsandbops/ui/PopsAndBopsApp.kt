package com.example.popsandbops.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var isHoldingRecord by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted && isHoldingRecord) {
            viewModel.startRecording()
        } else if (!isGranted) {
            viewModel.showRecordingPermissionDenied()
        }
    }

    fun startHoldRecording() {
        isHoldingRecord = true
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
        isHoldingRecord = false
        viewModel.stopRecording()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    BackHandler(
        enabled = state.isRecordPanelOpen ||
            state.isRecording ||
            state.pendingRecording != null ||
            state.editingBlobId != null ||
            state.selectedBlobId != null ||
            state.activeSection != SoundboardSection.Map,
    ) {
        when {
            state.isRecordPanelOpen || state.isRecording || state.pendingRecording != null -> {
                viewModel.cancelRecordingFlow()
            }

            state.editingBlobId != null -> {
                viewModel.closeEditor()
            }

            state.selectedBlobId != null -> {
                viewModel.selectBlob(null)
            }

            state.activeSection != SoundboardSection.Map -> {
                viewModel.selectBlob(null)
                viewModel.showSection(SoundboardSection.Map)
            }
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
                    onSave = viewModel::saveBlobEdits,
                )
            } else {
                when (state.activeSection) {
                    SoundboardSection.Map -> SoundMapScreen(
                        blobs = state.pinnedBlobs,
                        playingBlobId = state.playingBlobId,
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
