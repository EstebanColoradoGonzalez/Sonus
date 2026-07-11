package com.estebancoloradogonzalez.sonus.feature.settings.presentation.onboarding

/**
 * One-shot effects emitted by [SourceFoldersViewModel], consumed once by the screen (never part of
 * the durable [SourceFoldersUiState], so they are not re-emitted on recomposition).
 */
sealed interface SourceFoldersEvent {
    /** Ask the screen to launch the SAF `OpenDocumentTree` picker. */
    data object LaunchFolderPicker : SourceFoldersEvent

    /** Advance the first-run flow to the foundational scan (US-003). */
    data object NavigateToScan : SourceFoldersEvent

    /** Non-intrusive notice: the chosen folder was already added (AC4). */
    data object NotifyDuplicate : SourceFoldersEvent

    /** Non-intrusive notice: the OS did not grant access to the folder (AC5). */
    data object NotifyPermissionDenied : SourceFoldersEvent

    /** Non-intrusive notice: no folder was added because the picker was dismissed (AC5). */
    data object NotifySelectionCancelled : SourceFoldersEvent
}
