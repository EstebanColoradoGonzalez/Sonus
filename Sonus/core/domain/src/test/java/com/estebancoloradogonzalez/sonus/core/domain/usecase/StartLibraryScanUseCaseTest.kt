package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeScanScheduler
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StartLibraryScanUseCaseTest {
    @Test
    fun `enqueues a scan on the scheduler`() {
        // Arrange (AC1)
        val scheduler = FakeScanScheduler()
        val useCase = StartLibraryScanUseCase(scheduler)

        // Act
        useCase()

        // Assert
        assertThat(scheduler.enqueueCount).isEqualTo(1)
        assertThat(scheduler.cancelCount).isEqualTo(0)
    }
}
