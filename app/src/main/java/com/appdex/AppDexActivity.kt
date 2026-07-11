package com.appdex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appdex.nav.AppDexNavHost
import com.appdex.ui.theme.AppDexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppDexActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDexTheme {
                AppDexNavHost()
            }
        }
    }
}
