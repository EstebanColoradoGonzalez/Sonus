package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeNotificationPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.model.NotificationPermissionStep
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EvaluateNotificationPermissionUseCaseTest {
    @Test
    fun `returns Skip when notifications are not supported`() {
        // Arrange (Scenario 3 — API < 33)
        val useCase =
            EvaluateNotificationPermissionUseCase(
                FakeNotificationPermissionGateway(supported = false, granted = false),
            )

        // Act
        val step = useCase()

        // Assert
        assertThat(step).isEqualTo(NotificationPermissionStep.Skip)
    }

    @Test
    fun `returns Skip when permission is already granted`() {
        // Arrange (Scenario 5 — idempotence)
        val useCase =
            EvaluateNotificationPermissionUseCase(
                FakeNotificationPermissionGateway(supported = true, granted = true),
            )

        // Act
        val step = useCase()

        // Assert
        assertThat(step).isEqualTo(NotificationPermissionStep.Skip)
    }

    @Test
    fun `returns Request when supported and not granted`() {
        // Arrange (Scenario 1 — first-run request)
        val useCase =
            EvaluateNotificationPermissionUseCase(
                FakeNotificationPermissionGateway(supported = true, granted = false),
            )

        // Act
        val step = useCase()

        // Assert
        assertThat(step).isEqualTo(NotificationPermissionStep.Request)
    }
}
