package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeScanScheduler
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StartLibraryScanUseCaseTest {
    @Test
    fun `enqueues a FULL scan on the scheduler`() {
        // Arrange (AC1) — the foundational first-run scan reconstructs an empty catalog.
        val scheduler = FakeScanScheduler()
        val useCase = StartLibraryScanUseCase(scheduler)

        // Act
        useCase()

        // Assert
        assertThat(scheduler.enqueuedModes).containsExactly(ScanMode.FULL)
        assertThat(scheduler.cancelCount).isEqualTo(0)
    }
}
