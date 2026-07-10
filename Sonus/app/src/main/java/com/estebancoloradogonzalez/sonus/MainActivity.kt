package com.estebancoloradogonzalez.sonus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.estebancoloradogonzalez.sonus.navigation.SonusNavHost
import com.estebancoloradogonzalez.sonus.ui.theme.SonusTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single hosting Activity for the app (ADR-005); holds the [SonusNavHost] and the Hilt entry point. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SonusNavHost()
                }
            }
        }
    }
}
