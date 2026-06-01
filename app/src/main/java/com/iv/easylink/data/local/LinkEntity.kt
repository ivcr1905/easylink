package com.iv.easylink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved link detected from notifications or shared via the share sheet.
 */
@Entity(tableName = "links")
data class LinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The cleaned, navigable URL. */
    val url: String,

    /** Display name of the source application (e.g. "WhatsApp", "Telegram"). */
    val sourceApp: String,

    /** Sender / contact name extracted from the notification title. */
    val senderName: String,

    /** Epoch millis when the link was detected. */
    val timestamp: Long,

    /** Whether the SecurityScanner flagged this URL as suspicious. */
    val isSuspicious: Boolean,
)
