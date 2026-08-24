package com.iv.easylink.data.repository

import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.CollectionEntity
import com.iv.easylink.data.local.LinkCollectionEntity
import com.iv.easylink.data.local.LinkDao
import com.iv.easylink.data.local.LinkEntity

class LinkRepositoryImpl(private val dao: LinkDao) : LinkRepository {
    override fun observeLinks(query: LinkQuery) = dao.observeLinks(
        query.text.trim(), query.category, query.sourcePackage, query.since,
        query.favoriteOnly, query.suspiciousOnly, query.oldestFirst,
        query.limit.coerceIn(20, 500), query.offset.coerceAtLeast(0),
    )
    override fun observeCollectionLinks(collectionId: Long, limit: Int) =
        dao.observeCollectionLinks(collectionId, limit.coerceIn(20, 500))
    override suspend fun saveLink(link: NewLink): Long? = dao.insertIfNotRecent(
        LinkEntity(
            url = link.url, host = link.host, sourcePackage = link.sourcePackage,
            sourceApp = link.sourceApp, timestamp = link.capturedAt, category = link.category,
            isSuspicious = link.isSuspicious, wasCleaned = link.wasCleaned,
        ),
        link.capturedAt - LinkRepository.DEDUP_WINDOW_MS,
    )
    override suspend fun restoreLink(link: LinkEntity) { dao.insert(link) }
    override suspend fun deleteLinks(ids: List<Long>) { if (ids.isNotEmpty()) dao.deleteByIds(ids) }
    override suspend fun clearAll() = dao.deleteAll()
    override suspend fun clearCategory(category: Int) = dao.deleteCategory(category)
    override suspend fun clearOlderThan(timestamp: Long) = dao.deleteOlderThan(timestamp)
    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    override suspend fun setCategory(id: Long, category: Int) = dao.setCategory(id, category)
    override suspend fun updateMetadata(id: Long, title: String?, siteName: String?, imageUrl: String?, available: Boolean) =
        dao.updateMetadata(id, title, siteName, imageUrl, if (available) LinkEntity.METADATA_LOADED else LinkEntity.METADATA_UNAVAILABLE, System.currentTimeMillis())
    override suspend fun getLink(id: Long) = dao.getById(id)
    override fun observeCollections() = dao.observeCollections()
    override fun observeCollectionIds(linkId: Long) = dao.observeCollectionIds(linkId)
    override suspend fun getCollectionIds(linkId: Long) = dao.getCollectionIds(linkId)
    override suspend fun createCollection(name: String, colorToken: Int) = dao.insertCollection(
        CollectionEntity(name = name.trim(), colorToken = colorToken, createdAt = System.currentTimeMillis()),
    )
    override suspend fun updateCollection(collection: CollectionEntity) = dao.updateCollection(collection)
    override suspend fun renameCollection(id: Long, name: String) = dao.renameCollection(id, name.trim().take(40))
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
    override fun observeTopDomains(since: Long) = dao.observeTopDomains(since, 5)
    override fun observeTopSources(since: Long) = dao.observeTopSources(since, 5)
    override fun observeFavoriteCount() = dao.observeFavoriteCount()
    override fun observeSuspiciousCount() = dao.observeSuspiciousCount()
    override fun observeSourceApps() = dao.observeSourceApps()
}
