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

    /** Non-intrusive confirmation: the folder and its tracks were removed in cascade (US-006 AC1). */
    data object NotifyFolderRemoved : SettingsSourceFoldersEvent

    /** Non-intrusive notice: the folder was already gone when confirming (US-006 AC9, `ERR_ENTITY_NOT_FOUND`). */
    data object NotifyRemoveFailed : SettingsSourceFoldersEvent

    /** Non-intrusive confirmation: a manual re-scan was enqueued in the background (US-007 AC1/AC7). */
    data object NotifyRescanStarted : SettingsSourceFoldersEvent

    /** Non-intrusive notice: there are no source folders to scan, so the re-scan was not enqueued (US-007). */
    data object NotifyNoFoldersToScan : SettingsSourceFoldersEvent
}
