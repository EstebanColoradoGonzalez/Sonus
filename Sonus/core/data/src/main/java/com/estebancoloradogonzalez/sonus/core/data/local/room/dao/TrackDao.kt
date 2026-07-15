package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Track
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability

/** Room DAO for the `track` table (blueprint §3, C-03). */
@Dao
interface TrackDao {
    @Insert
    suspend fun insert(track: Track): Long

    @Update
    suspend fun update(track: Track)

    @Query("SELECT id FROM track WHERE uri = :uri")
    suspend fun findIdByUri(uri: String): Long?

    /** Purges tracks whose URI is no longer present in the file system (Invariant 2). */
    @Query("DELETE FROM track WHERE uri NOT IN (:uris)")
    suspend fun deleteWhereUriNotIn(uris: List<String>): Int

    @Query("DELETE FROM track")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM track WHERE availability = :availability")
    suspend fun countByAvailability(availability: TrackAvailability): Int
}
