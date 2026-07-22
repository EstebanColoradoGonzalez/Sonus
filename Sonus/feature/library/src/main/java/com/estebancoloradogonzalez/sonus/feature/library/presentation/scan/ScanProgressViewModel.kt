package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveScanStateUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.RescanLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the global scan-progress overlay (US-009). Purely observational: it projects the observed
 * `ScanState` (channel C2, `TRG-LIB-04`) into [ScanProgressUiState] and never starts a scan — the
 * scan is triggered elsewhere (re-scan US-007, folder changes US-005/US-006). Holds no business rule
 * nor I/O; the only action it delegates is the abort recovery re-scan (AC7).
 *
 * The scan-state channel retains its terminal value (`Finished`/`Aborted` are not reset to `Idle`),
 * so a [ScanProgressCommand.Dismiss] latches [dismissed] to hide the terminal overlay; the latch is
 * released when a fresh `Scanning` arrives, so a later scan presents the overlay again (AC2).
 */
@HiltViewModel
class ScanProgressViewModel
    @Inject
    constructor(
        observeScanState: ObserveScanStateUseCase,
        private val rescanLibrary: RescanLibraryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ScanProgressUiState())
        val uiState: StateFlow<ScanProgressUiState> = _uiState.asStateFlow()

        private var dismissed = false

        init {
            viewModelScope.launch {
                observeScanState().collect { state -> reduce(state) }
            }
        }

        fun onCommand(command: ScanProgressCommand) =
            when (command) {
                ScanProgressCommand.Retry -> retry()
                ScanProgressCommand.Dismiss -> dismiss()
            }

        private fun retry() {
            dismissed = true
            _uiState.update { it.copy(phase = ScanProgressPhase.HIDDEN) }
            rescanLibrary()
        }

        private fun dismiss() {
            dismissed = true
            _uiState.update { it.copy(phase = ScanProgressPhase.HIDDEN) }
        }

        private fun reduce(state: ScanState) {
            when (state) {
                ScanState.Idle ->
                    _uiState.update { it.copy(phase = ScanProgressPhase.HIDDEN) }
                is ScanState.Scanning -> {
                    dismissed = false
                    _uiState.update {
                        it.copy(
                            phase = ScanProgressPhase.SCANNING,
                            processed = state.processed,
                            total = state.total,
                            summary = null,
                            abortCode = null,
                        )
                    }
                }
                ScanState.Syncing ->
                    if (!dismissed) {
                        _uiState.update { it.copy(phase = ScanProgressPhase.SYNCING) }
                    }
                is ScanState.Finished ->
                    if (!dismissed) {
                        _uiState.update {
                            it.copy(
                                phase = ScanProgressPhase.FINISHED,
                                summary =
                                    ScanResultUi(
                                        added = state.summary.added,
                                        purged = state.summary.purged,
                                        unsupported = state.summary.unsupported,
                                        orphanDimsPurged = state.summary.orphanDimsPurged,
                                    ),
                            )
                        }
                    }
                is ScanState.Aborted ->
                    if (!dismissed) {
                        _uiState.update {
                            it.copy(
                                phase = ScanProgressPhase.ABORTED,
                                abortCode = state.reason.code,
                            )
                        }
                    }
            }
        }
    }
