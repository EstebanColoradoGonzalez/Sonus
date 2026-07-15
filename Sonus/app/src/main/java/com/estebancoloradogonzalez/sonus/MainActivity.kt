package com.estebancoloradogonzalez.sonus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.estebancoloradogonzalez.sonus.navigation.SonusAppViewModel
import com.estebancoloradogonzalez.sonus.navigation.SonusNavHost
import com.estebancoloradogonzalez.sonus.navigation.StartDestinationUiState
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
                    SonusApp()
                }
            }
        }
    }
}

/**
 * Gates the navigation graph on the resolved start destination (US-004): the graph is not rendered
 * until `AppSettings.onboardingCompleted` is read, so no onboarding screen flashes before a recurring
 * start settles on the library (Escenario 3). The onboarding is closed on the SCAN → LIBRARY
 * transition via the view model (Escenario 1/4).
 */
@Composable
private fun SonusApp(viewModel: SonusAppViewModel = hiltViewModel()) {
    val startDestination by viewModel.startDestination.collectAsState()
    when (startDestination) {
        StartDestinationUiState.Loading -> Unit
        StartDestinationUiState.Onboarding ->
            SonusNavHost(startAtLibrary = false, onOnboardingCompleted = viewModel::completeOnboarding)
        StartDestinationUiState.Library ->
            SonusNavHost(startAtLibrary = true, onOnboardingCompleted = viewModel::completeOnboarding)
    }
}
