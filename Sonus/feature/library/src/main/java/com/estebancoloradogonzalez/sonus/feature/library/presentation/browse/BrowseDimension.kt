package com.estebancoloradogonzalez.sonus.feature.library.presentation.browse

/**
 * Top-level navigation dimensions offered at the library root (US-010 Escenario 1). [MUSIC] and
 * [PODCASTS] list tracks filtered by content type (Escenario 2/3); [GENRES]/[ARTISTS]/[ALBUMS] open
 * the taxonomic drill-down. The Playlist dimension (Escenario 7) is deferred until the playlist
 * subsystem lands (EPIC-06).
 */
enum class BrowseDimension {
    MUSIC,
    PODCASTS,
    GENRES,
    ARTISTS,
    ALBUMS,
}
