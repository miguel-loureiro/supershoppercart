package com.migge.supershoppercartapp.ui.screens

import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.migge.supershoppercartapp.data.storage.TokenManager
import com.migge.supershoppercartapp.ui.viewmodels.LoginViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    tokenManager: TokenManager = viewModel.tokenManager // from Hilt
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val loginState by viewModel.loginState.collectAsState()
    val scope = rememberCoroutineScope()

    val WEB_CLIENT_ID = "656975893985-nnh8pi6ul268ve4uc0514u0sh06t83fe.apps.googleusercontent.com"

    fun getOrCreateDeviceId(onReady: (String) -> Unit) {
        scope.launch {
            val existing = tokenManager.getDeviceId().firstOrNull()
            if (!existing.isNullOrBlank()) {
                onReady(existing)
            } else {
                val newId = UUID.randomUUID().toString()
                tokenManager.saveTokens("", "", newId)
                onReady(newId)
            }
        }
    }

    fun generateNonce(length: Int = 24): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun startGoogleSignIn() {
        val credentialManager = CredentialManager.create(context)
        val nonce = generateNonce()

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            // nonce string to use when generating a Google ID token
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    getOrCreateDeviceId { deviceId ->
                        viewModel.exchangeGoogleIdToken(idToken, deviceId)
                    }
                } else {
                    Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginViewModel.LoginState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (loginState) {
            is LoginViewModel.LoginState.Loading -> CircularProgressIndicator()
            is LoginViewModel.LoginState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        (loginState as LoginViewModel.LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { startGoogleSignIn() }) {
                        Text("Retry Sign in with Google")
                    }
                }
            }
            else -> {
                Button(
                    onClick = { startGoogleSignIn() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}