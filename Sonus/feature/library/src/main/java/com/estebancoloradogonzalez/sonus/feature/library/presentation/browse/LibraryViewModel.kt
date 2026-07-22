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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Drives the library navigation screen (US-010). Holds the taxonomic navigation back-stack; the
 * current node's content is observed reactively via [flatMapLatest], so switching level cancels the
 * previous query and any catalog mutation re-emits automatically (Escenario 13). Holds no business
 * rule nor I/O — every query is delegated to a use case.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

        private val contentFlow: Flow<LibraryContent> =
            backStack.flatMapLatest { stack -> contentFor(stack.last()) }

        val uiState: StateFlow<LibraryUiState> =
            combine(backStack, contentFlow, observeCatalogEmpty()) { stack, content, catalogEmpty ->
                LibraryUiState(
                    backStack = stack,
                    content = content,
                    isCatalogEmpty = catalogEmpty,
                    isLoading = false,
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
        }
    }
