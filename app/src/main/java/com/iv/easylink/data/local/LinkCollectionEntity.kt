package com.iv.easylink.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "link_collections",
    primaryKeys = ["linkId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = LinkEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId")],
)
data class LinkCollectionEntity(val linkId: Long, val collectionId: Long)
