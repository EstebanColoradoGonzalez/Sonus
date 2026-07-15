package com.estebancoloradogonzalez.sonus.core.domain.usecase

import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogRepository
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Runs the library scan (`TRG-LIB-03`). Drives the observable state cycle
 * `Scanning → Syncing → Finished`/`Aborted` through the [ScanStateEmitter]: the data layer publishes
 * the intermediate progress while [CatalogRepository.synchronize] runs, and this use case publishes
 * the terminal state and returns the typed outcome.
 */
class ScanLibraryUseCase
    @Inject
    constructor(
        private val sourceFolderRepository: SourceFolderRepository,
        private val catalogRepository: CatalogRepository,
        private val scanStateEmitter: ScanStateEmitter,
    ) {
        suspend operator fun invoke(command: LibraryCommand.Scan): OperationResult<ScanSummary> {
            scanStateEmitter.update(ScanState.Scanning(processed = 0, total = null))
            val folders = sourceFolderRepository.observeAll().first()
            return when (val result = catalogRepository.synchronize(folders, command.mode)) {
                is OperationResult.Success -> {
                    scanStateEmitter.update(ScanState.Finished(result.data))
                    result
                }
                is OperationResult.Failure -> {
                    scanStateEmitter.update(ScanState.Aborted(result.error))
                    result
                }
            }
        }
    }
