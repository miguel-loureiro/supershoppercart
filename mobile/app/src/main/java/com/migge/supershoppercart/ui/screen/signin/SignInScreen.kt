package com.migge.supershoppercart.ui.screen.signin

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import java.util.UUID

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = Identity.getSignInClient(context)
                    .getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    viewModel.loginWithGoogle(idToken, deviceId = UUID.randomUUID().toString())
                }
            } catch (e: Exception) {
                Log.e("SignInScreen", "Google Sign-in failed", e)
            }
        }
    }

    // Configuração do OneTap
    val signInRequest = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId("SEU_SERVER_CLIENT_ID.apps.googleusercontent.com") // TODO
                .setFilterByAuthorizedAccounts(false)
                .build()
        )
        .build()

    val oneTapClient = remember { Identity.getSignInClient(context) }

    fun launchSignIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                launcher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            }
            .addOnFailureListener {
                Toast.makeText(context, "Google Sign-in failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is SignInState.Idle -> {
                Button(onClick = { launchSignIn() }) {
                    Text("Google Sign-in")
                }
            }
            is SignInState.Loading -> {
                CircularProgressIndicator()
            }
            is SignInState.Success -> {
                LaunchedEffect(Unit) {
                    navController.navigate("success") {
                        popUpTo("signin") { inclusive = true }
                    }
                }
            }
            is SignInState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Erro: ${(state as SignInState.Error).message}", color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { launchSignIn() }) {
                        Text("Tentar Novamente")
                    }
                }
            }
        }
    }
}