package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView

/**
 * Immutable UI state of the library navigation screen (US-010/US-011). [backStack] is never empty; its
 * first entry is always the selected dimension [Root] and its last entry is the level currently shown.
 * The observed [content] re-emits on any catalog change (Escenario 13). [isCatalogEmpty] distinguishes
 * an empty catalog (Escenario 10) from a dimension with no matches (Escenario 11); [isLoading] is true
 * until the first content emission. [textFilter] is the raw text in the search field (US-011); while
 * [isSearchActive] the [content] is always a track list filtered across title/artist/album.
 */
data class LibraryUiState(
    val backStack: List<LibraryNode> = listOf(LibraryNode.Root(BrowseDimension.GENRES)),
    val content: LibraryContent = LibraryContent.Genres(emptyList()),
    val isCatalogEmpty: Boolean = false,
    val isLoading: Boolean = true,
    val textFilter: String = "",
) {
    val currentNode: LibraryNode get() = backStack.last()

    val selectedDimension: BrowseDimension get() = (backStack.first() as LibraryNode.Root).dimension

    val canNavigateUp: Boolean get() = backStack.size > 1

    val isSearchActive: Boolean get() = textFilter.isNotBlank()
}

/**
 * Maps the current navigation node to the taxonomic scope of a [BrowseQuery] and folds in [textFilter]
 * (US-011). A blank filter becomes `null` (no textual restriction, Escenario 3/6). With an active
 * filter the query intersects the node's taxonomic dimension with the text (Escenario 4).
 */
internal fun LibraryNode.toBrowseQuery(textFilter: String?): BrowseQuery {
    val filter = textFilter?.trim()?.ifBlank { null }
    return when (this) {
        is LibraryNode.Root ->
            when (dimension) {
                BrowseDimension.MUSIC -> BrowseQuery(contentType = ContentType.MUSIC, textFilter = filter)
                BrowseDimension.PODCASTS -> BrowseQuery(contentType = ContentType.PODCAST, textFilter = filter)
                BrowseDimension.GENRES, BrowseDimension.ARTISTS, BrowseDimension.ALBUMS ->
                    BrowseQuery(textFilter = filter)
            }
        is LibraryNode.GenreArtists -> BrowseQuery(genreId = genreId, textFilter = filter)
        is LibraryNode.ArtistAlbums -> BrowseQuery(artistId = artistId, textFilter = filter)
        is LibraryNode.AlbumTracks -> BrowseQuery(albumId = albumId, textFilter = filter)
    }
}

/** Polymorphic content of the current navigation level. */
sealed interface LibraryContent {
    val isEmpty: Boolean

    data class Genres(val items: List<GenreView>) : LibraryContent {
        override val isEmpty: Boolean get() = items.isEmpty()
    }

    data class Artists(val items: List<ArtistView>) : LibraryContent {
        override val isEmpty: Boolean get() = items.isEmpty()
    }

    data class Albums(val items: List<AlbumView>) : LibraryContent {
        override val isEmpty: Boolean get() = items.isEmpty()
    }

    data class Tracks(val items: List<TrackView>) : LibraryContent {
        override val isEmpty: Boolean get() = items.isEmpty()
    }
}
