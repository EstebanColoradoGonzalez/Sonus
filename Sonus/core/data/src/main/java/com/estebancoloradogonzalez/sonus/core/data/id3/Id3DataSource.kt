package com.estebancoloradogonzalez.sonus.core.data.id3

import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability

/**
 * Raw metadata read from a single audio file. A `null` field means the tag was absent — it is never
 * inferred (Invariant 4). [availability] is [TrackAvailability.UNSUPPORTED] when the file cannot be
 * decoded.
 */
data class RawTrackMetadata(
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
)

/**
 * Local ID3/embedded-metadata reader (blueprint C-04). Read-only for US-003; tag writing
 * (`TRG-META-01`) belongs to a later story. Purely local decoding — no network (ADR-010).
 */
interface Id3DataSource {
    fun readMetadata(
        uri: String,
        mimeType: String,
    ): RawTrackMetadata
}
