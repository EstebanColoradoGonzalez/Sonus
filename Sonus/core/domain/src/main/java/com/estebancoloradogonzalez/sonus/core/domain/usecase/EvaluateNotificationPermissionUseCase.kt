package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.NotificationPermissionStep
import com.estebancoloradogonzalez.sonus.core.domain.port.NotificationPermissionGateway
import javax.inject.Inject

/**
 * Decides whether the first-run flow must request the notification permission or skip the step.
 *
 * Covers the deterministic branches of US-001: API < 33 (Scenario 3) and already-granted
 * (Scenario 5) both resolve to [NotificationPermissionStep.Skip]; a supported, not-yet-granted
 * device resolves to [NotificationPermissionStep.Request].
 */
class EvaluateNotificationPermissionUseCase
    @Inject
    constructor(
        private val gateway: NotificationPermissionGateway,
    ) {
        operator fun invoke(): NotificationPermissionStep =
            if (!gateway.isSupported() || gateway.isGranted()) {
                NotificationPermissionStep.Skip
            } else {
                NotificationPermissionStep.Request
            }
    }
