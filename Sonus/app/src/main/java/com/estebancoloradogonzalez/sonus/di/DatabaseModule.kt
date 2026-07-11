package com.estebancoloradogonzalez.sonus.di

import android.content.Context
import androidx.room.Room
import com.estebancoloradogonzalez.sonus.core.data.local.room.SonusDatabase
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the single Room database (ADR-001, C-03) and its DAOs. Lives in `:app` (composition root)
 * because it wires the infrastructure instance into the Hilt graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideSonusDatabase(
        @ApplicationContext context: Context,
    ): SonusDatabase =
        Room.databaseBuilder(
            context,
            SonusDatabase::class.java,
            DATABASE_NAME,
        ).build()

    @Provides
    fun provideSourceFolderDao(database: SonusDatabase): SourceFolderDao = database.sourceFolderDao()

    private const val DATABASE_NAME = "sonus.db"
}
