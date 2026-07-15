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
                    val scanned = ArrayList<ScannedTrack>(total)
                    discovered.forEachIndexed { index, (folderId, file) ->
                        val metadata = id3DataSource.readMetadata(file.uri, file.mimeType)
                        scanned.add(metadata.toScannedTrack(file, folderId))
                        scanStateEmitter.update(ScanState.Scanning(processed = index + 1, total = total))
                    }
                    scanStateEmitter.update(ScanState.Syncing)
                    OperationResult.Success(catalogSynchronizer.sync(scanned))
                } catch (securityException: SecurityException) {
                    OperationResult.Failure(DomainError.ScanAborted(DomainError.PermissionRevoked))
                }
            }
    }
