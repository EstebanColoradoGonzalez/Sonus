package com.estebancoloradogonzalez.sonus.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.estebancoloradogonzalez.sonus.feature.library.presentation.LibraryLandingScreen
import com.estebancoloradogonzalez.sonus.feature.library.presentation.scan.ScanScreen
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding.NotificationPermissionScreen
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding.SourceFoldersScreen

/** Route identifiers for the Single-Activity navigation graph. */
private object SonusRoute {
    const val ONBOARDING = "onboarding"
    const val SOURCE_FOLDERS = "source_folders"
    const val SCAN = "scan"
    const val LIBRARY = "library"
}

/**
 * Single-Activity navigation graph (ADR-005). Starts on the notification-permission onboarding
 * (US-001), advances to the Source Folders selection (US-002), then to the foundational scan
 * (US-003) and finally to the library.
 *
 * The start destination is gated on `AppSettings.onboardingCompleted` (US-004), resolved upstream in
 * [SonusAppViewModel]: on a recurring start [startAtLibrary] is `true` and the graph opens directly
 * on the library, omitting the onboarding steps (Escenario 3). [onOnboardingCompleted] fires once on
 * the SCAN → LIBRARY transition — the single funnel shared by both US-003 branches (Escenario 1/4) —
 * to close the first-run flow. The library destination is a placeholder for the real library view
 * (EPIC-02+).
 */
@Composable
fun SonusNavHost(
    startAtLibrary: Boolean,
    onOnboardingCompleted: () -> Unit,
) {
    val navController = rememberNavController()
    val startDestination = if (startAtLibrary) SonusRoute.LIBRARY else SonusRoute.ONBOARDING
    NavHost(navController = navController, startDestination = startDestination) {
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
            ScanScreen(
                onNavigateToLibrary = {
                    onOnboardingCompleted()
                    navController.navigate(SonusRoute.LIBRARY) {
                        popUpTo(SonusRoute.SCAN) { inclusive = true }
                    }
                },
                onNavigateBackToSourceFolders = {
                    navController.navigate(SonusRoute.SOURCE_FOLDERS) {
                        popUpTo(SonusRoute.SCAN) { inclusive = true }
                    }
                },
            )
        }
        composable(SonusRoute.LIBRARY) {
            LibraryLandingScreen()
        }
    }
}
