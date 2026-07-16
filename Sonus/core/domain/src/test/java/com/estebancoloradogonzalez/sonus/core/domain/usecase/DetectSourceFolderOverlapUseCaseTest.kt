package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeSourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DetectSourceFolderOverlapUseCaseTest {
    private val audio = "content://com.android.externalstorage.documents/tree/primary%3AAudio"
    private val podcasts = "content://com.android.externalstorage.documents/tree/primary%3AAudio%2FPodcasts"
    private val videos = "content://com.android.externalstorage.documents/tree/primary%3AVideos"
    private val audioBooks = "content://com.android.externalstorage.documents/tree/primary%3AAudioBooks"

    private suspend fun repositoryWith(vararg treeUris: String): FakeSourceFolderRepository {
        val repository = FakeSourceFolderRepository()
        treeUris.forEachIndexed { index, treeUri ->
            repository.add(
                SourceFolder(treeUri = treeUri, displayPath = "Folder $index", dateAddedMs = index.toLong()),
            )
        }
        return repository
    }

    @Test
    fun `returns false when there are no registered folders`() =
        runTest {
            // Arrange
            val useCase = DetectSourceFolderOverlapUseCase(FakeSourceFolderRepository())

            // Act
            val overlaps = useCase(podcasts)

            // Assert
            assertThat(overlaps).isFalse()
        }

    @Test
    fun `returns true when the candidate is a descendant of a registered folder`() =
        runTest {
            // Arrange (AC4 — nested subdirectory)
            val useCase = DetectSourceFolderOverlapUseCase(repositoryWith(audio))

            // Act
            val overlaps = useCase(podcasts)

            // Assert
            assertThat(overlaps).isTrue()
        }

    @Test
    fun `returns true when the candidate contains a registered folder`() =
        runTest {
            // Arrange (AC4 — container directory)
            val useCase = DetectSourceFolderOverlapUseCase(repositoryWith(podcasts))

            // Act
            val overlaps = useCase(audio)

            // Assert
            assertThat(overlaps).isTrue()
        }

    @Test
    fun `returns false for a sibling folder`() =
        runTest {
            // Arrange
            val useCase = DetectSourceFolderOverlapUseCase(repositoryWith(audio))

            // Act
            val overlaps = useCase(videos)

            // Assert
            assertThat(overlaps).isFalse()
        }

    @Test
    fun `returns false when a name is a prefix but not a path ancestor`() =
        runTest {
            // Arrange (AudioBooks is not inside Audio)
            val useCase = DetectSourceFolderOverlapUseCase(repositoryWith(audio))

            // Act
            val overlaps = useCase(audioBooks)

            // Assert
            assertThat(overlaps).isFalse()
        }

    @Test
    fun `returns false for an identical treeUri (that is a duplicate, not an overlap)`() =
        runTest {
            // Arrange (AC3 boundary — equality is handled as a duplicate elsewhere)
            val useCase = DetectSourceFolderOverlapUseCase(repositoryWith(audio))

            // Act
            val overlaps = useCase(audio)

            // Assert
            assertThat(overlaps).isFalse()
        }
}
