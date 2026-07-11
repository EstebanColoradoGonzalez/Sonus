package com.estebancoloradogonzalez.sonus.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.estebancoloradogonzalez.sonus.R
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding.NotificationPermissionScreen
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding.SourceFoldersScreen

/** Route identifiers for the Single-Activity navigation graph. */
private object SonusRoute {
    const val ONBOARDING = "onboarding"
    const val SOURCE_FOLDERS = "source_folders"
    const val SCAN = "scan"
}

/**
 * Single-Activity navigation graph (ADR-005). Starts on the notification-permission onboarding
 * (US-001), advances to the Source Folders selection (US-002) and then to the foundational scan.
 *
 * The start destination is fixed to onboarding for now; gating it on
 * `AppSettings.onboardingCompleted` (persisted in Room) is US-004. The scan destination is a
 * temporary placeholder that US-003 will replace.
 */
@Composable
fun SonusNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SonusRoute.ONBOARDING) {
        composable(SonusRoute.ONBOARDING) {
            NotificationPermissionScreen(
                onNavigateToSourceFolders = {
                    navController.navigate(SonusRoute.SOURCE_FOLDERS) {
                        popUpTo(SonusRoute.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(SonusRoute.SOURCE_FOLDERS) {
            SourceFoldersScreen(
                onNavigateToScan = {
                    navController.navigate(SonusRoute.SCAN) {
                        popUpTo(SonusRoute.SOURCE_FOLDERS) { inclusive = true }
                    }
                },
            )
        }
        composable(SonusRoute.SCAN) {
            ScanPlaceholderScreen()
        }
    }
}

/** Temporary destination standing in for the foundational scan until US-003 is implemented. */
@Composable
private fun ScanPlaceholderScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.scan_placeholder),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
