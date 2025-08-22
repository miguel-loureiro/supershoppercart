package com.migge.supershoppercart.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.migge.supershoppercart.viewmodel.SignInState

@Composable
fun SignInScreen(
    state: SignInState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)), // light background
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Google Sign In",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(Modifier.height(16.dp))
            when (state) {
                SignInState.Loading -> CircularProgressIndicator()
                SignInState.Failure -> {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
                else -> {}
            }
        }
    }
}