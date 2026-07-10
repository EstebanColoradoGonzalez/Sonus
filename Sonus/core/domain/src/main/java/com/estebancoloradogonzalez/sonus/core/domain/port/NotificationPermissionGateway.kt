package com.estebancoloradogonzalez.sonus.core.domain.port

/**
 * Port toward the operating system's notification permission model.
 *
 * Kept as a domain contract so the onboarding logic stays pure (Clean Architecture); the Android
 * implementation lives in `:core:data`. Authorization is delegated entirely to the OS
 * (interfaces_contract §1.2): there is no application-level authentication.
 */
interface NotificationPermissionGateway {
    /**
     * Whether the runtime `POST_NOTIFICATIONS` permission exists on this device (Android 13 / API 33+).
     * On lower APIs notifications are granted implicitly and no dialog is shown.
     */
    fun isSupported(): Boolean

    /** Whether the notification permission is currently granted (always true when unsupported). */
    fun isGranted(): Boolean
}
