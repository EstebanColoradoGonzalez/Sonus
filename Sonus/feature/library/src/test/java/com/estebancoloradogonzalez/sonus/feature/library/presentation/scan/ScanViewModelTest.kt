package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

import app.cash.turbine.test
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.usecase.CancelLibraryScanUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveScanStateUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.StartLibraryScanUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val observeScanState = mockk<ObserveScanStateUseCase>()
    private val startLibraryScan = mockk<StartLibraryScanUseCase>(relaxed = true)
    private val cancelLibraryScan = mockk<CancelLibraryScanUseCase>(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(state: ScanState): ScanViewModel {
        every { observeScanState() } returns MutableStateFlow(state)
        return ScanViewModel(observeScanState, startLibraryScan, cancelLibraryScan)
    }

    private fun summary() = ScanSummary(added = 5, purged = 0, unsupported = 2, orphanDimsPurged = 1)

    @Test
    fun `starts the scan on creation`() {
        // Arrange + Act (AC1)
        viewModel(ScanState.Idle)

        // Assert
        verify { startLibraryScan() }
    }

    @Test
    fun `projects Scanning progress into the ui state`() {
        // Arrange + Act (AC1)
        val vm = viewModel(ScanState.Scanning(processed = 12, total = 30))

        // Assert
        assertThat(vm.uiState.value.status).isEqualTo(ScanStatus.SCANNING)
        assertThat(vm.uiState.value.processed).isEqualTo(12)
        assertThat(vm.uiState.value.total).isEqualTo(30)
    }

    @Test
    fun `emits NavigateToLibrary and exposes the summary when finished`() =
        runTest {
            // Arrange (AC1/AC2)
            val vm = viewModel(ScanState.Finished(summary()))

            // Act + Assert
            vm.events.test {
                assertThat(awaitItem()).isEqualTo(ScanEvent.NavigateToLibrary)
            }
            assertThat(vm.uiState.value.status).isEqualTo(ScanStatus.FINISHED)
            assertThat(
                vm.uiState.value.summary,
            ).isEqualTo(ScanSummaryUi(added = 5, unsupported = 2, orphanDimsPurged = 1))
        }

    @Test
    fun `emits NotifyAborted with the error code when aborted`() =
        runTest {
            // Arrange (AC5)
            val vm = viewModel(ScanState.Aborted(DomainError.ScanAborted(DomainError.PermissionRevoked)))

            // Act + Assert
            vm.events.test {
                assertThat(awaitItem()).isEqualTo(ScanEvent.NotifyAborted("ERR_SCAN_ABORTED"))
            }
            assertThat(vm.uiState.value.status).isEqualTo(ScanStatus.ABORTED)
        }

    @Test
    fun `cancels the scan and notifies on CancelScan`() =
        runTest {
            // Arrange (AC6)
            val vm = viewModel(ScanState.Scanning(processed = 1, total = null))

            // Act + Assert
            vm.events.test {
                vm.onCommand(ScanCommand.CancelScan)
                assertThat(awaitItem()).isEqualTo(ScanEvent.NotifyCancelled)
            }
            verify { cancelLibraryScan() }
        }
}
