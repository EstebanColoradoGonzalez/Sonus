package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.port.NotificationPermissionGateway

/**
 * In-memory fake of [NotificationPermissionGateway] preserving its contract (fakes over mocks for
 * ports — coding-standards §5.3). No business rule lives here; it only exposes inspectable state.
 */
class FakeNotificationPermissionGateway(
    private val supported: Boolean = true,
    private val granted: Boolean = false,
) : NotificationPermissionGateway {
    override fun isSupported(): Boolean = supported

    override fun isGranted(): Boolean = granted
}
