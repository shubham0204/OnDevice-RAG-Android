package com.ml.shubham0204.docqa.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.docqa.data.ChunksDB
import com.ml.shubham0204.docqa.data.DocumentsDB
import com.ml.shubham0204.docqa.data.GeminiAPIKey
import com.ml.shubham0204.docqa.data.RetrievedContext
import com.ml.shubham0204.docqa.domain.SentenceEmbeddingProvider
import com.ml.shubham0204.docqa.domain.llm.GeminiRemoteAPI
import com.ml.shubham0204.docqa.domain.llm.LLMInferenceAPI
import com.ml.shubham0204.docqa.domain.llm.LiteRTAPI
import com.ml.shubham0204.docqa.ui.components.createAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

sealed interface ChatScreenUIEvent {
    data object OnEditAPIKeyClick : ChatScreenUIEvent

    data object OnOpenDocsClick : ChatScreenUIEvent

    data object OnLocalModelsClick : ChatScreenUIEvent

    sealed class ResponseGeneration {
        data class Start(
            val query: String,
            val prompt: String,
        ) : ChatScreenUIEvent

        data class StopWithSuccess(
            val response: String,
            val retrievedContextList: List<RetrievedContext>,
        ) : ChatScreenUIEvent

        data class StopWithError(
            val errorMessage: String,
        ) : ChatScreenUIEvent
    }
}

sealed interface ChatNavEvent {
    data object None : ChatNavEvent

    data object ToEditAPIKeyScreen : ChatNavEvent

    data object ToDocsScreen : ChatNavEvent

    data object ToLocalModelsScreen : ChatNavEvent
}

data class ChatScreenUIState(
    val question: String = "",
    val response: String = "",
    val isGeneratingResponse: Boolean = false,
    val retrievedContextList: List<RetrievedContext> = emptyList(),
)

@KoinViewModel
class ChatViewModel(
    private val context: Context,
    private val documentsDB: DocumentsDB,
    private val chunksDB: ChunksDB,
    private val geminiAPIKey: GeminiAPIKey,
    private val sentenceEncoder: SentenceEmbeddingProvider,
    private val liteRTAPI: LiteRTAPI,
) : ViewModel() {
    private val _chatScreenUIState = MutableStateFlow(ChatScreenUIState())
    val chatScreenUIState: StateFlow<ChatScreenUIState> = _chatScreenUIState

    private val _navEventChannel = Channel<ChatNavEvent>()
    val navEventChannel = _navEventChannel.receiveAsFlow()

    fun onChatScreenEvent(event: ChatScreenUIEvent) {
        when (event) {
            is ChatScreenUIEvent.ResponseGeneration.Start -> {
                if (!checkNumDocuments()) {
                    Toast
                        .makeText(
                            context,
                            "Add documents to execute queries",
                            Toast.LENGTH_LONG,
                        ).show()
                    return
                }
                if (!checkValidAPIKey()) {
                    createAlertDialog(
                        dialogTitle = "Invalid API Key",
                        dialogText = "Please enter a Gemini API key to use a LLM for generating responses.",
                        dialogPositiveButtonText = "Add API key",
                        onPositiveButtonClick = {
                            onChatScreenEvent(ChatScreenUIEvent.OnEditAPIKeyClick)
                        },
                        dialogNegativeButtonText = "Open Gemini Console",
                        onNegativeButtonClick = {
                            Intent(Intent.ACTION_VIEW).apply {
                                data = "https://aistudio.google.com/apikey".toUri()
                                context.startActivity(this)
                            }
                        },
                    )
                    return
                }
                if (event.query.trim().isEmpty()) {
                    Toast
                        .makeText(context, "Enter a query to execute", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(isGeneratingResponse = true)
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(question = event.query)

                val llm =
                    if (liteRTAPI.isLoaded) {
                        Toast.makeText(context, "Using local model...", Toast.LENGTH_LONG).show()
                        liteRTAPI
                    } else {
                        val apiKey = geminiAPIKey.getAPIKey() ?: throw Exception("Gemini API key is null")
                        Toast.makeText(context, "Using Gemini cloud model...", Toast.LENGTH_LONG).show()
                        GeminiRemoteAPI(apiKey)
                    }
                getAnswer(llm, event.query, event.prompt)
            }

            is ChatScreenUIEvent.ResponseGeneration.StopWithSuccess -> {
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(isGeneratingResponse = false)
                _chatScreenUIState.value = _chatScreenUIState.value.copy(response = event.response)
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(retrievedContextList = event.retrievedContextList)
            }

            is ChatScreenUIEvent.ResponseGeneration.StopWithError -> {
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(isGeneratingResponse = false)
                _chatScreenUIState.value = _chatScreenUIState.value.copy(question = "")
            }

            is ChatScreenUIEvent.OnOpenDocsClick -> {
                viewModelScope.launch {
                    _navEventChannel.send(ChatNavEvent.ToDocsScreen)
                }
            }

            is ChatScreenUIEvent.OnEditAPIKeyClick -> {
                viewModelScope.launch {
                    _navEventChannel.send(ChatNavEvent.ToEditAPIKeyScreen)
                }
            }

            is ChatScreenUIEvent.OnLocalModelsClick -> {
                viewModelScope.launch {
                    _navEventChannel.send(ChatNavEvent.ToLocalModelsScreen)
                }
            }
        }
    }

    private fun getAnswer(
        llm: LLMInferenceAPI,
        query: String,
        prompt: String,
    ) {
        try {
            var jointContext = ""
            val retrievedContextList = ArrayList<RetrievedContext>()
            val queryEmbedding = sentenceEncoder.encodeText(query)
            chunksDB.getSimilarChunks(queryEmbedding, n = 5).forEach {
                jointContext += " " + it.second.chunkData
                retrievedContextList.add(
                    RetrievedContext(
                        it.second.docFileName,
                        it.second.chunkData,
                    ),
                )
            }
            val inputPrompt = prompt.replace("\$CONTEXT", jointContext).replace("\$QUERY", query)
            CoroutineScope(Dispatchers.IO).launch {
                llm.getResponse(inputPrompt)?.let { llmResponse ->
                    onChatScreenEvent(
                        ChatScreenUIEvent.ResponseGeneration.StopWithSuccess(
                            llmResponse,
                            retrievedContextList,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            onChatScreenEvent(ChatScreenUIEvent.ResponseGeneration.StopWithError(e.message ?: ""))
            throw e
        }
    }

    fun checkNumDocuments(): Boolean = documentsDB.getDocsCount() > 0

    fun checkValidAPIKey(): Boolean = geminiAPIKey.getAPIKey() != null
}
