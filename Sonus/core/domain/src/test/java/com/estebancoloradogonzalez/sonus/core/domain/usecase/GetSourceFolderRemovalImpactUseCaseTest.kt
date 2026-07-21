package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GetSourceFolderRemovalImpactUseCaseTest {
    @Test
    fun `returns the track count discovered under the folder`() =
        runTest {
            // Arrange (US-006 AC1 — impact for the confirmation dialog)
            val repository = FakeSourceFolderRepository()
            val id =
                repository.add(
                    SourceFolder(treeUri = "content://tree/music", displayPath = "Music", dateAddedMs = 1L),
                )
            repository.tracksUnder[id] = 12
            val useCase = GetSourceFolderRemovalImpactUseCase(repository)

            // Act
            val impact = useCase(id)

            // Assert
            assertThat(impact).isEqualTo(12)
        }

    @Test
    fun `returns zero when the folder has no tracks`() =
        runTest {
            // Arrange (empty folder — e.g. removed before scanning)
            val repository = FakeSourceFolderRepository()
            val id =
                repository.add(
                    SourceFolder(treeUri = "content://tree/empty", displayPath = "Empty", dateAddedMs = 1L),
                )
            val useCase = GetSourceFolderRemovalImpactUseCase(repository)

            // Act + Assert
            assertThat(useCase(id)).isEqualTo(0)
        }
}
