package com.migge.supershoppercart.remote

import android.content.Context
import android.credentials.GetCredentialException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.migge.supershoppercart.storage.Config
import com.migge.supershoppercart.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.internal.concurrent.Task
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    /**
     * Attempts Google Sign-In using Credential Manager.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun attemptAutomaticGoogleSignIn(deviceId: String): Boolean =
        withContext(Dispatchers.IO) {
            val credentialManager = CredentialManager.create(context)

            // Configure GoogleIdOption for retrieving Google account
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true) // use false to allow new sign-ups
                .setServerClientId(Config.googleClientId(context))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val response: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential: Credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        return@withContext loginWithGoogle(idToken, deviceId)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Invalid Google ID Token", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential::class.simpleName}")
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed", e)
            }

            false
        }

    /**
     * Sends the Google ID token to the backend, expects DevLoginResponse.
     */
    private suspend fun loginWithGoogle(idToken: String, deviceId: String): Boolean =
        withContext(Dispatchers.IO) {
            val response: Response<DevLoginResponse> =
                apiService.googleLogin("Bearer $idToken", deviceId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.accessToken != null && body.refreshToken != null && body.error.isNullOrEmpty()) {
                    TokenStorage.saveTokens(context, body.accessToken, body.refreshToken)
                    return@withContext true
                }
            }
            false
        }

    /**
     * Dev login using email and optional deviceId.
     */
    suspend fun devLogin(email: String, deviceId: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val request = DevLoginRequest(email, deviceId)
            val response = apiService.devLogin(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.accessToken != null && body.refreshToken != null && body.error.isNullOrEmpty()) {
                    TokenStorage.saveTokens(context, body.accessToken, body.refreshToken)
                    return@withContext true
                }
            }
            false
        }
}