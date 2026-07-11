package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class VerifySourceFoldersReadyUseCaseTest {
    private suspend fun repositoryWithOneFolder(): FakeSourceFolderRepository =
        FakeSourceFolderRepository().apply {
            add(SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L))
        }

    @Test
    fun `returns true when there is at least one folder and all keep the persisted permission`() =
        runTest {
            // Arrange (Scenario 7)
            val useCase =
                VerifySourceFoldersReadyUseCase(repositoryWithOneFolder(), FakeSafPermissionGateway(persisted = true))

            // Act + Assert
            assertThat(useCase()).isTrue()
        }

    @Test
    fun `returns false when the list is empty`() =
        runTest {
            // Arrange (Scenario 3 — empty)
            val useCase = VerifySourceFoldersReadyUseCase(FakeSourceFolderRepository(), FakeSafPermissionGateway())

            // Act + Assert
            assertThat(useCase()).isFalse()
        }

    @Test
    fun `returns false when a folder lost its persisted permission`() =
        runTest {
            // Arrange
            val useCase =
                VerifySourceFoldersReadyUseCase(repositoryWithOneFolder(), FakeSafPermissionGateway(persisted = false))

            // Act + Assert
            assertThat(useCase()).isFalse()
        }
}
