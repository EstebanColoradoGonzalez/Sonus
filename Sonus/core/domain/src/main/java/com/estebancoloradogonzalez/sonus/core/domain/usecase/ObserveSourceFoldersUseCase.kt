package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observable list of registered Source Folders (AC1/AC2/AC3/AC6). Feeds the onboarding screen so the
 * list and the "continue" enablement re-emit on every change.
 */
class ObserveSourceFoldersUseCase
    @Inject
    constructor(
        private val repository: SourceFolderRepository,
    ) {
        operator fun invoke(): Flow<List<SourceFolder>> = repository.observeAll()
    }
