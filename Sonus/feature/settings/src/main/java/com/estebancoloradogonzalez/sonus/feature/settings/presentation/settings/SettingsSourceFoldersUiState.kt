package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

/**
 * Immutable UI state of the post-onboarding Source Folders management screen (US-005).
 *
 * [hasPendingScanContent] turns true after the Listener adds a folder in this session: the register
 * does not trigger a scan (AC6), so the screen surfaces a non-intrusive "content pending to scan"
 * notice, offering to start the re-scan explicitly (delegated to US-007).
 */
data class SettingsSourceFoldersUiState(
    val folders: List<SourceFolderUi> = emptyList(),
    val hasPendingScanContent: Boolean = false,
)

/** Presentation projection of a registered folder: only what the list renders. */
data class SourceFolderUi(
    val id: Long,
    val displayPath: String,
)
