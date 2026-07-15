package com.estebancoloradogonzalez.sonus.core.data.local.saf

/** An audio file discovered while traversing a source folder via SAF. */
data class DiscoveredFile(
    val uri: String,
    val mimeType: String,
    val lastModifiedMs: Long,
)

/**
 * Storage discovery port over SAF (ADR-003, canal C5). The single storage-access mechanism: no
 * `MediaStore`, no media-runtime permissions, no network (Invariant 1). A revoked permission surfaces
 * as a `SecurityException`, translated to a domain value at the repository border (principle P1).
 */
interface SafDataSource {
    suspend fun listAudioFiles(treeUri: String): List<DiscoveredFile>
}
