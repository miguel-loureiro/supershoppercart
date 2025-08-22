package com.migge.supershoppercart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.migge.supershoppercart.screen.FailureScreen
import com.migge.supershoppercart.screen.LoadingScreen
import com.migge.supershoppercart.screen.SuccessScreen
import com.migge.supershoppercart.viewmodel.SignInState
import com.migge.supershoppercart.viewmodel.SignInViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: SignInViewModel = hiltViewModel()
            val signInState by viewModel.signInState.collectAsState()
            // Replace with your actual device ID logic
            val deviceId = "some-device-id"
            if (signInState == SignInState.Idle) {
                viewModel.autoSignIn(deviceId)
            }
            when (signInState) {
                SignInState.Success -> SuccessScreen()
                SignInState.Failure -> FailureScreen()
                SignInState.Loading -> LoadingScreen()
                else -> {}
            }
        }
    }
}