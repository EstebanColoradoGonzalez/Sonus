package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeScanScheduler
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CancelLibraryScanUseCaseTest {
    @Test
    fun `cancels the scan on the scheduler`() {
        // Arrange (AC6)
        val scheduler = FakeScanScheduler()
        val useCase = CancelLibraryScanUseCase(scheduler)

        // Act
        useCase()

        // Assert
        assertThat(scheduler.cancelCount).isEqualTo(1)
        assertThat(scheduler.enqueueCount).isEqualTo(0)
    }
}
