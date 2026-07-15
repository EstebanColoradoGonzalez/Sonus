package com.estebancoloradogonzalez.sonus.core.domain.model

import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError

/**
 * Observable state of the Library Engine scan cycle (interfaces_contract §2.1, `TRG-LIB-04`,
 * channel C2). Emitted continuously so the Listener perceives progress if the scan exceeds 1 s
 * ([RNF-03]).
 */
sealed interface ScanState {
    /** No scan in progress. */
    data object Idle : ScanState

    /**
     * Recursive discovery and metadata extraction. [total] is `null` while the file count is still
     * being enumerated; deterministic progress once known ([RNF-03]).
     */
    data class Scanning(
        val processed: Int,
        val total: Int?,
    ) : ScanState

    /** Catalog write phase (upserts, purge). */
    data object Syncing : ScanState

    /** Scan completed successfully with its [summary]. */
    data class Finished(
        val summary: ScanSummary,
    ) : ScanState

    /** Scan interrupted; [reason] carries the wrapped cause (e.g. revoked permission). */
    data class Aborted(
        val reason: DomainError,
    ) : ScanState
}
