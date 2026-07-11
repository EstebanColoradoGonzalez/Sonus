package com.estebancoloradogonzalez.sonus.core.data.repository

import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import com.estebancoloradogonzalez.sonus.core.data.mapper.toDomain
import com.estebancoloradogonzalez.sonus.core.data.mapper.toEntity
import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed implementation of [SourceFolderRepository] (blueprint §3, C-03). */
class SourceFolderRepositoryImpl
    @Inject
    constructor(
        private val dao: SourceFolderDao,
    ) : SourceFolderRepository {
        override fun observeAll(): Flow<List<SourceFolder>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun exists(treeUri: String): Boolean = dao.countByTreeUri(treeUri) > 0

        override suspend fun add(folder: SourceFolder): FolderId = dao.insert(folder.toEntity())

        override suspend fun findById(id: FolderId): SourceFolder? = dao.findById(id)?.toDomain()

        override suspend fun remove(id: FolderId) = dao.deleteById(id)
    }
