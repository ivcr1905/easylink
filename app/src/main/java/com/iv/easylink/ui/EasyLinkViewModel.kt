package com.iv.easylink.ui

import android.app.Application
import android.util.Log
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iv.easylink.EasyLinkApplication
import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.repository.LinkQuery
import com.iv.easylink.data.repository.LinkLocation
import com.iv.easylink.data.settings.AppearanceMode
import com.iv.easylink.data.settings.InboxShortcut
import com.iv.easylink.data.settings.InboxShortcutType
import com.iv.easylink.data.settings.MuteBehavior
import com.iv.easylink.data.settings.MutedSenderRule
import com.iv.easylink.data.settings.SenderRuleBehavior
import com.iv.easylink.data.settings.SwipeMode
import com.iv.easylink.data.transfer.BackupCodec
import com.iv.easylink.data.transfer.EasyLinkBackup
import com.iv.easylink.data.transfer.ImportResult
import com.iv.easylink.ingestion.LinkSource
import com.iv.easylink.ingestion.PreparedLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.iv.easylink.utils.LinkExtractor

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EasyLinkViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as EasyLinkApplication).container
    private val repository = container.linkRepository
    private val mutableQuery = MutableStateFlow(LinkQuery())
    private val metadataInFlight = HashSet<Long>()

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
    val libraryCategoryCounts = repository.observeActiveCategoryCounts()
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
    val senders = repository.observeSenders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(value: String) { mutableQuery.value = mutableQuery.value.copy(text = value.take(100), offset = 0) }
    fun setCategory(value: Int) { mutableQuery.value = mutableQuery.value.copy(category = value, offset = 0) }
    fun setSource(packageName: String) { mutableQuery.value = mutableQuery.value.copy(sourcePackage = packageName, offset = 0) }
    fun setSender(senderName: String, packageName: String) {
        val selected = mutableQuery.value.senderName == senderName && mutableQuery.value.sourcePackage == packageName
        mutableQuery.value = mutableQuery.value.copy(
            senderName = if (selected) "" else senderName,
            sourcePackage = if (selected) "" else packageName,
            offset = 0,
        )
    }
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
    fun showActiveLinks() { mutableQuery.value = LinkQuery(location = LinkLocation.ACTIVE) }
    fun showArchivedLinks() { mutableQuery.value = LinkQuery(location = LinkLocation.ARCHIVED) }
    fun showBin() { mutableQuery.value = LinkQuery(location = LinkLocation.BIN) }
    fun showFavorites() { mutableQuery.value = LinkQuery(location = LinkLocation.ACTIVE, favoriteOnly = true) }
    fun showReadLater() { mutableQuery.value = LinkQuery(location = LinkLocation.ACTIVE, readLaterOnly = true) }
    fun showWarnings() { mutableQuery.value = LinkQuery(location = LinkLocation.ACTIVE, suspiciousOnly = true) }

    fun prepareLinks(text: String): List<PreparedLink> = container.linkIngestor.prepare(text.take(250_000))

    fun importPrepared(
        prepared: List<PreparedLink>,
        collectionId: Long?,
        newCollectionName: String?,
        onResult: (Int) -> Unit,
    ) = viewModelScope.launch {
        val count = try {
            val target = collectionId ?: newCollectionName?.takeIf { it.isNotBlank() }?.let {
                repository.createCollection(it.take(40), collections.value.size % 8)
            }
            if (target == null) {
                container.linkIngestor.ingestPrepared(prepared, LinkSource(null, "Manually added")).size
            } else {
                container.linkIngestor.ingestPreparedToCollection(prepared, LinkSource(null, "Manually added"), target)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to import pasted links", error)
            0
        }
        onResult(count)
    }

    fun delete(link: LinkEntity) = launchSafely("move link to Bin") {
        repository.deleteLinks(listOf(link.id))
    }
    fun deleteSelected(ids: Set<Long>) = launchSafely("move selected links to Bin") { repository.deleteLinks(ids.toList()) }
    fun undoDelete(linkId: Long) = launchSafely("restore link") { repository.restoreFromBin(listOf(linkId)) }
    fun archive(linkId: Long) = launchSafely("archive link") { repository.archiveLinks(listOf(linkId)) }
    fun archiveSelected(ids: Set<Long>) = launchSafely("archive selected links") { repository.archiveLinks(ids.toList()) }
    fun unarchive(linkId: Long) = launchSafely("unarchive link") { repository.unarchiveLinks(listOf(linkId)) }
    fun unarchiveSelected(ids: Set<Long>) = launchSafely("unarchive selected links") { repository.unarchiveLinks(ids.toList()) }
    fun restoreFromBin(linkId: Long) = launchSafely("restore link from Bin") { repository.restoreFromBin(listOf(linkId)) }
    fun restoreSelectedFromBin(ids: Set<Long>) = launchSafely("restore selected links") { repository.restoreFromBin(ids.toList()) }
    fun permanentlyDelete(linkId: Long) = launchSafely("permanently delete link") { repository.permanentlyDelete(listOf(linkId)) }
    fun permanentlyDeleteSelected(ids: Set<Long>) = launchSafely("permanently delete selected links") { repository.permanentlyDelete(ids.toList()) }
    fun clearAll() = launchSafely("clear active links") { repository.clearAll() }
    fun clearCategory(category: Int) = launchSafely("clear category") { repository.clearCategory(category) }
    fun clearOlderThan(days: Int) = launchSafely("clear older links") {
        repository.clearOlderThan(System.currentTimeMillis() - days.coerceAtLeast(1) * 86_400_000L)
    }
    fun toggleFavorite(link: LinkEntity) = launchSafely("update favorite") { repository.setFavorite(link.id, !link.isFavorite) }
    fun toggleReadLater(link: LinkEntity) = launchSafely("update Read later") { repository.setReadLater(link.id, !link.isReadLater) }
    fun setLinkCategory(linkId: Long, category: Int) = launchSafely("update category") { repository.setCategory(linkId, category) }
    fun previewLinkEdit(rawUrl: String) = LinkExtractor.process(rawUrl.take(MAX_URL_LENGTH))
    fun updateLinkUrl(linkId: Long, rawUrl: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val success = try {
            repository.updateLinkUrl(linkId, rawUrl.take(MAX_URL_LENGTH))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to edit link", error)
            false
        }
        onResult(success)
    }
    fun addToCollection(linkId: Long, collectionId: Long) = launchSafely("add link to collection") { repository.addToCollection(linkId, collectionId) }
    fun addSelectedToCollection(linkIds: Set<Long>, collectionId: Long) = launchSafely("add selected links to collection") {
        linkIds.forEach { repository.addToCollection(it, collectionId) }
    }
    fun collectionLinks(collectionId: Long): Flow<List<LinkEntity>> = repository.observeCollectionLinks(collectionId)

    fun requestMetadata(link: LinkEntity) {
        if (link.metadataState != LinkEntity.METADATA_PENDING || !settings.value.richPreviews) return
        synchronized(metadataInFlight) { if (!metadataInFlight.add(link.id)) return }
        viewModelScope.launch {
            try {
                val metadata = container.metadataClient.fetch(link.url)
                repository.updateMetadata(link.id, metadata?.title, metadata?.siteName, metadata != null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(LOG_TAG, "Unable to load link preview", error)
            } finally {
                synchronized(metadataInFlight) { metadataInFlight.remove(link.id) }
            }
        }
    }

    fun createCollection(name: String) = launchSafely("create collection") {
        if (name.isNotBlank()) repository.createCollection(name.take(40), collections.value.size % 8)
    }
    fun deleteCollection(id: Long) = launchSafely("delete collection") {
        repository.deleteCollection(id)
        container.settings.setInboxShortcuts(
            settings.value.inboxShortcuts.filterNot { it.type == InboxShortcutType.COLLECTION && it.targetId == id },
        )
    }
    fun renameCollection(id: Long, name: String) = launchSafely("rename collection") {
        if (name.isNotBlank()) repository.renameCollection(id, name)
    }
    fun setCollectionPinned(id: Long, pinned: Boolean) = launchSafely("pin collection") {
        repository.setCollectionPinned(id, pinned)
    }
    fun createRule(name: String, host: String, category: Int?, collectionId: Long?) = launchSafely("create automation rule") {
        val cleanHost = host.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (name.isBlank() || cleanHost.isBlank() || (category == null && collectionId == null)) return@launchSafely
        repository.createRule(
            AutomationRuleEntity(
                name = name.take(50), hostPattern = cleanHost.take(253), category = category,
                collectionId = collectionId, priority = rules.value.size,
            ),
        )
    }
    fun toggleRule(rule: AutomationRuleEntity) = launchSafely("toggle automation rule") {
        repository.updateRule(rule.copy(enabled = !rule.enabled))
    }
    fun editRule(rule: AutomationRuleEntity, name: String, host: String, category: Int?, collectionId: Long?) = launchSafely("edit automation rule") {
        val cleanHost = host.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (name.isNotBlank() && cleanHost.isNotBlank()) {
            repository.updateRule(rule.copy(name = name.take(50), hostPattern = cleanHost.take(253), category = category, collectionId = collectionId))
        }
    }
    fun moveRule(rule: AutomationRuleEntity, direction: Int) = launchSafely("reorder automation rule") {
        val snapshot = rules.value
        if (snapshot.size < 2) return@launchSafely
        val current = snapshot.indexOfFirst { it.id == rule.id }
        if (current < 0) return@launchSafely
        val target = current + direction
        if (target !in snapshot.indices) return@launchSafely
        val other = snapshot[target]
        repository.updateRule(rule.copy(priority = other.priority))
        repository.updateRule(other.copy(priority = rule.priority))
    }
    fun deleteRule(id: Long) = launchSafely("delete automation rule") { repository.deleteRule(id) }

    fun setAppearance(value: AppearanceMode) = container.settings.setAppearanceMode(value)
    fun setRichPreviews(value: Boolean) = container.settings.setRichPreviews(value)
    fun setRetentionDays(value: Int) {
        container.settings.setRetentionDays(value)
        if (value > 0) clearOlderThan(value)
    }
    fun setPackageIgnored(packageName: String, ignored: Boolean) = container.settings.setPackageIgnored(packageName, ignored)
    fun setOnboardingSeen() = container.settings.setOnboardingSeen()
    fun setSwipeMode(value: SwipeMode) = container.settings.setSwipeMode(value)
    fun setMuteBehavior(value: MuteBehavior) = container.settings.setMuteBehavior(value)
    fun setMoveExistingOnIgnore(value: Boolean) = container.settings.setMoveExistingOnIgnore(value)
    fun unmuteSender(rule: MutedSenderRule) = container.settings.unmuteSender(rule.sourcePackage, rule.senderName)

    fun selectShortcut(shortcut: InboxShortcut) {
        val text = mutableQuery.value.text
        mutableQuery.value = when (shortcut.type) {
            InboxShortcutType.ALL -> LinkQuery(text = text)
            InboxShortcutType.RECENT -> LinkQuery(text = text, since = System.currentTimeMillis() - MILLIS_PER_DAY, dateRangeDays = 1)
            InboxShortcutType.FAVORITES -> LinkQuery(text = text, favoriteOnly = true)
            InboxShortcutType.READ_LATER -> LinkQuery(text = text, readLaterOnly = true)
            InboxShortcutType.CATEGORY -> LinkQuery(text = text, category = shortcut.targetId.toInt())
            InboxShortcutType.COLLECTION -> LinkQuery(text = text, collectionId = shortcut.targetId)
        }
    }

    fun setShortcutVisible(shortcut: InboxShortcut, visible: Boolean) {
        val current = settings.value.inboxShortcuts
        val next = if (visible) {
            if (shortcut in current || current.size >= MAX_SHORTCUTS) current else current + shortcut
        } else {
            if (shortcut.type == InboxShortcutType.ALL) current else current.filterNot { it == shortcut }
        }
        container.settings.setInboxShortcuts(next)
    }

    fun moveShortcut(shortcut: InboxShortcut, direction: Int) {
        if (shortcut.type == InboxShortcutType.ALL) return
        val next = settings.value.inboxShortcuts.toMutableList()
        val index = next.indexOf(shortcut)
        val target = index + direction
        if (index <= 0 || target !in 1 until next.size) return
        next[index] = next[target].also { next[target] = shortcut }
        container.settings.setInboxShortcuts(next)
    }

    fun applyMute(
        sourcePackage: String,
        sourceApp: String,
        senderName: String,
        behavior: SenderRuleBehavior,
    ) {
        container.settings.muteSender(sourcePackage, sourceApp, senderName, behavior)
        if (behavior == SenderRuleBehavior.IGNORE_FUTURE_LINKS && settings.value.moveExistingOnIgnore) {
            launchSafely("move ignored sender links to Bin") { repository.moveSenderLinksToBin(sourcePackage, senderName) }
        }
    }

    fun countActiveSenderLinks(sourcePackage: String, senderName: String, onResult: (Int) -> Unit) = viewModelScope.launch {
        val count = try {
            repository.countActiveSenderLinks(sourcePackage, senderName)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to count sender links", error)
            0
        }
        onResult(count)
    }

    fun exportAll(uri: Uri, onResult: (Boolean) -> Unit) = exportTo(uri, repository::exportAll, onResult)

    fun exportCollection(collectionId: Long, uri: Uri, onResult: (Boolean) -> Unit) =
        exportTo(uri, { repository.exportCollection(collectionId) }, onResult)

    fun exportCategory(category: Int, uri: Uri, onResult: (Boolean) -> Unit) =
        exportTo(uri, { repository.exportCategory(category) }, onResult)

    fun collectionBackup(collectionId: Long, onResult: (EasyLinkBackup?) -> Unit) = viewModelScope.launch {
        onResult(loadBackupSafely("load collection") { repository.exportCollection(collectionId) })
    }

    fun categoryBackup(category: Int, onResult: (EasyLinkBackup?) -> Unit) = viewModelScope.launch {
        onResult(loadBackupSafely("load category") { repository.exportCategory(category) })
    }

    fun readBackup(uri: Uri, onResult: (Result<EasyLinkBackup>) -> Unit) = viewModelScope.launch {
        val result = try {
            Result.success(withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use(BackupCodec::read)
                    ?: error("The selected file could not be opened")
            })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        onResult(result)
    }

    fun importBackup(backup: EasyLinkBackup, replace: Boolean, onResult: (Result<ImportResult>) -> Unit) = viewModelScope.launch {
        val result = try {
            Result.success(withContext(Dispatchers.IO) { repository.importBackup(backup, replace) })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        onResult(result)
    }

    private fun exportTo(
        uri: Uri,
        createBackup: suspend () -> EasyLinkBackup?,
        onResult: (Boolean) -> Unit,
    ) = viewModelScope.launch {
        val success = try {
            val backup = createBackup() ?: return@launch onResult(false)
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "w")?.use { BackupCodec.write(backup, it) }
                    ?: error("The destination file could not be opened")
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to export data", error)
            false
        }
        onResult(success)
    }

    private suspend fun loadBackupSafely(operation: String, block: suspend () -> EasyLinkBackup?): EasyLinkBackup? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.e(LOG_TAG, "Unable to $operation", error)
        null
    }

    private fun launchSafely(operation: String, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to $operation", error)
        }
    }

    private companion object {
        const val LOG_TAG = "EasyLink"
        const val MAX_URL_LENGTH = 4_096
        const val MAX_SHORTCUTS = 5
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
