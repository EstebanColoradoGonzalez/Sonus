package com.estebancoloradogonzalez.sonus.core.domain.command

import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode

/**
 * Library-management intentions of the Listener (channel C1). Mirror of the `TRG-LIB-*` triggers
 * (interfaces_contract §2.1). Only the subtypes used so far are declared.
 *
 * Note: `AddSourceFolder` carries only the `treeUri`; the human-readable `displayPath` is derived in
 * the data layer via SAF (contract §1.1 keeps SAF out of presentation), not passed from the View.
 */
sealed interface LibraryCommand {
    /** Register a directory as a source folder (`TRG-LIB-01`). */
    data class AddSourceFolder(
        val treeUri: String,
    ) : LibraryCommand

    /** Remove a source folder before scanning — light removal, no cascade yet (`TRG-LIB-02`). */
    data class RemoveSourceFolder(
        val folderId: FolderId,
    ) : LibraryCommand

    /** Run a library scan (`TRG-LIB-03`); the foundational first-run scan uses [ScanMode.FULL]. */
    data class Scan(
        val mode: ScanMode = ScanMode.INCREMENTAL,
    ) : LibraryCommand
}
