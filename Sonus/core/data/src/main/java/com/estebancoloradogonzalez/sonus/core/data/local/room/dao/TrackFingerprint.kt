package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

/**
 * Minimal `track` projection used by the `INCREMENTAL` scan diff (US-008): the natural identity
 * [uri] paired with the on-disk modification time [fileLastModifiedMs] last persisted for it. Lets
 * the scan decide, without loading full `Track` rows, which discovered files changed since the last
 * scan and therefore need re-processing.
 */
data class TrackFingerprint(
    val uri: String,
    val fileLastModifiedMs: Long,
)
