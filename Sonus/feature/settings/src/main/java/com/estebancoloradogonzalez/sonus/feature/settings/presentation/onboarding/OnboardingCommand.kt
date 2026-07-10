package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

/**
 * Intentions the Listener expresses on the onboarding screen (UI-interaction channel C1).
 *
 * These are presentation-level interactions, not domain business commands: there is no `TRG-*`
 * trigger for "tap Allow", so they live in the feature module rather than `:core:domain`.
 */
sealed interface OnboardingCommand {
    /** Entry point: decide whether to request the permission or skip the step. */
    data object EvaluateStep : OnboardingCommand

    /** The Listener tapped "Allow notifications". */
    data object RequestPermission : OnboardingCommand

    /** The Listener chose to continue without granting ("Skip for now"). */
    data object Skip : OnboardingCommand

    /**
     * Outcome of the OS permission dialog reported back by the screen.
     *
     * @param granted whether the permission was granted.
     * @param permanentlyDenied true when the OS will no longer show the dialog ("don't ask again").
     */
    data class PermissionResult(
        val granted: Boolean,
        val permanentlyDenied: Boolean,
    ) : OnboardingCommand

    /** The Listener tapped "Open settings" from the permanent-denial state. */
    data object OpenSettings : OnboardingCommand
}
