package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the observable scan state to the presentation layer (`TRG-LIB-04`, channel C2). */
class ObserveScanStateUseCase
    @Inject
    constructor(
        private val scanStateEmitter: ScanStateEmitter,
    ) {
        operator fun invoke(): StateFlow<ScanState> = scanStateEmitter.state
    }
