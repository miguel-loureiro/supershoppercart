package com.migge.supershoppercart.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.migge.supershoppercart.ui.screen.WelcomeScreen
import com.migge.supershoppercart.ui.screen.signin.SignInScreen

/**
 * Gerencia a navegação entre as telas principais do app.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.WELCOME
    ) {
        composable(Destinations.WELCOME) {
            WelcomeScreen(
                onTimeout = {
                    navController.navigate(Destinations.SIGN_IN) {
                        popUpTo(Destinations.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Destinations.SIGN_IN,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SignInScreen(navController = navController)
        }

        composable(Destinations.SUCCESS) {
            SuccessScreen()
        }
    }
}

@Composable
fun SuccessScreen() {
    TODO("Not yet implemented")
}

/**
 * Centraliza as rotas da navegação para evitar erros de digitação.
 */
object Destinations {
    const val WELCOME = "welcome"
    const val SIGN_IN = "signin"
    const val SUCCESS = "success"
}