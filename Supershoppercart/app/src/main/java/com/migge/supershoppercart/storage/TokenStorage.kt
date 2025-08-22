@file:Suppress("DEPRECATION")

package com.migge.supershoppercart.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

object TokenStorage {
    private const val PREFS_NAME = "secure_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun prefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        prefs(context).edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(context: Context): String? = prefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens(context: Context) {
        prefs(context).edit { clear() }
    }
}