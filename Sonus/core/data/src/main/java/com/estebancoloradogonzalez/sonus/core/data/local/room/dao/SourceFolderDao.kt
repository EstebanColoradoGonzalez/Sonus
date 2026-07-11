package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder
import kotlinx.coroutines.flow.Flow

/** Room DAO for the `source_folder` table (blueprint §3, C-03). */
@Dao
interface SourceFolderDao {
    @Query("SELECT * FROM source_folder ORDER BY dateAddedMs ASC")
    fun observeAll(): Flow<List<SourceFolder>>

    @Query("SELECT COUNT(*) FROM source_folder WHERE treeUri = :treeUri")
    suspend fun countByTreeUri(treeUri: String): Int

    @Insert
    suspend fun insert(folder: SourceFolder): Long

    @Query("SELECT * FROM source_folder WHERE id = :id")
    suspend fun findById(id: Long): SourceFolder?

    @Query("DELETE FROM source_folder WHERE id = :id")
    suspend fun deleteById(id: Long)
}
