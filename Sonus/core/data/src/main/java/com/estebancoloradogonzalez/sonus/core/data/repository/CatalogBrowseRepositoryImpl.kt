package com.estebancoloradogonzalez.sonus.core.data.repository

import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.CatalogBrowseDao
import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implements the read-side catalog port over [CatalogBrowseDao] (blueprint §3). Room's `Flow` queries
 * already run off the main thread and re-emit on catalog mutations, so no dispatcher orchestration is
 * needed here. When a track view is scoped to a single album the album-ordered query is used so tracks
 * come back by `trackNumber` (US-010 Escenario 6); otherwise the general title-ordered query applies.
 */
class CatalogBrowseRepositoryImpl
    @Inject
    constructor(
        private val catalogBrowseDao: CatalogBrowseDao,
    ) : CatalogBrowseRepository {
        override fun browse(query: BrowseQuery): Flow<List<TrackView>> {
            val albumId = query.albumId
            return if (albumId != null) {
                catalogBrowseDao.browseAlbumTracks(albumId, query.availability)
            } else {
                catalogBrowseDao.browseTracks(
                    contentType = query.contentType,
                    genreId = query.genreId,
                    artistId = query.artistId,
                    availability = query.availability,
                )
            }
        }

        override fun genres(contentType: ContentType?): Flow<List<GenreView>> = catalogBrowseDao.genres(contentType)

        override fun artists(
            genreId: Long?,
            contentType: ContentType?,
        ): Flow<List<ArtistView>> = catalogBrowseDao.artists(genreId, contentType)

        override fun albums(
            artistId: Long?,
            contentType: ContentType?,
        ): Flow<List<AlbumView>> = catalogBrowseDao.albums(artistId, contentType)

        override fun observeCatalogEmpty(): Flow<Boolean> = catalogBrowseDao.observeCatalogEmpty()
    }
