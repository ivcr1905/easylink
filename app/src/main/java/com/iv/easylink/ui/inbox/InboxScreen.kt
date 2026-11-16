package com.iv.easylink.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.core.net.toUri
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.repository.LinkLocation
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.data.settings.InboxShortcut
import com.iv.easylink.data.settings.InboxShortcutType
import com.iv.easylink.data.settings.SwipeMode
import com.iv.easylink.ui.theme.EasyLinkTheme
import com.iv.easylink.ui.theme.EasyLinkDimensions
import com.iv.easylink.ui.components.CompactLinkRow
import com.iv.easylink.ui.components.EasyLinkIcons
import androidx.compose.foundation.text.selection.SelectionContainer
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: EasyLinkViewModel,
    showShortcuts: Boolean = false,
    onOpenMenu: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
) {
    val links by viewModel.links.collectAsState()
    val query by viewModel.query.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingOpen by remember { mutableStateOf<LinkEntity?>(null) }
    var actionLink by remember { mutableStateOf<LinkEntity?>(null) }
    var editLink by remember { mutableStateOf<LinkEntity?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<LinkEntity?>(null) }
    var pendingPermanentSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var customizeShortcuts by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (query.location == LinkLocation.ACTIVE) FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(EasyLinkIcons.Add, "Add link", modifier = Modifier.size(22.dp)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val shortcuts = settings.inboxShortcuts
        val selectedShortcutIndex = shortcuts.indexOfFirst { it.matches(query) }.coerceAtLeast(0)
        var horizontalDrag by remember { mutableFloatStateOf(0f) }
        val browseModifier = if (showShortcuts && query.location == LinkLocation.ACTIVE && settings.swipeMode == SwipeMode.BROWSE_LISTS && shortcuts.isNotEmpty()) {
            Modifier.pointerInput(shortcuts, selectedShortcutIndex) {
                val swipeThreshold = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onHorizontalDrag = { _, amount -> horizontalDrag += amount },
                    onDragEnd = {
                        when {
                            horizontalDrag > swipeThreshold && selectedShortcutIndex == 0 -> onOpenMenu()
                            horizontalDrag < -swipeThreshold && selectedShortcutIndex == shortcuts.lastIndex -> onOpenLibrary()
                            horizontalDrag > swipeThreshold -> viewModel.selectShortcut(shortcuts[selectedShortcutIndex - 1])
                            horizontalDrag < -swipeThreshold -> viewModel.selectShortcut(shortcuts[selectedShortcutIndex + 1])
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f },
                )
            }
        } else Modifier
        Column(Modifier.fillMaxSize().padding(padding).then(browseModifier)) {
            if (selected.isNotEmpty()) {
                SelectionBar(
                    count = selected.size,
                    location = query.location,
                    collections = collections,
                    onClose = { selected = emptySet() },
                    onArchive = { viewModel.archiveSelected(selected); selected = emptySet() },
                    onUnarchive = { viewModel.unarchiveSelected(selected); selected = emptySet() },
                    onRestore = { viewModel.restoreSelectedFromBin(selected); selected = emptySet() },
                    onDelete = { viewModel.deleteSelected(selected); selected = emptySet() },
                    onPermanentDelete = { pendingPermanentSelection = selected; selected = emptySet() },
                    onMove = { viewModel.addSelectedToCollection(selected, it); selected = emptySet() },
                )
            } else {
                if (showShortcuts && query.location == LinkLocation.ACTIVE && shortcuts.size > 1) {
                    InboxShortcutBar(
                        shortcuts,
                        query,
                        collections,
                        viewModel::selectShortcut,
                        onCustomize = { customizeShortcuts = true },
                    )
                }
            }
            if (links.isEmpty()) {
                EmptyInbox(
                    location = query.location,
                    filtered = query != com.iv.easylink.data.repository.LinkQuery(location = query.location),
                    onReset = {
                        when (query.location) {
                            LinkLocation.ACTIVE -> viewModel.showActiveLinks()
                            LinkLocation.ARCHIVED -> viewModel.showArchivedLinks()
                            LinkLocation.BIN -> viewModel.showBin()
                        }
                    },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = EasyLinkDimensions.screenPadding, end = EasyLinkDimensions.screenPadding, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(links, key = { _, link -> link.id }) { index, link ->
                        val label = dateLabel(link.timestamp)
                        val previousTimestamp = links.getOrNull(index - 1)?.timestamp
                        if (previousTimestamp == null || dateLabel(previousTimestamp) != label) {
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = EasyLinkTheme.extraColors.mutedText,
                                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
                            )
                        }
                        LinkSwipeCard(
                            link = link,
                            selected = link.id in selected,
                            selectionMode = selected.isNotEmpty(),
                            location = query.location,
                            onSelect = { selected = if (link.id in selected) selected - link.id else selected + link.id },
                            onClick = { actionLink = link },
                            onArchive = { viewModel.archive(link.id) },
                            onUnarchive = { viewModel.unarchive(link.id) },
                            onDelete = {
                                viewModel.delete(link)
                                scope.launch {
                                    if (snackbar.showSnackbar("Moved to Bin", "Undo") == androidx.compose.material3.SnackbarResult.ActionPerformed) viewModel.undoDelete(link.id)
                                }
                            },
                            onMetadata = { if (query.location != LinkLocation.BIN) viewModel.requestMetadata(link) },
                            onFavorite = { viewModel.toggleFavorite(link) },
                            swipeEnabled = settings.swipeMode == SwipeMode.LINK_ACTIONS,
                        )
                    }
                    if (links.size >= query.limit && query.limit < 500) {
                        item { TextButton(viewModel::loadMore, Modifier.fillMaxWidth()) { Text("Load more") } }
                    }
                }
            }
        }
    }

    if (showAdd) AddLinkDialog(collections = collections, onDismiss = { showAdd = false }) { value, collectionId ->
        val prepared = viewModel.prepareLinks(value)
        if (prepared.isEmpty()) {
            scope.launch { snackbar.showSnackbar("No valid link found") }
        } else {
            viewModel.importPrepared(prepared, collectionId, null) { count ->
                scope.launch {
                    snackbar.showSnackbar(
                        if (count > 0) "$count link${if (count == 1) "" else "s"} saved"
                        else "No new link saved",
                    )
                }
            }
        }
        showAdd = false
    }
    if (customizeShortcuts) {
        InboxShortcutsDialog(
            shortcuts = settings.inboxShortcuts,
            collections = collections,
            onToggle = viewModel::setShortcutVisible,
            onMove = viewModel::moveShortcut,
            onDismiss = { customizeShortcuts = false },
        )
    }
    actionLink?.let { link ->
        LinkActionSheet(
            link = link,
            location = query.location,
            collections = collections,
            onDismiss = { actionLink = null },
            onOpen = { actionLink = null; if (link.isSuspicious) pendingOpen = link else openUrl(context, link.url) },
            onCopy = { copyUrl(context, link.url); actionLink = null },
            onShare = { shareUrl(context, link.url); actionLink = null },
            onFavorite = { viewModel.toggleFavorite(link); actionLink = null },
            onReadLater = { viewModel.toggleReadLater(link); actionLink = null },
            onEdit = { editLink = link; actionLink = null },
            onCopyOriginal = { link.originalUrl?.let { copyUrl(context, it) } },
            onCollection = { viewModel.addToCollection(link.id, it); actionLink = null },
            onCategory = { viewModel.setLinkCategory(link.id, it); actionLink = null },
            onArchive = { viewModel.archive(link.id); actionLink = null },
            onUnarchive = { viewModel.unarchive(link.id); actionLink = null },
            onDelete = { viewModel.delete(link); actionLink = null },
            onRestore = { viewModel.restoreFromBin(link.id); actionLink = null },
            onPermanentDelete = { pendingPermanentDelete = link; actionLink = null },
        )
    }
    editLink?.let { link ->
        EditLinkDialog(
            link = link,
            preview = viewModel::previewLinkEdit,
            onDismiss = { editLink = null },
            onSave = { value ->
                viewModel.updateLinkUrl(link.id, value) { success ->
                    scope.launch { snackbar.showSnackbar(if (success) "Link updated" else "Enter a valid HTTP or HTTPS link") }
                }
                editLink = null
            },
        )
    }
    pendingPermanentDelete?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("Delete permanently?") },
            text = { Text("This link will be removed immediately and cannot be restored.") },
            confirmButton = { TextButton(onClick = { viewModel.permanentlyDelete(link.id); pendingPermanentDelete = null }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingPermanentDelete = null }) { Text("Cancel") } },
        )
    }
    if (pendingPermanentSelection.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingPermanentSelection = emptySet() },
            title = { Text("Delete ${pendingPermanentSelection.size} links permanently?") },
            text = { Text("These links cannot be restored.") },
            confirmButton = { TextButton(onClick = { viewModel.permanentlyDeleteSelected(pendingPermanentSelection); pendingPermanentSelection = emptySet() }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingPermanentSelection = emptySet() }) { Text("Cancel") } },
        )
    }
    pendingOpen?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingOpen = null },
            icon = { Icon(EasyLinkIcons.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Potentially unsafe link") },
            text = { Text("EasyLink found characteristics commonly used by misleading links. This is a local heuristic, not a guarantee.\n\n${link.url}") },
            confirmButton = { TextButton(onClick = { openUrl(context, link.url); pendingOpen = null }) { Text("Open anyway") } },
            dismissButton = { TextButton(onClick = { pendingOpen = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InboxShortcutBar(
    shortcuts: List<InboxShortcut>,
    query: com.iv.easylink.data.repository.LinkQuery,
    collections: List<CollectionSummary>,
    onSelect: (InboxShortcut) -> Unit,
    onCustomize: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = EasyLinkDimensions.screenPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(shortcuts, key = InboxShortcut::storageKey) { shortcut ->
            val selected = shortcut.matches(query)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.combinedClickable(onClick = { onSelect(shortcut) }, onLongClick = onCustomize),
            ) {
                Text(
                    text = shortcut.label(collections),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else EasyLinkTheme.extraColors.mutedText,
                )
            }
        }
    }
}

private fun InboxShortcut.label(collections: List<CollectionSummary>): String = when (type) {
    InboxShortcutType.ALL -> "All"
    InboxShortcutType.RECENT -> "Recent"
    InboxShortcutType.FAVORITES -> "Favorites"
    InboxShortcutType.READ_LATER -> "Read later"
    InboxShortcutType.CATEGORY -> LinkCategory.fromCode(targetId.toInt()).label
    InboxShortcutType.COLLECTION -> collections.firstOrNull { it.id == targetId }?.name ?: "Collection"
}

private fun InboxShortcut.matches(query: com.iv.easylink.data.repository.LinkQuery): Boolean = when (type) {
    InboxShortcutType.ALL -> query.category < 0 && query.collectionId == 0L && !query.favoriteOnly && !query.readLaterOnly && query.dateRangeDays == 0
    InboxShortcutType.RECENT -> query.dateRangeDays == 1
    InboxShortcutType.FAVORITES -> query.favoriteOnly
    InboxShortcutType.READ_LATER -> query.readLaterOnly
    InboxShortcutType.CATEGORY -> query.category == targetId.toInt()
    InboxShortcutType.COLLECTION -> query.collectionId == targetId
}

@Composable
private fun SelectionBar(
    count: Int,
    location: LinkLocation,
    collections: List<CollectionSummary>,
    onClose: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onPermanentDelete: () -> Unit,
    onMove: (Long) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(EasyLinkIcons.Close, "Cancel selection") }
        Text("$count selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (location == LinkLocation.ACTIVE && collections.isNotEmpty()) Box {
            IconButton(onClick = { menu = true }) { Icon(EasyLinkIcons.Collection, "Move to collection") }
            DropdownMenu(menu, { menu = false }) {
                collections.forEach { item -> DropdownMenuItem({ Text(item.name) }, { onMove(item.id); menu = false }) }
            }
        }
        when (location) {
            LinkLocation.ACTIVE -> {
                IconButton(onClick = onArchive) { Icon(EasyLinkIcons.Archive, "Archive selected") }
                IconButton(onClick = onDelete) { Icon(EasyLinkIcons.Bin, "Move selected to Bin", tint = MaterialTheme.colorScheme.error) }
            }
            LinkLocation.ARCHIVED -> {
                IconButton(onClick = onUnarchive) { Icon(EasyLinkIcons.Unarchive, "Unarchive selected") }
                IconButton(onClick = onDelete) { Icon(EasyLinkIcons.Bin, "Move selected to Bin", tint = MaterialTheme.colorScheme.error) }
            }
            LinkLocation.BIN -> {
                IconButton(onClick = onRestore) { Icon(EasyLinkIcons.Restore, "Restore selected") }
                IconButton(onClick = onPermanentDelete) { Icon(EasyLinkIcons.Bin, "Delete selected permanently", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun EmptyInbox(location: LinkLocation, filtered: Boolean, onReset: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (filtered) EasyLinkIcons.FilterOff else EasyLinkIcons.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            val title = when {
                filtered -> "No matches found"
                location == LinkLocation.ARCHIVED -> "No archived links"
                location == LinkLocation.BIN -> "Bin is empty"
                else -> "Inbox is clear"
            }
            val body = when {
                filtered -> "Try adjusting your search terms or clearing active filters."
                location == LinkLocation.ARCHIVED -> "Links archived from your inbox will appear here."
                location == LinkLocation.BIN -> "Deleted links stay in Bin for 30 days before being purged."
                else -> "Links captured from notifications or shared to EasyLink will arrive here silently."
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = EasyLinkTheme.extraColors.mutedText,
                textAlign = TextAlign.Center,
            )
            if (filtered) {
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = onReset) { Text("Reset filters") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkSwipeCard(
    link: LinkEntity,
    selected: Boolean,
    selectionMode: Boolean,
    location: LinkLocation,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onMetadata: () -> Unit,
    onFavorite: () -> Unit,
    swipeEnabled: Boolean,
) {
    if (!swipeEnabled) {
        CompactLinkRow(
            link = link,
            selected = selected,
            selectionMode = selectionMode,
            onSelect = onSelect,
            onClick = onClick,
            onMetadata = onMetadata,
            onFavorite = onFavorite,
        )
        return
    }
    val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        when {
            value == SwipeToDismissBoxValue.StartToEnd && location == LinkLocation.ACTIVE -> { onArchive(); true }
            value == SwipeToDismissBoxValue.StartToEnd && location == LinkLocation.ARCHIVED -> { onUnarchive(); true }
            value == SwipeToDismissBoxValue.EndToStart && location != LinkLocation.BIN -> { onDelete(); true }
            else -> false
        }
    })
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = swipeEnabled && location != LinkLocation.BIN,
        enableDismissFromEndToStart = swipeEnabled && location != LinkLocation.BIN,
        backgroundContent = {
            val movingToBin = dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier.fillMaxSize().background(if (movingToBin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large).padding(20.dp),
                contentAlignment = if (movingToBin) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Icon(
                    if (movingToBin) EasyLinkIcons.Bin else if (location == LinkLocation.ARCHIVED) EasyLinkIcons.Unarchive else EasyLinkIcons.Archive,
                    if (movingToBin) "Move to Bin" else if (location == LinkLocation.ARCHIVED) "Unarchive" else "Archive",
                    tint = if (movingToBin) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) {
        CompactLinkRow(
            link = link,
            selected = selected,
            selectionMode = selectionMode,
            onSelect = onSelect,
            onClick = onClick,
            onMetadata = onMetadata,
            onFavorite = onFavorite,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkActionSheet(
    link: LinkEntity,
    location: LinkLocation,
    collections: List<CollectionSummary>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onReadLater: () -> Unit,
    onCopyOriginal: () -> Unit,
    onCollection: (Long) -> Unit,
    onCategory: (Int) -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
) {
    var collectionMenu by remember(link.id) { mutableStateOf(false) }
    var categoryMenu by remember(link.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val category = LinkCategory.fromCode(link.category)
                val color = EasyLinkTheme.extraColors.categoryColors.getOrNull(category.code) ?: MaterialTheme.colorScheme.primary
                Surface(Modifier.size(40.dp), shape = MaterialTheme.shapes.medium, color = color.copy(alpha = .14f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(EasyLinkIcons.category(category), null, tint = color, modifier = Modifier.size(21.dp)) }
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(link.title ?: link.host, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(link.sourceApp.takeIf(String::isNotBlank), link.senderName?.takeIf(String::isNotBlank)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = EasyLinkTheme.extraColors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactAction(EasyLinkIcons.Open, "Open", onOpen, Modifier.weight(1f))
                CompactAction(EasyLinkIcons.Copy, "Copy", onCopy, Modifier.weight(1f))
                CompactAction(EasyLinkIcons.Share, "Share", onShare, Modifier.weight(1f))
                CompactAction(EasyLinkIcons.Edit, "Edit", onEdit, Modifier.weight(1f))
            }
            if (link.originalUrl != null && link.originalUrl != link.url) {
                UrlValueRow("Original URL", link.originalUrl, onCopyOriginal)
                UrlValueRow("Cleaned URL", link.url, onCopy)
            } else {
                UrlValueRow("Saved URL", link.url, onCopy)
            }
            if (location != LinkLocation.BIN) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactSecondaryAction(
                        EasyLinkIcons.ReadLater,
                        if (link.isReadLater) "Remove Read later" else "Read later",
                        onReadLater,
                        Modifier.weight(1f),
                    )
                    CompactSecondaryAction(
                        if (link.isFavorite) EasyLinkIcons.Favorite else EasyLinkIcons.FavoriteBorder,
                        if (link.isFavorite) "Remove favorite" else "Favorite",
                        onFavorite,
                        Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f)) {
                        CompactSecondaryAction(EasyLinkIcons.Collection, "Add to collection", { collectionMenu = true }, Modifier.fillMaxWidth())
                        DropdownMenu(collectionMenu, { collectionMenu = false }) {
                            if (collections.isEmpty()) {
                                DropdownMenuItem({ Text("No collections yet") }, {}, enabled = false)
                            } else {
                                collections.forEach { item -> DropdownMenuItem({ Text(item.name) }, { collectionMenu = false; onCollection(item.id) }) }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        CompactSecondaryAction(EasyLinkIcons.Category, "Change category", { categoryMenu = true }, Modifier.fillMaxWidth())
                        DropdownMenu(categoryMenu, { categoryMenu = false }) {
                            LinkCategory.displayEntries.forEach { item -> DropdownMenuItem({ Text(item.label) }, { categoryMenu = false; onCategory(item.code) }) }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
            when (location) {
                LinkLocation.ACTIVE -> {
                    Row(Modifier.fillMaxWidth()) {
                        CompactSecondaryAction(EasyLinkIcons.Archive, "Archive", onArchive, Modifier.weight(1f))
                        CompactSecondaryAction(EasyLinkIcons.Bin, "Move to Bin", onDelete, Modifier.weight(1f), destructive = true)
                    }
                }
                LinkLocation.ARCHIVED -> {
                    Row(Modifier.fillMaxWidth()) {
                        CompactSecondaryAction(EasyLinkIcons.Unarchive, "Unarchive", onUnarchive, Modifier.weight(1f))
                        CompactSecondaryAction(EasyLinkIcons.Bin, "Move to Bin", onDelete, Modifier.weight(1f), destructive = true)
                    }
                }
                LinkLocation.BIN -> {
                    Row(Modifier.fillMaxWidth()) {
                        CompactSecondaryAction(EasyLinkIcons.Restore, "Restore", onRestore, Modifier.weight(1f))
                        CompactSecondaryAction(EasyLinkIcons.Bin, "Delete permanently", onPermanentDelete, Modifier.weight(1f), destructive = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = EasyLinkTheme.extraColors.softSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CompactSecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(19.dp), tint = color)
        Text(label, Modifier.padding(start = 8.dp), color = color, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Composable
private fun UrlValueRow(label: String, url: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = EasyLinkTheme.extraColors.mutedText)
            SelectionContainer {
                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(40.dp)) { Icon(EasyLinkIcons.Copy, "Copy $label", Modifier.size(18.dp)) }
    }
}

@Composable
internal fun EditLinkDialog(
    link: LinkEntity,
    preview: (String) -> com.iv.easylink.utils.NormalizedLink?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(link.id) { mutableStateOf(link.originalUrl ?: link.url) }
    val processed = remember(value) { preview(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit saved link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(4_096) },
                    label = { Text("URL") },
                    minLines = 2,
                    maxLines = 4,
                )
                when {
                    value.isNotBlank() && processed == null -> Text("Enter a valid HTTP or HTTPS link.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    processed?.originalUrl != null -> {
                        Text("EasyLink will keep both values:", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
                        SelectionContainer { Text("Original: ${processed.originalUrl}", style = MaterialTheme.typography.bodySmall) }
                        SelectionContainer { Text("Cleaned: ${processed.url}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }, enabled = processed != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}


@Composable
private fun AddLinkDialog(
    collections: List<CollectionSummary>,
    onDismiss: () -> Unit,
    onAdd: (String, Long?) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf<CollectionSummary?>(null) }
    var collectionMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Add a link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value, { value = it.take(2_000) }, label = { Text("Paste URL or text") }, minLines = 3)
                Box {
                    TextButton(onClick = { collectionMenu = true }) {
                        Icon(EasyLinkIcons.Collection, null, Modifier.size(18.dp))
                        Text("  ${selectedCollection?.name ?: "Inbox"}")
                    }
                    DropdownMenu(collectionMenu, { collectionMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Inbox") },
                            onClick = { selectedCollection = null; collectionMenu = false },
                        )
                        collections.forEach { collection ->
                            DropdownMenuItem(
                                text = { Text(collection.name) },
                                onClick = { selectedCollection = collection; collectionMenu = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(value, selectedCollection?.id) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun ClearHistoryDialog(category: Int, onDismiss: () -> Unit, onAll: () -> Unit, onCategory: (Int) -> Unit, onOlder: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Clear link history") },
        text = {
            Column {
                Text("Removed links can be restored from Bin for 30 days.")
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { onOlder(90); onDismiss() }) { Text("Move links older than 90 days to Bin") }
                if (category >= 0) TextButton(onClick = { onCategory(category); onDismiss() }) { Text("Move ${LinkCategory.fromCode(category).label} to Bin") }
                TextButton(onClick = { onAll(); onDismiss() }) { Text("Move everything to Bin", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private fun dateLabel(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter)

internal fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        .onFailure { Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show() }
}
internal fun copyUrl(context: Context, url: String) {
    try {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("EasyLink URL", url))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Link could not be copied", Toast.LENGTH_SHORT).show()
    }
}
internal fun shareUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }, "Share link"))
    } catch (_: Exception) {
        Toast.makeText(context, "No app can share this link", Toast.LENGTH_SHORT).show()
    }
}
