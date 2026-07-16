package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

/**
 * Intentions the Listener expresses on the post-onboarding Source Folders screen (channel C1).
 *
 * Removing a folder (with cascade purge) is US-006 and scanning is US-007, both out of this story's
 * scope: only adding a folder is modelled here.
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
}
