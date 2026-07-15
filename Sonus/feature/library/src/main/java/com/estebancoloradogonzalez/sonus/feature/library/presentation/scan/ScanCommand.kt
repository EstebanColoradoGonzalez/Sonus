package com.estebancoloradogonzalez.sonus.feature.library.presentation.scan

/** Listener intentions on the scan screen (channel C1). The scan starts automatically on entry. */
sealed interface ScanCommand {
    /** Stop the running scan cleanly, preserving the partial catalog (AC6). */
    data object CancelScan : ScanCommand

    /** Re-run the scan after an abort or cancellation (AC5/AC6). */
    data object RetryScan : ScanCommand
}
