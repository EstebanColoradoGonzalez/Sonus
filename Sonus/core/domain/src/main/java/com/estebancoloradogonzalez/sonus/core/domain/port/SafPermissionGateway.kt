package com.estebancoloradogonzalez.sonus.core.domain.port

/**
 * Port toward the Storage Access Framework's per-folder authorization model (ADR-003, contract §1.2).
 *
 * SAF is the single access mechanism to storage; each folder's `treeUri` is the effective
 * authorization boundary. The Android implementation lives in `:core:data`; infrastructure
 * exceptions are caught at that edge and surfaced here as plain values (principle P1) — no exception
 * crosses the domain boundary.
 */
interface SafPermissionGateway {
    /**
     * Take the persistable read/write permission over [treeUri]. Returns false when the OS does not
     * grant it (mapped to `ERR_PERMISSION_DENIED`), never throws across the boundary.
     */
    suspend fun takePersistablePermission(treeUri: String): Boolean

    /** Release the persisted permission over [treeUri] (light removal, AC6). */
    suspend fun releasePersistablePermission(treeUri: String)

    /** Human-readable path derived from [treeUri] for display to the Listener. */
    fun resolveDisplayPath(treeUri: String): String

    /** Whether the persisted permission over [treeUri] is still held (readiness check, AC7). */
    suspend fun hasPersistedPermission(treeUri: String): Boolean
}
