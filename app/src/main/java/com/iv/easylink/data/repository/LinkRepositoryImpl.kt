package com.iv.easylink.data.repository

import com.iv.easylink.data.local.LinkDao
import com.iv.easylink.data.local.LinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Concrete [LinkRepository] backed by Room via [LinkDao].
 */
class LinkRepositoryImpl(private val linkDao: LinkDao) : LinkRepository {

    override fun getAllLinks(): Flow<List<LinkEntity>> = linkDao.getAllLinksFlow()

    override suspend fun saveLink(
        url: String,
        sourceApp: String,
        senderName: String,
        isSuspicious: Boolean,
    ) {
        val now = System.currentTimeMillis()
        // Skip if an identical URL was saved within the dedup window
        val recentCount = linkDao.countRecentByUrl(url, now - LinkRepository.DEDUP_WINDOW_MS)
        if (recentCount > 0) return

        linkDao.insertLink(
            LinkEntity(
                url = url,
                sourceApp = sourceApp,
                senderName = senderName,
                timestamp = now,
                isSuspicious = isSuspicious,
            )
        )
    }

    override suspend fun deleteLink(link: LinkEntity) = linkDao.deleteLink(link)

    override suspend fun deleteAllLinks() = linkDao.deleteAllLinks()
}
