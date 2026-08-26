package com.iv.easylink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorToken: Int,
    val createdAt: Long,
    val isPinned: Boolean = false,
)
