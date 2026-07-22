package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.ArtistView
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes the artist dimension, optionally within a genre (US-010 Escenario 4/5). */
class ListArtistsUseCase
    @Inject
    constructor(
        private val catalogBrowseRepository: CatalogBrowseRepository,
    ) {
        operator fun invoke(
            genreId: Long? = null,
            contentType: ContentType? = null,
        ): Flow<List<ArtistView>> = catalogBrowseRepository.artists(genreId, contentType)
    }
