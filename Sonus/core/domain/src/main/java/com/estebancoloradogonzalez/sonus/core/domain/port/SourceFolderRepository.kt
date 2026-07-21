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

    /** How many tracks were discovered under this folder (removal impact, US-006 `TRG-LIB-02`). */
    suspend fun countTracksUnder(id: FolderId): Int

    /**
     * Delete the folder with this id. Its tracks are removed in cascade (`onDelete = CASCADE`,
     * model §3) and the orphan dimensions (Artist/Album/Genre no longer referenced, except the
     * `id = 1` sentinels) are purged in the same transaction (§6.2, US-006). Before any track exists
     * (onboarding, US-002) the purge is a harmless no-op.
     */
    suspend fun remove(id: FolderId)
}
