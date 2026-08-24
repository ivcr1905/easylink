package com.iv.easylink.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "automation_rules",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("collectionId")],
)
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hostPattern: String,
    val pathContains: String? = null,
    val category: Int? = null,
    val collectionId: Long? = null,
    val enabled: Boolean = true,
    val priority: Int = 0,
)
