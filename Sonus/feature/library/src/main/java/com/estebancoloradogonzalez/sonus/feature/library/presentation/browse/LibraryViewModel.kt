package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.usecase.BrowseCatalogUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ListAlbumsUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ListArtistsUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ListGenresUseCase
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ObserveCatalogEmptyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Drives the library navigation screen (US-010/US-011). Holds the taxonomic navigation back-stack and
 * the free-text filter. The filter is debounced (~280 ms, contract §4.1) to avoid a query per keystroke
 * over large catalogs ([RNF-01]); clearing it restores the view immediately (no debounce). The current
 * node and the debounced filter are combined and observed via [flatMapLatest], so switching level or
 * typing cancels the previous query and any catalog mutation re-emits automatically. When the filter is
 * active the content is always a track list scoped by the node's taxonomy (Escenario 4). Holds no
 * business rule nor I/O — every query is delegated to a use case; the term is never persisted (CT-02).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val browseCatalog: BrowseCatalogUseCase,
        private val listGenres: ListGenresUseCase,
        private val listArtists: ListArtistsUseCase,
        private val listAlbums: ListAlbumsUseCase,
        observeCatalogEmpty: ObserveCatalogEmptyUseCase,
    ) : ViewModel() {
        private val backStack = MutableStateFlow<List<LibraryNode>>(listOf(LibraryNode.Root(BrowseDimension.GENRES)))

        private val searchInput = MutableStateFlow("")

        private val debouncedFilter: Flow<String?> =
            searchInput
                .debounce { input -> if (input.isBlank()) IMMEDIATE_MS else SEARCH_DEBOUNCE_MS }
                .map { it.trim().ifBlank { null } }
                .distinctUntilChanged()

        private val contentFlow: Flow<LibraryContent> =
            combine(backStack, debouncedFilter) { stack, filter -> stack.last() to filter }
                .flatMapLatest { (node, filter) -> contentFor(node, filter) }

        val uiState: StateFlow<LibraryUiState> =
            combine(backStack, searchInput, contentFlow, observeCatalogEmpty()) { stack, input, content, catalogEmpty ->
                LibraryUiState(
                    backStack = stack,
                    content = content,
                    isCatalogEmpty = catalogEmpty,
                    isLoading = false,
                    textFilter = input,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = LibraryUiState(),
            )

        fun onCommand(command: LibraryCommand) =
            when (command) {
                is LibraryCommand.SelectDimension -> selectDimension(command)
                is LibraryCommand.OpenGenre -> push(LibraryNode.GenreArtists(command.genre.id, command.genre.name))
                is LibraryCommand.OpenArtist -> push(LibraryNode.ArtistAlbums(command.artist.id, command.artist.name))
                is LibraryCommand.OpenAlbum -> push(LibraryNode.AlbumTracks(command.album.id, command.album.name))
                LibraryCommand.NavigateUp -> navigateUp()
                is LibraryCommand.SetTextFilter -> setTextFilter(command.query)
                LibraryCommand.ClearTextFilter -> clearTextFilter()
            }

        private fun selectDimension(command: LibraryCommand.SelectDimension) {
            backStack.value = listOf(LibraryNode.Root(command.dimension))
        }

        private fun push(node: LibraryNode) {
            backStack.update { it + node }
        }

        private fun navigateUp() {
            backStack.update { if (it.size > 1) it.dropLast(1) else it }
        }

        private fun setTextFilter(query: String) {
            searchInput.value = query
        }

        private fun clearTextFilter() {
            searchInput.value = ""
        }

        private fun contentFor(
            node: LibraryNode,
            filter: String?,
        ): Flow<LibraryContent> =
            if (filter != null) {
                browseCatalog(node.toBrowseQuery(filter)).map { LibraryContent.Tracks(it) }
            } else {
                contentFor(node)
            }

        private fun contentFor(node: LibraryNode): Flow<LibraryContent> =
            when (node) {
                is LibraryNode.Root -> rootContent(node.dimension)
                is LibraryNode.GenreArtists ->
                    listArtists(genreId = node.genreId).map { LibraryContent.Artists(it) }
                is LibraryNode.ArtistAlbums ->
                    listAlbums(artistId = node.artistId).map { LibraryContent.Albums(it) }
                is LibraryNode.AlbumTracks ->
                    browseCatalog(BrowseQuery(albumId = node.albumId)).map { LibraryContent.Tracks(it) }
            }

        private fun rootContent(dimension: BrowseDimension): Flow<LibraryContent> =
            when (dimension) {
                BrowseDimension.MUSIC ->
                    browseCatalog(BrowseQuery(contentType = ContentType.MUSIC)).map { LibraryContent.Tracks(it) }
                BrowseDimension.PODCASTS ->
                    browseCatalog(BrowseQuery(contentType = ContentType.PODCAST)).map { LibraryContent.Tracks(it) }
                BrowseDimension.GENRES -> listGenres().map { LibraryContent.Genres(it) }
                BrowseDimension.ARTISTS -> listArtists().map { LibraryContent.Artists(it) }
                BrowseDimension.ALBUMS -> listAlbums().map { LibraryContent.Albums(it) }
            }

        companion object {
            private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
            private const val SEARCH_DEBOUNCE_MS = 280L
            private const val IMMEDIATE_MS = 0L
        }
    }
