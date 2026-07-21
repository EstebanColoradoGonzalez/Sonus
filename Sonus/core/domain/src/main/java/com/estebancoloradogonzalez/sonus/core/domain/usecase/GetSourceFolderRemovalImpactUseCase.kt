package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import javax.inject.Inject

/**
 * Computes the removal impact of a Source Folder for the confirmation dialog (US-006, `TRG-LIB-02`,
 * Invariant 5): how many tracks would be purged in cascade if the folder is removed.
 *
 * The Listener decides with real data before confirming (sovereignty, Invariant 3). Playlist/queue
 * impact is out of scope until those tables exist; only the track count is reported today.
 */
class GetSourceFolderRemovalImpactUseCase
    @Inject
    constructor(
        private val repository: SourceFolderRepository,
    ) {
        suspend operator fun invoke(folderId: FolderId): Int = repository.countTracksUnder(folderId)
    }
