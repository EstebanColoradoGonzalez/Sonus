package com.estebancoloradogonzalez.sonus.navigation

import com.estebancoloradogonzalez.sonus.core.domain.command.SettingsCommand
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.CompleteOnboardingUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.IsOnboardingCompletedUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SonusAppViewModelTest {
    private val isOnboardingCompleted = mockk<IsOnboardingCompletedUseCase>()
    private val completeOnboarding = mockk<CompleteOnboardingUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SonusAppViewModel(isOnboardingCompleted, completeOnboarding)

    @Test
    fun `resolves the onboarding destination on a fresh install`() =
        runTest {
            // Arrange (Escenario 5)
            coEvery { isOnboardingCompleted() } returns false

            // Act
            val vm = viewModel()

            // Assert
            assertThat(vm.startDestination.value).isEqualTo(StartDestinationUiState.Onboarding)
        }

    @Test
    fun `resolves the library destination when the onboarding was completed`() =
        runTest {
            // Arrange (Escenario 3)
            coEvery { isOnboardingCompleted() } returns true

            // Act
            val vm = viewModel()

            // Assert
            assertThat(vm.startDestination.value).isEqualTo(StartDestinationUiState.Library)
        }

    @Test
    fun `completeOnboarding emits the CompleteOnboarding command`() =
        runTest {
            // Arrange (Escenario 1/4)
            coEvery { isOnboardingCompleted() } returns false
            coEvery { completeOnboarding(SettingsCommand.CompleteOnboarding) } returns OperationResult.Success(Unit)
            val vm = viewModel()

            // Act
            vm.completeOnboarding()

            // Assert
            coVerify { completeOnboarding(SettingsCommand.CompleteOnboarding) }
        }
}
