package com.estebancoloradogonzalez.sonus.core.domain.port

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode

/**
 * Schedules a library scan as a single background job (ADR-006, `TRG-LIB-03`). The implementation
 * (`:service:indexer`) enforces single-flight (`ExistingWorkPolicy.KEEP`, contract §4.1): a running
 * scan is never duplicated, so the onboarding foundational scan (US-003) and a manual re-scan
 * (US-007) never run in parallel. The chosen [ScanMode] is carried to the worker.
 */
interface ScanScheduler {
    fun enqueueScan(mode: ScanMode)

    fun cancel()
}
