package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes whether the catalog holds no visible track, letting presentation distinguish an empty
 * catalog (US-010 Escenario 10) from a dimension with no matches (Escenario 11).
 */
class ObserveCatalogEmptyUseCase
    @Inject
    constructor(
        private val catalogBrowseRepository: CatalogBrowseRepository,
    ) {
        operator fun invoke(): Flow<Boolean> = catalogBrowseRepository.observeCatalogEmpty()
    }
