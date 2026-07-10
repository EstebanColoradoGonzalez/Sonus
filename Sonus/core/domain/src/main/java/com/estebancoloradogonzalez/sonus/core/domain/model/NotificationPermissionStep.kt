package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Outcome of evaluating the notification-permission step of the first-run flow.
 *
 * Closed hierarchy so the presentation layer resolves it with an exhaustive `when` (no `else`).
 */
sealed interface NotificationPermissionStep {
    /** Nothing to ask: unsupported API (< 33) or the permission is already granted — advance directly. */
    data object Skip : NotificationPermissionStep

    /** Supported and not granted yet: show the rationale and request the permission. */
    data object Request : NotificationPermissionStep
}
