package com.iv.easylink.data.repository

import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.CollectionEntity
import com.iv.easylink.data.local.LinkCollectionEntity
import com.iv.easylink.data.local.LinkDao
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.transfer.BackupCollection
import com.iv.easylink.data.transfer.BackupCodec
import com.iv.easylink.data.transfer.BackupKind
import com.iv.easylink.data.transfer.BackupLink
import com.iv.easylink.data.transfer.BackupRule
import com.iv.easylink.data.transfer.EasyLinkBackup
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.utils.LinkCategorizer
import com.iv.easylink.utils.LinkExtractor
import com.iv.easylink.utils.SecurityScanner

class LinkRepositoryImpl(private val dao: LinkDao) : LinkRepository {
    override fun observeLinks(query: LinkQuery) = dao.observeLinks(
        query.text.trim(), query.category, query.sourcePackage, query.senderName, query.since,
        query.favoriteOnly, query.readLaterOnly, query.suspiciousOnly, query.collectionId,
        query.location.code, query.oldestFirst,
        query.limit.coerceIn(20, 500), query.offset.coerceAtLeast(0),
    )
    override fun observeCollectionLinks(collectionId: Long, limit: Int) =
        dao.observeCollectionLinks(collectionId, limit.coerceIn(20, 500))
    override suspend fun saveLink(link: NewLink): Long? = dao.insertIfNotRecent(
        LinkEntity(
            url = link.url, originalUrl = link.originalUrl, host = link.host, sourcePackage = link.sourcePackage,
            sourceApp = link.sourceApp, senderName = link.senderName,
            conversationName = link.conversationName, timestamp = link.capturedAt, category = link.category,
            isSuspicious = link.isSuspicious, wasCleaned = link.wasCleaned,
        ),
        link.capturedAt - LinkRepository.DEDUP_WINDOW_MS,
    )
    override suspend fun findRecentLink(url: String, sourceApp: String, capturedAt: Long) =
        dao.recentLinkId(url, sourceApp, capturedAt - LinkRepository.DEDUP_WINDOW_MS)
    override suspend fun deleteLinks(ids: List<Long>) { if (ids.isNotEmpty()) dao.moveToBin(ids, System.currentTimeMillis()) }
    override suspend fun archiveLinks(ids: List<Long>) { if (ids.isNotEmpty()) dao.archive(ids, System.currentTimeMillis()) }
    override suspend fun unarchiveLinks(ids: List<Long>) { if (ids.isNotEmpty()) dao.unarchive(ids) }
    override suspend fun restoreFromBin(ids: List<Long>) { if (ids.isNotEmpty()) dao.restoreFromBin(ids) }
    override suspend fun permanentlyDelete(ids: List<Long>) { if (ids.isNotEmpty()) dao.deleteByIds(ids) }
    override suspend fun purgeExpiredBin(before: Long) = dao.purgeExpiredBin(before)
    override suspend fun clearAll() = dao.moveAllToBin(System.currentTimeMillis())
    override suspend fun clearCategory(category: Int) = dao.moveCategoryToBin(category, System.currentTimeMillis())
    override suspend fun clearOlderThan(timestamp: Long) = dao.moveOlderToBin(timestamp, System.currentTimeMillis())
    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    override suspend fun setReadLater(id: Long, readLater: Boolean) = dao.setReadLater(id, readLater)
    override suspend fun setCategory(id: Long, category: Int) = dao.setCategory(id, category)
    override suspend fun updateLinkUrl(id: Long, rawUrl: String): Boolean {
        val processed = LinkExtractor.process(rawUrl) ?: return false
        dao.updateLinkUrl(
            id = id,
            url = processed.url,
            originalUrl = processed.originalUrl,
            host = processed.host,
            category = LinkCategorizer.categorize(processed.host, processed.url).code,
            suspicious = SecurityScanner.isSuspiciousUrl(processed.url),
            wasCleaned = processed.wasCleaned,
        )
        return true
    }
    override suspend fun countActiveSenderLinks(sourcePackage: String, senderName: String): Int {
        val packageName = sourcePackage.trim()
        val name = senderName.trim()
        return if (packageName.isEmpty() || name.isEmpty()) 0 else dao.countActiveSenderLinks(packageName, name)
    }
    override suspend fun moveSenderLinksToBin(sourcePackage: String, senderName: String) {
        val packageName = sourcePackage.trim()
        val name = senderName.trim()
        if (packageName.isNotEmpty() && name.isNotEmpty()) dao.moveSenderLinksToBin(packageName, name, System.currentTimeMillis())
    }
    override suspend fun updateMetadata(id: Long, title: String?, siteName: String?, available: Boolean) =
        dao.updateMetadata(id, title, siteName, null, if (available) LinkEntity.METADATA_LOADED else LinkEntity.METADATA_UNAVAILABLE, System.currentTimeMillis())
    override suspend fun getLink(id: Long) = dao.getById(id)
    override fun observeCollections() = dao.observeCollections()
    override fun observeCollectionIds(linkId: Long) = dao.observeCollectionIds(linkId)
    override suspend fun getCollectionIds(linkId: Long) = dao.getCollectionIds(linkId)
    override suspend fun createCollection(name: String, colorToken: Int) = dao.insertCollection(
        CollectionEntity(name = name.trim(), colorToken = colorToken, createdAt = System.currentTimeMillis()),
    )
    override suspend fun updateCollection(collection: CollectionEntity) = dao.updateCollection(collection)
    override suspend fun renameCollection(id: Long, name: String) = dao.renameCollection(id, name.trim().take(40))
    override suspend fun setCollectionPinned(id: Long, pinned: Boolean) = dao.setCollectionPinned(id, pinned)
    override suspend fun deleteCollection(id: Long) = dao.deleteCollection(id)
    override suspend fun addToCollection(linkId: Long, collectionId: Long) = dao.addToCollection(LinkCollectionEntity(linkId, collectionId))
    override suspend fun removeFromCollection(linkId: Long, collectionId: Long) = dao.removeFromCollection(linkId, collectionId)
    override fun observeRules() = dao.observeRules()
    override suspend fun enabledRules() = dao.enabledRules()
    override suspend fun createRule(rule: AutomationRuleEntity) = dao.insertRule(rule)
    override suspend fun updateRule(rule: AutomationRuleEntity) = dao.updateRule(rule)
    override suspend fun deleteRule(id: Long) = dao.deleteRule(id)
    override fun observeTotalSince(since: Long) = dao.observeTotalSince(since)
    override fun observeCategoryCounts(since: Long) = dao.observeCategoryCounts(since)
    override fun observeActiveCategoryCounts() = dao.observeActiveCategoryCounts()
    override fun observeTopDomains(since: Long) = dao.observeTopDomains(since, 5)
    override fun observeTopSources(since: Long) = dao.observeTopSources(since, 5)
    override fun observeFavoriteCount() = dao.observeFavoriteCount()
    override fun observeSuspiciousCount() = dao.observeSuspiciousCount()
    override fun observeSourceApps() = dao.observeSourceApps()
    override fun observeSenders() = dao.observeSenders()
    override suspend fun exportAll(): EasyLinkBackup {
        val collections = dao.allCollections()
        val links = dao.allLinks()
        require(links.size <= BackupCodec.MAX_LINKS) {
            "EasyLink can export up to ${BackupCodec.MAX_LINKS} links in one file"
        }
        val memberships = dao.allMemberships().groupBy({ it.linkId }, { "c${it.collectionId}" })
        return EasyLinkBackup(
            kind = BackupKind.FULL,
            exportedAt = System.currentTimeMillis(),
            collections = collections.map { it.toBackup() },
            links = links.map { it.toBackup(memberships[it.id].orEmpty()) },
            rules = dao.allRules().map { it.toBackup() },
        )
    }

    override suspend fun exportCollection(collectionId: Long): EasyLinkBackup? {
        val collection = dao.allCollections().firstOrNull { it.id == collectionId } ?: return null
        val ref = "c${collection.id}"
        val links = dao.linksForCollectionExport(collectionId)
        require(links.size <= BackupCodec.MAX_LINKS) {
            "EasyLink can export up to ${BackupCodec.MAX_LINKS} links in one file"
        }
        return EasyLinkBackup(
            kind = BackupKind.COLLECTION,
            exportedAt = System.currentTimeMillis(),
            name = collection.name,
            collections = listOf(collection.toBackup()),
            links = links.map { it.toBackup(listOf(ref)) },
        )
    }

    override suspend fun exportCategory(category: Int): EasyLinkBackup {
        val links = dao.linksForCategoryExport(category)
        require(links.size <= BackupCodec.MAX_LINKS) {
            "EasyLink can export up to ${BackupCodec.MAX_LINKS} links in one file"
        }
        return EasyLinkBackup(
            kind = BackupKind.CATEGORY,
            exportedAt = System.currentTimeMillis(),
            name = LinkCategory.fromCode(category).label,
            links = links.map { it.toBackup(emptyList()) },
        )
    }

    override suspend fun importBackup(backup: EasyLinkBackup, replace: Boolean) = dao.importBackup(backup, replace)

    private fun CollectionEntity.toBackup() = BackupCollection("c$id", name, colorToken, createdAt, isPinned)

    private fun LinkEntity.toBackup(collectionRefs: List<String>) = BackupLink(
        ref = "l$id", url = url, originalUrl = originalUrl, sourcePackage = sourcePackage, sourceApp = sourceApp,
        senderName = senderName, conversationName = conversationName, timestamp = timestamp,
        category = category, isSuspicious = isSuspicious, isFavorite = isFavorite, isReadLater = isReadLater,
        wasCleaned = wasCleaned, title = title, siteName = siteName,
        archivedAt = archivedAt, deletedAt = deletedAt, collectionRefs = collectionRefs,
    )

    private fun AutomationRuleEntity.toBackup() = BackupRule(
        name, hostPattern, pathContains, category, collectionId?.let { "c$it" }, enabled, priority,
    )
}
