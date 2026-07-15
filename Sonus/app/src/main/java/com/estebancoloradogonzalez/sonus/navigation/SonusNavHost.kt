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
 * The start destination is fixed to onboarding for now; gating it on
 * `AppSettings.onboardingCompleted` (persisted in Room) is US-004. The library destination is a
 * placeholder standing in for the real library view (EPIC-02+).
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
            ScanScreen(
                onNavigateToLibrary = {
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
