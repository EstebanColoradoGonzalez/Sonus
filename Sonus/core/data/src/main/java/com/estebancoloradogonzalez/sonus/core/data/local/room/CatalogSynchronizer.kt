package com.estebancoloradogonzalez.sonus.core.data.local.room

import androidx.room.withTransaction
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.AlbumDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.ArtistDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.GenreDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.TrackDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Album
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Artist
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Genre
import com.estebancoloradogonzalez.sonus.core.data.mapper.toEntity
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.model.ScannedTrack
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.port.TimeProvider
import javax.inject.Inject

/**
 * Deterministic catalog synchronizer (blueprint C-04). Runs the whole diff in a **single Room
 * transaction** ([androidx.room.withTransaction]): resolves each dimension name to its id
 * (get-or-create; absent name → `id = 1` sentinel, Invariant 4), upserts the [processed][sync] tracks
 * by URI, purges tracks whose URI is no longer among the discovered set (Invariant 2) and purges
 * orphan dimensions (§6.2). Returns the counts that make up the [ScanSummary].
 *
 * The `INCREMENTAL` skip of unchanged files (US-008 AC1) happens upstream in `CatalogRepositoryImpl`,
 * which reads [indexedFingerprints] to avoid re-extracting metadata; this class stays the
 * deterministic writer and receives the full discovered URI set so purge never removes a skipped but
 * still-present track.
 */
class CatalogSynchronizer
    @Inject
    constructor(
        private val database: SonusDatabase,
        private val artistDao: ArtistDao,
        private val genreDao: GenreDao,
        private val albumDao: AlbumDao,
        private val trackDao: TrackDao,
        private val timeProvider: TimeProvider,
    ) {
        /** `uri → fileLastModifiedMs` of the persisted catalog, driving the `INCREMENTAL` diff (AC1). */
        suspend fun indexedFingerprints(): Map<String, Long> =
            trackDao.fingerprints().associate { it.uri to it.fileLastModifiedMs }

        /**
         * @param processed tracks the scan decided to (re)write — new or changed files; in
         *   `INCREMENTAL` the unchanged ones are already filtered out upstream.
         * @param discoveredUris every URI found by the SAF traversal (including skipped ones); tracks
         *   whose URI is absent from this set are purged (Invariant 2).
         */
        suspend fun sync(
            processed: List<ScannedTrack>,
            discoveredUris: List<String>,
        ): ScanSummary =
            database.withTransaction {
                val dateAddedMs = timeProvider.nowMs()
                var added = 0
                processed.forEach { track ->
                    val artistId = resolveArtist(track.artistName)
                    val albumId = resolveAlbum(track.albumName, artistId)
                    val genreId = resolveGenre(track.genreName)
                    val entity = track.toEntity(artistId, albumId, genreId, dateAddedMs)
                    val existingId = trackDao.findIdByUri(track.uri)
                    if (existingId == null) {
                        trackDao.insert(entity)
                        added++
                    } else {
                        trackDao.update(entity.copy(id = existingId))
                    }
                }
                val purged =
                    if (discoveredUris.isEmpty()) {
                        trackDao.deleteAll()
                    } else {
                        trackDao.deleteWhereUriNotIn(discoveredUris)
                    }
                val orphanDimsPurged =
                    albumDao.purgeOrphans() + genreDao.purgeOrphans() + artistDao.purgeOrphans()
                ScanSummary(
                    added = added,
                    purged = purged,
                    unsupported = trackDao.countByAvailability(TrackAvailability.UNSUPPORTED),
                    orphanDimsPurged = orphanDimsPurged,
                )
            }

        private suspend fun resolveArtist(name: String?): Long {
            if (name.isNullOrBlank()) return SENTINEL_ID
            artistDao.insertIgnore(Artist(name = name))
            return artistDao.findIdByName(name) ?: SENTINEL_ID
        }

        private suspend fun resolveAlbum(
            name: String?,
            artistId: Long,
        ): Long {
            if (name.isNullOrBlank()) return SENTINEL_ID
            albumDao.insertIgnore(Album(name = name, artistId = artistId))
            return albumDao.findId(name, artistId) ?: SENTINEL_ID
        }

        private suspend fun resolveGenre(name: String?): Long {
            if (name.isNullOrBlank()) return SENTINEL_ID
            genreDao.insertIgnore(Genre(name = name))
            return genreDao.findIdByName(name) ?: SENTINEL_ID
        }

        private companion object {
            const val SENTINEL_ID = 1L
        }
    }
