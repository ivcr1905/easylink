package com.iv.easylink.data.repository

import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.CollectionEntity
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.data.local.CountByKey
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.local.SourceAppSummary
import com.iv.easylink.data.local.SenderSummary
import kotlinx.coroutines.flow.Flow
import com.iv.easylink.data.transfer.EasyLinkBackup
import com.iv.easylink.data.transfer.ImportResult

data class LinkQuery(
    val text: String = "",
    val category: Int = -1,
    val sourcePackage: String = "",
    val senderName: String = "",
    val since: Long = 0,
    val dateRangeDays: Int = 0,
    val favoriteOnly: Boolean = false,
    val readLaterOnly: Boolean = false,
    val suspiciousOnly: Boolean = false,
    val collectionId: Long = 0,
    val location: LinkLocation = LinkLocation.ACTIVE,
    val oldestFirst: Boolean = false,
    val limit: Int = 100,
    val offset: Int = 0,
)

enum class LinkLocation(val code: Int) { ACTIVE(0), ARCHIVED(1), BIN(2) }

data class NewLink(
    val url: String,
    val host: String,
    val sourcePackage: String?,
    val sourceApp: String,
    val category: Int,
    val isSuspicious: Boolean,
    val wasCleaned: Boolean,
    val capturedAt: Long = System.currentTimeMillis(),
    val senderName: String? = null,
    val conversationName: String? = null,
    val originalUrl: String? = null,
)

interface LinkRepository {
    fun observeLinks(query: LinkQuery): Flow<List<LinkEntity>>
    fun observeCollectionLinks(collectionId: Long, limit: Int = 200): Flow<List<LinkEntity>>
    suspend fun saveLink(link: NewLink): Long?
    suspend fun findRecentLink(url: String, sourceApp: String, capturedAt: Long = System.currentTimeMillis()): Long?
    suspend fun deleteLinks(ids: List<Long>)
    suspend fun archiveLinks(ids: List<Long>)
    suspend fun unarchiveLinks(ids: List<Long>)
    suspend fun restoreFromBin(ids: List<Long>)
    suspend fun permanentlyDelete(ids: List<Long>)
    suspend fun purgeExpiredBin(before: Long)
    suspend fun clearAll()
    suspend fun clearCategory(category: Int)
    suspend fun clearOlderThan(timestamp: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setReadLater(id: Long, readLater: Boolean)
    suspend fun setCategory(id: Long, category: Int)
    suspend fun updateLinkUrl(id: Long, rawUrl: String): Boolean
    suspend fun countActiveSenderLinks(sourcePackage: String, senderName: String): Int
    suspend fun moveSenderLinksToBin(sourcePackage: String, senderName: String)
    suspend fun updateMetadata(id: Long, title: String?, siteName: String?, available: Boolean)
    suspend fun getLink(id: Long): LinkEntity?
    fun observeCollections(): Flow<List<CollectionSummary>>
    fun observeCollectionIds(linkId: Long): Flow<List<Long>>
    suspend fun getCollectionIds(linkId: Long): List<Long>
    suspend fun createCollection(name: String, colorToken: Int): Long
    suspend fun updateCollection(collection: CollectionEntity)
    suspend fun renameCollection(id: Long, name: String)
    suspend fun setCollectionPinned(id: Long, pinned: Boolean)
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
    fun observeActiveCategoryCounts(): Flow<List<CountByKey>>
    fun observeTopDomains(since: Long): Flow<List<CountByKey>>
    fun observeTopSources(since: Long): Flow<List<CountByKey>>
    fun observeFavoriteCount(): Flow<Int>
    fun observeSuspiciousCount(): Flow<Int>
    fun observeSourceApps(): Flow<List<SourceAppSummary>>
    fun observeSenders(): Flow<List<SenderSummary>>
    suspend fun exportAll(): EasyLinkBackup
    suspend fun exportCollection(collectionId: Long): EasyLinkBackup?
    suspend fun exportCategory(category: Int): EasyLinkBackup
    suspend fun importBackup(backup: EasyLinkBackup, replace: Boolean): ImportResult

    companion object { const val DEDUP_WINDOW_MS = 30_000L }
}
