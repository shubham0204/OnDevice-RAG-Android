package com.ml.shubham0204.docqa.ui.screens.local_models

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ketch.Ketch
import com.ketch.Status
import com.ml.shubham0204.docqa.data.HFAccessToken
import com.ml.shubham0204.docqa.data.LocalModel
import com.ml.shubham0204.docqa.domain.llm.LiteRTAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koin.android.annotation.KoinViewModel

sealed class LocalModelsUIEvent {
    data class OnModelDownloadClick(
        val model: LocalModel,
    ) : LocalModelsUIEvent()

    data class OnUseModelClick(
        val model: LocalModel,
    ) : LocalModelsUIEvent()

    data object RefreshModelsList : LocalModelsUIEvent()
}

data class LocalModelsUIState(
    val models: List<LocalModel> = emptyList(),
    val downloadModelDialogState: DownloadModelDialogUIState = DownloadModelDialogUIState(),
)

data class DownloadModelDialogUIState(
    val isDialogVisible: Boolean = false,
    val showProgress: Boolean = false,
    val progress: Int = 0,
)

@KoinViewModel
class LocalModelsViewModel(
    private val context: Context,
    private val liteRTAPI: LiteRTAPI,
    private val hfAccessToken: HFAccessToken,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            LocalModelsUIState(
                models =
                    listOf(
                        LocalModel(
                            name = "Qwen2.5 0.5B Instruct Q8",
                            description = "A Qwen family model series",
                            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                        ),
                        LocalModel(
                            name = "Qwen2.5 1.5B Instruct Q8",
                            description = "A Qwen family model series",
                            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task",
                        ),
                        LocalModel(
                            name = "Qwen2.5 3B Instruct Q8",
                            description = "A Qwen family model series",
                            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-3B-Instruct/resolve/main/Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                        ),
                        LocalModel(
                            name = "Phi 4 Mini Instruct Q8",
                            description = "A Microsoft Phi 4 model series",
                            downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
                        ),
                        LocalModel(
                            name = "DeepSeek R1 Distill Qwen 1.5B Q8",
                            description = "DeepSeek R1",
                            downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.task",
                        ),
                        LocalModel(
                            name = "Gemma3 1B IT",
                            description = "Gemma 3 1B Instruction-Tuned (gated model)",
                            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv4096.task",
                        ),
                        LocalModel(
                            name = "Gemma3 4B IT",
                            description = "Gemma 3 4B Instruction-Tuned (gated model)",
                            downloadUrl = "https://huggingface.co/litert-community/Gemma3-4B-IT/resolve/main/gemma3-4b-it-int8-web.task",
                        ),
                        LocalModel(
                            name = "Llama 3.2 1B Q8",
                            description = "Llama 3.2 1B (gated model)",
                            downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct/resolve/main/Llama-3.2-1B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                        ),
                        LocalModel(
                            name = "Llama 3.2 3B Q8",
                            description = "Llama 3.2 3B (gated model)",
                            downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct/resolve/main/Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
                        ),
                    ),
            ),
        )
    val uiState: StateFlow<LocalModelsUIState> = _uiState.asStateFlow()

    private var ketch: Ketch =
        Ketch
            .builder()
            .setOkHttpClient(
                OkHttpClient
                    .Builder()
                    .connectTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .build(),
            ).build(context)

    fun onEvent(event: LocalModelsUIEvent) {
        when (event) {
            is LocalModelsUIEvent.OnModelDownloadClick -> {
                viewModelScope.launch(Dispatchers.IO) {
                    downloadModel(event.model)
                }
            }
            is LocalModelsUIEvent.OnUseModelClick -> {
                if (liteRTAPI.isLoaded) {
                    liteRTAPI.unload()
                }
                viewModelScope.launch(Dispatchers.IO) {
                    loadModel(event.model)
                    onEvent(LocalModelsUIEvent.RefreshModelsList)
                }
            }
            is LocalModelsUIEvent.RefreshModelsList -> {
                _uiState.update {
                    it.copy(
                        models =
                            it.models.map { model ->
                                model.copy(
                                    isLoaded =
                                        liteRTAPI.loadedModelPath == model.getLocalModelPath(context.filesDir.absolutePath),
                                )
                            },
                    )
                }
            }
        }
    }

    private suspend fun loadModel(model: LocalModel) =
        withContext(Dispatchers.IO) {
            liteRTAPI.load(
                context,
                model.getLocalModelPath(context.filesDir.absolutePath),
                onSuccess = {},
                onError = { exception ->
                    Log.e("APP", "Failed to load LiteRT model: ${exception.message}")
                },
            )
        }

    private suspend fun downloadModel(model: LocalModel) {
        val headers =
            if (hfAccessToken.getToken() != null) {
                HashMap(
                    mapOf("Authorization" to "Bearer ${hfAccessToken.getToken()}"),
                )
            } else {
                HashMap()
            }
        val downloadId =
            ketch.download(
                model.downloadUrl,
                context.filesDir.absolutePath,
                model.getFileName(),
                headers = headers,
            )
        ketch
            .observeDownloadById(downloadId)
            .flowOn(Dispatchers.IO)
            .collect { downloadModel ->
                downloadModel?.let { ketchDownload ->
                    when (ketchDownload.status) {
                        Status.QUEUED -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(isDialogVisible = true),
                                )
                            }
                        }

                        Status.PROGRESS -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(progress = ketchDownload.progress),
                                )
                            }
                        }

                        Status.SUCCESS -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(isDialogVisible = false),
                                )
                            }
                            onEvent(LocalModelsUIEvent.OnUseModelClick(model))
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        context,
                                        "Model downloaded successfully",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }

                        Status.FAILED -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(isDialogVisible = false),
                                )
                            }
                            withContext(Dispatchers.Main) {
                                Log.e("APP", "Failure reason ${ketchDownload.failureReason}")
                                Toast
                                    .makeText(
                                        context,
                                        "Model downloaded failed ${ketchDownload.failureReason}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }

                        Status.STARTED -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState =
                                        it.downloadModelDialogState.copy(
                                            isDialogVisible = true,
                                            showProgress = true,
                                        ),
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
    }
}
