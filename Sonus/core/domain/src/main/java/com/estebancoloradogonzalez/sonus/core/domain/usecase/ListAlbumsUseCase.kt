package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.AlbumView
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes the album dimension by `Album.artistId` (`[F-7]`, US-010 Escenario 5). */
class ListAlbumsUseCase
    @Inject
    constructor(
        private val catalogBrowseRepository: CatalogBrowseRepository,
    ) {
        operator fun invoke(
            artistId: Long? = null,
            contentType: ContentType? = null,
        ): Flow<List<AlbumView>> = catalogBrowseRepository.albums(artistId, contentType)
    }
