package com.iv.easylink.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.iv.easylink.EasyLinkApplication
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(viewModel: EasyLinkViewModel) {
    val links by viewModel.links.collectAsState()
    val query by viewModel.query.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val sourceApps by viewModel.sourceApps.collectAsState()
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showAdd by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var pendingOpen by remember { mutableStateOf<LinkEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add link") } },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selected.isNotEmpty()) {
                SelectionBar(selected.size, collections, onClose = { selected = emptySet() }, onDelete = {
                    viewModel.deleteSelected(selected); selected = emptySet()
                }, onMove = { viewModel.addSelectedToCollection(selected, it); selected = emptySet() })
            } else {
                SearchAndClear(query.text, viewModel::setSearch, links.isNotEmpty(), onClear = { showClear = true })
                LinkFilters(query, sourceApps, viewModel)
            }
            if (links.isEmpty()) {
                EmptyInbox(filtered = query != com.iv.easylink.data.repository.LinkQuery(), onReset = viewModel::resetFilters)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(links, key = { _, link -> link.id }) { index, link ->
                        val label = dateLabel(link.timestamp)
                        val previousTimestamp = links.getOrNull(index - 1)?.timestamp
                        if (previousTimestamp == null || dateLabel(previousTimestamp) != label) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        }
                        LinkSwipeCard(
                            link = link,
                            selected = link.id in selected,
                            collections = collections,
                            onSelect = { selected = if (link.id in selected) selected - link.id else selected + link.id },
                            onOpen = { if (link.isSuspicious) pendingOpen = link else openUrl(context, link.url) },
                            onFavorite = { viewModel.toggleFavorite(link) },
                            onDelete = {
                                viewModel.delete(link)
                                scope.launch {
                                    if (snackbar.showSnackbar("Link deleted", "Undo") == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete(link.id)
                                    } else {
                                        viewModel.discardUndo(link.id)
                                    }
                                }
                            },
                            onCollection = { viewModel.addToCollection(link.id, it) },
                            onCategory = { viewModel.setLinkCategory(link.id, it) },
                            onMetadata = { viewModel.requestMetadata(link) },
                        )
                    }
                    if (links.size >= query.limit && query.limit < 500) {
                        item { TextButton(viewModel::loadMore, Modifier.fillMaxWidth()) { Text("Load more") } }
                    }
                }
            }
        }
    }

    if (showAdd) AddLinkDialog(onDismiss = { showAdd = false }) { value ->
        viewModel.addManual(value) { count ->
            scope.launch { snackbar.showSnackbar(if (count > 0) "$count link${if (count == 1) "" else "s"} saved" else "No new link found") }
        }
        showAdd = false
    }
    if (showClear) ClearHistoryDialog(query.category, onDismiss = { showClear = false }, onAll = viewModel::clearAll, onCategory = viewModel::clearCategory, onOlder = viewModel::clearOlderThan)
    pendingOpen?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingOpen = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Potentially unsafe link") },
            text = { Text("EasyLink found characteristics commonly used by misleading links. This is a local heuristic, not a guarantee.\n\n${link.url}") },
            confirmButton = { TextButton(onClick = { openUrl(context, link.url); pendingOpen = null }) { Text("Open anyway") } },
            dismissButton = { TextButton(onClick = { pendingOpen = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SearchAndClear(value: String, onValue: (String) -> Unit, hasLinks: Boolean, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value, onValueChange = onValue, singleLine = true,
            placeholder = { Text("Search links") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onValue("") }) { Icon(Icons.Default.Close, "Clear search") } },
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
        )
        if (hasLinks) IconButton(onClick = onClear) { Icon(Icons.Default.DeleteSweep, "Clear history") }
    }
}

@Composable
private fun LinkFilters(
    query: com.iv.easylink.data.repository.LinkQuery,
    sources: List<com.iv.easylink.data.local.SourceAppSummary>,
    viewModel: EasyLinkViewModel,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(query.category < 0, { viewModel.setCategory(-1) }, { Text("All") }) }
        items(LinkCategory.entries, key = { it.code }) { item ->
            FilterChip(query.category == item.code, { viewModel.setCategory(item.code) }, { Text(item.label) })
        }
        item { FilterChip(query.favoriteOnly, viewModel::toggleFavorites, { Text("Favorites") }, leadingIcon = { Icon(Icons.Default.Favorite, null, Modifier.size(16.dp)) }) }
        item { FilterChip(query.suspiciousOnly, viewModel::toggleSuspicious, { Text("Warnings") }, leadingIcon = { Icon(Icons.Default.Warning, null, Modifier.size(16.dp)) }) }
        item { FilterChip(query.dateRangeDays == 1, { viewModel.setDateRange(if (query.dateRangeDays == 1) 0 else 1) }, { Text("24 hours") }) }
        item { FilterChip(query.dateRangeDays == 7, { viewModel.setDateRange(if (query.dateRangeDays == 7) 0 else 7) }, { Text("7 days") }) }
        item { FilterChip(query.oldestFirst, viewModel::toggleSort, { Text(if (query.oldestFirst) "Oldest first" else "Newest first") }) }
        sources.forEach { source ->
            item(key = "source-${source.packageName}") {
                FilterChip(query.sourcePackage == source.packageName, { viewModel.setSource(if (query.sourcePackage == source.packageName) "" else source.packageName) }, { Text(source.appName) })
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, collections: List<CollectionSummary>, onClose: () -> Unit, onDelete: () -> Unit, onMove: (Long) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Cancel selection") }
        Text("$count selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (collections.isNotEmpty()) Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.Folder, "Move to collection") }
            DropdownMenu(menu, { menu = false }) {
                collections.forEach { item -> DropdownMenuItem({ Text(item.name) }, { onMove(item.id); menu = false }) }
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete selected", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EmptyInbox(filtered: Boolean, onReset: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (filtered) Icons.Default.FilterAltOff else Icons.Default.Inbox, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = .5f))
            Spacer(Modifier.height(16.dp))
            Text(if (filtered) "Nothing matches" else "Your link inbox is ready", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(if (filtered) "Try another search or clear the filters." else "Share a link to EasyLink or receive one in a notification.", color = EasyLinkTheme.extraColors.mutedText)
            if (filtered) TextButton(onClick = onReset) { Text("Reset filters") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkSwipeCard(
    link: LinkEntity,
    selected: Boolean,
    collections: List<CollectionSummary>,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCollection: (Long) -> Unit,
    onCategory: (Int) -> Unit,
    onMetadata: () -> Unit,
) {
    val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
    })
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error, MaterialTheme.shapes.large).padding(20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        LinkCard(link, selected, collections, onSelect, onOpen, onFavorite, onDelete, onCollection, onCategory, onMetadata)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkCard(
    link: LinkEntity,
    selected: Boolean,
    collections: List<CollectionSummary>,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCollection: (Long) -> Unit,
    onCategory: (Int) -> Unit,
    onMetadata: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var collectionMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    LaunchedEffect(link.id, link.metadataState) { onMetadata() }
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else EasyLinkTheme.extraColors.card,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).combinedClickable(onClick = { if (selected) onSelect() else onOpen() }, onLongClick = onSelect),
    ) {
        Column {
            link.imageUrl?.let { PreviewImage(it) }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    if (link.isSuspicious) Icon(Icons.Default.Warning, "Potentially unsafe", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    if (selected || link.isSuspicious) Spacer(Modifier.width(8.dp))
                    Text(link.title ?: link.host, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Link actions") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Open") }, { onOpen(); menu = false }, leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) })
                            DropdownMenuItem({ Text("Copy") }, { copyUrl(context, link.url); menu = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                            DropdownMenuItem({ Text("Share") }, { shareUrl(context, link.url); menu = false }, leadingIcon = { Icon(Icons.Default.Share, null) })
                            DropdownMenuItem({ Text(if (link.isFavorite) "Remove favorite" else "Favorite") }, { onFavorite(); menu = false }, leadingIcon = { Icon(if (link.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) })
                            if (collections.isNotEmpty()) DropdownMenuItem({ Text("Add to collection") }, { menu = false; collectionMenu = true }, leadingIcon = { Icon(Icons.Default.Folder, null) })
                            DropdownMenuItem({ Text("Change category") }, { menu = false; categoryMenu = true })
                            HorizontalDivider()
                            DropdownMenuItem({ Text("Delete", color = MaterialTheme.colorScheme.error) }, { onDelete(); menu = false }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) })
                        }
                        DropdownMenu(collectionMenu, { collectionMenu = false }) {
                            collections.forEach { collection -> DropdownMenuItem({ Text(collection.name) }, { onCollection(collection.id); collectionMenu = false }) }
                        }
                        DropdownMenu(categoryMenu, { categoryMenu = false }) {
                            LinkCategory.entries.forEach { category -> DropdownMenuItem({ Text(category.label) }, { onCategory(category.code); categoryMenu = false }) }
                        }
                    }
                }
                Text(link.siteName ?: link.url, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = if (link.title == null) FontFamily.Monospace else FontFamily.SansSerif), color = EasyLinkTheme.extraColors.mutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(LinkCategory.fromCode(link.category).label) })
                    Text(link.sourceApp, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (link.wasCleaned) Text("Cleaned", style = MaterialTheme.typography.labelMedium, color = EasyLinkTheme.extraColors.success)
                    Text(timeLabel(link.timestamp), style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
                }
            }
        }
    }
}

@Composable
private fun PreviewImage(url: String) {
    val app = LocalContext.current.applicationContext as EasyLinkApplication
    val bitmap by produceState<android.graphics.Bitmap?>(null, url) { value = app.container.previewImageLoader.load(url) }
    bitmap?.let {
        Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(2.1f), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun AddLinkDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Add a link") },
        text = { OutlinedTextField(value, { value = it.take(2_000) }, label = { Text("Paste URL or text") }, minLines = 3) },
        confirmButton = { Button(onClick = { onAdd(value) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ClearHistoryDialog(category: Int, onDismiss: () -> Unit, onAll: () -> Unit, onCategory: (Int) -> Unit, onOlder: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Clear link history") },
        text = {
            Column {
                Text("This cannot be undone.")
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { onOlder(90); onDismiss() }) { Text("Delete links older than 90 days") }
                if (category >= 0) TextButton(onClick = { onCategory(category); onDismiss() }) { Text("Clear ${LinkCategory.fromCode(category).label}") }
                TextButton(onClick = { onAll(); onDismiss() }) { Text("Clear everything", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private fun dateLabel(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter)
private fun timeLabel(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(timeFormatter)

internal fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        .onFailure { Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show() }
}
private fun copyUrl(context: Context, url: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("EasyLink URL", url))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
}
private fun shareUrl(context: Context, url: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }, "Share link"))
}
