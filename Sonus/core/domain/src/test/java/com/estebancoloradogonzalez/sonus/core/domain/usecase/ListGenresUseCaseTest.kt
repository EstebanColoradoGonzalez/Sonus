package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.fake.FakeCatalogBrowseRepository
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ListGenresUseCaseTest {
    @Test
    fun `emits the genres and forwards the content type filter`() =
        runTest {
            // Arrange
            val expected = listOf(GenreView(id = 4, name = "Rock", trackCount = 48))
            val repository = FakeCatalogBrowseRepository(genres = expected)
            val useCase = ListGenresUseCase(repository)

            // Act
            val result = useCase(ContentType.MUSIC).first()

            // Assert
            assertThat(result).isEqualTo(expected)
            assertThat(repository.receivedGenresContentType).isEqualTo(ContentType.MUSIC)
        }
}
