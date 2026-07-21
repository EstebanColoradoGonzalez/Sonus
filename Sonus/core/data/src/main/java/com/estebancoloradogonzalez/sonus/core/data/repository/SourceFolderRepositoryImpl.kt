package com.estebancoloradogonzalez.sonus.core.data.repository

import androidx.room.withTransaction
import com.estebancoloradogonzalez.sonus.core.data.local.room.SonusDatabase
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.AlbumDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.ArtistDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.GenreDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.TrackDao
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
        private val database: SonusDatabase,
        private val dao: SourceFolderDao,
        private val trackDao: TrackDao,
        private val artistDao: ArtistDao,
        private val albumDao: AlbumDao,
        private val genreDao: GenreDao,
    ) : SourceFolderRepository {
        override fun observeAll(): Flow<List<SourceFolder>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun exists(treeUri: String): Boolean = dao.countByTreeUri(treeUri) > 0

        override suspend fun add(folder: SourceFolder): FolderId = dao.insert(folder.toEntity())

        override suspend fun findById(id: FolderId): SourceFolder? = dao.findById(id)?.toDomain()

        override suspend fun countTracksUnder(id: FolderId): Int = trackDao.countBySourceFolder(id)

        /**
         * Deletes the folder and, in the same Room transaction, purges the dimensions left orphan by
         * the CASCADE removal of its tracks (§6.2, US-006). Purge order matches [CatalogSynchronizer]:
         * albums first (frees their artists), then genres, then artists; the `id = 1` sentinels are
         * preserved by each DAO's `purgeOrphans` query.
         */
        override suspend fun remove(id: FolderId) {
            database.withTransaction {
                dao.deleteById(id)
                albumDao.purgeOrphans()
                genreDao.purgeOrphans()
                artistDao.purgeOrphans()
            }
        }
    }
