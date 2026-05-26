package com.example.popsandbops.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.popsandbops.ui.editor.SoundEditorScreen
import com.example.popsandbops.ui.library.SoundLibraryScreen
import com.example.popsandbops.ui.map.SoundMapScreen
import com.example.popsandbops.ui.recording.RecordingBlobOverlay
import com.example.popsandbops.ui.recording.TrimRecordingSheet

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
        if (isGranted) viewModel.startRecording()
    }

    fun requestOrRecord() {
        if (state.isRecording) {
            viewModel.stopRecording()
            return
        }
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

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.activeSection == SoundboardSection.Map,
                    onClick = { viewModel.showSection(SoundboardSection.Map) },
                    icon = { Icon(Icons.Filled.TravelExplore, contentDescription = "Sound map") },
                    label = { Text("Map") },
                )
                NavigationBarItem(
                    selected = state.activeSection == SoundboardSection.Library,
                    onClick = { viewModel.showSection(SoundboardSection.Library) },
                    icon = { Icon(Icons.Filled.GraphicEq, contentDescription = "Sound library") },
                    label = { Text("Library") },
                )
            }
        },
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
                        modifier = Modifier.fillMaxSize(),
                        onBlobClick = viewModel::previewBlob,
                        onRecordClick = ::requestOrRecord,
                    )

                    SoundboardSection.Library -> SoundLibraryScreen(
                        blobs = state.blobs,
                        playingBlobId = state.playingBlobId,
                        modifier = Modifier.fillMaxSize(),
                        onPlay = viewModel::previewBlob,
                        onEdit = { viewModel.startEditing(it.id) },
                        onPinnedChange = { blob, isPinned ->
                            viewModel.toggleBlobPinned(blob.id, isPinned)
                        },
                    )
                }
            }

            if (state.isRecording) {
                RecordingBlobOverlay(
                    elapsedMs = state.recordingElapsedMs,
                    waveform = state.recordingWaveform,
                    modifier = Modifier.align(Alignment.Center),
                    onStop = viewModel::stopRecording,
                )
            }

            state.pendingRecording?.let { pending ->
                TrimRecordingSheet(
                    pendingRecording = pending,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onNameChange = viewModel::updatePendingName,
                    onTrimChange = viewModel::updatePendingTrim,
                    onSave = viewModel::savePendingRecording,
                    onDiscard = viewModel::discardPendingRecording,
                )
            }
        }
    }
}
