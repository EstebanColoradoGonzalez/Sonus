package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.fake.FixedTimeProvider
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AddSourceFolderUseCaseTest {
    @Test
    fun `returns Success and persists the folder when granted and unique`() =
        runTest {
            // Arrange (Scenario 1)
            val repository = FakeSourceFolderRepository()
            val useCase =
                AddSourceFolderUseCase(
                    repository,
                    FakeSafPermissionGateway(displayPath = "Podcasts"),
                    FixedTimeProvider(fixedNowMs = 1234L),
                )

            // Act
            val result = useCase(LibraryCommand.AddSourceFolder("content://tree/music"))

            // Assert
            assertThat(result).isInstanceOf(OperationResult.Success::class.java)
            val folders = repository.observeAll().first()
            assertThat(folders).hasSize(1)
            assertThat(folders.first().displayPath).isEqualTo("Podcasts")
            assertThat(folders.first().dateAddedMs).isEqualTo(1234L)
        }

    @Test
    fun `returns Failure DuplicateSourceFolder without adding when treeUri already exists`() =
        runTest {
            // Arrange (Scenario 4)
            val repository = FakeSourceFolderRepository()
            repository.add(
                SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L),
            )
            val useCase = AddSourceFolderUseCase(repository, FakeSafPermissionGateway(), FixedTimeProvider())

            // Act
            val result = useCase(LibraryCommand.AddSourceFolder("content://tree/music"))

            // Assert
            assertThat(result).isEqualTo(OperationResult.Failure(DomainError.DuplicateSourceFolder))
            assertThat(repository.observeAll().first()).hasSize(1)
        }

    @Test
    fun `returns Failure PermissionDenied when the OS does not grant the permission`() =
        runTest {
            // Arrange (Scenario 5 — permission denied)
            val repository = FakeSourceFolderRepository()
            val useCase =
                AddSourceFolderUseCase(
                    repository,
                    FakeSafPermissionGateway(grantPermission = false),
                    FixedTimeProvider(),
                )

            // Act
            val result = useCase(LibraryCommand.AddSourceFolder("content://tree/music"))

            // Assert
            assertThat(result).isEqualTo(OperationResult.Failure(DomainError.PermissionDenied))
            assertThat(repository.observeAll().first()).isEmpty()
        }
}
