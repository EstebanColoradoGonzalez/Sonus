package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.port.SafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import javax.inject.Inject

/**
 * Removes a Source Folder before scanning (`TRG-LIB-02`, AC6).
 *
 * Light removal: it releases the SAF permission and deletes the folder. There is no cascade purge
 * because no tracks exist yet (the cascade removal after scanning is US-006). A missing id fails as
 * `ERR_ENTITY_NOT_FOUND`.
 */
class RemoveSourceFolderUseCase
    @Inject
    constructor(
        private val repository: SourceFolderRepository,
        private val safPermissionGateway: SafPermissionGateway,
    ) {
        suspend operator fun invoke(command: LibraryCommand.RemoveSourceFolder): OperationResult<Unit> {
            val folder =
                repository.findById(command.folderId)
                    ?: return OperationResult.Failure(
                        DomainError.EntityNotFound("SourceFolder", command.folderId),
                    )
            safPermissionGateway.releasePersistablePermission(folder.treeUri)
            repository.remove(folder.id)
            return OperationResult.Success(Unit)
        }
    }
