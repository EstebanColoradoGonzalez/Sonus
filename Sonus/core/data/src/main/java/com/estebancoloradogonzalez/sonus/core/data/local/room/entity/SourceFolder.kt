package com.estebancoloradogonzalez.sonus.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room `@Entity` of a Source Folder (domain_and_state_model §2). Persistence-facing counterpart of
 * the pure domain model; mapped to/from it in `SourceFolderMappers`.
 *
 * `treeUri` is unique: re-registering the same folder is rejected upstream as a duplicate (AC4).
 */
@Entity(
    tableName = "source_folder",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class SourceFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeUri: String,
    val displayPath: String,
    val dateAddedMs: Long,
)
