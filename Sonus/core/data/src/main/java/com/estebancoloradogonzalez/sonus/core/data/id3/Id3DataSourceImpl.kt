package com.estebancoloradogonzalez.sonus.core.data.id3

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads embedded metadata with the platform [MediaMetadataRetriever] (ADR-010: no third-party
 * library to audit, fully local, air-gapped). Absent tags stay `null` (Invariant 4); an undecodable
 * file yields [TrackAvailability.UNSUPPORTED] instead of aborting the scan (AC4). `contentType` is
 * left [ContentType.UNKNOWN] — it is never inferred from the file.
 */
class Id3DataSourceImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Id3DataSource {
        // MediaMetadataRetriever throws IllegalArgumentException/RuntimeException for undecodable
        // media at the infrastructure border; it is translated to a value (UNSUPPORTED), never
        // swallowed, so the scan continues (AC4).
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        override fun readMetadata(
            uri: String,
            mimeType: String,
        ): RawTrackMetadata {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, Uri.parse(uri))
                val durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (durationMs == null) {
                    unsupported()
                } else {
                    RawTrackMetadata(
                        title = retriever.tag(MediaMetadataRetriever.METADATA_KEY_TITLE),
                        artistName = retriever.tag(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                        albumName = retriever.tag(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                        genreName = retriever.tag(MediaMetadataRetriever.METADATA_KEY_GENRE),
                        contentType = ContentType.UNKNOWN,
                        trackNumber =
                            retriever
                                .tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                                ?.substringBefore('/')
                                ?.trim()
                                ?.toIntOrNull(),
                        releaseYear =
                            retriever.tag(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull(),
                        durationMs = durationMs,
                        hasEmbeddedArtwork = retriever.embeddedPicture != null,
                        availability = TrackAvailability.AVAILABLE,
                    )
                }
            } catch (error: RuntimeException) {
                unsupported()
            } finally {
                retriever.release()
            }
        }

        private fun MediaMetadataRetriever.tag(key: Int): String? = extractMetadata(key)?.takeIf { it.isNotBlank() }

        private fun unsupported(): RawTrackMetadata =
            RawTrackMetadata(
                title = null,
                artistName = null,
                albumName = null,
                genreName = null,
                contentType = ContentType.UNKNOWN,
                trackNumber = null,
                releaseYear = null,
                durationMs = 0L,
                hasEmbeddedArtwork = false,
                availability = TrackAvailability.UNSUPPORTED,
            )
    }
