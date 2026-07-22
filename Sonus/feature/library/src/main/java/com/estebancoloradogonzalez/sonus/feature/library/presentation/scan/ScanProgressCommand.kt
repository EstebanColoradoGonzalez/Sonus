package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

/**
 * Listener intentions on the global scan-progress overlay (US-009, channel C1). The overlay never
 * starts a scan (it only observes); it merely re-triggers or dismisses after a terminal state.
 */
sealed interface ScanProgressCommand {
    /** Re-run the scan after an abort (AC7). Delegates to the re-scan use case. */
    data object Retry : ScanProgressCommand

    /** Dismiss a terminal overlay (`Finished`/`Aborted`) so it hides until the next scan (AC6). */
    data object Dismiss : ScanProgressCommand
}
