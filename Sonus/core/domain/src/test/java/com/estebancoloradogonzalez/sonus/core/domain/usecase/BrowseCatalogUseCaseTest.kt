package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeCatalogBrowseRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class BrowseCatalogUseCaseTest {
    private fun trackView(id: Long) =
        TrackView(
            id = id,
            uri = "content://track/$id",
            title = "Title $id",
            artistId = 2,
            artistName = "Artist",
            albumId = 3,
            albumName = "Album",
            genreId = 4,
            genreName = "Genre",
            contentType = ContentType.MUSIC,
            trackNumber = 1,
            durationMs = 1000,
            hasEmbeddedArtwork = false,
            availability = TrackAvailability.AVAILABLE,
        )

    @Test
    fun `emits the browsed tracks and forwards the query`() =
        runTest {
            // Arrange
            val expected = listOf(trackView(1), trackView(2))
            val repository = FakeCatalogBrowseRepository(tracks = expected)
            val useCase = BrowseCatalogUseCase(repository)
            val query = BrowseQuery(contentType = ContentType.MUSIC, genreId = 7)

            // Act
            val result = useCase(query).first()

            // Assert
            assertThat(result).isEqualTo(expected)
            assertThat(repository.receivedBrowseQuery).isEqualTo(query)
        }
}
