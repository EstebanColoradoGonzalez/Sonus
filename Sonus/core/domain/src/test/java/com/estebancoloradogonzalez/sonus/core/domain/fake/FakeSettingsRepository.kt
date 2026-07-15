package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.port.SettingsRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult

/**
 * In-memory fake of [SettingsRepository] preserving its contract (fakes over mocks for ports —
 * coding-standards §5.3). Marking is idempotent; [failOnComplete] simulates the storage failure of
 * Escenario 6. Holds no business rule.
 */
class FakeSettingsRepository(
    initialCompleted: Boolean = false,
    private val failOnComplete: Boolean = false,
) : SettingsRepository {
    var onboardingCompleted: Boolean = initialCompleted
        private set

    var completeCallCount: Int = 0
        private set

    override suspend fun completeOnboarding(): OperationResult<Unit> {
        completeCallCount++
        if (failOnComplete) return OperationResult.Failure(DomainError.SettingsPersistenceFailed)
        onboardingCompleted = true
        return OperationResult.Success(Unit)
    }

    override suspend fun isOnboardingCompleted(): Boolean = onboardingCompleted
}
