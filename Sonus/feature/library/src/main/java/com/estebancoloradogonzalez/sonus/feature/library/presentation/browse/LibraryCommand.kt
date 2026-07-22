package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView

/** Navigation intents of the Listener over the library screen (channel C1, US-010/US-011). */
sealed interface LibraryCommand {
    data class SelectDimension(val dimension: BrowseDimension) : LibraryCommand

    data class OpenGenre(val genre: GenreView) : LibraryCommand

    data class OpenArtist(val artist: ArtistView) : LibraryCommand

    data class OpenAlbum(val album: AlbumView) : LibraryCommand

    data object NavigateUp : LibraryCommand

    /** Updates the free-text filter as the Listener types (US-011, debounced downstream). */
    data class SetTextFilter(val query: String) : LibraryCommand

    /** Clears the free-text filter, restoring the unfiltered catalog view (US-011 Escenario 3). */
    data object ClearTextFilter : LibraryCommand
}
