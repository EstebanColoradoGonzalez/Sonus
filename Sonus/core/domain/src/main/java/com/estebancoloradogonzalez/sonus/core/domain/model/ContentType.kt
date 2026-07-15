package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Semantic nature of a [Track] (domain_and_state_model §4). Persisted by stable name, never by
 * ordinal. An absent type tag is [UNKNOWN]; it is never inferred (Invariant 4) and the Listener may
 * correct it later.
 */
enum class ContentType {
    MUSIC,
    PODCAST,
    UNKNOWN,
}
