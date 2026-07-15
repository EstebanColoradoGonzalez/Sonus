package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

/**
 * Immutable UI state of the foundational scan screen (US-003), projected from `ScanState`
 * (channel C2). [total] is `null` while the file count is still being enumerated (indeterminate
 * progress); [summary] is present only once the scan finishes.
 */
data class ScanUiState(
    val status: ScanStatus = ScanStatus.STARTING,
    val processed: Int = 0,
    val total: Int? = null,
    val summary: ScanSummaryUi? = null,
)

/** Coarse phase of the scan cycle as the screen renders it. */
enum class ScanStatus {
    STARTING,
    SCANNING,
    SYNCING,
    FINISHED,
    ABORTED,
}

/** Presentation projection of the scan result: only the counts the summary renders. */
data class ScanSummaryUi(
    val added: Int,
    val unsupported: Int,
    val orphanDimsPurged: Int,
)
