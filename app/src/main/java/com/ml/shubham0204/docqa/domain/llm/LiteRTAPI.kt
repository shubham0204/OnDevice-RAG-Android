package com.ml.shubham0204.docqa.domain.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class LiteRTAPI : LLMInferenceAPI() {
    private lateinit var llmInference: LlmInference
    var isLoaded = false
    var loadedModelPath: String? = null

    class PartialProgressListener(
        private val onPartialResponseGenerated: (String) -> Unit,
        private val onSuccess: (String) -> Unit,
    ) : ProgressListener<String> {
        private var result = ""

        override fun run(
            partialResult: String?,
            done: Boolean,
        ) {
            if (done) {
                onSuccess(result)
                result = ""
            } else {
                result += partialResult ?: ""
                onPartialResponseGenerated(result)
            }
        }
    }

    fun load(
        context: Context,
        modelPath: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val taskOptions =
            LlmInference.LlmInferenceOptions
                .builder()
                .setModelPath(modelPath)
                .setMaxTopK(64)
                .setMaxTokens(2048)
                .build()
        llmInference = LlmInference.createFromOptions(context, taskOptions)
        isLoaded = true
        loadedModelPath = modelPath
        onSuccess()
    }

    override suspend fun getResponse(prompt: String): String? =
        withContext(Dispatchers.Default) {
            Log.e("APP", "Prompt given: $prompt")
            llmInference.generateResponse(prompt)
        }

    fun unload() {
        llmInference.close()
        isLoaded = false
        loadedModelPath = null
    }
}
