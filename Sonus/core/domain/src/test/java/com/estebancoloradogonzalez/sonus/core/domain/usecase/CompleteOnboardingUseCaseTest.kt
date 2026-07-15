package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.SettingsCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSettingsRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CompleteOnboardingUseCaseTest {
    @Test
    fun `returns Success and marks the onboarding as completed`() =
        runTest {
            // Arrange (Escenario 1)
            val repository = FakeSettingsRepository()
            val useCase = CompleteOnboardingUseCase(repository)

            // Act
            val result = useCase(SettingsCommand.CompleteOnboarding)

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(Unit))
            assertThat(repository.onboardingCompleted).isTrue()
        }

    @Test
    fun `is idempotent - marking again when already completed stays true and succeeds`() =
        runTest {
            // Arrange (Escenario 2)
            val repository = FakeSettingsRepository(initialCompleted = true)
            val useCase = CompleteOnboardingUseCase(repository)

            // Act
            val result = useCase(SettingsCommand.CompleteOnboarding)

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(Unit))
            assertThat(repository.onboardingCompleted).isTrue()
            assertThat(repository.completeCallCount).isEqualTo(1)
        }

    @Test
    fun `propagates Failure when persistence fails`() =
        runTest {
            // Arrange (Escenario 6)
            val repository = FakeSettingsRepository(failOnComplete = true)
            val useCase = CompleteOnboardingUseCase(repository)

            // Act
            val result = useCase(SettingsCommand.CompleteOnboarding)

            // Assert
            assertThat(result).isEqualTo(OperationResult.Failure(DomainError.SettingsPersistenceFailed))
        }
}
