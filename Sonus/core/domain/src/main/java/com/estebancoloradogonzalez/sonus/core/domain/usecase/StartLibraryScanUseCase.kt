package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler
import javax.inject.Inject

/**
 * Triggers the foundational first-run scan as a single background job (`TRG-LIB-03`, ADR-006). The
 * catalog is empty on onboarding, so the mode is always [ScanMode.FULL]; the manual re-scan is
 * [RescanLibraryUseCase] (US-007).
 */
class StartLibraryScanUseCase
    @Inject
    constructor(
        private val scanScheduler: ScanScheduler,
    ) {
        operator fun invoke() = scanScheduler.enqueueScan(ScanMode.FULL)
    }
