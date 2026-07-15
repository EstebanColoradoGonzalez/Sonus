package com.estebancoloradogonzalez.sonus.core.domain.port

import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult

/**
 * Port for the durable system configuration singleton `AppSettings` (contrato §2.6, blueprint §4).
 * Implemented over Room in `:core:data`; the persistence technology is invisible to the domain.
 *
 * US-004 only touches the onboarding switch; theme preference (`TRG-CFG-01`) arrives with US-039.
 */
interface SettingsRepository {
    /**
     * Persist `onboardingCompleted = true` closing the first-run flow (`TRG-CFG-02`). Idempotent:
     * marking it when already `true` is a no-op. A persistence failure is returned as a typed
     * [OperationResult.Failure] (P1), never thrown across the boundary.
     */
    suspend fun completeOnboarding(): OperationResult<Unit>

    /** Whether the first-run flow was already completed; `false` on a fresh install (§6.1). */
    suspend fun isOnboardingCompleted(): Boolean
}
