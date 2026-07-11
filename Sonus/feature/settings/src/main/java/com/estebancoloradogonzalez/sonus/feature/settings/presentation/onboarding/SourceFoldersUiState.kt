package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

/**
 * Immutable UI state of the Source Folders onboarding screen (US-002).
 *
 * [canContinue] gates the transition to scanning: it is true only when at least one folder is
 * registered (AC3 / SDD §4.1 Prerrequisito 4).
 */
data class SourceFoldersUiState(
    val folders: List<SourceFolderUi> = emptyList(),
    val canContinue: Boolean = false,
)

/** Presentation projection of a registered folder: only what the list renders. */
data class SourceFolderUi(
    val id: Long,
    val displayPath: String,
)
