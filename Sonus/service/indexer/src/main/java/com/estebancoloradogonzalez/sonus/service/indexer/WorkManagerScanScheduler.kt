package com.estebancoloradogonzalez.sonus.service.indexer

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler
import javax.inject.Inject

/**
 * WorkManager-backed [ScanScheduler]. Enqueues the scan as unique work with
 * [ExistingWorkPolicy.KEEP], so a running scan is never duplicated (single-flight, contract §4.1).
 * The requested [ScanMode] travels to [LibraryScanWorker] through its input data.
 */
class WorkManagerScanScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
    ) : ScanScheduler {
        override fun enqueueScan(mode: ScanMode) {
            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<LibraryScanWorker>()
                    .setInputData(workDataOf(LibraryScanWorker.KEY_MODE to mode.name))
                    .build(),
            )
        }

        override fun cancel() {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        private companion object {
            const val UNIQUE_WORK_NAME = "library_scan"
        }
    }
