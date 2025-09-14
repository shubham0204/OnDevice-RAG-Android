package com.ml.shubham0204.docqa.ui.screens.local_models

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.docqa.data.LocalModel
import com.ml.shubham0204.docqa.ui.theme.DocQATheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalModelsScreen(
    uiState: LocalModelsUIState,
    onEvent: (LocalModelsUIEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    DocQATheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Manage Local Models",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxWidth(),
            ) {
                LaunchedEffect(0) {
                    onEvent(LocalModelsUIEvent.RefreshModelsList)
                }
                LocalModelsList(
                    uiState.models,
                    onDownloadModelClick = { localModel ->
                        onEvent(LocalModelsUIEvent.OnModelDownloadClick(localModel))
                    },
                    onLoadModelClick = { localModel ->
                        onEvent(LocalModelsUIEvent.OnUseModelClick(localModel))
                    },
                )
                DownloadDialogModel(uiState.downloadModelDialogState, onDismiss = {})
            }
        }
    }
}

@Composable
private fun LocalModelsList(
    modelsList: List<LocalModel>,
    onDownloadModelClick: (LocalModel) -> Unit,
    onLoadModelClick: (LocalModel) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn {
        items(modelsList) { localModel ->
            LocalModelListItem(
                modelName = localModel.name,
                modelDescription = localModel.description,
                isDownloaded =
                    if (context.filesDir != null) {
                        // `context.filesDir` can be null when rendering
                        // the Compose preview
                        localModel.isDownloaded(context.filesDir.absolutePath)
                    } else {
                        true
                    },
                isLoaded = localModel.isLoaded,
                onDownloadClick = { onDownloadModelClick(localModel) },
                onLoadModelClick = { onLoadModelClick(localModel) },
            )
        }
    }
}

@Composable
private fun LocalModelListItem(
    modelName: String,
    modelDescription: String,
    isDownloaded: Boolean,
    isLoaded: Boolean,
    onDownloadClick: () -> Unit,
    onLoadModelClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = modelDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isDownloaded) {
            if (isLoaded) {
                Box(
                    modifier =
                        Modifier
                            .background(Color.White)
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                ) {
                    Text("Loaded", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(onClick = onLoadModelClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Load model",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            IconButton(onClick = onDownloadClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download model",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
@Preview
private fun LocalModelsScreenPreview() {
    LocalModelsScreen(
        uiState =
            LocalModelsUIState(
                models =
                    listOf<LocalModel>(
                        LocalModel(
                            name = "Qwen3 8B",
                            description = "A Qwen family model series",
                            isLoaded = false,
                            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_seq128_q8_ekv4096.task",
                        ),
                        LocalModel(
                            name = "Qwen2.5 1.5B",
                            description = "A Qwen family model series",
                            isLoaded = true,
                            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_seq128_q8_ekv4096.task",
                        ),
                    ),
            ),
        onEvent = { },
        onBackClick = {},
    )
}
