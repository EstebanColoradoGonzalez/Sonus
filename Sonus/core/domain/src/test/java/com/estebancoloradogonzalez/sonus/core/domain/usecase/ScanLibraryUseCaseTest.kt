package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeCatalogRepository
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeScanStateEmitter
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ScanLibraryUseCaseTest {
    @Test
    fun `returns Success and publishes Scanning then Finished when the scan completes`() =
        runTest {
            // Arrange (AC1)
            val summary = ScanSummary(added = 3, purged = 0, unsupported = 1, orphanDimsPurged = 0)
            val sourceFolders = FakeSourceFolderRepository()
            sourceFolders.add(
                SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L),
            )
            val catalog = FakeCatalogRepository(OperationResult.Success(summary))
            val emitter = FakeScanStateEmitter()
            val useCase = ScanLibraryUseCase(sourceFolders, catalog, emitter)

            // Act
            val result = useCase(LibraryCommand.Scan(ScanMode.FULL))

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(summary))
            assertThat(catalog.receivedMode).isEqualTo(ScanMode.FULL)
            assertThat(catalog.receivedFolders).hasSize(1)
            assertThat(emitter.emissions.first()).isEqualTo(ScanState.Scanning(processed = 0, total = null))
            assertThat(emitter.emissions.last()).isEqualTo(ScanState.Finished(summary))
        }

    @Test
    fun `returns Failure and publishes Aborted when a folder loses access`() =
        runTest {
            // Arrange (AC5)
            val error = DomainError.ScanAborted(DomainError.PermissionRevoked)
            val catalog = FakeCatalogRepository(OperationResult.Failure(error))
            val emitter = FakeScanStateEmitter()
            val useCase = ScanLibraryUseCase(FakeSourceFolderRepository(), catalog, emitter)

            // Act
            val result = useCase(LibraryCommand.Scan(ScanMode.FULL))

            // Assert
            assertThat(result).isEqualTo(OperationResult.Failure(error))
            assertThat(emitter.emissions.last()).isEqualTo(ScanState.Aborted(error))
        }
}
