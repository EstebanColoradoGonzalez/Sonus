package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RemoveSourceFolderUseCaseTest {
    @Test
    fun `returns Success releasing the permission and deleting when the folder exists`() =
        runTest {
            // Arrange (Scenario 6 — light removal)
            val repository = FakeSourceFolderRepository()
            val id =
                repository.add(
                    SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L),
                )
            val saf = FakeSafPermissionGateway()
            val useCase = RemoveSourceFolderUseCase(repository, saf)

            // Act
            val result = useCase(LibraryCommand.RemoveSourceFolder(id))

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(Unit))
            assertThat(repository.observeAll().first()).isEmpty()
            assertThat(saf.releasedUris).containsExactly("content://tree/music")
        }

    @Test
    fun `returns Failure EntityNotFound when the folder does not exist`() =
        runTest {
            // Arrange
            val repository = FakeSourceFolderRepository()
            val saf = FakeSafPermissionGateway()
            val useCase = RemoveSourceFolderUseCase(repository, saf)

            // Act
            val result = useCase(LibraryCommand.RemoveSourceFolder(99L))

            // Assert
            assertThat(result).isEqualTo(OperationResult.Failure(DomainError.EntityNotFound("SourceFolder", 99L)))
            assertThat(saf.releasedUris).isEmpty()
        }
}
