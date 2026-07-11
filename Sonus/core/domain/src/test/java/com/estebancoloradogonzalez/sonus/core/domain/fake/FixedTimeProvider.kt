package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.port.TimeProvider

/** Deterministic [TimeProvider] returning a fixed instant, injected in tests (coding-standards §5). */
class FixedTimeProvider(
    private val fixedNowMs: Long = 1_000L,
) : TimeProvider {
    override fun nowMs(): Long = fixedNowMs
}
