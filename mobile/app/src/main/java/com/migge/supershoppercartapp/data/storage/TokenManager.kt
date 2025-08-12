package com.migge.supershoppercartapp.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    fun getAccessToken(): Flow<String?> =
        dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    fun getRefreshToken(): Flow<String?> =
        dataStore.data.map { it[REFRESH_TOKEN_KEY] }

    fun getDeviceId(): Flow<String?> =
        dataStore.data.map { it[DEVICE_ID_KEY] }

    suspend fun isLoggedIn(): Boolean =
        getAccessToken().first() != null

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        deviceId: String,
        userEmail: String? = null,
        userName: String? = null
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            preferences[DEVICE_ID_KEY] = deviceId
            userEmail?.let { preferences[USER_EMAIL_KEY] = it }
            userName?.let { preferences[USER_NAME_KEY] = it }
        }
    }

    suspend fun clearTokens() {
        dataStore.edit { it.clear() }
    }
}