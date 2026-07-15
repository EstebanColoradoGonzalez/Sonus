package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class IsOnboardingCompletedUseCaseTest {
    @Test
    fun `returns false on a fresh install`() =
        runTest {
            // Arrange (Escenario 5)
            val useCase = IsOnboardingCompletedUseCase(FakeSettingsRepository(initialCompleted = false))

            // Act + Assert
            assertThat(useCase()).isFalse()
        }

    @Test
    fun `returns true once the onboarding was completed`() =
        runTest {
            // Arrange (Escenario 3)
            val useCase = IsOnboardingCompletedUseCase(FakeSettingsRepository(initialCompleted = true))

            // Act + Assert
            assertThat(useCase()).isTrue()
        }
}
