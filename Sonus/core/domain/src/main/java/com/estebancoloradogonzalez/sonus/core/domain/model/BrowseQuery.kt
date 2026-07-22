package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Intersection of taxonomic filters for the catalog track view (`TRG-NAV-01`, [RF-12]). Every `null`
 * field means "no restriction on that dimension". `MISSING` tracks are always excluded regardless of
 * [availability] (Invariant 2); [availability] further narrows the visible set (e.g. only `AVAILABLE`).
 *
 * US-010 covers the objective dimensions. The textual filter (US-011) and the sort criterion
 * (US-012, `TrackSort`) are intentionally out of scope here and will extend this query later; the Room
 * layer applies the default ordering each navigation context requires (track number inside an album,
 * title otherwise).
 */
data class BrowseQuery(
    val contentType: ContentType? = null,
    val genreId: Long? = null,
    val artistId: Long? = null,
    val albumId: Long? = null,
    val availability: TrackAvailability? = null,
)
