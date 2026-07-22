package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import kotlinx.coroutines.flow.Flow

/**
 * Read-only DAO for taxonomic navigation (`TRG-NAV-01`, [RF-12], blueprint §3). Every query is
 * observable (`Flow`) so any catalog mutation re-emits to the UI (SDD §4.1) and runs off the main
 * thread (Room, [CT-08]). It projects directly onto the domain read-models via column aliases; the
 * domain classes stay Room-agnostic.
 *
 * Invariants enforced in SQL: `MISSING` tracks are always excluded (Invariant 2); counts consider
 * only visible tracks; the `id = 1` sentinel rows are sorted last (their blank name is localized in
 * presentation, Invariant 4); album listing keys on `Album.artistId` (`[F-7]`). Absent metadata is
 * never inferred — nullable columns are projected as-is.
 */
@Dao
interface CatalogBrowseDao {
    @Query(
        "SELECT t.id AS id, t.uri AS uri, t.title AS title, " +
            "t.artistId AS artistId, ar.name AS artistName, " +
            "t.albumId AS albumId, al.name AS albumName, " +
            "t.genreId AS genreId, g.name AS genreName, " +
            "t.contentType AS contentType, t.trackNumber AS trackNumber, " +
            "t.durationMs AS durationMs, t.hasEmbeddedArtwork AS hasEmbeddedArtwork, " +
            "t.availability AS availability " +
            "FROM track t " +
            "JOIN artist ar ON ar.id = t.artistId " +
            "JOIN album al ON al.id = t.albumId " +
            "JOIN genre g ON g.id = t.genreId " +
            "WHERE t.availability != 'MISSING' " +
            "AND (:contentType IS NULL OR t.contentType = :contentType) " +
            "AND (:genreId IS NULL OR t.genreId = :genreId) " +
            "AND (:artistId IS NULL OR t.artistId = :artistId) " +
            "AND (:availability IS NULL OR t.availability = :availability) " +
            "ORDER BY t.title IS NULL, t.title COLLATE NOCASE",
    )
    fun browseTracks(
        contentType: ContentType?,
        genreId: Long?,
        artistId: Long?,
        availability: TrackAvailability?,
    ): Flow<List<TrackView>>

    @Query(
        "SELECT t.id AS id, t.uri AS uri, t.title AS title, " +
            "t.artistId AS artistId, ar.name AS artistName, " +
            "t.albumId AS albumId, al.name AS albumName, " +
            "t.genreId AS genreId, g.name AS genreName, " +
            "t.contentType AS contentType, t.trackNumber AS trackNumber, " +
            "t.durationMs AS durationMs, t.hasEmbeddedArtwork AS hasEmbeddedArtwork, " +
            "t.availability AS availability " +
            "FROM track t " +
            "JOIN artist ar ON ar.id = t.artistId " +
            "JOIN album al ON al.id = t.albumId " +
            "JOIN genre g ON g.id = t.genreId " +
            "WHERE t.availability != 'MISSING' AND t.albumId = :albumId " +
            "AND (:availability IS NULL OR t.availability = :availability) " +
            "ORDER BY t.trackNumber IS NULL, t.trackNumber ASC, t.title COLLATE NOCASE",
    )
    fun browseAlbumTracks(
        albumId: Long,
        availability: TrackAvailability?,
    ): Flow<List<TrackView>>

    @Query(
        "SELECT g.id AS id, g.name AS name, COUNT(t.id) AS trackCount " +
            "FROM genre g " +
            "JOIN track t ON t.genreId = g.id " +
            "WHERE t.availability != 'MISSING' " +
            "AND (:contentType IS NULL OR t.contentType = :contentType) " +
            "GROUP BY g.id, g.name " +
            "ORDER BY g.id = 1, g.name COLLATE NOCASE",
    )
    fun genres(contentType: ContentType?): Flow<List<GenreView>>

    @Query(
        "SELECT ar.id AS id, ar.name AS name, COUNT(t.id) AS trackCount " +
            "FROM artist ar " +
            "JOIN track t ON t.artistId = ar.id " +
            "WHERE t.availability != 'MISSING' " +
            "AND (:genreId IS NULL OR t.genreId = :genreId) " +
            "AND (:contentType IS NULL OR t.contentType = :contentType) " +
            "GROUP BY ar.id, ar.name " +
            "ORDER BY ar.id = 1, ar.name COLLATE NOCASE",
    )
    fun artists(
        genreId: Long?,
        contentType: ContentType?,
    ): Flow<List<ArtistView>>

    @Query(
        "SELECT al.id AS id, al.name AS name, al.artistId AS artistId, ar.name AS artistName, " +
            "EXISTS(SELECT 1 FROM track tk WHERE tk.albumId = al.id " +
            "AND tk.hasEmbeddedArtwork = 1 AND tk.availability != 'MISSING') AS hasArtwork, " +
            "COUNT(t.id) AS trackCount " +
            "FROM album al " +
            "JOIN artist ar ON ar.id = al.artistId " +
            "JOIN track t ON t.albumId = al.id " +
            "WHERE t.availability != 'MISSING' " +
            "AND (:artistId IS NULL OR al.artistId = :artistId) " +
            "AND (:contentType IS NULL OR t.contentType = :contentType) " +
            "GROUP BY al.id, al.name, al.artistId, ar.name " +
            "ORDER BY al.id = 1, al.name COLLATE NOCASE",
    )
    fun albums(
        artistId: Long?,
        contentType: ContentType?,
    ): Flow<List<AlbumView>>

    @Query("SELECT COUNT(*) = 0 FROM track WHERE availability != 'MISSING'")
    fun observeCatalogEmpty(): Flow<Boolean>
}
