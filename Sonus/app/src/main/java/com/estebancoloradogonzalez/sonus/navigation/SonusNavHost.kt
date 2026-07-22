package com.estebancoloradogonzalez.sonus.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.estebancoloradogonzalez.sonus.feature.library.presentation.LibraryLandingScreen
import com.estebancoloradogonzalez.sonus.feature.library.presentation.scan.ScanProgressOverlay
import com.estebancoloradogonzalez.sonus.feature.library.presentation.scan.ScanScreen
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding.NotificationPermissionScreen
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding.SourceFoldersScreen
import com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings.SettingsSourceFoldersScreen

/** Route identifiers for the Single-Activity navigation graph. */
private object SonusRoute {
    const val ONBOARDING = "onboarding"
    const val SOURCE_FOLDERS = "source_folders"
    const val SCAN = "scan"
    const val LIBRARY = "library"
    const val SETTINGS_SOURCE_FOLDERS = "settings_source_folders"
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
 *
 * The graph is wrapped in a [Box] so the global scan-progress overlay (US-009) can be presented on
 * top of the main-app destinations (`LIBRARY`/`SETTINGS_SOURCE_FOLDERS`), blocking navigation while a
 * post-onboarding scan runs; it is gated out of the onboarding routes so it never doubles up with the
 * foundational scan route (US-003).
 */
@Composable
fun SonusNavHost(
    startAtLibrary: Boolean,
    onOnboardingCompleted: () -> Unit,
) {
    val navController = rememberNavController()
    val startDestination = if (startAtLibrary) SonusRoute.LIBRARY else SonusRoute.ONBOARDING
    Box(modifier = Modifier.fillMaxSize()) {
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
                LibraryLandingScreen(
                    onNavigateToSourceFolders = {
                        navController.navigate(SonusRoute.SETTINGS_SOURCE_FOLDERS)
                    },
                )
            }
            composable(SonusRoute.SETTINGS_SOURCE_FOLDERS) {
                SettingsSourceFoldersScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
        MainScanProgressOverlay(navController = navController)
    }
}

/**
 * Global scan-progress overlay (US-009): observes the scan-state channel and blocks the main
 * navigation while any post-onboarding scan runs. Gated to the main-app routes so it never collides
 * with the foundational scan route (US-003), which owns progress during onboarding.
 */
@Composable
private fun MainScanProgressOverlay(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    if (currentRoute != SonusRoute.LIBRARY && currentRoute != SonusRoute.SETTINGS_SOURCE_FOLDERS) {
        return
    }
    ScanProgressOverlay(
        onGoToLibrary = {
            navController.navigate(SonusRoute.LIBRARY) {
                popUpTo(SonusRoute.LIBRARY) { inclusive = true }
                launchSingleTop = true
            }
        },
        onConfigureFolders = {
            navController.navigate(SonusRoute.SETTINGS_SOURCE_FOLDERS) {
                launchSingleTop = true
            }
        },
    )
}
