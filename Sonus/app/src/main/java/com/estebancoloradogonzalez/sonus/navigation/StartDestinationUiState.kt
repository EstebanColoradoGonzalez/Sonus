package com.estebancoloradogonzalez.sonus.navigation

/**
 * Resolution of the start-up destination gating (US-004). While [Loading] the graph is not rendered;
 * once the `AppSettings.onboardingCompleted` switch is read it settles into [Onboarding] (first run,
 * Escenario 5) or [Library] (recurring start, Escenario 3).
 */
sealed interface StartDestinationUiState {
    data object Loading : StartDestinationUiState

    data object Onboarding : StartDestinationUiState

    data object Library : StartDestinationUiState
}
