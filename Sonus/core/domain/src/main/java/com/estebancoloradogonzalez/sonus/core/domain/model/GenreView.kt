package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Presentation projection of a Genre dimension entry (US-010 Escenario 4). [name] is blank for the
 * `id = 1` sentinel, resolved to a localized label in presentation (Invariant 4). [trackCount] counts
 * only visible tracks (not `MISSING`, Invariant 2).
 */
data class GenreView(
    val id: Long,
    val name: String,
    val trackCount: Int,
)
