package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeCatalogBrowseRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ListAlbumsUseCaseTest {
    @Test
    fun `emits the albums and forwards the artist filter`() =
        runTest {
            // Arrange
            val expected =
                listOf(
                    AlbumView(
                        id = 3,
                        name = "A Night at the Opera",
                        artistId = 2,
                        artistName = "Queen",
                        hasArtwork = true,
                        trackCount = 12,
                    ),
                )
            val repository = FakeCatalogBrowseRepository(albums = expected)
            val useCase = ListAlbumsUseCase(repository)

            // Act
            val result = useCase(artistId = 2).first()

            // Assert
            assertThat(result).isEqualTo(expected)
            assertThat(repository.receivedAlbumsArtistId).isEqualTo(2)
        }
}
