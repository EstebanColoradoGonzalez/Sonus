package com.estebancoloradogonzalez.sonus.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Normalized Album dimension (domain_and_state_model §2). Unique by `(name, artistId)` so homonym
 * albums by different artists stay distinct. `id = 1` is the sentinel row (empty name, `artistId = 1`)
 * seeded at the Big Bang and never purged. The artist FK is `RESTRICT`: an artist with albums cannot
 * be deleted.
 */
@Entity(
    tableName = "album",
    indices = [
        Index(value = ["name", "artistId"], unique = true),
        Index(value = ["artistId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val artistId: Long,
)
