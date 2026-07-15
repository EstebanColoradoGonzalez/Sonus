package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.SettingsCommand
import com.estebancoloradogonzalez.sonus.core.domain.port.SettingsRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import javax.inject.Inject

/**
 * Closes the first-run flow marking the onboarding as completed (`TRG-CFG-02`, Apalancamiento 5).
 *
 * A thin orchestration over [SettingsRepository]: the idempotency (Escenario 2) and the graceful
 * degradation on a storage failure (Escenario 6, mapped to [OperationResult.Failure]) live in the
 * repository. Consumers treat the result fire-and-forget, so the library transition never blocks.
 */
class CompleteOnboardingUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        suspend operator fun invoke(command: SettingsCommand.CompleteOnboarding): OperationResult<Unit> =
            repository.completeOnboarding()
    }
