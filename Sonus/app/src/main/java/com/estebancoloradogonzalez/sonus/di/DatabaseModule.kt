package com.estebancoloradogonzalez.sonus.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.estebancoloradogonzalez.sonus.core.data.local.room.SonusDatabase
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.AlbumDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.ArtistDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.CatalogBrowseDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.GenreDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SettingsDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.TrackDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.APP_SETTINGS_SINGLETON_SEED
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.CATALOG_SENTINEL_SEED
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.MIGRATION_1_2
import com.estebancoloradogonzalez.sonus.core.data.local.room.migration.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the single Room database (ADR-001, C-03) and its DAOs. Lives in `:app` (composition root)
 * because it wires the infrastructure instance into the Hilt graph. The catalog schema arrives in
 * version 2 (US-003) and the `AppSettings` singleton in version 3 (US-004): [MIGRATION_1_2] and
 * [MIGRATION_2_3] upgrade existing installs and the creation callback seeds the dimension sentinels
 * and the `AppSettings` singleton (§6.1) on fresh installs.
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(SentinelSeedCallback)
            .build()

    @Provides
    fun provideSourceFolderDao(database: SonusDatabase): SourceFolderDao = database.sourceFolderDao()

    @Provides
    fun provideArtistDao(database: SonusDatabase): ArtistDao = database.artistDao()

    @Provides
    fun provideGenreDao(database: SonusDatabase): GenreDao = database.genreDao()

    @Provides
    fun provideAlbumDao(database: SonusDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideTrackDao(database: SonusDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideCatalogBrowseDao(database: SonusDatabase): CatalogBrowseDao = database.catalogBrowseDao()

    @Provides
    fun provideSettingsDao(database: SonusDatabase): SettingsDao = database.settingsDao()

    private const val DATABASE_NAME = "sonus.db"

    /**
     * Seeds the dimension sentinels and the `AppSettings` singleton on a fresh install (§6.1); the
     * migrations cover upgrades of existing installs.
     */
    private object SentinelSeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            CATALOG_SENTINEL_SEED.forEach(db::execSQL)
            APP_SETTINGS_SINGLETON_SEED.forEach(db::execSQL)
        }
    }
}
