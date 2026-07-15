package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.port.SettingsRepository
import javax.inject.Inject

/**
 * Reports whether the first-run flow was already completed, so the start-up gating can decide the
 * initial destination (Escenario 3/5): `true` → straight to the library, `false` → the onboarding
 * flow. Reads the `AppSettings` singleton once at start-up; holds no business rule.
 */
class IsOnboardingCompletedUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        suspend operator fun invoke(): Boolean = repository.isOnboardingCompleted()
    }
