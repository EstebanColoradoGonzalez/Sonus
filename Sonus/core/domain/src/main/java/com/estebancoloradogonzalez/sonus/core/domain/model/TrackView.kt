package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Presentation projection of a `Track` with its dimensions resolved (blueprint §3, output of
 * `TRG-NAV-01`). Absent metadata is carried as-is and never inferred (Invariant 4): [title] is `null`
 * when the tag was absent, and [artistName]/[albumName]/[genreName] are blank for the `id = 1`
 * sentinel rows. The presentation layer maps those to localized labels ("Sin título", "Sin artista",
 * …); the blank/`null` value is never a persisted literal.
 *
 * [availability] is always `AVAILABLE` or `UNSUPPORTED` — `MISSING` tracks are excluded from every
 * browse query (Invariant 2). `UNSUPPORTED` tracks are visible but not playable (US-010 Escenario 9).
 */
data class TrackView(
    val id: Long,
    val uri: String,
    val title: String?,
    val artistId: Long,
    val artistName: String,
    val albumId: Long,
    val albumName: String,
    val genreId: Long,
    val genreName: String,
    val contentType: ContentType,
    val trackNumber: Int?,
    val durationMs: Long,
    val hasEmbeddedArtwork: Boolean,
    val availability: TrackAvailability,
)
