package com.estebancoloradogonzalez.sonus.core.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.estebancoloradogonzalez.sonus.core.data.local.room.dao.SourceFolderDao
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder

/**
 * Single Room database of the app (ADR-001, contenedor C-03). Starts at version 1 with only the
 * `SourceFolder` entity (US-002); the remaining entities and the Big Bang seeding (§6.1) are added
 * by later stories as they are needed. `SourceFolder` is born empty, so no seeding is required here.
 */
@Database(
    entities = [SourceFolder::class],
    version = 1,
    exportSchema = true,
)
abstract class SonusDatabase : RoomDatabase() {
    abstract fun sourceFolderDao(): SourceFolderDao
}
