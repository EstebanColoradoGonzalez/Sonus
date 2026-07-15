package com.estebancoloradogonzalez.sonus.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.Genre

/** Room DAO for the `genre` dimension (blueprint §3, C-03). */
@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(genre: Genre): Long

    @Query("SELECT id FROM genre WHERE name = :name")
    suspend fun findIdByName(name: String): Long?

    /** Purges genres referenced by no track, preserving the sentinel (§6.2). */
    @Query("DELETE FROM genre WHERE id != 1 AND id NOT IN (SELECT genreId FROM track)")
    suspend fun purgeOrphans(): Int
}
