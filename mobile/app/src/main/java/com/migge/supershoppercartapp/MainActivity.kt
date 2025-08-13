package com.migge.supershoppercartapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.migge.supershoppercartapp.data.storage.TokenManager
import com.migge.supershoppercartapp.ui.screens.GreenScreen
import com.migge.supershoppercartapp.ui.screens.LoginScreen
import com.migge.supershoppercartapp.ui.screens.SplashScreen
import com.migge.supershoppercartapp.ui.screens.WelcomeScreen
import com.migge.supershoppercartapp.ui.theme.SuperShopperCartAppTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    enum class AppStep { INTRO, AUTH_CHECK, MAIN, GREEN }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SuperShopperCartAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFADD8E6)
                ) {
                    var appStep by remember { mutableStateOf(AppStep.INTRO) }
                    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
                    val scope = rememberCoroutineScope()

                    AnimatedContent(
                        targetState = appStep,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "AppStepTransition"
                    ) { step ->
                        when (step) {
                            AppStep.INTRO -> WelcomeScreen(
                                onFinished = { appStep = AppStep.AUTH_CHECK }
                            )

                            AppStep.AUTH_CHECK -> {
                                LaunchedEffect(Unit) {
                                    scope.launch {
                                        val startTime = System.currentTimeMillis()
                                        val loggedIn = tokenManager.isLoggedIn()
                                        val elapsed = System.currentTimeMillis() - startTime
                                        val remaining = 1000 - elapsed
                                        if (remaining > 0) delay(remaining)
                                        isLoggedIn = loggedIn
                                        appStep = AppStep.MAIN
                                    }
                                }
                                SplashScreen()
                            }

                            AppStep.MAIN -> {
                                when (isLoggedIn) {
                                    true -> GreenScreen()
                                    false -> LoginScreen(
                                        onLoginSuccess = { isLoggedIn = true }
                                    )
                                    else -> SplashScreen()
                                }
                            }

                            AppStep.GREEN -> {
                                GreenScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    // This is just for placeholder, you can replace with your main app UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Welcome to SuperShopperCart!", color = Color.Black)
    }
}