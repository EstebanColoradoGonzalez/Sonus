package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query

/** Room DAO for the `app_settings` singleton (blueprint §3, C-03; contrato §2.6). */
@Dao
interface SettingsDao {
    /** The onboarding switch, or null if the singleton row is missing (treated as `false` upstream). */
    @Query("SELECT onboardingCompleted FROM app_settings WHERE id = 1")
    suspend fun isOnboardingCompleted(): Boolean?

    /**
     * Marks the onboarding as completed on the singleton (`TRG-CFG-02`). Idempotent by construction:
     * an `UPDATE ... = 1` leaves an already-`true` row unchanged (Escenario 2).
     */
    @Query("UPDATE app_settings SET onboardingCompleted = 1 WHERE id = 1")
    suspend fun markOnboardingCompleted()
}
