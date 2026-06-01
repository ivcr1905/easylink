package com.iv.easylink.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for the links table.
 */
@Dao
interface LinkDao {

    /** Live-updating list of all links, newest first. */
    @Query("SELECT * FROM links ORDER BY timestamp DESC")
    fun getAllLinksFlow(): Flow<List<LinkEntity>>

    /** Insert a link. Replaces on primary-key conflict (shouldn't happen with auto-gen IDs). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: LinkEntity)

    /**
     * Check whether a link with the same URL was already saved within the given time window.
     * Used to avoid duplicate entries from the same notification/share event.
     */
    @Query("SELECT COUNT(*) FROM links WHERE url = :url AND timestamp > :sinceTimestamp")
    suspend fun countRecentByUrl(url: String, sinceTimestamp: Long): Int

    /** Delete a single link. */
    @Delete
    suspend fun deleteLink(link: LinkEntity)

    /** Clear entire link history. */
    @Query("DELETE FROM links")
    suspend fun deleteAllLinks()
}
