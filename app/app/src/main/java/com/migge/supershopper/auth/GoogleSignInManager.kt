package com.migge.supershopper.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val displayName: String?
)

class GoogleSignInManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleSignInResult? {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId("YOUR_WEB_CLIENT_ID") // Replace with your actual web client ID
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            handleSignIn(result)
        } catch (e: GetCredentialException) {
            // Handle various credential exceptions
            when (e) {
                is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                    // User cancelled the operation
                    null
                }
                is androidx.credentials.exceptions.NoCredentialException -> {
                    // No credential available
                    null
                }
                else -> {
                    // Other credential exceptions
                    null
                }
            }
        } catch (e: CancellationException) {
            // Handle coroutine cancellation
            throw e
        } catch (e: Exception) {
            // Handle other exceptions
            null
        }
    }

    private fun handleSignIn(result: GetCredentialResponse): GoogleSignInResult? {
        val credential = result.credential

        return when (credential) {
            is androidx.credentials.CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        GoogleSignInResult(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (e: Exception) {
            // Handle sign out exceptions
        }
    }
}