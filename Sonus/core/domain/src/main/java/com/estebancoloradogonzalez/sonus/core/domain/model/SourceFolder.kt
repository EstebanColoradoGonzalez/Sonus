package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Pure domain model of a Source Folder: a directory the Listener explicitly authorizes as a scan
 * perimeter ([RF-01] / SDD §4.1 Apalancamiento 2). Agnostic to persistence — the Room `@Entity`
 * lives in `:core:data` and is mapped to/from this type (domain_and_state_model §2).
 *
 * The identity of access is [treeUri] (a SAF tree URI held as a `String` so the domain stays free of
 * `android.net.Uri`), unique across the registry.
 */
data class SourceFolder(
    val id: FolderId = 0,
    val treeUri: String,
    val displayPath: String,
    val dateAddedMs: Long,
)
