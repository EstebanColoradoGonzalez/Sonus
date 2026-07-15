package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.usecase.CancelLibraryScanUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveScanStateUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.StartLibraryScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the foundational scan screen (US-003). Starts the scan on creation and projects the
 * observed `ScanState` (channel C2) into [ScanUiState]; on `Finished` it emits the automatic
 * transition to the library, on `Aborted` a non-intrusive notice. Holds no business rule nor I/O.
 */
@HiltViewModel
class ScanViewModel
    @Inject
    constructor(
        observeScanState: ObserveScanStateUseCase,
        private val startLibraryScan: StartLibraryScanUseCase,
        private val cancelLibraryScan: CancelLibraryScanUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ScanUiState())
        val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

        private val _events = Channel<ScanEvent>(Channel.BUFFERED)
        val events: Flow<ScanEvent> = _events.receiveAsFlow()

        init {
            startLibraryScan()
            viewModelScope.launch {
                observeScanState().collect { state -> reduce(state) }
            }
        }

        fun onCommand(command: ScanCommand) =
            when (command) {
                ScanCommand.CancelScan -> cancelScan()
                ScanCommand.RetryScan -> startLibraryScan()
            }

        private fun cancelScan() {
            cancelLibraryScan()
            _events.trySend(ScanEvent.NotifyCancelled)
        }

        private fun reduce(state: ScanState) {
            when (state) {
                ScanState.Idle ->
                    _uiState.update { it.copy(status = ScanStatus.STARTING) }
                is ScanState.Scanning ->
                    _uiState.update {
                        it.copy(
                            status = ScanStatus.SCANNING,
                            processed = state.processed,
                            total = state.total,
                        )
                    }
                ScanState.Syncing ->
                    _uiState.update { it.copy(status = ScanStatus.SYNCING) }
                is ScanState.Finished -> {
                    _uiState.update {
                        it.copy(
                            status = ScanStatus.FINISHED,
                            summary =
                                ScanSummaryUi(
                                    added = state.summary.added,
                                    unsupported = state.summary.unsupported,
                                    orphanDimsPurged = state.summary.orphanDimsPurged,
                                ),
                        )
                    }
                    _events.trySend(ScanEvent.NavigateToLibrary)
                }
                is ScanState.Aborted -> {
                    _uiState.update { it.copy(status = ScanStatus.ABORTED) }
                    _events.trySend(ScanEvent.NotifyAborted(state.reason.code))
                }
            }
        }
    }
