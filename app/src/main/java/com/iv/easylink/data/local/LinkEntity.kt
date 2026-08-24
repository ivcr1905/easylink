package com.iv.easylink.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "links",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["url", "timestamp"]),
        Index(value = ["host"]),
        Index(value = ["category"]),
        Index(value = ["sourcePackage"]),
    ],
)
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val host: String,
    val sourcePackage: String?,
    val sourceApp: String,
    val timestamp: Long,
    val category: Int,
    val isSuspicious: Boolean,
    val isFavorite: Boolean = false,
    val wasCleaned: Boolean = false,
    val title: String? = null,
    val siteName: String? = null,
    val imageUrl: String? = null,
    val metadataState: Int = METADATA_PENDING,
    val metadataUpdatedAt: Long? = null,
) {
    companion object {
        const val METADATA_PENDING = 0
        const val METADATA_LOADED = 1
        const val METADATA_UNAVAILABLE = 2
    }
}
