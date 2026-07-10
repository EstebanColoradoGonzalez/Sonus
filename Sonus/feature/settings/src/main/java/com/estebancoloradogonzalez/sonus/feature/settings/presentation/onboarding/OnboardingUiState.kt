package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

/**
 * Immutable UI state of the notification-permission onboarding screen.
 *
 * The soft-denial case (Scenario 2) is not a screen: it is a one-shot event followed by navigation,
 * so only [OnboardingPermissionPhase.Rationale] and [OnboardingPermissionPhase.PermanentlyDenied]
 * are rendered here.
 */
data class OnboardingUiState(
    val phase: OnboardingPermissionPhase = OnboardingPermissionPhase.Rationale,
)

sealed interface OnboardingPermissionPhase {
    /** Full-screen rationale explaining why notifications are useful, before the system dialog. */
    data object Rationale : OnboardingPermissionPhase

    /** The system no longer shows the dialog ("don't ask again"): offer a link to system settings. */
    data object PermanentlyDenied : OnboardingPermissionPhase
}
