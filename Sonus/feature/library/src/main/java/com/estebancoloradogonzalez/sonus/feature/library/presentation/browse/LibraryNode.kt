package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

/**
 * A single level of the taxonomic navigation stack. [Root] is the selected dimension tab; the drill
 * nodes carry the raw dimension name (blank for the `id = 1` sentinel) for the breadcrumb, resolved to
 * a localized label only when rendered (Invariant 4). The hierarchy is Genre → Artist → Album →
 * Tracks (US-010 Escenario 4/5/6).
 */
sealed interface LibraryNode {
    data class Root(val dimension: BrowseDimension) : LibraryNode

    data class GenreArtists(val genreId: Long, val genreName: String) : LibraryNode

    data class ArtistAlbums(val artistId: Long, val artistName: String) : LibraryNode

    data class AlbumTracks(val albumId: Long, val albumName: String) : LibraryNode
}
