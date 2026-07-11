package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.port.SafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Confirms the selection is ready to transit to scanning (`US-003`, AC7): there is at least one
 * folder and every `treeUri` still holds its persisted SAF permission. Does not start the scan.
 */
class VerifySourceFoldersReadyUseCase
    @Inject
    constructor(
        private val repository: SourceFolderRepository,
        private val safPermissionGateway: SafPermissionGateway,
    ) {
        suspend operator fun invoke(): Boolean {
            val folders = repository.observeAll().first()
            return folders.isNotEmpty() &&
                folders.all { safPermissionGateway.hasPersistedPermission(it.treeUri) }
        }
    }
