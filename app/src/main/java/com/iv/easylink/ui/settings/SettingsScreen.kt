package com.iv.easylink.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.iv.easylink.AppLinks
import com.iv.easylink.BuildConfig
import com.iv.easylink.data.settings.AppearanceMode
import com.iv.easylink.data.settings.InboxShortcut
import com.iv.easylink.data.settings.InboxShortcutType
import com.iv.easylink.data.settings.MuteBehavior
import com.iv.easylink.data.settings.MutedSenderRule
import com.iv.easylink.data.settings.SwipeMode
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.data.local.CollectionSummary
import com.iv.easylink.ui.components.EasyLinkIcons
import com.iv.easylink.ui.theme.EasyLinkDimensions
import com.iv.easylink.ui.theme.EasyLinkTheme

@Composable
fun SettingsScreen(viewModel: EasyLinkViewModel) {
    val settings by viewModel.settings.collectAsState()
    val sources by viewModel.sourceApps.collectAsState()
    val context = LocalContext.current
    var retentionMenu by remember { mutableStateOf(false) }
    var sourceDialog by remember { mutableStateOf(false) }
    var mutedSendersDialog by remember { mutableStateOf(false) }
    var muteBehaviorMenu by remember { mutableStateOf(false) }
    var clearHistoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = EasyLinkDimensions.screenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(EasyLinkDimensions.sectionSpacing),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Notifications")
                SettingsGroup {
                    Box {
                        SettingsRow(EasyLinkIcons.Silence, "Mute button behavior", muteBehaviorLabel(settings.muteBehavior)) { muteBehaviorMenu = true }
                        DropdownMenu(muteBehaviorMenu, { muteBehaviorMenu = false }) {
                            MuteBehavior.entries.forEach { behavior -> DropdownMenuItem({ Text(muteBehaviorLabel(behavior)) }, { viewModel.setMuteBehavior(behavior); muteBehaviorMenu = false }) }
                        }
                    }
                    if (settings.muteBehavior == MuteBehavior.IGNORE_FUTURE_LINKS) {
                        HorizontalDivider()
                        ToggleRow(
                            EasyLinkIcons.CleanUp,
                            "Move existing links to Bin",
                            "Requires confirmation when enabled; recoverable for 30 days",
                            settings.moveExistingOnIgnore,
                            viewModel::setMoveExistingOnIgnore,
                        )
                    }
                    HorizontalDivider()
                    SettingsRow(EasyLinkIcons.Block, "Muted senders", "${settings.mutedSenders.size} exact sender rule${if (settings.mutedSenders.size == 1) "" else "s"}") { mutedSendersDialog = true }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Data & previews")
                SettingsGroup {
                    ToggleRow(
                        EasyLinkIcons.Website,
                        "Rich link previews",
                        "Fetch titles and site names from linked websites",
                        settings.richPreviews,
                        viewModel::setRichPreviews,
                    )
                    HorizontalDivider()
                    Box {
                        SettingsRow(EasyLinkIcons.Cleaning, "Automatic cleanup", retentionLabel(settings.retentionDays)) { retentionMenu = true }
                        DropdownMenu(retentionMenu, { retentionMenu = false }) {
                            listOf(0, 30, 90, 365).forEach { days ->
                                DropdownMenuItem({ Text(retentionLabel(days)) }, { viewModel.setRetentionDays(days); retentionMenu = false })
                            }
                        }
                    }
                    HorizontalDivider()
                    SettingsRow(EasyLinkIcons.Block, "Source applications", "Choose apps EasyLink should ignore") { sourceDialog = true }
                    HorizontalDivider()
                    SettingsRow(EasyLinkIcons.CleanUp, "Clear link history", "Move active links to the 30-day Bin") { clearHistoryDialog = true }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Permissions")
                SettingsGroup {
                    SettingsRow(EasyLinkIcons.Notifications, "Notification access", "Control automatic link capture") {
                        context.openSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    HorizontalDivider()
                    SettingsRow(EasyLinkIcons.Notifications, "EasyLink notifications", "Choose alerts and notification behavior") {
                        context.openSafely(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Privacy & support")
                SettingsGroup {
                    WebsiteRow("Privacy policy (placeholder)", AppLinks.PRIVACY_POLICY)
                    HorizontalDivider()
                    WebsiteRow("Terms (placeholder)", AppLinks.TERMS)
                    HorizontalDivider()
                    WebsiteRow("EasyLink website (placeholder)", AppLinks.WEBSITE)
                }
                Text("Support: ${AppLinks.SUPPORT_EMAIL}", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }
        }
        item {
            Text(
                "EasyLink ${BuildConfig.VERSION_NAME} · Local-first link manager",
                style = MaterialTheme.typography.bodySmall,
                color = EasyLinkTheme.extraColors.mutedText,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    if (sourceDialog) {
        AlertDialog(
            onDismissRequest = { sourceDialog = false },
            title = { Text("Source applications") },
            text = {
                if (sources.isEmpty()) {
                    Text("Source apps appear here after EasyLink saves a notification link.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(sources, key = { "${it.packageName}:${it.appName}" }) { source ->
                            val ignored = source.packageName in settings.ignoredPackages
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(source.appName)
                                    Text("${source.linkCount} saved", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
                                }
                                Switch(!ignored, { enabled -> viewModel.setPackageIgnored(source.packageName, !enabled) })
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("Done") } },
        )
    }
    if (mutedSendersDialog) {
        MutedSendersDialog(settings.mutedSenders.values.toList(), viewModel::unmuteSender) { mutedSendersDialog = false }
    }
    if (clearHistoryDialog) {
        ClearHistoryDialog(
            category = -1,
            onDismiss = { clearHistoryDialog = false },
            onAll = viewModel::clearAll,
            onCategory = viewModel::clearCategory,
            onOlder = viewModel::clearOlderThan,
        )
    }
}

@Composable
internal fun AppearanceSelector(selected: AppearanceMode, onSelect: (AppearanceMode) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppearanceMode.entries.forEach { mode ->
                val active = selected == mode
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelect(mode) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    contentColor = if (active) MaterialTheme.colorScheme.onSurface else EasyLinkTheme.extraColors.mutedText,
                ) {
                    Text(
                        mode.name.lowercase().replaceFirstChar(Char::uppercase),
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
        }
        Switch(checked, onChange)
    }
}

@Composable
internal fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
        }
        Icon(EasyLinkIcons.Chevron, null, tint = EasyLinkTheme.extraColors.mutedText)
    }
}

@Composable
private fun WebsiteRow(label: String, url: String) {
    val context = LocalContext.current
    SettingsRow(EasyLinkIcons.Privacy, label, url.removePrefix("https://")) {
        context.openSafely(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

private fun Context.openSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(this, "This option is not available on this device", Toast.LENGTH_SHORT).show()
    }
}

private fun retentionLabel(days: Int): String = if (days == 0) "Keep until I delete" else "Move to Bin after $days days"

internal fun swipeLabel(value: SwipeMode): String = when (value) {
    SwipeMode.LINK_ACTIONS -> "Link actions"
    SwipeMode.BROWSE_LISTS -> "Browse Inbox lists"
    SwipeMode.NO_SWIPE -> "No swipe actions"
}

private fun muteBehaviorLabel(value: MuteBehavior): String = when (value) {
    MuteBehavior.SILENCE_ALERTS -> "Silence alerts · keep saving"
    MuteBehavior.IGNORE_FUTURE_LINKS -> "Ignore future links"
    MuteBehavior.ASK_EVERY_TIME -> "Ask every time"
}

@Composable
internal fun InboxShortcutsDialog(
    shortcuts: List<InboxShortcut>,
    collections: List<CollectionSummary>,
    onToggle: (InboxShortcut, Boolean) -> Unit,
    onMove: (InboxShortcut, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidates = remember(collections, shortcuts) {
        buildList {
            add(InboxShortcut(InboxShortcutType.RECENT))
            add(InboxShortcut(InboxShortcutType.FAVORITES))
            add(InboxShortcut(InboxShortcutType.READ_LATER))
            LinkCategory.displayEntries.forEach { add(InboxShortcut(InboxShortcutType.CATEGORY, it.code.toLong())) }
            collections.forEach { add(InboxShortcut(InboxShortcutType.COLLECTION, it.id)) }
        }.filterNot(shortcuts::contains)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inbox lists") },
        text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                item { Text("Choose up to five. All stays first.", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }
                items(shortcuts, key = InboxShortcut::storageKey) { shortcut ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(shortcut.settingsLabel(collections), Modifier.weight(1f))
                        if (shortcut.type != InboxShortcutType.ALL) {
                            IconButton(onClick = { onMove(shortcut, -1) }, enabled = shortcuts.indexOf(shortcut) > 1) { Icon(EasyLinkIcons.MoveUp, "Move up") }
                            IconButton(onClick = { onMove(shortcut, 1) }, enabled = shortcuts.indexOf(shortcut) < shortcuts.lastIndex) { Icon(EasyLinkIcons.MoveDown, "Move down") }
                            IconButton(onClick = { onToggle(shortcut, false) }) { Icon(EasyLinkIcons.Close, "Remove") }
                        }
                    }
                }
                if (shortcuts.size < 5 && candidates.isNotEmpty()) {
                    item { Text("Add", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                    items(candidates, key = InboxShortcut::storageKey) { shortcut ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggle(shortcut, true) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(EasyLinkIcons.Add, null, Modifier.padding(end = 10.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(shortcut.settingsLabel(collections), maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun MutedSendersDialog(
    rules: List<MutedSenderRule>,
    onUnmute: (MutedSenderRule) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Muted senders") },
        text = {
            if (rules.isEmpty()) {
                Text("Muted senders appear here after you use Mute on an EasyLink notification.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(rules.sortedWith(compareBy(MutedSenderRule::sourceApp, MutedSenderRule::senderName)), key = { "${it.sourcePackage}\u0000${it.senderName}" }) { rule ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(rule.senderName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${rule.sourceApp.ifBlank { rule.sourcePackage }} · ${if (rule.behavior == com.iv.easylink.data.settings.SenderRuleBehavior.SILENCE_ALERTS) "Alerts muted" else "Links ignored"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EasyLinkTheme.extraColors.mutedText,
                                )
                            }
                            TextButton(onClick = { onUnmute(rule) }) { Text("Unmute") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun InboxShortcut.settingsLabel(collections: List<CollectionSummary>): String = when (type) {
    InboxShortcutType.ALL -> "All"
    InboxShortcutType.RECENT -> "Recent · past 24 hours"
    InboxShortcutType.FAVORITES -> "Favorites"
    InboxShortcutType.READ_LATER -> "Read later"
    InboxShortcutType.CATEGORY -> "Category · ${LinkCategory.fromCode(targetId.toInt()).label}"
    InboxShortcutType.COLLECTION -> "Collection · ${collections.firstOrNull { it.id == targetId }?.name ?: "Unavailable"}"
}
