package com.migge.supershopper.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.migge.supershopper.screen.HomeScreen
import com.migge.supershopper.screen.SignInScreen
import com.migge.supershopper.screen.WelcomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen {
                navController.navigate("signin") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }
        composable("signin") {
            SignInScreen(
                onNavigateToHome = { // Now explicitly passing the onNavigateToHome parameter
                    navController.navigate("home") {
                        popUpTo("signin") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}