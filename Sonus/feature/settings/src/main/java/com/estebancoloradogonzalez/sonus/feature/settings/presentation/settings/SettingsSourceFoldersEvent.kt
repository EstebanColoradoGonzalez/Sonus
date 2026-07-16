package com.estebancoloradogonzalez.sonus.feature.settings.presentation.settings

/**
 * One-shot effects emitted by [SettingsSourceFoldersViewModel], consumed once by the screen (never
 * part of the durable [SettingsSourceFoldersUiState], so they do not re-emit on recomposition).
 */
sealed interface SettingsSourceFoldersEvent {
    /** Ask the screen to launch the SAF `OpenDocumentTree` picker. */
    data object LaunchFolderPicker : SettingsSourceFoldersEvent

    /** Non-intrusive confirmation: the folder was registered (AC1). */
    data object NotifyFolderAdded : SettingsSourceFoldersEvent

    /** Non-intrusive notice: the folder was registered but overlaps an existing one (AC4). */
    data object NotifyOverlap : SettingsSourceFoldersEvent

    /** Non-intrusive notice: the chosen folder was already added (AC3). */
    data object NotifyDuplicate : SettingsSourceFoldersEvent

    /** Non-intrusive notice: the OS did not grant access to the folder (AC5). */
    data object NotifyPermissionDenied : SettingsSourceFoldersEvent

    /** Non-intrusive notice: no folder was added because the picker was dismissed (AC5). */
    data object NotifySelectionCancelled : SettingsSourceFoldersEvent
}
