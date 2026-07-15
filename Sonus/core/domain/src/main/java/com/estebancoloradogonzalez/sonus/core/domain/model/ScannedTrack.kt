package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * A single audio file discovered and read during a scan, before it is persisted (blueprint C-04).
 *
 * It carries dimension *names* (not foreign keys): resolving each name to a normalized `Artist`,
 * `Album` or `Genre` id — or to the `id = 1` sentinel when absent — is a data-layer concern
 * (domain_and_state_model §2, Invariant 4). A `null` name means the ID3 tag was absent; the value is
 * never inferred.
 */
data class ScannedTrack(
    val uri: String,
    val title: String?,
    val artistName: String?,
    val albumName: String?,
    val genreName: String?,
    val contentType: ContentType,
    val trackNumber: Int?,
    val releaseYear: Int?,
    val durationMs: Long,
    val hasEmbeddedArtwork: Boolean,
    val availability: TrackAvailability,
    val sourceFolderId: FolderId,
    val fileLastModifiedMs: Long,
)
