package com.estebancoloradogonzalez.sonus.di

import com.estebancoloradogonzalez.sonus.core.data.repository.SourceFolderRepositoryImpl
import com.estebancoloradogonzalez.sonus.core.data.saf.SafPermissionGatewayImpl
import com.estebancoloradogonzalez.sonus.core.data.time.SystemTimeProvider
import com.estebancoloradogonzalez.sonus.core.domain.port.SafPermissionGateway
import com.estebancoloradogonzalez.sonus.core.domain.port.SourceFolderRepository
import com.estebancoloradogonzalez.sonus.core.domain.port.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the library/source-folder domain ports (US-002) to their `:core:data` implementations.
 * Lives in `:app` (composition root): the only module that knows both the contracts and the impls.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {
    @Binds
    abstract fun bindSourceFolderRepository(impl: SourceFolderRepositoryImpl): SourceFolderRepository

    @Binds
    abstract fun bindSafPermissionGateway(impl: SafPermissionGatewayImpl): SafPermissionGateway

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
