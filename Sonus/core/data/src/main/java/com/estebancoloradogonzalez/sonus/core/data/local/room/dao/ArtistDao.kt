package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Artist

/** Room DAO for the `artist` dimension (blueprint §3, C-03). */
@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(artist: Artist): Long

    @Query("SELECT id FROM artist WHERE name = :name")
    suspend fun findIdByName(name: String): Long?

    /** Purges artists referenced by no album and no track, preserving the sentinel (§6.2). */
    @Query(
        "DELETE FROM artist WHERE id != 1 AND id NOT IN " +
            "(SELECT artistId FROM album UNION SELECT artistId FROM track)",
    )
    suspend fun purgeOrphans(): Int
}
