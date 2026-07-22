package com.estebancoloradogonzalez.sonus.core.data.repository

import com.estebancoloradogonzalez.sonus.core.data.id3.Id3DataSource
import com.estebancoloradogonzalez.sonus.core.data.local.room.CatalogSynchronizer
import com.estebancoloradogonzalez.sonus.core.data.local.saf.SafDataSource
import com.estebancoloradogonzalez.sonus.core.data.mapper.toScannedTrack
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanState
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.model.ScannedTrack
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogRepository
import com.estebancoloradogonzalez.sonus.core.domain.port.DispatcherProvider
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Orchestrates the scan on an injected I/O dispatcher (blueprint C-04): recursively discovers audio
 * files (SAF), extracts metadata (ID3) publishing deterministic `Scanning` progress, then delegates
 * the deterministic write to [CatalogSynchronizer] while publishing `Syncing`. A lost folder access
 * (`SecurityException`) is caught at this border and returned as a value —
 * `ScanAborted(PermissionRevoked)` — preserving the last coherent catalog (§5.3, principle P1).
 *
 * In [ScanMode.INCREMENTAL] the real diff by `fileLastModifiedMs` (US-008) skips unchanged files
 * *before* extracting their metadata (AC1): a discovered file whose mtime equals the fingerprint
 * persisted for its URI is neither re-read nor re-written, while new or modified files are processed
 * as usual. The purge still runs against the *full* discovered URI set, so skipped-but-present tracks
 * are never removed. [ScanMode.FULL] processes every discovered file regardless of fingerprints (AC8).
 */
class CatalogRepositoryImpl
    @Inject
    constructor(
        private val safDataSource: SafDataSource,
        private val id3DataSource: Id3DataSource,
        private val catalogSynchronizer: CatalogSynchronizer,
        private val scanStateEmitter: ScanStateEmitter,
        private val dispatcherProvider: DispatcherProvider,
    ) : CatalogRepository {
        override suspend fun synchronize(
            sourceFolders: List<SourceFolder>,
            mode: ScanMode,
        ): OperationResult<ScanSummary> =
            withContext(dispatcherProvider.io) {
                try {
                    scanStateEmitter.update(ScanState.Scanning(processed = 0, total = null))
                    val discovered =
                        sourceFolders.flatMap { folder ->
                            safDataSource.listAudioFiles(folder.treeUri).map { folder.id to it }
                        }
                    val total = discovered.size
                    val fingerprints =
                        if (mode == ScanMode.INCREMENTAL) catalogSynchronizer.indexedFingerprints() else emptyMap()
                    val processed = ArrayList<ScannedTrack>(total)
                    discovered.forEachIndexed { index, (folderId, file) ->
                        val unchanged =
                            mode == ScanMode.INCREMENTAL && fingerprints[file.uri] == file.lastModifiedMs
                        if (!unchanged) {
                            val metadata = id3DataSource.readMetadata(file.uri, file.mimeType)
                            processed.add(metadata.toScannedTrack(file, folderId))
                        }
                        scanStateEmitter.update(ScanState.Scanning(processed = index + 1, total = total))
                    }
                    scanStateEmitter.update(ScanState.Syncing)
                    OperationResult.Success(catalogSynchronizer.sync(processed, discovered.map { it.second.uri }))
                } catch (securityException: SecurityException) {
                    OperationResult.Failure(DomainError.ScanAborted(DomainError.PermissionRevoked))
                }
            }
    }
