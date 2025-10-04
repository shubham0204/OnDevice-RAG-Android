package com.ml.shubham0204.docqa.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.koin.core.annotation.Single

@Single
class HFAccessToken(
    context: Context,
) {
    private val securedSharedPrefFileName = "secret_shared_prefs"
    private val accessTokenSharedPrefKey = "hf_access_token"

    private val masterKey: MasterKey =
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val sharedPreferences: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            securedSharedPrefFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun saveToken(accessToken: String) {
        sharedPreferences.edit().putString(accessTokenSharedPrefKey, accessToken).apply()
    }

    fun getToken(): String? = sharedPreferences.getString(accessTokenSharedPrefKey, null)
}
