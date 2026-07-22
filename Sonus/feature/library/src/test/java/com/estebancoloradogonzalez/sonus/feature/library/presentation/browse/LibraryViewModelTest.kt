package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import com.estebancoloradogonzalez.sonus.core.domain.usecase.BrowseCatalogUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ListAlbumsUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ListArtistsUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ListGenresUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveCatalogEmptyUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val browseCatalog = mockk<BrowseCatalogUseCase>()
    private val listGenres = mockk<ListGenresUseCase>()
    private val listArtists = mockk<ListArtistsUseCase>()
    private val listAlbums = mockk<ListAlbumsUseCase>()
    private val observeCatalogEmpty = mockk<ObserveCatalogEmptyUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val genre = GenreView(id = 10, name = "Rock", trackCount = 3)
    private val artist = ArtistView(id = 20, name = "Queen", trackCount = 12)
    private val album =
        AlbumView(id = 30, name = "Opera", artistId = 20, artistName = "Queen", hasArtwork = true, trackCount = 12)
    private val track =
        TrackView(
            id = 40,
            uri = "content://track/40",
            title = "Bohemian Rhapsody",
            artistId = 20,
            artistName = "Queen",
            albumId = 30,
            albumName = "Opera",
            genreId = 10,
            genreName = "Rock",
            contentType = ContentType.MUSIC,
            trackNumber = 1,
            durationMs = 1000,
            hasEmbeddedArtwork = true,
            availability = TrackAvailability.AVAILABLE,
        )

    private fun viewModel(catalogEmpty: Boolean = false): LibraryViewModel {
        every { browseCatalog(any()) } returns flowOf(listOf(track))
        every { listGenres(any()) } returns flowOf(listOf(genre))
        every { listArtists(any(), any()) } returns flowOf(listOf(artist))
        every { listAlbums(any(), any()) } returns flowOf(listOf(album))
        every { observeCatalogEmpty() } returns flowOf(catalogEmpty)
        return LibraryViewModel(browseCatalog, listGenres, listArtists, listAlbums, observeCatalogEmpty)
    }

    private fun TestScope.keepSubscribed(viewModel: LibraryViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `starts on the genres dimension`() =
        runTest {
            // Arrange
            val vm = viewModel()

            // Act
            keepSubscribed(vm)

            // Assert (Esc 1)
            assertThat(vm.uiState.value.selectedDimension).isEqualTo(BrowseDimension.GENRES)
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Genres(listOf(genre)))
            assertThat(vm.uiState.value.isLoading).isFalse()
        }

    @Test
    fun `switches dimension to music tracks`() =
        runTest {
            // Arrange
            val vm = viewModel()
            keepSubscribed(vm)

            // Act (Esc 2)
            vm.onCommand(LibraryCommand.SelectDimension(BrowseDimension.MUSIC))

            // Assert
            assertThat(vm.uiState.value.selectedDimension).isEqualTo(BrowseDimension.MUSIC)
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Tracks(listOf(track)))
        }

    @Test
    fun `drills into a genre and navigates back up`() =
        runTest {
            // Arrange
            val vm = viewModel()
            keepSubscribed(vm)

            // Act (Esc 4) — open a genre
            vm.onCommand(LibraryCommand.OpenGenre(genre))

            // Assert — pushed the artist level
            assertThat(vm.uiState.value.canNavigateUp).isTrue()
            assertThat(vm.uiState.value.currentNode).isEqualTo(LibraryNode.GenreArtists(10, "Rock"))
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Artists(listOf(artist)))

            // Act — navigate up
            vm.onCommand(LibraryCommand.NavigateUp)

            // Assert — back at the root dimension
            assertThat(vm.uiState.value.canNavigateUp).isFalse()
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Genres(listOf(genre)))
        }

    @Test
    fun `drills genre to artist to album to tracks`() =
        runTest {
            // Arrange
            val vm = viewModel()
            keepSubscribed(vm)

            // Act (Esc 4/5/6)
            vm.onCommand(LibraryCommand.OpenGenre(genre))
            vm.onCommand(LibraryCommand.OpenArtist(artist))
            vm.onCommand(LibraryCommand.OpenAlbum(album))

            // Assert — three levels deep showing the album tracks
            assertThat(vm.uiState.value.backStack).hasSize(4)
            assertThat(vm.uiState.value.currentNode).isEqualTo(LibraryNode.AlbumTracks(30, "Opera"))
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Tracks(listOf(track)))
        }

    @Test
    fun `exposes the empty catalog signal`() =
        runTest {
            // Arrange (Esc 10)
            val vm = viewModel(catalogEmpty = true)

            // Act
            keepSubscribed(vm)

            // Assert
            assertThat(vm.uiState.value.isCatalogEmpty).isTrue()
        }
}
