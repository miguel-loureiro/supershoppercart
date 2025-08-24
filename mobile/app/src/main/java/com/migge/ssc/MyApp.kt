package com.migge.ssc

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.migge.ssc.ui.screen.signin.SignInScreen
import com.migge.ssc.ui.screen.WelcomeScreen

@Composable
fun MyApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                onTimeout = {
                    navController.navigate("signin") {
                        popUpTo("welcome") { inclusive = true } // remove da pilha
                    }
                }
            )
        }
        composable("signin",
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SignInScreen()
        }
    }
}
