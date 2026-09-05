package com.mapme.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mapme.core.design.theme.MapMeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so the ink window and the mark are the first
        // things drawn. MapMe should never flash white on the way in.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // The map will eventually run under the status bar, so the interface
        // is edge to edge from the first commit rather than being retrofitted.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            MapMeTheme {
                MapMeApp()
            }
        }
    }
}
