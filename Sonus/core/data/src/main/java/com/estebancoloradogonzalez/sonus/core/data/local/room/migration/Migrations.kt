package com.estebancoloradogonzalez.sonus.core.data.local.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SQL that seeds the dimension sentinels (`id = 1`, domain_and_state_model §6.1). Shared by
 * [MIGRATION_1_2] (existing installs) and the creation callback (fresh installs) so both paths stay
 * in sync. Order matters: the album sentinel references the artist sentinel.
 */
val CATALOG_SENTINEL_SEED: List<String> =
    listOf(
        "INSERT OR IGNORE INTO `artist` (`id`, `name`) VALUES (1, '')",
        "INSERT OR IGNORE INTO `genre` (`id`, `name`) VALUES (1, '')",
        "INSERT OR IGNORE INTO `album` (`id`, `name`, `artistId`) VALUES (1, '', 1)",
    )

/**
 * Adds the catalog schema on top of the US-002 database (version 1 held only `source_folder`). The
 * `CREATE TABLE`/`CREATE INDEX` statements mirror Room's generated schema for version 2; the
 * sentinels are seeded once the dimension tables exist. Validated by `MigrationTest` against the
 * exported `2.json`.
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `artist` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_artist_name` ON `artist` (`name`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `genre` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_genre_name` ON `genre` (`name`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                    "`artistId` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT )",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_album_name_artistId` " +
                    "ON `album` (`name`, `artistId`)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artistId` ON `album` (`artistId`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `track` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uri` TEXT NOT NULL, " +
                    "`title` TEXT, `artistId` INTEGER NOT NULL, `albumId` INTEGER NOT NULL, " +
                    "`genreId` INTEGER NOT NULL, `sourceFolderId` INTEGER NOT NULL, " +
                    "`contentType` TEXT NOT NULL, `trackNumber` INTEGER, `releaseYear` INTEGER, " +
                    "`durationMs` INTEGER NOT NULL, `hasEmbeddedArtwork` INTEGER NOT NULL, " +
                    "`availability` TEXT NOT NULL, `fileLastModifiedMs` INTEGER NOT NULL, " +
                    "`dateAddedMs` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                    "FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                    "FOREIGN KEY(`genreId`) REFERENCES `genre`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                    "FOREIGN KEY(`sourceFolderId`) REFERENCES `source_folder`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_track_uri` ON `track` (`uri`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_artistId` ON `track` (`artistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_albumId` ON `track` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_genreId` ON `track` (`genreId`)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_track_sourceFolderId` " +
                    "ON `track` (`sourceFolderId`)",
            )

            CATALOG_SENTINEL_SEED.forEach(db::execSQL)
        }
    }

/**
 * SQL that seeds the `AppSettings` singleton (`id = 1`, domain_and_state_model §6.1) with its Big
 * Bang defaults: `onboardingCompleted = 0` (forces the first-run flow) and `themePreference =
 * SYSTEM`. Shared by [MIGRATION_2_3] (existing installs) and the creation callback (fresh installs).
 */
val APP_SETTINGS_SINGLETON_SEED: List<String> =
    listOf(
        "INSERT OR IGNORE INTO `app_settings` " +
            "(`id`, `onboardingCompleted`, `themePreference`) VALUES (1, 0, 'SYSTEM')",
    )

/**
 * Adds the `AppSettings` singleton on top of the catalog database (US-004). The `CREATE TABLE`
 * mirrors Room's generated schema for version 3; the singleton is seeded once the table exists.
 * Validated by `MigrationTest` against the exported `3.json`.
 */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_settings` " +
                    "(`id` INTEGER NOT NULL, `onboardingCompleted` INTEGER NOT NULL, " +
                    "`themePreference` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )

            APP_SETTINGS_SINGLETON_SEED.forEach(db::execSQL)
        }
    }
