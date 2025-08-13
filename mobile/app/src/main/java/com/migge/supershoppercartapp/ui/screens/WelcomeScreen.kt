package com.migge.supershoppercartapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.migge.supershoppercartapp.R
import com.migge.supershoppercartapp.ui.theme.SuperShopperCartAppTheme
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onFinished: (() -> Unit)? = null // Callback for auto-navigation
) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300) // delay before showing logo
        logoVisible = true
        delay(300) // delay before showing text
        textVisible = true
        delay(2500) // wait after animations
        onFinished?.invoke()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.outline_shopping_cart_24),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SuperShopperCart App",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                    Text(
                        text = "2025",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    SuperShopperCartAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFADD8E6) // Light Blue background
        ) {
            WelcomeScreen()
        }
    }
}
