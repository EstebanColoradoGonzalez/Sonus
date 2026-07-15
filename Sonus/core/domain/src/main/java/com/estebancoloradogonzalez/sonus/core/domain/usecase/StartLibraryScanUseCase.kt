package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler
import javax.inject.Inject

/** Triggers the foundational scan as a single background job (`TRG-LIB-03`, ADR-006). */
class StartLibraryScanUseCase
    @Inject
    constructor(
        private val scanScheduler: ScanScheduler,
    ) {
        operator fun invoke() = scanScheduler.enqueueFullScan()
    }
