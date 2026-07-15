package com.estebancoloradogonzalez.sonus.core.domain.port

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import kotlinx.coroutines.flow.StateFlow

/**
 * Observable scan-state channel (interfaces_contract §2.1, `TRG-LIB-04`, channel C2). A single
 * shared instance is written by the Library Engine (data/service) and observed by the presentation
 * layer, so progress survives across the worker and the UI.
 */
interface ScanStateEmitter {
    val state: StateFlow<ScanState>

    suspend fun update(state: ScanState)
}
