package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Whether a candidate folder overlaps any already-registered Source Folder (US-005 AC4).
 *
 * In normal operation the Listener may add a folder nested inside (or containing) an existing one.
 * That is permitted (sovereignty, Invariante 3); this use case only reports the overlap so the
 * presentation can warn without blocking the registration. Track de-duplication is guaranteed later
 * by the natural identity `Track.uri` during scanning (US-007/US-008), never here.
 */
class DetectSourceFolderOverlapUseCase
    @Inject
    constructor(
        private val repository: SourceFolderRepository,
    ) {
        suspend operator fun invoke(candidateTreeUri: String): Boolean =
            repository.observeAll().first().any { existing ->
                SourceFolderTreeUri.overlaps(existing.treeUri, candidateTreeUri)
            }
    }
