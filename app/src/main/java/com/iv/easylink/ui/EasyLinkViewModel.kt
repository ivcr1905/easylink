package com.iv.easylink.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iv.easylink.EasyLinkApplication
import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.repository.LinkQuery
import com.iv.easylink.data.settings.AppearanceMode
import com.iv.easylink.ingestion.LinkSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EasyLinkViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as EasyLinkApplication).container
    private val repository = container.linkRepository
    private val mutableQuery = MutableStateFlow(LinkQuery())
    private val metadataInFlight = HashSet<Long>()
    private val pendingUndo = LinkedHashMap<Long, Pair<LinkEntity, List<Long>>>()

    val query = mutableQuery.asStateFlow()
    val settings = container.settings.state
    val links = mutableQuery.flatMapLatest(repository::observeLinks)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collections = repository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rules = repository.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val weekStart = System.currentTimeMillis() - 7L * 86_400_000L
    val weekTotal = repository.observeTotalSince(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val categoryCounts = repository.observeCategoryCounts(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topDomains = repository.observeTopDomains(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topSources = repository.observeTopSources(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteCount = repository.observeFavoriteCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val suspiciousCount = repository.observeSuspiciousCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val sourceApps = repository.observeSourceApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(value: String) { mutableQuery.value = mutableQuery.value.copy(text = value.take(100), offset = 0) }
    fun setCategory(value: Int) { mutableQuery.value = mutableQuery.value.copy(category = value, offset = 0) }
    fun setSource(packageName: String) { mutableQuery.value = mutableQuery.value.copy(sourcePackage = packageName, offset = 0) }
    fun setDateRange(days: Int) {
        mutableQuery.value = mutableQuery.value.copy(
            since = if (days <= 0) 0 else System.currentTimeMillis() - days * 86_400_000L,
            dateRangeDays = days,
            offset = 0,
        )
    }
    fun toggleSort() { mutableQuery.value = mutableQuery.value.copy(oldestFirst = !mutableQuery.value.oldestFirst, offset = 0) }
    fun toggleFavorites() { mutableQuery.value = mutableQuery.value.copy(favoriteOnly = !mutableQuery.value.favoriteOnly, offset = 0) }
    fun toggleSuspicious() { mutableQuery.value = mutableQuery.value.copy(suspiciousOnly = !mutableQuery.value.suspiciousOnly, offset = 0) }
    fun loadMore() { mutableQuery.value = mutableQuery.value.copy(limit = (mutableQuery.value.limit + 100).coerceAtMost(500)) }
    fun resetFilters() { mutableQuery.value = LinkQuery() }

    fun addManual(text: String, onResult: (Int) -> Unit) = viewModelScope.launch {
        val count = runCatching { container.linkIngestor.ingest(text, LinkSource(null, "Manually added")).size }.getOrDefault(0)
        onResult(count)
    }

    fun delete(link: LinkEntity) = viewModelScope.launch {
        val memberships = repository.getCollectionIds(link.id)
        synchronized(pendingUndo) {
            pendingUndo[link.id] = link to memberships
            while (pendingUndo.size > 10) pendingUndo.remove(pendingUndo.keys.first())
        }
        repository.deleteLinks(listOf(link.id))
    }
    fun deleteSelected(ids: Set<Long>) = viewModelScope.launch { repository.deleteLinks(ids.toList()) }
    fun undoDelete(linkId: Long) = viewModelScope.launch {
        val deleted = synchronized(pendingUndo) { pendingUndo.remove(linkId) } ?: return@launch
        repository.restoreLink(deleted.first)
        deleted.second.forEach { repository.addToCollection(linkId, it) }
    }
    fun discardUndo(linkId: Long) { synchronized(pendingUndo) { pendingUndo.remove(linkId) } }
    fun clearAll() = viewModelScope.launch { repository.clearAll() }
    fun clearCategory(category: Int) = viewModelScope.launch { repository.clearCategory(category) }
    fun clearOlderThan(days: Int) = viewModelScope.launch {
        repository.clearOlderThan(System.currentTimeMillis() - days.coerceAtLeast(1) * 86_400_000L)
    }
    fun toggleFavorite(link: LinkEntity) = viewModelScope.launch { repository.setFavorite(link.id, !link.isFavorite) }
    fun setLinkCategory(linkId: Long, category: Int) = viewModelScope.launch { repository.setCategory(linkId, category) }
    fun addToCollection(linkId: Long, collectionId: Long) = viewModelScope.launch { repository.addToCollection(linkId, collectionId) }
    fun addSelectedToCollection(linkIds: Set<Long>, collectionId: Long) = viewModelScope.launch {
        linkIds.forEach { repository.addToCollection(it, collectionId) }
    }
    fun collectionLinks(collectionId: Long): Flow<List<LinkEntity>> = repository.observeCollectionLinks(collectionId)

    fun requestMetadata(link: LinkEntity) {
        if (link.metadataState != LinkEntity.METADATA_PENDING || !settings.value.richPreviews) return
        synchronized(metadataInFlight) { if (!metadataInFlight.add(link.id)) return }
        viewModelScope.launch {
            try {
                val metadata = container.metadataClient.fetch(link.url)
                repository.updateMetadata(link.id, metadata?.title, metadata?.siteName, metadata?.imageUrl, metadata != null)
            } finally {
                synchronized(metadataInFlight) { metadataInFlight.remove(link.id) }
            }
        }
    }

    fun createCollection(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.createCollection(name.take(40), collections.value.size % 8)
    }
    fun deleteCollection(id: Long) = viewModelScope.launch { repository.deleteCollection(id) }
    fun renameCollection(id: Long, name: String) = viewModelScope.launch { if (name.isNotBlank()) repository.renameCollection(id, name) }
    fun createRule(name: String, host: String, category: Int?, collectionId: Long?) = viewModelScope.launch {
        val cleanHost = host.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (name.isBlank() || cleanHost.isBlank() || (category == null && collectionId == null)) return@launch
        repository.createRule(
            AutomationRuleEntity(
                name = name.take(50), hostPattern = cleanHost.take(253), category = category,
                collectionId = collectionId, priority = rules.value.size,
            ),
        )
    }
    fun toggleRule(rule: AutomationRuleEntity) = viewModelScope.launch { repository.updateRule(rule.copy(enabled = !rule.enabled)) }
    fun editRule(rule: AutomationRuleEntity, name: String, host: String, category: Int?, collectionId: Long?) = viewModelScope.launch {
        val cleanHost = host.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (name.isNotBlank() && cleanHost.isNotBlank()) {
            repository.updateRule(rule.copy(name = name.take(50), hostPattern = cleanHost.take(253), category = category, collectionId = collectionId))
        }
    }
    fun moveRule(rule: AutomationRuleEntity, direction: Int) = viewModelScope.launch {
        val current = rules.value.indexOfFirst { it.id == rule.id }
        val target = (current + direction).coerceIn(0, rules.value.lastIndex)
        if (current >= 0 && target != current) {
            val other = rules.value[target]
            repository.updateRule(rule.copy(priority = other.priority))
            repository.updateRule(other.copy(priority = rule.priority))
        }
    }
    fun deleteRule(id: Long) = viewModelScope.launch { repository.deleteRule(id) }

    fun setAppearance(value: AppearanceMode) = container.settings.setAppearanceMode(value)
    fun setRichPreviews(value: Boolean) = container.settings.setRichPreviews(value)
    fun setRetentionDays(value: Int) {
        container.settings.setRetentionDays(value)
        if (value > 0) clearOlderThan(value)
    }
    fun setPackageIgnored(packageName: String, ignored: Boolean) = container.settings.setPackageIgnored(packageName, ignored)
    fun setOnboardingSeen() = container.settings.setOnboardingSeen()
}
