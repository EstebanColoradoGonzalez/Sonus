package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.model.ScanSummary
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult

/**
 * Fake [CatalogRepository] returning a preconfigured [result] and recording the arguments it was
 * called with (fakes over mocks for ports — coding-standards §5.3). Holds no business rule.
 */
class FakeCatalogRepository(
    private val result: OperationResult<ScanSummary>,
) : CatalogRepository {
    var receivedFolders: List<SourceFolder>? = null
    var receivedMode: ScanMode? = null

    override suspend fun synchronize(
        sourceFolders: List<SourceFolder>,
        mode: ScanMode,
    ): OperationResult<ScanSummary> {
        receivedFolders = sourceFolders
        receivedMode = mode
        return result
    }
}
