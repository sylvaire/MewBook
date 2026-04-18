package com.mewbook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mewbook.app.ui.theme.SplashBackgroundDark
import com.mewbook.app.ui.navigation.MewBookNavHost
import com.mewbook.app.ui.theme.MewBookTheme
import com.mewbook.app.ui.theme.SplashBackground
import com.mewbook.app.data.preferences.AppThemeMode
import com.mewbook.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDarkTheme
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            var showCustomSplash by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(550)
                showCustomSplash = false
            }

            MewBookTheme(
                darkTheme = isDarkTheme,
                systemBarColorOverride = if (showCustomSplash) {
                    if (isDarkTheme) SplashBackgroundDark else SplashBackground
                } else {
                    null
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MewBookNavHost()
                    }

                    if (showCustomSplash) {
                        SquareSplashScreen(isDarkTheme = isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareSplashScreen(isDarkTheme: Boolean) {
    val splashBackground = if (isDarkTheme) SplashBackgroundDark else SplashBackground
    val splashImageRes = if (isDarkTheme) R.drawable.ic_splash_logo_dark else R.drawable.ic_splash_logo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(splashImageRes),
            contentDescription = null,
            modifier = Modifier.size(220.dp)
        )
    }
}
