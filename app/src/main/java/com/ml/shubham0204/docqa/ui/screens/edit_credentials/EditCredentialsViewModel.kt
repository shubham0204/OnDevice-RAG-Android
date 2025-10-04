package com.ml.shubham0204.docqa.ui.screens.edit_credentials

import androidx.lifecycle.ViewModel
import com.ml.shubham0204.docqa.data.GeminiAPIKey
import com.ml.shubham0204.docqa.data.HFAccessToken
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class EditCredentialsViewModel(
    private val geminiAPIKey: GeminiAPIKey,
    private val hfAccessToken: HFAccessToken,
) : ViewModel() {
    fun getGeminiAPIKey(): String? = geminiAPIKey.getAPIKey()

    fun saveGeminiAPIKey(apiKey: String) {
        geminiAPIKey.saveAPIKey(apiKey)
    }

    fun getHFAccessToken(): String? = hfAccessToken.getToken()

    fun saveHFAccessToken(accessToken: String) {
        hfAccessToken.saveToken(accessToken)
    }
}
