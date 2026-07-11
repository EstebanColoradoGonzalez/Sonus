package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.port.SafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.port.TimeProvider
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import javax.inject.Inject

/**
 * Registers a Source Folder (`TRG-LIB-01`, [RF-01]).
 *
 * Enforces uniqueness before acquiring any permission (AC4): a duplicate `treeUri` fails as a
 * non-destructive WARNING keeping the existing folder. Otherwise it takes the persistable SAF
 * permission (AC1/AC5) and persists the folder stamped with the injected time.
 */
class AddSourceFolderUseCase
    @Inject
    constructor(
        private val repository: SourceFolderRepository,
        private val safPermissionGateway: SafPermissionGateway,
        private val timeProvider: TimeProvider,
    ) {
        suspend operator fun invoke(command: LibraryCommand.AddSourceFolder): OperationResult<FolderId> =
            when {
                repository.exists(command.treeUri) ->
                    OperationResult.Failure(DomainError.DuplicateSourceFolder)
                !safPermissionGateway.takePersistablePermission(command.treeUri) ->
                    OperationResult.Failure(DomainError.PermissionDenied)
                else -> {
                    val folder =
                        SourceFolder(
                            treeUri = command.treeUri,
                            displayPath = safPermissionGateway.resolveDisplayPath(command.treeUri),
                            dateAddedMs = timeProvider.nowMs(),
                        )
                    OperationResult.Success(repository.add(folder))
                }
            }
    }
