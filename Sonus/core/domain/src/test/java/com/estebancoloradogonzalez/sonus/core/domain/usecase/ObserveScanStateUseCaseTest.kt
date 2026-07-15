package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeScanStateEmitter
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveScanStateUseCaseTest {
    @Test
    fun `reflects the emitter state stream`() =
        runTest {
            // Arrange
            val emitter = FakeScanStateEmitter()
            val useCase = ObserveScanStateUseCase(emitter)

            // Act
            val summary = ScanSummary(added = 1, purged = 0, unsupported = 0, orphanDimsPurged = 0)
            emitter.update(ScanState.Finished(summary))

            // Assert
            assertThat(useCase().value).isEqualTo(ScanState.Finished(summary))
        }
}
