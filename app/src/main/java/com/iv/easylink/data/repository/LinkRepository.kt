package com.iv.easylink.data.repository

import com.iv.easylink.data.local.LinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over link storage operations.
 * Consumers depend on this interface — the implementation can be swapped for testing.
 */
interface LinkRepository {

    /** Observe all saved links, ordered newest-first. */
    fun getAllLinks(): Flow<List<LinkEntity>>

    /**
     * Persist a detected link.
     * Internally deduplicates: if the same URL was saved within [DEDUP_WINDOW_MS] it is skipped.
     */
    suspend fun saveLink(
        url: String,
        sourceApp: String,
        senderName: String,
        isSuspicious: Boolean,
    )

    /** Remove a single link from the history. */
    suspend fun deleteLink(link: LinkEntity)

    /** Clear the entire link history. */
    suspend fun deleteAllLinks()

    companion object {
        /** Milliseconds within which the same URL is considered a duplicate (5 seconds). */
        const val DEDUP_WINDOW_MS = 5_000L
    }
}
