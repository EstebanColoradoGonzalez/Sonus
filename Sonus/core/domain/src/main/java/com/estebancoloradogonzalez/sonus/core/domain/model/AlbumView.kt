package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Presentation projection of an Album dimension entry (US-010 Escenario 5). [name]/[artistName] are
 * blank for the `id = 1` sentinels, resolved to localized labels in presentation (Invariant 4).
 * [artistId] is `Album.artistId` (the album's artist, `[F-7]`), not the track performer. [hasArtwork]
 * is `true` when at least one visible track of the album carries embedded artwork; the image itself is
 * never persisted and is loaded on demand (ADR-009 / `[F-5]`). [trackCount] counts only visible tracks.
 */
data class AlbumView(
    val id: Long,
    val name: String,
    val artistId: Long,
    val artistName: String,
    val hasArtwork: Boolean,
    val trackCount: Int,
)
