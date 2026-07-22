package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView

/**
 * Immutable UI state of the library navigation screen (US-010). [backStack] is never empty; its first
 * entry is always the selected dimension [Root] and its last entry is the level currently shown. The
 * observed [content] re-emits on any catalog change (Escenario 13). [isCatalogEmpty] distinguishes an
 * empty catalog (Escenario 10) from a dimension with no matches (Escenario 11); [isLoading] is true
 * until the first content emission.
 */
data class LibraryUiState(
    val backStack: List<LibraryNode> = listOf(LibraryNode.Root(BrowseDimension.GENRES)),
    val content: LibraryContent = LibraryContent.Genres(emptyList()),
    val isCatalogEmpty: Boolean = false,
    val isLoading: Boolean = true,
) {
    val currentNode: LibraryNode get() = backStack.last()

    val selectedDimension: BrowseDimension get() = (backStack.first() as LibraryNode.Root).dimension

    val canNavigateUp: Boolean get() = backStack.size > 1
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
