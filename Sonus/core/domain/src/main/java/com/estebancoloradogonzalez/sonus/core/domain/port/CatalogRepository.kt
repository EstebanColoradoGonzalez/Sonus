package com.estebancoloradogonzalez.sonus.core.domain.port

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult

/**
 * Catalog write port (blueprint C-04, coding-standards §2.1 `Catalog`). Implemented in `:core:data`,
 * where SAF traversal, ID3 extraction and the deterministic Room synchronization live.
 */
interface CatalogRepository {
    /**
     * Recursively scans [sourceFolders], extracts ID3 tags and synchronizes the catalog according to
     * [mode]. Publishes `Scanning`/`Syncing` progress through the scan-state channel while running.
     *
     * Returns [ScanSummary] on success; a `Failure(DomainError.ScanAborted(..))` if a folder loses
     * access mid-scan, preserving the last coherent catalog (§5.3). Absent metadata is represented
     * with the `id = 1` sentinel or `NULL`, never inferred (Invariant 4).
     */
    suspend fun synchronize(
        sourceFolders: List<SourceFolder>,
        mode: ScanMode,
    ): OperationResult<ScanSummary>
}
