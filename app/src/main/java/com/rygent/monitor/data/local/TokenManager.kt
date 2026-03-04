package com.rygent.monitor.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(deviceId: String, token: String) {
        sharedPreferences.edit().putString("token_$deviceId", token).apply()
    }

    fun getToken(deviceId: String): String? {
        return sharedPreferences.getString("token_$deviceId", null)
    }

    fun clearToken(deviceId: String) {
        sharedPreferences.edit().remove("token_$deviceId").apply()
    }
}
