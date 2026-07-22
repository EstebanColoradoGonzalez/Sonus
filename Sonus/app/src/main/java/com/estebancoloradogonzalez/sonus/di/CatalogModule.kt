package com.estebancoloradogonzalez.sonus.di

import android.content.Context
import androidx.work.WorkManager
import com.estebancoloradogonzalez.sonus.core.data.id3.Id3DataSource
import com.estebancoloradogonzalez.sonus.core.data.id3.Id3DataSourceImpl
import com.estebancoloradogonzalez.sonus.core.data.local.saf.SafDataSource
import com.estebancoloradogonzalez.sonus.core.data.local.saf.SafDataSourceImpl
import com.estebancoloradogonzalez.sonus.core.data.repository.CatalogBrowseRepositoryImpl
import com.estebancoloradogonzalez.sonus.core.data.repository.CatalogRepositoryImpl
import com.estebancoloradogonzalez.sonus.core.data.scan.ScanStateEmitterImpl
import com.estebancoloradogonzalez.sonus.core.data.time.DefaultDispatcherProvider
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogBrowseRepository
import com.estebancoloradogonzalez.sonus.core.domain.port.CatalogRepository
import com.estebancoloradogonzalez.sonus.core.domain.port.DispatcherProvider
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanStateEmitter
import com.estebancoloradogonzalez.sonus.service.indexer.WorkManagerScanScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the scan/catalog domain ports to their implementations across `:core:data` and
 * `:service:indexer` (scan side, US-003; read-side taxonomic browsing, US-010), and provides the
 * process-wide `WorkManager`. Lives in `:app` (composition root): the only module that knows both the
 * contracts and the impls.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Binds
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    abstract fun bindCatalogBrowseRepository(impl: CatalogBrowseRepositoryImpl): CatalogBrowseRepository

    @Binds
    @Singleton
    abstract fun bindScanStateEmitter(impl: ScanStateEmitterImpl): ScanStateEmitter

    @Binds
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    abstract fun bindScanScheduler(impl: WorkManagerScanScheduler): ScanScheduler

    @Binds
    abstract fun bindSafDataSource(impl: SafDataSourceImpl): SafDataSource

    @Binds
    abstract fun bindId3DataSource(impl: Id3DataSourceImpl): Id3DataSource

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context,
        ): WorkManager = WorkManager.getInstance(context)
    }
}
