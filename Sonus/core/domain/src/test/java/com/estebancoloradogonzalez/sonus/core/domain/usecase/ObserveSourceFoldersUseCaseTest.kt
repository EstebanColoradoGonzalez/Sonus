package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveSourceFoldersUseCaseTest {
    @Test
    fun `emits all registered folders accumulated`() =
        runTest {
            // Arrange (Scenario 2 — multiple folders)
            val repository = FakeSourceFolderRepository()
            repository.add(SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L))
            repository.add(
                SourceFolder(treeUri = "content://tree/podcasts", displayPath = "Podcasts", dateAddedMs = 2L),
            )
            val useCase = ObserveSourceFoldersUseCase(repository)

            // Act
            val folders = useCase().first()

            // Assert
            assertThat(folders).hasSize(2)
            assertThat(folders.map { it.displayPath }).containsExactly("Music", "Podcasts").inOrder()
        }
}
