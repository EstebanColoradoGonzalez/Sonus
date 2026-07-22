package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val dispatcher = UnconfinedTestDispatcher()
    private val browseQueries = mutableListOf<BrowseQuery>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
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
        every { browseCatalog(capture(browseQueries)) } returns flowOf(listOf(track))
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

    @Test
    fun `starts with an empty inactive text filter`() =
        runTest(dispatcher) {
            // Arrange (US-011 Esc 6)
            val vm = viewModel()

            // Act
            keepSubscribed(vm)

            // Assert — empty field on entry, unfiltered content
            assertThat(vm.uiState.value.textFilter).isEmpty()
            assertThat(vm.uiState.value.isSearchActive).isFalse()
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Genres(listOf(genre)))
        }

    @Test
    fun `applies the debounced text filter producing a filtered track list`() =
        runTest(dispatcher) {
            // Arrange (US-011 Esc 1)
            val vm = viewModel()
            keepSubscribed(vm)

            // Act — type a term and let the debounce elapse
            vm.onCommand(LibraryCommand.SetTextFilter("Bohemian"))
            advanceUntilIdle()

            // Assert — the field updates, content becomes a track list, the query carries the term
            assertThat(vm.uiState.value.textFilter).isEqualTo("Bohemian")
            assertThat(vm.uiState.value.isSearchActive).isTrue()
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Tracks(listOf(track)))
            assertThat(browseQueries.last()).isEqualTo(BrowseQuery(textFilter = "Bohemian"))
        }

    @Test
    fun `clearing the text filter restores the unfiltered view`() =
        runTest(dispatcher) {
            // Arrange (US-011 Esc 3)
            val vm = viewModel()
            keepSubscribed(vm)
            vm.onCommand(LibraryCommand.SetTextFilter("Bohemian"))
            advanceUntilIdle()

            // Act
            vm.onCommand(LibraryCommand.ClearTextFilter)
            advanceUntilIdle()

            // Assert — back to the dimension content with no active filter
            assertThat(vm.uiState.value.textFilter).isEmpty()
            assertThat(vm.uiState.value.isSearchActive).isFalse()
            assertThat(vm.uiState.value.content).isEqualTo(LibraryContent.Genres(listOf(genre)))
        }

    @Test
    fun `intersects the text filter with the active dimension`() =
        runTest(dispatcher) {
            // Arrange (US-011 Esc 4)
            val vm = viewModel()
            keepSubscribed(vm)
            vm.onCommand(LibraryCommand.SelectDimension(BrowseDimension.MUSIC))

            // Act
            vm.onCommand(LibraryCommand.SetTextFilter("Queen"))
            advanceUntilIdle()

            // Assert — the query intersects the MUSIC content type with the term
            assertThat(browseQueries.last())
                .isEqualTo(BrowseQuery(contentType = ContentType.MUSIC, textFilter = "Queen"))
        }

    @Test
    fun `coalesces rapid keystrokes into a single filtered query`() =
        runTest(dispatcher) {
            // Arrange (US-011 Esc 5 / contract §4.1) — no browse query yet on the genres root
            val vm = viewModel()
            keepSubscribed(vm)

            // Act — three keystrokes closer together than the debounce window
            vm.onCommand(LibraryCommand.SetTextFilter("B"))
            advanceTimeBy(100)
            vm.onCommand(LibraryCommand.SetTextFilter("Bo"))
            advanceTimeBy(100)
            vm.onCommand(LibraryCommand.SetTextFilter("Boh"))
            advanceUntilIdle()

            // Assert — only the final term reached the catalog query (no per-keystroke queries)
            assertThat(browseQueries.mapNotNull { it.textFilter }).containsExactly("Boh")
        }
}
