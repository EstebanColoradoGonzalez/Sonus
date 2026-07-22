package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeCatalogBrowseRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ListArtistsUseCaseTest {
    @Test
    fun `emits the artists and forwards the genre filter`() =
        runTest {
            // Arrange
            val expected = listOf(ArtistView(id = 2, name = "Queen", trackCount = 12))
            val repository = FakeCatalogBrowseRepository(artists = expected)
            val useCase = ListArtistsUseCase(repository)

            // Act
            val result = useCase(genreId = 4).first()

            // Assert
            assertThat(result).isEqualTo(expected)
            assertThat(repository.receivedArtistsGenreId).isEqualTo(4)
        }
}
