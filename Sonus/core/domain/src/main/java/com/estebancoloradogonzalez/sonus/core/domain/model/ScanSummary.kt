package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Outcome of a completed scan (interfaces_contract §2.1, `TRG-LIB-03`). Reported to the Listener as
 * the closing summary of the Scan Cycle (SDD §1.3).
 */
data class ScanSummary(
    val added: Int,
    val purged: Int,
    val unsupported: Int,
    val orphanDimsPurged: Int,
)
