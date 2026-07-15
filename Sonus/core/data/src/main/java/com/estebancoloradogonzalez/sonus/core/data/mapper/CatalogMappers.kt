package com.estebancoloradogonzalez.sonus.core.data.mapper

import com.estebancoloradogonzalez.sonus.core.data.id3.RawTrackMetadata
import com.estebancoloradogonzalez.sonus.core.data.local.saf.DiscoveredFile
import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.model.ScannedTrack
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Track as TrackEntity

/** Pure mapping functions for the scan pipeline: raw metadata → domain → Room entity. */

fun RawTrackMetadata.toScannedTrack(
    file: DiscoveredFile,
    sourceFolderId: FolderId,
): ScannedTrack =
    ScannedTrack(
        uri = file.uri,
        title = title,
        artistName = artistName,
        albumName = albumName,
        genreName = genreName,
        contentType = contentType,
        trackNumber = trackNumber,
        releaseYear = releaseYear,
        durationMs = durationMs,
        hasEmbeddedArtwork = hasEmbeddedArtwork,
        availability = availability,
        sourceFolderId = sourceFolderId,
        fileLastModifiedMs = file.lastModifiedMs,
    )

fun ScannedTrack.toEntity(
    artistId: Long,
    albumId: Long,
    genreId: Long,
    dateAddedMs: Long,
): TrackEntity =
    TrackEntity(
        uri = uri,
        title = title,
        artistId = artistId,
        albumId = albumId,
        genreId = genreId,
        sourceFolderId = sourceFolderId,
        contentType = contentType,
        trackNumber = trackNumber,
        releaseYear = releaseYear,
        durationMs = durationMs,
        hasEmbeddedArtwork = hasEmbeddedArtwork,
        availability = availability,
        fileLastModifiedMs = fileLastModifiedMs,
        dateAddedMs = dateAddedMs,
    )
