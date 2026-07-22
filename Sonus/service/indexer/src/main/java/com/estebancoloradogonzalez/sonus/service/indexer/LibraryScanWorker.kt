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
 * Runs the library scan off the main thread (ADR-006, [RNF-03]). A `CoroutineWorker` so it is
 * cancelable and survives UI lifecycle changes; single-flight is enforced by the scheduler. Progress
 * is published by the use case through the shared scan-state channel, not by this worker.
 *
 * The [ScanMode] is read from the [WorkerParameters] input data ([KEY_MODE]) so the onboarding
 * foundational scan (US-003, `FULL`) and the manual re-scan (US-007, `INCREMENTAL`/`FULL`) share a
 * single worker; a missing/invalid value defaults defensively to [ScanMode.FULL].
 */
@HiltWorker
class LibraryScanWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val scanLibraryUseCase: ScanLibraryUseCase,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val mode =
                inputData.getString(KEY_MODE)
                    ?.let { runCatching { ScanMode.valueOf(it) }.getOrNull() }
                    ?: ScanMode.FULL
            return when (scanLibraryUseCase(LibraryCommand.Scan(mode))) {
                is OperationResult.Success -> Result.success()
                is OperationResult.Failure -> Result.failure()
            }
        }

        companion object {
            /** Input-data key carrying the [ScanMode] name enqueued by the scheduler. */
            const val KEY_MODE = "scan_mode"
        }
    }
