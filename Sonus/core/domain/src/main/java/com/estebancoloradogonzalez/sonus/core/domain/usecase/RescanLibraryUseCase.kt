package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler
import javax.inject.Inject

/**
 * Triggers a manual library re-scan requested by the Listener (`TRG-LIB-03`, US-007). Enqueues the
 * scan as a single background job through the [ScanScheduler], which enforces single-flight
 * (`ExistingWorkPolicy.KEEP`): a re-scan requested while one is already running is ignored (AC6),
 * and the UI is never blocked (AC7).
 *
 * The default [ScanMode.INCREMENTAL] is the efficient strategy for habitual re-scans; the real diff
 * by `fileLastModifiedMs` is US-008 (until then it runs a full deterministic, idempotent re-sync).
 */
class RescanLibraryUseCase
    @Inject
    constructor(
        private val scanScheduler: ScanScheduler,
    ) {
        operator fun invoke(mode: ScanMode = ScanMode.INCREMENTAL) = scanScheduler.enqueueScan(mode)
    }
