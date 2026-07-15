package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler
import javax.inject.Inject

/** Cancels the running scan cleanly, preserving the partially built catalog (AC6). */
class CancelLibraryScanUseCase
    @Inject
    constructor(
        private val scanScheduler: ScanScheduler,
    ) {
        operator fun invoke() = scanScheduler.cancel()
    }
