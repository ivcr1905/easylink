package com.iv.easylink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query(
        """SELECT * FROM links
        WHERE (:query = '' OR url LIKE '%' || :query || '%' OR host LIKE '%' || :query || '%'
            OR IFNULL(title, '') LIKE '%' || :query || '%' OR sourceApp LIKE '%' || :query || '%')
          AND (:category < 0 OR category = :category)
          AND (:sourcePackage = '' OR sourcePackage = :sourcePackage)
          AND timestamp >= :since
          AND (:favoriteOnly = 0 OR isFavorite = 1)
          AND (:suspiciousOnly = 0 OR isSuspicious = 1)
        ORDER BY CASE WHEN :oldestFirst = 1 THEN timestamp END ASC,
                 CASE WHEN :oldestFirst = 0 THEN timestamp END DESC
        LIMIT :limit OFFSET :offset""",
    )
    fun observeLinks(
        query: String,
        category: Int,
        sourcePackage: String,
        since: Long,
        favoriteOnly: Boolean,
        suspiciousOnly: Boolean,
        oldestFirst: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<LinkEntity>>

    @Query(
        """SELECT links.* FROM links
        INNER JOIN link_collections ON links.id = link_collections.linkId
        WHERE link_collections.collectionId = :collectionId
        ORDER BY links.timestamp DESC LIMIT :limit""",
    )
    fun observeCollectionLinks(collectionId: Long, limit: Int): Flow<List<LinkEntity>>

    @Query("SELECT COUNT(*) FROM links WHERE url = :url AND sourceApp = :sourceApp AND timestamp >= :since")
    suspend fun countRecent(url: String, sourceApp: String, since: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: LinkEntity): Long

    @Transaction
    suspend fun insertIfNotRecent(link: LinkEntity, since: Long): Long? {
        if (countRecent(link.url, link.sourceApp, since) != 0) return null
        return insert(link).takeIf { it > 0 }
    }

    @Query("DELETE FROM links WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM links")
    suspend fun deleteAll()

    @Query("DELETE FROM links WHERE category = :category")
    suspend fun deleteCategory(category: Int)

    @Query("DELETE FROM links WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("UPDATE links SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE links SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: Int)

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

    @Query("SELECT COUNT(*) FROM links WHERE timestamp >= :since")
    fun observeTotalSince(since: Long): Flow<Int>

    @Query("SELECT CAST(category AS TEXT) AS `key`, COUNT(*) AS count FROM links WHERE timestamp >= :since GROUP BY category ORDER BY count DESC")
    fun observeCategoryCounts(since: Long): Flow<List<CountByKey>>

    @Query("SELECT host AS `key`, COUNT(*) AS count FROM links WHERE timestamp >= :since GROUP BY host ORDER BY count DESC LIMIT :limit")
    fun observeTopDomains(since: Long, limit: Int): Flow<List<CountByKey>>

    @Query("SELECT sourceApp AS `key`, COUNT(*) AS count FROM links WHERE timestamp >= :since GROUP BY sourceApp ORDER BY count DESC LIMIT :limit")
    fun observeTopSources(since: Long, limit: Int): Flow<List<CountByKey>>

    @Query("SELECT COUNT(*) FROM links WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM links WHERE isSuspicious = 1")
    fun observeSuspiciousCount(): Flow<Int>

    @Query("SELECT sourcePackage AS packageName, sourceApp AS appName, COUNT(*) AS linkCount FROM links WHERE sourcePackage IS NOT NULL GROUP BY sourcePackage, sourceApp ORDER BY linkCount DESC")
    fun observeSourceApps(): Flow<List<SourceAppSummary>>

    @Insert
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String)

    @Query(
        """SELECT collections.id, collections.name, collections.colorToken, COUNT(link_collections.linkId) AS linkCount
        FROM collections LEFT JOIN link_collections ON collections.id = link_collections.collectionId
        GROUP BY collections.id ORDER BY collections.createdAt ASC""",
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
}
