package com.estebancoloradogonzalez.sonus.service.indexer

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.estebancoloradogonzalez.sonus.core.domain.command.LibraryCommand
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.estebancoloradogonzalez.sonus.core.domain.usecase.ScanLibraryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the foundational scan off the main thread (ADR-006, [RNF-03]). A `CoroutineWorker` so it is
 * cancelable and survives UI lifecycle changes; single-flight is enforced by the scheduler. Progress
 * is published by the use case through the shared scan-state channel, not by this worker.
 */
@HiltWorker
class LibraryScanWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val scanLibraryUseCase: ScanLibraryUseCase,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            when (scanLibraryUseCase(LibraryCommand.Scan(ScanMode.FULL))) {
                is OperationResult.Success -> Result.success()
                is OperationResult.Failure -> Result.failure()
            }
    }
