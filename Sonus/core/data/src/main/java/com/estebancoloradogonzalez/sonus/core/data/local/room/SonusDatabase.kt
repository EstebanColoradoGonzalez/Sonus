package com.estebancoloradogonzalez.sonus.core.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.estebancoloradogonzalez.sonus.core.data.local.room.converter.RoomTypeConverters
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.AlbumDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.ArtistDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.GenreDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.TrackDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Album
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Artist
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Genre
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Track

/**
 * Single Room database of the app (ADR-001, contenedor C-03). Version 2 adds the catalog schema
 * (`Track` + the `Artist`/`Album`/`Genre` dimensions) for the foundational scan (US-003). The
 * dimension sentinels (`id = 1`, §6.1) are seeded by the migration (existing installs) and by the
 * creation callback (fresh installs), wired in `DatabaseModule`.
 */
@Database(
    entities = [
        SourceFolder::class,
        Artist::class,
        Genre::class,
        Album::class,
        Track::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class SonusDatabase : RoomDatabase() {
    abstract fun sourceFolderDao(): SourceFolderDao

    abstract fun artistDao(): ArtistDao

    abstract fun genreDao(): GenreDao

    abstract fun albumDao(): AlbumDao

    abstract fun trackDao(): TrackDao
}
