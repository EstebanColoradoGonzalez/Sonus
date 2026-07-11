package com.estebancoloradogonzalez.sonus.core.domain.port

import com.estebancoloradogonzalez.sonus.core.domain.model.FolderId
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder
import kotlinx.coroutines.flow.Flow

/**
 * Port for the durable registry of Source Folders ([RF-01], blueprint §4.1). Implemented over Room
 * in `:core:data`; the persistence technology is invisible to the domain.
 */
interface SourceFolderRepository {
    /** Observable list of registered folders; re-emits on every change (channel C2). */
    fun observeAll(): Flow<List<SourceFolder>>

    /** Whether a folder with this `treeUri` is already registered (uniqueness guard, AC4). */
    suspend fun exists(treeUri: String): Boolean

    /** Persist a new folder and return its generated [FolderId]. */
    suspend fun add(folder: SourceFolder): FolderId

    /** The folder with this id, or null if it does not exist. */
    suspend fun findById(id: FolderId): SourceFolder?

    /** Delete the folder with this id (light removal, no cascade in US-002). */
    suspend fun remove(id: FolderId)
}
