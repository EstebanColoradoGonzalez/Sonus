package com.estebancoloradogonzalez.sonus.di

import com.estebancoloradogonzalez.sonus.core.data.repository.SettingsRepositoryImpl
import com.estebancoloradogonzalez.sonus.core.domain.port.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the system-configuration domain port (US-004) to its `:core:data` implementation. Lives in
 * `:app` (composition root): the only module that knows both the contract and the impl.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
