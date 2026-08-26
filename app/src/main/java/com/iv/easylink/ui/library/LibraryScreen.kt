package com.iv.easylink.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.data.repository.LinkLocation
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkTheme
import com.iv.easylink.ui.theme.EasyLinkDimensions
import com.iv.easylink.ui.components.CompactLinkRow
import com.iv.easylink.ui.components.EasyLinkIcons
import com.iv.easylink.data.settings.InboxShortcut
import com.iv.easylink.data.settings.InboxShortcutType
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.widget.Toast

@Composable
fun LibraryScreen(
    viewModel: EasyLinkViewModel,
    onOpenCategory: (Int) -> Unit,
    onOpenCollection: (Long, String) -> Unit,
) {
    val collections by viewModel.collections.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val categoryCounts by viewModel.libraryCategoryCounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val counts = remember(categoryCounts) { categoryCounts.associate { (key, count) -> (key.toIntOrNull() ?: 0) to count } }
    var addCollection by remember { mutableStateOf(false) }
    var addRule by remember { mutableStateOf(false) }
    var renameCollection by remember { mutableStateOf<CollectionSummary?>(null) }
    var deleteCollection by remember { mutableStateOf<CollectionSummary?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = EasyLinkDimensions.screenPadding,
            top = 16.dp,
            end = EasyLinkDimensions.screenPadding,
            bottom = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(EasyLinkDimensions.itemSpacing),
    ) {
        item {
            SectionTitle(
                "Categories",
                "Automatic, on-device organization",
                Modifier.padding(bottom = 4.dp),
            )
        }
        item { CategoryGrid(counts, settings.inboxShortcuts, onOpenCategory, viewModel::setShortcutVisible) }
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Collections", "Your own reading lists", Modifier.weight(1f))
                FilledTonalButton(onClick = { addCollection = true }, shape = MaterialTheme.shapes.small) { Icon(EasyLinkIcons.Add, null); Text(" New") }
            }
        }
        if (collections.isEmpty()) item { EmptyLibraryCard("No collections", "Create Reading, Wishlist, Research, or anything else.") }
        // Collection and rule IDs come from separate tables and can be equal. LazyColumn keys cannot.
        items(collections, key = { "collection:${it.id}" }) { collection ->
            val inboxShortcut = remember(collection.id) { InboxShortcut(InboxShortcutType.COLLECTION, collection.id) }
            val shownInInbox = inboxShortcut in settings.inboxShortcuts
            CollectionRow(
                collection = collection,
                onOpen = { onOpenCollection(collection.id, collection.name) },
                onPin = { viewModel.setCollectionPinned(collection.id, !collection.isPinned) },
                onRename = { renameCollection = collection },
                onDelete = { deleteCollection = collection },
                shownInInbox = shownInInbox,
                canToggleInbox = shownInInbox || settings.inboxShortcuts.size < 5,
                onToggleInbox = { visible -> viewModel.setShortcutVisible(inboxShortcut, visible) },
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Automation", "Route new links by domain", Modifier.weight(1f))
                FilledTonalButton(onClick = { addRule = true }, enabled = collections.isNotEmpty(), shape = MaterialTheme.shapes.small) { Icon(EasyLinkIcons.Automation, null); Text(" Rule") }
            }
        }
        if (collections.isEmpty()) item { Text("Create a collection before adding an automation rule.", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }
        if (rules.isEmpty() && collections.isNotEmpty()) item { EmptyLibraryCard("No rules yet", "Example: youtube.com → Anime") }
        itemsIndexed(rules, key = { _, rule -> "rule:${rule.id}" }) { index, rule ->
            RuleRow(
                rule = rule,
                canMoveUp = index > 0,
                canMoveDown = index >= 0 && index < rules.lastIndex,
                collections = collections,
                viewModel = viewModel,
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
    if (addCollection) NameDialog("New collection", "Collection name", onDismiss = { addCollection = false }) {
        viewModel.createCollection(it); addCollection = false
    }
    renameCollection?.let { collection ->
        NameDialog("Rename collection", "Collection name", initial = collection.name, onDismiss = { renameCollection = null }) {
            viewModel.renameCollection(collection.id, it); renameCollection = null
        }
    }
    deleteCollection?.let { collection ->
        AlertDialog(
            onDismissRequest = { deleteCollection = null },
            title = { Text("Delete ${collection.name}?") },
            text = { Text("The collection will be removed. Its links will remain in your Inbox.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCollection(collection.id); deleteCollection = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteCollection = null }) { Text("Cancel") } },
        )
    }
    if (addRule) RuleDialog(collections, null, onDismiss = { addRule = false }) { name, host, category, collection ->
        viewModel.createRule(name, host, category, collection); addRule = false
    }
}

@Composable
private fun CategoryGrid(
    counts: Map<Int, Int>,
    shortcuts: List<InboxShortcut>,
    onOpenCategory: (Int) -> Unit,
    onShortcutVisible: (InboxShortcut, Boolean) -> Unit,
) {
    val categories = LinkCategory.displayEntries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (rowStart in categories.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val first = categories[rowStart]
                val firstShortcut = InboxShortcut(InboxShortcutType.CATEGORY, first.code.toLong())
                val firstShown = firstShortcut in shortcuts
                CategoryCard(first, counts[first.code] ?: 0, firstShown, firstShown || shortcuts.size < 5, Modifier.weight(1f), onOpenCategory, onShortcutVisible)
                val second = rowStart + 1
                if (second < categories.size) {
                    val value = categories[second]
                    val valueShortcut = InboxShortcut(InboxShortcutType.CATEGORY, value.code.toLong())
                    val valueShown = valueShortcut in shortcuts
                    CategoryCard(value, counts[value.code] ?: 0, valueShown, valueShown || shortcuts.size < 5, Modifier.weight(1f), onOpenCategory, onShortcutVisible)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: LinkCategory,
    count: Int,
    shownInInbox: Boolean,
    canToggleInbox: Boolean,
    modifier: Modifier,
    onOpenCategory: (Int) -> Unit,
    onShortcutVisible: (InboxShortcut, Boolean) -> Unit,
) {
    var menu by remember(category) { mutableStateOf(false) }
    Surface(
        modifier = modifier.clickable { onOpenCategory(category.code) },
        color = EasyLinkTheme.extraColors.softSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 11.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(EasyLinkIcons.category(category), null, Modifier.size(20.dp), tint = categoryColor(category.code))
            Text(category.label, Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = EasyLinkTheme.extraColors.mutedText)
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(EasyLinkIcons.More, "Category actions", Modifier.size(19.dp)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (!canToggleInbox) "Inbox list limit reached" else if (shownInInbox) "Remove from Inbox" else "Show in Inbox") },
                        onClick = {
                            onShortcutVisible(InboxShortcut(InboxShortcutType.CATEGORY, category.code.toLong()), !shownInInbox)
                            menu = false
                        },
                        enabled = canToggleInbox,
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryColor(token: Int) =
    EasyLinkTheme.extraColors.categoryColors.getOrNull(token) ?: MaterialTheme.colorScheme.primary

@Composable
private fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
    }
}

@Composable
private fun EmptyLibraryCard(title: String, body: String) {
    Surface(color = EasyLinkTheme.extraColors.softSurface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(body, color = EasyLinkTheme.extraColors.mutedText) }
    }
}

@Composable
private fun CollectionRow(
    collection: CollectionSummary,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    shownInInbox: Boolean,
    canToggleInbox: Boolean,
    onToggleInbox: (Boolean) -> Unit,
) {
    var menu by remember(collection.id) { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth().clickable(onClick = onOpen), color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = MaterialTheme.shapes.small, color = categoryColor(collection.colorToken).copy(alpha = .14f)) {
                Box(contentAlignment = Alignment.Center) { Icon(EasyLinkIcons.Collection, null, Modifier.size(21.dp), tint = categoryColor(collection.colorToken)) }
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(collection.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (collection.isPinned) Icon(EasyLinkIcons.Pin, "Pinned", Modifier.padding(start = 6.dp).size(15.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text("${collection.linkCount} link${if (collection.linkCount == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(EasyLinkIcons.MoreVertical, "Collection actions") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        { Text(if (!canToggleInbox) "Inbox list limit reached" else if (shownInInbox) "Remove from Inbox" else "Show in Inbox") },
                        { menu = false; onToggleInbox(!shownInInbox) },
                        enabled = canToggleInbox,
                    )
                    DropdownMenuItem({ Text(if (collection.isPinned) "Unpin" else "Pin") }, { menu = false; onPin() }, leadingIcon = { Icon(EasyLinkIcons.Pin, null) })
                    DropdownMenuItem({ Text("Rename") }, { menu = false; onRename() }, leadingIcon = { Icon(EasyLinkIcons.Edit, null) })
                    DropdownMenuItem({ Text("Delete", color = MaterialTheme.colorScheme.error) }, { menu = false; onDelete() }, leadingIcon = { Icon(EasyLinkIcons.Bin, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}

@Composable
private fun RuleRow(
    rule: AutomationRuleEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    collections: List<CollectionSummary>,
    viewModel: EasyLinkViewModel,
) {
    var edit by remember(rule.id) { mutableStateOf(false) }
    var menu by remember(rule.id) { mutableStateOf(false) }
    val target = collections.firstOrNull { it.id == rule.collectionId }?.name
        ?: rule.category?.let { LinkCategory.fromCode(it).label } ?: "No target"
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(EasyLinkIcons.Automation, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text("${rule.hostPattern} → $target", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
            }
            Switch(rule.enabled, { viewModel.toggleRule(rule) })
            Box {
                IconButton(onClick = { menu = true }) { Icon(EasyLinkIcons.MoreVertical, "Rule actions") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Move up") }, { viewModel.moveRule(rule, -1); menu = false }, enabled = canMoveUp, leadingIcon = { Icon(EasyLinkIcons.MoveUp, null) })
                    DropdownMenuItem({ Text("Move down") }, { viewModel.moveRule(rule, 1); menu = false }, enabled = canMoveDown, leadingIcon = { Icon(EasyLinkIcons.MoveDown, null) })
                    DropdownMenuItem({ Text("Edit") }, { edit = true; menu = false }, leadingIcon = { Icon(EasyLinkIcons.Edit, null) })
                    DropdownMenuItem({ Text("Delete", color = MaterialTheme.colorScheme.error) }, { viewModel.deleteRule(rule.id); menu = false }, leadingIcon = { Icon(EasyLinkIcons.Bin, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
    if (edit) RuleDialog(collections, rule, onDismiss = { edit = false }) { name, host, category, collection ->
        viewModel.editRule(rule, name, host, category, collection); edit = false
    }
}

@Composable
private fun NameDialog(title: String, label: String, initial: String = "", onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(value, { value = it.take(40) }, label = { Text(label) }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text(if (initial.isEmpty()) "Create" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RuleDialog(
    collections: List<CollectionSummary>,
    initial: AutomationRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int?, Long?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var host by remember { mutableStateOf(initial?.hostPattern.orEmpty()) }
    var selectedCollection by remember { mutableStateOf(collections.firstOrNull { it.id == initial?.collectionId } ?: collections.firstOrNull()) }
    var collectionMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(initial?.category?.let(LinkCategory::fromCode)) }
    var categoryMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New automation rule" else "Edit automation rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(50) }, label = { Text("Rule name") }, singleLine = true)
                OutlinedTextField(host, { host = it.take(253) }, label = { Text("Domain, e.g. youtube.com") }, singleLine = true)
                Box {
                    Button(onClick = { collectionMenu = true }) { Text("Collection: ${selectedCollection?.name ?: "None"}") }
                    DropdownMenu(collectionMenu, { collectionMenu = false }) {
                        collections.forEach { item -> DropdownMenuItem({ Text(item.name) }, { selectedCollection = item; collectionMenu = false }) }
                    }
                }
                Box {
                    TextButton(onClick = { categoryMenu = true }) { Text("Category override: ${selectedCategory?.label ?: "Keep automatic"}") }
                    DropdownMenu(categoryMenu, { categoryMenu = false }) {
                        DropdownMenuItem({ Text("Keep automatic") }, { selectedCategory = null; categoryMenu = false })
                        LinkCategory.displayEntries.forEach { item -> DropdownMenuItem({ Text(item.label) }, { selectedCategory = item; categoryMenu = false }) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, host, selectedCategory?.code, selectedCollection?.id) }, enabled = name.isNotBlank() && host.isNotBlank()) { Text(if (initial == null) "Create rule" else "Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun CollectionScreen(viewModel: EasyLinkViewModel, collectionId: Long) {
    val flow = remember(collectionId) { viewModel.collectionLinks(collectionId) }
    val links by flow.collectAsState(emptyList())
    val collections by viewModel.collections.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var actionLink by remember { mutableStateOf<LinkEntity?>(null) }
    var editLink by remember { mutableStateOf<LinkEntity?>(null) }
    var pendingOpen by remember { mutableStateOf<LinkEntity?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = {
                viewModel.collectionBackup(collectionId) { backup ->
                    val copied = backup != null && copyBackup(context, backup)
                    Toast.makeText(context, if (copied) "Collection copied" else "Collection is too large; share it as a file", Toast.LENGTH_SHORT).show()
                }
            }) { Icon(EasyLinkIcons.Copy, null); Text(" Copy") }
            TextButton(onClick = {
                viewModel.collectionBackup(collectionId) { backup ->
                    val shared = backup != null && shareBackupText(context, backup)
                    if (!shared) Toast.makeText(context, "Collection is too large; share it as a file", Toast.LENGTH_SHORT).show()
                }
            }) { Icon(EasyLinkIcons.Share, null); Text(" Share") }
            TextButton(onClick = {
                viewModel.collectionBackup(collectionId) { backup ->
                    backup?.let { scope.launch {
                        try {
                            shareBackupFile(context, it)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
                        }
                    } }
                }
            }) { Icon(EasyLinkIcons.File, null); Text(" File") }
        }
        if (links.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This collection is empty", color = EasyLinkTheme.extraColors.mutedText) }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(links, key = { it.id }) { link ->
                    CompactLinkRow(link = link, onClick = { actionLink = link }, onFavorite = { viewModel.toggleFavorite(link) })
                }
            }
        }
    }
    actionLink?.let { link ->
        LinkActionSheet(
            link, LinkLocation.ACTIVE, collections, onDismiss = { actionLink = null },
            onOpen = { actionLink = null; if (link.isSuspicious) pendingOpen = link else openUrl(context, link.url) },
            onCopy = { copyUrl(context, link.url); actionLink = null },
            onShare = { shareUrl(context, link.url); actionLink = null },
            onEdit = { editLink = link; actionLink = null },
            onFavorite = { viewModel.toggleFavorite(link); actionLink = null },
            onReadLater = { viewModel.toggleReadLater(link); actionLink = null },
            onCopyOriginal = { link.originalUrl?.let { copyUrl(context, it) } },
            onCollection = { viewModel.addToCollection(link.id, it); actionLink = null },
            onCategory = { viewModel.setLinkCategory(link.id, it); actionLink = null },
            onArchive = { viewModel.archive(link.id); actionLink = null },
            onUnarchive = {},
            onDelete = { viewModel.delete(link); actionLink = null },
            onRestore = {},
            onPermanentDelete = {},
        )
    }
    editLink?.let { link ->
        EditLinkDialog(
            link = link,
            preview = viewModel::previewLinkEdit,
            onDismiss = { editLink = null },
            onSave = { value ->
                viewModel.updateLinkUrl(link.id, value) { success ->
                    Toast.makeText(context, if (success) "Link updated" else "Enter a valid HTTP or HTTPS link", Toast.LENGTH_SHORT).show()
                }
                editLink = null
            },
        )
    }
    pendingOpen?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingOpen = null },
            icon = { Icon(EasyLinkIcons.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Potentially unsafe link") },
            text = { Text("EasyLink found characteristics commonly used by misleading links.\n\n${link.url}") },
            confirmButton = { TextButton(onClick = { openUrl(context, link.url); pendingOpen = null }) { Text("Open anyway") } },
            dismissButton = { TextButton(onClick = { pendingOpen = null }) { Text("Cancel") } },
        )
    }
}
