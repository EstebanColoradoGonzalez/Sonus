package com.estebancoloradogonzalez.sonus.core.data.scan

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide holder of the current [ScanState] (channel C2). A single instance is shared between
 * the scan worker (writer) and the presentation layer (reader), so progress is observed consistently
 * across the WorkManager job and the UI.
 */
@Singleton
class ScanStateEmitterImpl
    @Inject
    constructor() : ScanStateEmitter {
        private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
        override val state: StateFlow<ScanState> = _state.asStateFlow()

        override suspend fun update(state: ScanState) {
            _state.value = state
        }
    }
