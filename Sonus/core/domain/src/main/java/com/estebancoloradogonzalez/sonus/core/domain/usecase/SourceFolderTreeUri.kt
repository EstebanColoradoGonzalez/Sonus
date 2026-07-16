package com.estebancoloradogonzalez.sonus.core.domain.usecase

/**
 * Pure comparison of SAF tree URIs to detect Source Folder overlap (US-005 AC4).
 *
 * Two folders overlap when one is a proper ancestor of the other in the storage tree. A SAF tree URI
 * encodes its document id after `/tree/`, with path segments separated by the encoded slash `%2F`
 * (e.g. `primary%3AMusic` contains `primary%3AMusic%2FPodcasts`). The comparison stays on the raw
 * encoded id so the domain needs neither `android.net.Uri` nor URL decoding, keeping `:core:domain`
 * pure (coding-standards §4.1).
 *
 * Equality is deliberately NOT overlap: an identical `treeUri` is a duplicate, handled apart as
 * [com.estebancoloradogonzalez.sonus.core.domain.error.DomainError.DuplicateSourceFolder] (AC3).
 */
object SourceFolderTreeUri {
    private const val TREE_MARKER = "/tree/"
    private const val SEGMENT_SEPARATOR = "%2F"

    /** True when [first] and [second] are in a proper ancestor/descendant relationship. */
    fun overlaps(
        first: String,
        second: String,
    ): Boolean {
        val a = documentId(first)
        val b = documentId(second)
        if (a.isEmpty() || b.isEmpty() || a == b) {
            return false
        }
        return b.startsWith(a + SEGMENT_SEPARATOR) || a.startsWith(b + SEGMENT_SEPARATOR)
    }

    private fun documentId(treeUri: String): String = treeUri.substringAfter(TREE_MARKER, "")
}
