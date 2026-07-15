package com.estebancoloradogonzalez.sonus.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability

/**
 * Catalog entry for a single audio file (domain_and_state_model §2/§3). Identity is the SAF [uri]
 * (unique). Dimension FKs are `RESTRICT` (always point to a real row or the `id = 1` sentinel);
 * [sourceFolderId] is `CASCADE` (removing a folder removes its tracks). `title` is nullable: `NULL`
 * means the tag was absent (never a presentation literal, Invariant 4).
 */
@Entity(
    tableName = "track",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["artistId"]),
        Index(value = ["albumId"]),
        Index(value = ["genreId"]),
        Index(value = ["sourceFolderId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = Album::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["id"],
            childColumns = ["genreId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SourceFolder::class,
            parentColumns = ["id"],
            childColumns = ["sourceFolderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String?,
    val artistId: Long,
    val albumId: Long,
    val genreId: Long,
    val sourceFolderId: Long,
    val contentType: ContentType,
    val trackNumber: Int?,
    val releaseYear: Int?,
    val durationMs: Long,
    val hasEmbeddedArtwork: Boolean,
    val availability: TrackAvailability,
    val fileLastModifiedMs: Long,
    val dateAddedMs: Long,
)
