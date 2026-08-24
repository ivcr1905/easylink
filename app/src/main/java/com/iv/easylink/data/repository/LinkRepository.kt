package com.iv.easylink.data.repository

import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.CollectionEntity
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.data.local.CountByKey
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.local.SourceAppSummary
import kotlinx.coroutines.flow.Flow

data class LinkQuery(
    val text: String = "",
    val category: Int = -1,
    val sourcePackage: String = "",
    val since: Long = 0,
    val dateRangeDays: Int = 0,
    val favoriteOnly: Boolean = false,
    val suspiciousOnly: Boolean = false,
    val oldestFirst: Boolean = false,
    val limit: Int = 100,
    val offset: Int = 0,
)

data class NewLink(
    val url: String,
    val host: String,
    val sourcePackage: String?,
    val sourceApp: String,
    val category: Int,
    val isSuspicious: Boolean,
    val wasCleaned: Boolean,
    val capturedAt: Long = System.currentTimeMillis(),
)

interface LinkRepository {
    fun observeLinks(query: LinkQuery): Flow<List<LinkEntity>>
    fun observeCollectionLinks(collectionId: Long, limit: Int = 200): Flow<List<LinkEntity>>
    suspend fun saveLink(link: NewLink): Long?
    suspend fun restoreLink(link: LinkEntity)
    suspend fun deleteLinks(ids: List<Long>)
    suspend fun clearAll()
    suspend fun clearCategory(category: Int)
    suspend fun clearOlderThan(timestamp: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setCategory(id: Long, category: Int)
    suspend fun updateMetadata(id: Long, title: String?, siteName: String?, imageUrl: String?, available: Boolean)
    suspend fun getLink(id: Long): LinkEntity?
    fun observeCollections(): Flow<List<CollectionSummary>>
    fun observeCollectionIds(linkId: Long): Flow<List<Long>>
    suspend fun getCollectionIds(linkId: Long): List<Long>
    suspend fun createCollection(name: String, colorToken: Int): Long
    suspend fun updateCollection(collection: CollectionEntity)
    suspend fun renameCollection(id: Long, name: String)
    suspend fun deleteCollection(id: Long)
    suspend fun addToCollection(linkId: Long, collectionId: Long)
    suspend fun removeFromCollection(linkId: Long, collectionId: Long)
    fun observeRules(): Flow<List<AutomationRuleEntity>>
    suspend fun enabledRules(): List<AutomationRuleEntity>
    suspend fun createRule(rule: AutomationRuleEntity): Long
    suspend fun updateRule(rule: AutomationRuleEntity)
    suspend fun deleteRule(id: Long)
    fun observeTotalSince(since: Long): Flow<Int>
    fun observeCategoryCounts(since: Long): Flow<List<CountByKey>>
    fun observeTopDomains(since: Long): Flow<List<CountByKey>>
    fun observeTopSources(since: Long): Flow<List<CountByKey>>
    fun observeFavoriteCount(): Flow<Int>
    fun observeSuspiciousCount(): Flow<Int>
    fun observeSourceApps(): Flow<List<SourceAppSummary>>

    companion object { const val DEDUP_WINDOW_MS = 30_000L }
}
