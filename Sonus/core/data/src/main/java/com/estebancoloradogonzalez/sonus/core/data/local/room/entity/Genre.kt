package com.estebancoloradogonzalez.sonus.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Normalized Genre dimension (domain_and_state_model §2). `id = 1` is the sentinel row (empty name)
 * seeded at the Big Bang and never purged; an absent genre tag resolves to it (Invariant 4).
 */
@Entity(
    tableName = "genre",
    indices = [Index(value = ["name"], unique = true)],
)
data class Genre(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
