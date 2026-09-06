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
        // Before super.onCreate, so the brand mark is the first thing drawn.
        // MapMe should never flash a blank window on the way in.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // The map will eventually run under the status bar, so the interface is
        // edge to edge from the start rather than being retrofitted. Both bars
        // are transparent in both modes; MapMeTheme sets the icon appearance to
        // match whichever face is on.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            MapMeTheme {
                MapMeApp()
            }
        }
    }
}
