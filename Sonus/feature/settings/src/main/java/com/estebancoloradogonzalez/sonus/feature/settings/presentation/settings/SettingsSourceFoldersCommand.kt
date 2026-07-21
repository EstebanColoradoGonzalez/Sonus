package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

/**
 * Intentions the Listener expresses on the post-onboarding Source Folders screen (channel C1).
 *
 * Adding a folder is US-005; removing a folder with cascade purge is US-006 (`TRG-LIB-02`). Scanning
 * (US-007) stays out of scope here.
 */
sealed interface SettingsSourceFoldersCommand {
    /** The Listener tapped "Add folder": open the SAF tree picker. */
    data object AddFolderClicked : SettingsSourceFoldersCommand

    /** The SAF picker returned a chosen directory tree URI. */
    data class FolderPicked(
        val treeUri: String,
    ) : SettingsSourceFoldersCommand

    /** The SAF picker was dismissed without choosing a directory (AC5). */
    data object SelectionCancelled : SettingsSourceFoldersCommand

    /** The Listener tapped "Remove" on a folder: compute impact and raise the confirm dialog (US-006 AC1). */
    data class RemoveFolderClicked(
        val id: Long,
        val displayPath: String,
    ) : SettingsSourceFoldersCommand

    /** The Listener confirmed the destructive removal in the dialog (Invariant 5). */
    data object RemoveFolderConfirmed : SettingsSourceFoldersCommand

    /** The Listener cancelled the confirm dialog without removing (US-006 AC6). */
    data object RemoveFolderDismissed : SettingsSourceFoldersCommand
}
