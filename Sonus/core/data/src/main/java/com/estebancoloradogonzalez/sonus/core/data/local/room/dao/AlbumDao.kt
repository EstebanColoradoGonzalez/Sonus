package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Album

/** Room DAO for the `album` dimension (blueprint §3, C-03). */
@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(album: Album): Long

    @Query("SELECT id FROM album WHERE name = :name AND artistId = :artistId")
    suspend fun findId(
        name: String,
        artistId: Long,
    ): Long?

    /** Purges albums referenced by no track, preserving the sentinel (§6.2). */
    @Query("DELETE FROM album WHERE id != 1 AND id NOT IN (SELECT albumId FROM track)")
    suspend fun purgeOrphans(): Int
}
