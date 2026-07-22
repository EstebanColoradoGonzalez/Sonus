package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.BrowseQuery
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackView
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes the filtered track view of the catalog (`TRG-NAV-01`, [RF-12], channel C2). */
class BrowseCatalogUseCase
    @Inject
    constructor(
        private val catalogBrowseRepository: CatalogBrowseRepository,
    ) {
        operator fun invoke(query: BrowseQuery): Flow<List<TrackView>> = catalogBrowseRepository.browse(query)
    }
