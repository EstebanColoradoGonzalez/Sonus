package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeScanScheduler
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class RescanLibraryUseCaseTest {
    @Test
    fun `enqueues an INCREMENTAL scan by default when no mode is given`() {
        // Arrange (US-007 AC1) — habitual manual re-scan defaults to the efficient mode.
        val scheduler = FakeScanScheduler()
        val useCase = RescanLibraryUseCase(scheduler)

        // Act
        useCase()

        // Assert
        assertThat(scheduler.enqueuedModes).containsExactly(ScanMode.INCREMENTAL)
        assertThat(scheduler.cancelCount).isEqualTo(0)
    }

    @Test
    fun `enqueues a FULL scan when the FULL mode is requested`() {
        // Arrange (US-007 AC1) — the Listener may request a total reconstruction.
        val scheduler = FakeScanScheduler()
        val useCase = RescanLibraryUseCase(scheduler)

        // Act
        useCase(ScanMode.FULL)

        // Assert
        assertThat(scheduler.enqueuedModes).containsExactly(ScanMode.FULL)
    }
}
