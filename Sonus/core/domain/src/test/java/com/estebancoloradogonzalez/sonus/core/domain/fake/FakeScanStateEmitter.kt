package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory fake of [ScanStateEmitter] recording every emission for inspection. */
class FakeScanStateEmitter : ScanStateEmitter {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    override val state: StateFlow<ScanState> = _state

    val emissions = mutableListOf<ScanState>()

    override suspend fun update(state: ScanState) {
        emissions.add(state)
        _state.value = state
    }
}
