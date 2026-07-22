package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.GenreView
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes the genre dimension for navigation (US-010 Escenario 4). */
class ListGenresUseCase
    @Inject
    constructor(
        private val catalogBrowseRepository: CatalogBrowseRepository,
    ) {
        operator fun invoke(contentType: ContentType? = null): Flow<List<GenreView>> =
            catalogBrowseRepository.genres(contentType)
    }
