package com.wickedcoder.myadvisor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User zone (data-model.md §2): mutable, owned by the user, survives catalog
 * re-imports. No CASCADE delete — the validator rejects any dataset that
 * would drop a card the user owns.
 */
@Entity(tableName = "user_cards")
data class UserCardEntity(
    @PrimaryKey val cardId: String,
    val addedAt: String,        // ISO datetime
    val sortOrder: Int,
)
