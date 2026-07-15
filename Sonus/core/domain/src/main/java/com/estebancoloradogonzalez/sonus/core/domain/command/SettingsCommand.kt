package com.estebancoloradogonzalez.sonus.core.domain.command

/**
 * System-configuration intentions of the Listener (channel C1). Mirror of the `TRG-CFG-*` triggers
 * (interfaces_contract §2.6). Only the subtypes used so far are declared; `SetTheme` (`TRG-CFG-01`)
 * arrives with US-039.
 */
sealed interface SettingsCommand {
    /**
     * Close the first-run flow marking `AppSettings.onboardingCompleted = true` (`TRG-CFG-02`).
     * Carries no parameters and is idempotent: re-emitting it once already completed is a no-op.
     */
    data object CompleteOnboarding : SettingsCommand
}
