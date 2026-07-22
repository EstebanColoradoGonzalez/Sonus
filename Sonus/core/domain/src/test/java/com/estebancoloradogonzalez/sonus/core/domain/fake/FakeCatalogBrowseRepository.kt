package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [CatalogBrowseRepository] returning preconfigured streams and recording the arguments it
 * was called with (fakes over mocks for ports — coding-standards §5.3). Holds no query rule.
 */
class FakeCatalogBrowseRepository(
    private val tracks: List<TrackView> = emptyList(),
    private val genres: List<GenreView> = emptyList(),
    private val artists: List<ArtistView> = emptyList(),
    private val albums: List<AlbumView> = emptyList(),
    private val catalogEmpty: Boolean = false,
) : CatalogBrowseRepository {
    var receivedBrowseQuery: BrowseQuery? = null
    var receivedGenresContentType: ContentType? = null
    var receivedArtistsGenreId: Long? = null
    var receivedAlbumsArtistId: Long? = null

    override fun browse(query: BrowseQuery): Flow<List<TrackView>> {
        receivedBrowseQuery = query
        return flowOf(tracks)
    }

    override fun genres(contentType: ContentType?): Flow<List<GenreView>> {
        receivedGenresContentType = contentType
        return flowOf(genres)
    }

    override fun artists(
        genreId: Long?,
        contentType: ContentType?,
    ): Flow<List<ArtistView>> {
        receivedArtistsGenreId = genreId
        return flowOf(artists)
    }

    override fun albums(
        artistId: Long?,
        contentType: ContentType?,
    ): Flow<List<AlbumView>> {
        receivedAlbumsArtistId = artistId
        return flowOf(albums)
    }

    override fun observeCatalogEmpty(): Flow<Boolean> = flowOf(catalogEmpty)
}
