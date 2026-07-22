package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveScanStateUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RescanLibraryUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanProgressViewModelTest {
    private val observeScanState = mockk<ObserveScanStateUseCase>()
    private val rescanLibrary = mockk<RescanLibraryUseCase>(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(states: MutableStateFlow<ScanState>): ScanProgressViewModel {
        every { observeScanState() } returns states
        return ScanProgressViewModel(observeScanState, rescanLibrary)
    }

    private fun viewModel(state: ScanState): ScanProgressViewModel = viewModel(MutableStateFlow(state))

    private fun summary() = ScanSummary(added = 5, purged = 3, unsupported = 2, orphanDimsPurged = 1)

    @Test
    fun `hides the overlay when idle`() {
        // Arrange + Act (AC1)
        val vm = viewModel(ScanState.Idle)

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.HIDDEN)
    }

    @Test
    fun `projects scanning progress when total is known`() {
        // Arrange + Act (AC3)
        val vm = viewModel(ScanState.Scanning(processed = 12, total = 30))

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.SCANNING)
        assertThat(vm.uiState.value.processed).isEqualTo(12)
        assertThat(vm.uiState.value.total).isEqualTo(30)
    }

    @Test
    fun `projects scanning progress with no denominator when total is unknown`() {
        // Arrange + Act (AC4)
        val vm = viewModel(ScanState.Scanning(processed = 7, total = null))

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.SCANNING)
        assertThat(vm.uiState.value.total).isNull()
    }

    @Test
    fun `shows the syncing phase when the catalog is being reconciled`() {
        // Arrange + Act (AC5)
        val vm = viewModel(ScanState.Syncing)

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.SYNCING)
    }

    @Test
    fun `exposes the four-counter summary when finished`() {
        // Arrange + Act (AC6)
        val vm = viewModel(ScanState.Finished(summary()))

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.FINISHED)
        assertThat(vm.uiState.value.summary)
            .isEqualTo(ScanResultUi(added = 5, purged = 3, unsupported = 2, orphanDimsPurged = 1))
    }

    @Test
    fun `exposes the abort code when aborted`() {
        // Arrange + Act (AC7)
        val vm = viewModel(ScanState.Aborted(DomainError.ScanAborted(DomainError.PermissionRevoked)))

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.ABORTED)
        assertThat(vm.uiState.value.abortCode).isEqualTo("ERR_SCAN_ABORTED")
    }

    @Test
    fun `re-enqueues the scan on retry`() {
        // Arrange (AC7)
        val vm = viewModel(ScanState.Aborted(DomainError.ScanAborted(DomainError.PermissionRevoked)))

        // Act
        vm.onCommand(ScanProgressCommand.Retry)

        // Assert
        verify { rescanLibrary() }
    }

    @Test
    fun `hides the terminal overlay on dismiss`() {
        // Arrange (AC6)
        val vm = viewModel(ScanState.Finished(summary()))

        // Act
        vm.onCommand(ScanProgressCommand.Dismiss)

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.HIDDEN)
    }

    @Test
    fun `presents the overlay again when a new scan starts after dismiss`() {
        // Arrange (AC2): a finished scan was dismissed
        val states = MutableStateFlow<ScanState>(ScanState.Finished(summary()))
        val vm = viewModel(states)
        vm.onCommand(ScanProgressCommand.Dismiss)

        // Act: a new scan begins
        states.value = ScanState.Scanning(processed = 0, total = null)

        // Assert
        assertThat(vm.uiState.value.phase).isEqualTo(ScanProgressPhase.SCANNING)
    }
}
