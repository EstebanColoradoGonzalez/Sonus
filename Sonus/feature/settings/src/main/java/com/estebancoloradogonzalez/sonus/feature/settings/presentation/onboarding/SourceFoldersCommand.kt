package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

/**
 * Intentions the Listener expresses on the Source Folders screen (UI-interaction channel C1).
 */
sealed interface SourceFoldersCommand {
    /** The Listener tapped "Add folder": open the SAF tree picker. */
    data object AddFolderClicked : SourceFoldersCommand

    /** The SAF picker returned a chosen directory tree URI. */
    data class FolderPicked(
        val treeUri: String,
    ) : SourceFoldersCommand

    /** The SAF picker was dismissed without choosing a directory (AC5). */
    data object SelectionCancelled : SourceFoldersCommand

    /** The Listener removed a folder from the list before scanning (AC6). */
    data class RemoveFolder(
        val id: Long,
    ) : SourceFoldersCommand

    /** The Listener tapped "Continue" to transit to scanning (AC7). */
    data object ContinueClicked : SourceFoldersCommand
}
