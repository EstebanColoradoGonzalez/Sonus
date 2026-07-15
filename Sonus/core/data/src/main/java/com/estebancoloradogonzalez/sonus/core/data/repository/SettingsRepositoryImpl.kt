package com.estebancoloradogonzalez.sonus.core.data.repository

import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SettingsDao
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.port.SettingsRepository
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** Room-backed implementation of [SettingsRepository] (blueprint §3, C-03; contrato §2.6). */
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dao: SettingsDao,
    ) : SettingsRepository {
        override suspend fun completeOnboarding(): OperationResult<Unit> =
            try {
                dao.markOnboardingCompleted()
                OperationResult.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Storage failure captured at the border: mapped to a typed error as a value (P1);
                // the operation is idempotent and retriable, so callers degrade gracefully (P3).
                OperationResult.Failure(DomainError.SettingsPersistenceFailed)
            }

        override suspend fun isOnboardingCompleted(): Boolean = dao.isOnboardingCompleted() ?: false
    }
