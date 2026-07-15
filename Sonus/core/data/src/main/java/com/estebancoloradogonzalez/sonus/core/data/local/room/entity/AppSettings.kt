package com.estebancoloradogonzalez.sonus.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.estebancoloradogonzalez.sonus.core.domain.model.ThemePreference

/**
 * Room `@Entity` of the system configuration (domain_and_state_model §2). A **singleton**: a single
 * row `id = 1`, seeded in the Big Bang (§6.1) with `onboardingCompleted = false` and
 * `themePreference = SYSTEM`. US-004 only mutates `onboardingCompleted`; `themePreference` is held
 * for US-039 (`TRG-CFG-01`) and serialised by stable name via `RoomTypeConverters`.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean,
    val themePreference: ThemePreference,
)
