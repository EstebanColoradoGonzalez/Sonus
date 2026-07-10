package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

import app.cash.turbine.test
import com.estebancoloradogonzalez.sonus.core.domain.model.NotificationPermissionStep
import com.estebancoloradogonzalez.sonus.core.domain.usecase.EvaluateNotificationPermissionUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OnboardingViewModelTest {
    private val evaluate = mockk<EvaluateNotificationPermissionUseCase>()

    private fun viewModel() = OnboardingViewModel(evaluate)

    @Test
    fun `navigates to source folders when step is Skip on EvaluateStep`() =
        runTest {
            // Arrange (Scenarios 3 & 5)
            every { evaluate() } returns NotificationPermissionStep.Skip
            val vm = viewModel()

            // Act + Assert
            vm.events.test {
                vm.onCommand(OnboardingCommand.EvaluateStep)
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToSourceFolders)
            }
        }

    @Test
    fun `keeps rationale and emits nothing when step is Request on EvaluateStep`() =
        runTest {
            // Arrange (Scenario 1 — request path)
            every { evaluate() } returns NotificationPermissionStep.Request
            val vm = viewModel()

            // Act + Assert
            vm.events.test {
                vm.onCommand(OnboardingCommand.EvaluateStep)
                expectNoEvents()
            }
            assertThat(vm.uiState.value.phase).isEqualTo(OnboardingPermissionPhase.Rationale)
        }

    @Test
    fun `emits launch dialog on RequestPermission`() =
        runTest {
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(OnboardingCommand.RequestPermission)
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.LaunchSystemPermissionDialog)
            }
        }

    @Test
    fun `navigates when permission is granted`() =
        runTest {
            // Scenario 1 — granted
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(OnboardingCommand.PermissionResult(granted = true, permanentlyDenied = false))
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToSourceFolders)
            }
        }

    @Test
    fun `notifies degradation and navigates when denied not permanently`() =
        runTest {
            // Scenario 2 — soft denial
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(OnboardingCommand.PermissionResult(granted = false, permanentlyDenied = false))
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.NotifyNotificationsDegraded)
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToSourceFolders)
            }
        }

    @Test
    fun `shows permanently denied phase and emits nothing when denied permanently`() =
        runTest {
            // Scenario 4 — permanent denial
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(OnboardingCommand.PermissionResult(granted = false, permanentlyDenied = true))
                expectNoEvents()
            }
            assertThat(vm.uiState.value.phase).isEqualTo(OnboardingPermissionPhase.PermanentlyDenied)
        }

    @Test
    fun `navigates on Skip`() =
        runTest {
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(OnboardingCommand.Skip)
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.NavigateToSourceFolders)
            }
        }

    @Test
    fun `emits open settings on OpenSettings`() =
        runTest {
            val vm = viewModel()

            vm.events.test {
                vm.onCommand(OnboardingCommand.OpenSettings)
                assertThat(awaitItem()).isEqualTo(OnboardingEvent.OpenNotificationSettings)
            }
        }
}
