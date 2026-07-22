package com.estebancoloradogonzalez.sonus.core.domain.port

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import kotlinx.coroutines.flow.Flow

/**
 * Read-side catalog port for taxonomic navigation (`TRG-NAV-01`, [RF-12]). Split from the scan-side
 * `CatalogRepository` by SRP: this contract is purely observable queries backed by the indexed Room
 * tables (blueprint §3, [F-1]) and implemented in `:core:data`. Every stream re-emits on any catalog
 * mutation (Bucle de Coherencia del Catálogo, SDD §4.1); an empty result is a valid empty list, never
 * an error (US-010 Escenario 10/11). All queries exclude `MISSING` tracks (Invariant 2).
 */
interface CatalogBrowseRepository {
    /** Tracks matching [query]; ordered by track number inside an album, by title otherwise. */
    fun browse(query: BrowseQuery): Flow<List<TrackView>>

    /** Genres that have at least one visible track, optionally restricted to [contentType]. */
    fun genres(contentType: ContentType?): Flow<List<GenreView>>

    /** Artists that have at least one visible track (`Track.artistId`), optionally within [genreId]/[contentType]. */
    fun artists(
        genreId: Long?,
        contentType: ContentType?,
    ): Flow<List<ArtistView>>

    /** Albums (by `Album.artistId`, `[F-7]`), optionally restricted to [artistId]/[contentType]. */
    fun albums(
        artistId: Long?,
        contentType: ContentType?,
    ): Flow<List<AlbumView>>

    /** `true` while the catalog holds no visible track — distinguishes empty catalog from empty dimension. */
    fun observeCatalogEmpty(): Flow<Boolean>
}
