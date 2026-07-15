package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

/**
 * One-shot effects emitted by [ScanViewModel], consumed once by the screen (never part of the
 * durable [ScanUiState], so they are not re-emitted on recomposition).
 */
sealed interface ScanEvent {
    /** Transition automatically to the library once the scan finishes (AC1/AC2). */
    data object NavigateToLibrary : ScanEvent

    /** Non-intrusive notice that the scan was aborted; [code] is the stable error code (AC5). */
    data class NotifyAborted(val code: String) : ScanEvent

    /** Non-intrusive notice that the scan was cancelled by the Listener (AC6). */
    data object NotifyCancelled : ScanEvent
}
