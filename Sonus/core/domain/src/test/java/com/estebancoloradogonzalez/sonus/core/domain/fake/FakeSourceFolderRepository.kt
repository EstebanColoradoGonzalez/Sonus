package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory fake of [SourceFolderRepository] preserving its contract (fakes over mocks for ports —
 * coding-standards §5.3). Assigns incremental ids; holds no business rule.
 */
class FakeSourceFolderRepository : SourceFolderRepository {
    private val folders = MutableStateFlow<List<SourceFolder>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<SourceFolder>> = folders

    override suspend fun exists(treeUri: String): Boolean = folders.value.any { it.treeUri == treeUri }

    override suspend fun add(folder: SourceFolder): FolderId {
        val id = nextId++
        folders.update { current -> current + folder.copy(id = id) }
        return id
    }

    override suspend fun findById(id: FolderId): SourceFolder? = folders.value.firstOrNull { it.id == id }

    override suspend fun remove(id: FolderId) {
        folders.update { current -> current.filterNot { it.id == id } }
    }
}
