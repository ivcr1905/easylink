package com.iv.easylink.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkTheme

@Composable
fun LibraryScreen(
    viewModel: EasyLinkViewModel,
    onOpenCategory: (Int) -> Unit,
    onOpenCollection: (Long, String) -> Unit,
) {
    val collections by viewModel.collections.collectAsState()
    val rules by viewModel.rules.collectAsState()
    var addCollection by remember { mutableStateOf(false) }
    var addRule by remember { mutableStateOf(false) }
    var renameCollection by remember { mutableStateOf<CollectionSummary?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("Categories", "Automatic, on-device organization") }
        item { CategoryGrid(onOpenCategory) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Collections", "Your own reading lists", Modifier.weight(1f))
                FilledTonalButton(onClick = { addCollection = true }) { Icon(Icons.Default.Add, null); Text(" New") }
            }
        }
        if (collections.isEmpty()) item { EmptyLibraryCard("No collections", "Create Watch Later, Wishlist, Research, or anything else.") }
        // Collection and rule IDs come from separate tables and can be equal. LazyColumn keys cannot.
        items(collections, key = { "collection:${it.id}" }) { collection ->
            Card(
                Modifier.fillMaxWidth().clickable { onOpenCollection(collection.id, collection.name) },
                colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = categoryColor(collection.colorToken))
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Text(collection.name, style = MaterialTheme.typography.titleMedium)
                        Text("${collection.linkCount} link${if (collection.linkCount == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
                    }
                    IconButton(onClick = { renameCollection = collection }) { Icon(Icons.Default.Edit, "Rename collection") }
                    IconButton(onClick = { viewModel.deleteCollection(collection.id) }) { Icon(Icons.Default.DeleteOutline, "Delete collection") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Automation", "Route new links by domain", Modifier.weight(1f))
                FilledTonalButton(onClick = { addRule = true }, enabled = collections.isNotEmpty()) { Icon(Icons.Default.AutoAwesome, null); Text(" Rule") }
            }
        }
        if (collections.isEmpty()) item { Text("Create a collection before adding an automation rule.", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }
        if (rules.isEmpty() && collections.isNotEmpty()) item { EmptyLibraryCard("No rules yet", "Example: youtube.com → Watch Later") }
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
    if (addRule) RuleDialog(collections, null, onDismiss = { addRule = false }) { name, host, category, collection ->
        viewModel.createRule(name, host, category, collection); addRule = false
    }
}

@Composable
private fun CategoryGrid(onOpenCategory: (Int) -> Unit) {
    val categories = LinkCategory.entries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (rowStart in categories.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CategoryCard(categories[rowStart], Modifier.weight(1f), onOpenCategory)
                val second = rowStart + 1
                if (second < categories.size) {
                    CategoryCard(categories[second], Modifier.weight(1f), onOpenCategory)
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
    modifier: Modifier,
    onOpenCategory: (Int) -> Unit,
) {
    Card(
        modifier = modifier.clickable { onOpenCategory(category.code) },
        colors = CardDefaults.cardColors(
            containerColor = categoryColor(category.code).copy(alpha = .16f),
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(category.label, style = MaterialTheme.typography.titleMedium)
            Text("View links", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
        }
    }
}

@Composable
private fun categoryColor(token: Int) =
    EasyLinkTheme.extraColors.categoryColors.getOrNull(token) ?: MaterialTheme.colorScheme.primary

@Composable
private fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
    }
}

@Composable
private fun EmptyLibraryCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.softSurface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(body, color = EasyLinkTheme.extraColors.mutedText) }
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
    Card(colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text("${rule.hostPattern} → $target", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
            }
            Switch(rule.enabled, { viewModel.toggleRule(rule) })
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Rule actions") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Move up") }, { viewModel.moveRule(rule, -1); menu = false }, enabled = canMoveUp, leadingIcon = { Icon(Icons.Default.ArrowUpward, null) })
                    DropdownMenuItem({ Text("Move down") }, { viewModel.moveRule(rule, 1); menu = false }, enabled = canMoveDown, leadingIcon = { Icon(Icons.Default.ArrowDownward, null) })
                    DropdownMenuItem({ Text("Edit") }, { edit = true; menu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem({ Text("Delete", color = MaterialTheme.colorScheme.error) }, { viewModel.deleteRule(rule.id); menu = false }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) })
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
                        LinkCategory.entries.forEach { item -> DropdownMenuItem({ Text(item.label) }, { selectedCategory = item; categoryMenu = false }) }
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
    val context = LocalContext.current
    if (links.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This collection is empty", color = EasyLinkTheme.extraColors.mutedText) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(links, key = { it.id }) { link ->
                Card(Modifier.fillMaxWidth().clickable { openUrl(context, link.url) }, colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(link.title ?: link.host, style = MaterialTheme.typography.titleMedium)
                            Text(link.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
                        }
                    }
                }
            }
        }
    }
}
