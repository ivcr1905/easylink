package com.iv.easylink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.iv.easylink.data.transfer.EasyLinkBackup
import com.iv.easylink.data.transfer.ImportResult
import com.iv.easylink.utils.LinkExtractor
import java.util.Locale

@Dao
interface LinkDao {
    @Query(
        """SELECT * FROM links
        WHERE (:query = '' OR url LIKE '%' || :query || '%' OR IFNULL(originalUrl, '') LIKE '%' || :query || '%' OR host LIKE '%' || :query || '%'
            OR IFNULL(title, '') LIKE '%' || :query || '%' OR sourceApp LIKE '%' || :query || '%'
            OR IFNULL(senderName, '') LIKE '%' || :query || '%')
          AND (:category < 0 OR category = :category)
          AND (:sourcePackage = '' OR sourcePackage = :sourcePackage)
          AND (:senderName = '' OR senderName = :senderName)
          AND ((:location = 0 AND archivedAt IS NULL AND deletedAt IS NULL)
            OR (:location = 1 AND archivedAt IS NOT NULL AND deletedAt IS NULL)
            OR (:location = 2 AND deletedAt IS NOT NULL))
          AND timestamp >= :since
          AND (:favoriteOnly = 0 OR isFavorite = 1)
          AND (:readLaterOnly = 0 OR isReadLater = 1)
          AND (:suspiciousOnly = 0 OR isSuspicious = 1)
          AND (:collectionId = 0 OR EXISTS (
              SELECT 1 FROM link_collections
              WHERE link_collections.linkId = links.id AND link_collections.collectionId = :collectionId
          ))
        ORDER BY CASE WHEN :oldestFirst = 1 THEN timestamp END ASC,
                 CASE WHEN :oldestFirst = 0 THEN timestamp END DESC
        LIMIT :limit OFFSET :offset""",
    )
    fun observeLinks(
        query: String,
        category: Int,
        sourcePackage: String,
        senderName: String,
        since: Long,
        favoriteOnly: Boolean,
        readLaterOnly: Boolean,
        suspiciousOnly: Boolean,
        collectionId: Long,
        location: Int,
        oldestFirst: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<LinkEntity>>

    @Query(
        """SELECT links.* FROM links
        INNER JOIN link_collections ON links.id = link_collections.linkId
        WHERE link_collections.collectionId = :collectionId
          AND links.archivedAt IS NULL AND links.deletedAt IS NULL
        ORDER BY links.timestamp DESC LIMIT :limit""",
    )
    fun observeCollectionLinks(collectionId: Long, limit: Int): Flow<List<LinkEntity>>

    @Query("SELECT COUNT(*) FROM links WHERE url = :url AND sourceApp = :sourceApp AND timestamp >= :since AND deletedAt IS NULL")
    suspend fun countRecent(url: String, sourceApp: String, since: Long): Int

    @Query("SELECT id FROM links WHERE url = :url AND sourceApp = :sourceApp AND timestamp >= :since AND deletedAt IS NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun recentLinkId(url: String, sourceApp: String, since: Long): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: LinkEntity): Long

    @Transaction
    suspend fun insertIfNotRecent(link: LinkEntity, since: Long): Long? {
        if (countRecent(link.url, link.sourceApp, since) != 0) return null
        return insert(link).takeIf { it > 0 }
    }

    @Query("UPDATE links SET deletedAt = :deletedAt WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun moveToBin(ids: List<Long>, deletedAt: Long)

    @Query("UPDATE links SET archivedAt = :archivedAt WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun archive(ids: List<Long>, archivedAt: Long)

    @Query("UPDATE links SET archivedAt = NULL WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun unarchive(ids: List<Long>)

    @Query("UPDATE links SET deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreFromBin(ids: List<Long>)

    @Query("DELETE FROM links WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM links WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeExpiredBin(before: Long)

    @Query("UPDATE links SET deletedAt = :deletedAt WHERE deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun moveAllToBin(deletedAt: Long)

    @Query("UPDATE links SET deletedAt = :deletedAt WHERE category = :category AND deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun moveCategoryToBin(category: Int, deletedAt: Long)

    @Query("UPDATE links SET deletedAt = :deletedAt WHERE timestamp < :before AND deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun moveOlderToBin(before: Long, deletedAt: Long)

    @Query("UPDATE links SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE links SET isReadLater = :readLater WHERE id = :id")
    suspend fun setReadLater(id: Long, readLater: Boolean)

    @Query("UPDATE links SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: Int)

    @Query(
        """UPDATE links SET url = :url, originalUrl = :originalUrl, host = :host,
        category = :category, isSuspicious = :suspicious, wasCleaned = :wasCleaned,
        title = NULL, siteName = NULL, imageUrl = NULL, metadataState = 0, metadataUpdatedAt = NULL
        WHERE id = :id""",
    )
    suspend fun updateLinkUrl(
        id: Long,
        url: String,
        originalUrl: String?,
        host: String,
        category: Int,
        suspicious: Boolean,
        wasCleaned: Boolean,
    )

    @Query("SELECT COUNT(*) FROM links WHERE sourcePackage = :sourcePackage AND senderName = :senderName AND deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun countActiveSenderLinks(sourcePackage: String, senderName: String): Int

    @Query("UPDATE links SET deletedAt = :deletedAt WHERE sourcePackage = :sourcePackage AND senderName = :senderName AND deletedAt IS NULL AND archivedAt IS NULL")
    suspend fun moveSenderLinksToBin(sourcePackage: String, senderName: String, deletedAt: Long)

    @Query(
        """UPDATE links SET title = :title, siteName = :siteName, imageUrl = :imageUrl,
        metadataState = :state, metadataUpdatedAt = :updatedAt WHERE id = :id""",
    )
    suspend fun updateMetadata(
        id: Long,
        title: String?,
        siteName: String?,
        imageUrl: String?,
        state: Int,
        updatedAt: Long,
    )

    @Query("SELECT * FROM links WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LinkEntity?

    @Query("SELECT COUNT(*) FROM links WHERE timestamp >= :since AND deletedAt IS NULL")
    fun observeTotalSince(since: Long): Flow<Int>

    @Query("SELECT CAST(category AS TEXT) AS `key`, COUNT(*) AS count FROM links WHERE timestamp >= :since AND deletedAt IS NULL GROUP BY category ORDER BY count DESC")
    fun observeCategoryCounts(since: Long): Flow<List<CountByKey>>

    /** Uses the same active-link predicate as the Library category destination. */
    @Query("SELECT CAST(category AS TEXT) AS `key`, COUNT(*) AS count FROM links WHERE archivedAt IS NULL AND deletedAt IS NULL GROUP BY category ORDER BY count DESC")
    fun observeActiveCategoryCounts(): Flow<List<CountByKey>>

    @Query("SELECT host AS `key`, COUNT(*) AS count FROM links WHERE timestamp >= :since AND deletedAt IS NULL GROUP BY host ORDER BY count DESC LIMIT :limit")
    fun observeTopDomains(since: Long, limit: Int): Flow<List<CountByKey>>

    @Query("SELECT sourceApp AS `key`, COUNT(*) AS count FROM links WHERE timestamp >= :since AND deletedAt IS NULL GROUP BY sourceApp ORDER BY count DESC LIMIT :limit")
    fun observeTopSources(since: Long, limit: Int): Flow<List<CountByKey>>

    @Query("SELECT COUNT(*) FROM links WHERE isFavorite = 1 AND deletedAt IS NULL")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM links WHERE isSuspicious = 1 AND deletedAt IS NULL")
    fun observeSuspiciousCount(): Flow<Int>

    @Query("SELECT sourcePackage AS packageName, sourceApp AS appName, COUNT(*) AS linkCount FROM links WHERE sourcePackage IS NOT NULL AND deletedAt IS NULL GROUP BY sourcePackage, sourceApp ORDER BY linkCount DESC")
    fun observeSourceApps(): Flow<List<SourceAppSummary>>

    @Query("""SELECT senderName, sourcePackage AS packageName, sourceApp AS appName, COUNT(*) AS linkCount
        FROM links WHERE senderName IS NOT NULL AND senderName != '' AND sourcePackage IS NOT NULL AND deletedAt IS NULL
        GROUP BY senderName, sourcePackage, sourceApp ORDER BY linkCount DESC LIMIT 30""")
    fun observeSenders(): Flow<List<SenderSummary>>

    @Insert
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String)

    @Query("UPDATE collections SET isPinned = :pinned WHERE id = :id")
    suspend fun setCollectionPinned(id: Long, pinned: Boolean)

    @Query(
        """SELECT collections.id, collections.name, collections.colorToken, collections.isPinned,
        SUM(CASE WHEN links.archivedAt IS NULL AND links.deletedAt IS NULL AND links.id IS NOT NULL THEN 1 ELSE 0 END) AS linkCount
        FROM collections LEFT JOIN link_collections ON collections.id = link_collections.collectionId
        LEFT JOIN links ON links.id = link_collections.linkId
        GROUP BY collections.id ORDER BY collections.isPinned DESC, collections.createdAt ASC""",
    )
    fun observeCollections(): Flow<List<CollectionSummary>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToCollection(value: LinkCollectionEntity)

    @Query("DELETE FROM link_collections WHERE linkId = :linkId AND collectionId = :collectionId")
    suspend fun removeFromCollection(linkId: Long, collectionId: Long)

    @Query("SELECT collectionId FROM link_collections WHERE linkId = :linkId")
    fun observeCollectionIds(linkId: Long): Flow<List<Long>>

    @Query("SELECT collectionId FROM link_collections WHERE linkId = :linkId")
    suspend fun getCollectionIds(linkId: Long): List<Long>

    @Insert
    suspend fun insertRule(rule: AutomationRuleEntity): Long

    @Update
    suspend fun updateRule(rule: AutomationRuleEntity)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("SELECT * FROM automation_rules ORDER BY priority ASC, id ASC")
    fun observeRules(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules WHERE enabled = 1 ORDER BY priority ASC, id ASC")
    suspend fun enabledRules(): List<AutomationRuleEntity>

    @Query("SELECT * FROM links ORDER BY timestamp ASC, id ASC")
    suspend fun allLinks(): List<LinkEntity>

    @Query("SELECT * FROM collections ORDER BY createdAt ASC, id ASC")
    suspend fun allCollections(): List<CollectionEntity>

    @Query("SELECT * FROM link_collections")
    suspend fun allMemberships(): List<LinkCollectionEntity>

    @Query("SELECT * FROM automation_rules ORDER BY priority ASC, id ASC")
    suspend fun allRules(): List<AutomationRuleEntity>

    @Query("SELECT links.* FROM links INNER JOIN link_collections ON links.id = link_collections.linkId WHERE link_collections.collectionId = :collectionId AND links.archivedAt IS NULL AND links.deletedAt IS NULL ORDER BY links.timestamp ASC")
    suspend fun linksForCollectionExport(collectionId: Long): List<LinkEntity>

    @Query("SELECT * FROM links WHERE category = :category AND archivedAt IS NULL AND deletedAt IS NULL ORDER BY timestamp ASC")
    suspend fun linksForCategoryExport(category: Int): List<LinkEntity>

    @Query("SELECT id FROM links WHERE url = :url AND sourceApp = :sourceApp AND timestamp = :timestamp LIMIT 1")
    suspend fun findExactLink(url: String, sourceApp: String, timestamp: Long): Long?

    @Query("""SELECT COUNT(*) FROM automation_rules WHERE name = :name AND hostPattern = :host
        AND IFNULL(pathContains, '') = IFNULL(:path, '') AND IFNULL(category, -1) = IFNULL(:category, -1)
        AND IFNULL(collectionId, -1) = IFNULL(:collectionId, -1)""")
    suspend fun countExactRule(name: String, host: String, path: String?, category: Int?, collectionId: Long?): Int

    @Query("DELETE FROM automation_rules")
    suspend fun deleteAllRules()

    @Query("DELETE FROM collections")
    suspend fun deleteAllCollections()

    @Query("DELETE FROM links")
    suspend fun deleteAllLinksImmediately()

    @Transaction
    suspend fun importBackup(backup: EasyLinkBackup, replace: Boolean): ImportResult {
        if (replace) {
            deleteAllRules()
            deleteAllCollections()
            deleteAllLinksImmediately()
        }
        val knownCollections = HashMap(allCollections().associateBy { it.name.lowercase(Locale.ROOT) })
        val collectionIds = HashMap<String, Long>(backup.collections.size)
        var importedCollections = 0
        backup.collections.forEach { value ->
            val key = value.name.lowercase(Locale.ROOT)
            val existing = knownCollections[key]
            val id = existing?.id ?: insertCollection(
                CollectionEntity(
                    name = value.name,
                    colorToken = value.colorToken,
                    createdAt = value.createdAt,
                    isPinned = value.isPinned,
                ),
            ).also { newId ->
                knownCollections[key] = CollectionEntity(newId, value.name, value.colorToken, value.createdAt, value.isPinned)
                importedCollections++
            }
            collectionIds[value.ref] = id
        }
        var importedLinks = 0
        backup.links.forEach { value ->
            val existingId = findExactLink(value.url, value.sourceApp, value.timestamp)
            val id = existingId ?: run {
                val processed = LinkExtractor.process(value.url) ?: return@forEach
                insert(
                    LinkEntity(
                        url = processed.url,
                        originalUrl = value.originalUrl ?: processed.originalUrl,
                        host = processed.host,
                        sourcePackage = value.sourcePackage,
                        sourceApp = value.sourceApp,
                        senderName = value.senderName,
                        conversationName = value.conversationName,
                        timestamp = value.timestamp,
                        category = value.category,
                        isSuspicious = value.isSuspicious,
                        isFavorite = value.isFavorite,
                        isReadLater = value.isReadLater,
                        wasCleaned = value.wasCleaned,
                        title = value.title,
                        siteName = value.siteName,
                        metadataState = if (value.title != null || value.siteName != null) LinkEntity.METADATA_LOADED else LinkEntity.METADATA_PENDING,
                        archivedAt = value.archivedAt,
                        deletedAt = value.deletedAt,
                    ),
                ).also { importedLinks++ }
            }
            value.collectionRefs.forEach { ref -> collectionIds[ref]?.let { addToCollection(LinkCollectionEntity(id, it)) } }
        }
        backup.rules.forEach { value ->
            val collectionId = value.collectionRef?.let(collectionIds::get)
            if (countExactRule(value.name, value.hostPattern, value.pathContains, value.category, collectionId) == 0) insertRule(
                AutomationRuleEntity(
                    name = value.name,
                    hostPattern = value.hostPattern,
                    pathContains = value.pathContains,
                    category = value.category,
                    collectionId = collectionId,
                    enabled = value.enabled,
                    priority = value.priority,
                ),
            )
        }
        return ImportResult(importedLinks, importedCollections)
    }
}
