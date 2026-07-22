package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

/**
 * Immutable UI state of the global scan-progress overlay (US-009), projected from the observed
 * `ScanState` (channel C2, `TRG-LIB-04`). Unlike the foundational [ScanUiState] (US-003), this state
 * is purely observational: it drives a blocking overlay presented over the main navigation for any
 * scan triggered post-onboarding (re-scan US-007, source-folder changes US-005/US-006).
 *
 * [phase] is [ScanProgressPhase.HIDDEN] while no scan is active or after the Listener dismisses a
 * terminal state, so the overlay renders nothing (AC1). [total] is `null` while the file count is
 * still being enumerated (indeterminate progress, AC4); [summary] is present only on [FINISHED]
 * (AC6); [abortCode] carries the stable error code on [ABORTED] (AC7).
 */
data class ScanProgressUiState(
    val phase: ScanProgressPhase = ScanProgressPhase.HIDDEN,
    val processed: Int = 0,
    val total: Int? = null,
    val summary: ScanResultUi? = null,
    val abortCode: String? = null,
)

/** Coarse phase the overlay renders; [HIDDEN] means the overlay is not shown. */
enum class ScanProgressPhase {
    HIDDEN,
    SCANNING,
    SYNCING,
    FINISHED,
    ABORTED,
}

/** Presentation projection of the scan result: the four counters the summary renders (AC6). */
data class ScanResultUi(
    val added: Int,
    val purged: Int,
    val unsupported: Int,
    val orphanDimsPurged: Int,
)
