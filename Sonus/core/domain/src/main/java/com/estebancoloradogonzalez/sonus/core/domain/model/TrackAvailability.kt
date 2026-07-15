package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Playability state of a [Track] (domain_and_state_model §4). Persisted by stable name.
 *
 * - [AVAILABLE]: the file exists and is decodable.
 * - [UNSUPPORTED]: the format cannot be decoded; the track stays visible but not playable
 *   ([Restricción 2]).
 * - [MISSING]: detected absent during playback; pending purge on the next scan.
 */
enum class TrackAvailability {
    AVAILABLE,
    UNSUPPORTED,
    MISSING,
}
