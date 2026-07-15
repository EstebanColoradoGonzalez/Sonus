package com.estebancoloradogonzalez.sonus.core.data.repository

import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SettingsDao
import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError
import com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SettingsRepositoryImplTest {
    private val dao = mockk<SettingsDao>()

    @Test
    fun `completeOnboarding returns Success when the DAO write succeeds`() =
        runTest {
            // Arrange (Escenario 1)
            coEvery { dao.markOnboardingCompleted() } just Runs
            val repository = SettingsRepositoryImpl(dao)

            // Act
            val result = repository.completeOnboarding()

            // Assert
            assertThat(result).isEqualTo(OperationResult.Success(Unit))
            coVerify { dao.markOnboardingCompleted() }
        }

    @Test
    fun `completeOnboarding maps a storage failure to a typed Failure`() =
        runTest {
            // Arrange (Escenario 6): the Room write throws at the border
            coEvery { dao.markOnboardingCompleted() } throws RuntimeException("disk full")
            val repository = SettingsRepositoryImpl(dao)

            // Act
            val result = repository.completeOnboarding()

            // Assert
            assertThat(result).isEqualTo(OperationResult.Failure(DomainError.SettingsPersistenceFailed))
        }

    @Test
    fun `isOnboardingCompleted returns false when the singleton row is missing`() =
        runTest {
            // Arrange (Escenario 5 — defensive default)
            coEvery { dao.isOnboardingCompleted() } returns null
            val repository = SettingsRepositoryImpl(dao)

            // Act + Assert
            assertThat(repository.isOnboardingCompleted()).isFalse()
        }

    @Test
    fun `isOnboardingCompleted reflects the persisted value`() =
        runTest {
            // Arrange (Escenario 3)
            coEvery { dao.isOnboardingCompleted() } returns true
            val repository = SettingsRepositoryImpl(dao)

            // Act + Assert
            assertThat(repository.isOnboardingCompleted()).isTrue()
        }
}
