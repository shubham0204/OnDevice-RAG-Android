package com.ml.shubham0204.docqa.domain.llm

abstract class LLMInferenceAPI {
    abstract suspend fun getResponse(prompt: String): String?
}
