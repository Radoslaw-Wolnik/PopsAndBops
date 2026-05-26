package com.example.popsandbops.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.popsandbops.ui.map.SoundMapScreen

@Composable
fun PopsAndBopsApp(
    viewModel: SoundboardViewModel = viewModel(),
) {
    val state by viewModel.uiState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
        when (state.activeSection) {
            SoundboardSection.Map -> SoundMapScreen(
                blobs = state.pinnedBlobs,
                playingBlobId = state.playingBlobId,
                modifier = Modifier.padding(innerPadding),
                onBlobClick = viewModel::previewBlob,
                onRecordClick = { },
            )

            SoundboardSection.Library -> SoundMapScreen(
                blobs = state.pinnedBlobs,
                playingBlobId = state.playingBlobId,
                modifier = Modifier.padding(innerPadding),
                onBlobClick = viewModel::previewBlob,
                onRecordClick = { },
            )
        }
    }
}
