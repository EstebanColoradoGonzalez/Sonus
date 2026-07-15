package com.estebancoloradogonzalez.sonus.core.domain.port

/**
 * Schedules the foundational scan as a single background job (ADR-006). The implementation
 * (`:service:indexer`) enforces single-flight (`ExistingWorkPolicy.KEEP`, contract §4.1): a running
 * scan is never duplicated.
 */
interface ScanScheduler {
    fun enqueueFullScan()

    fun cancel()
}
