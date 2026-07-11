package com.estebancoloradogonzalez.sonus.core.domain.port

/**
 * Injectable source of the current instant (coding-standards §5): time is never read from the real
 * clock inside domain logic, so it can be fixed deterministically in tests.
 */
interface TimeProvider {
    /** Current instant as epoch milliseconds UTC (domain_and_state_model §1). */
    fun nowMs(): Long
}
