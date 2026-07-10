package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

/**
 * One-shot effects emitted by [OnboardingViewModel], consumed once by the screen (never part of the
 * durable [OnboardingUiState], so they are not re-emitted on recomposition — coding-standards §4.2).
 */
sealed interface OnboardingEvent {
    /** Ask the screen to launch the OS runtime permission dialog. */
    data object LaunchSystemPermissionDialog : OnboardingEvent

    /** Advance the first-run flow to the Source Folders selection (US-002). */
    data object NavigateToSourceFolders : OnboardingEvent

    /** Open the system notification settings for this app (permanent-denial recovery). */
    data object OpenNotificationSettings : OnboardingEvent

    /** Non-intrusive notice that background controls stay unavailable until the permission is granted. */
    data object NotifyNotificationsDegraded : OnboardingEvent
}
