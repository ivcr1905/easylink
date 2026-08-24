package com.iv.easylink.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.iv.easylink.AppLinks
import com.iv.easylink.BuildConfig
import com.iv.easylink.data.settings.AppearanceMode
import com.iv.easylink.ui.theme.EasyLinkTheme

@Composable
fun SettingsScreen(viewModel: EasyLinkViewModel) {
    val settings by viewModel.settings.collectAsState()
    val sources by viewModel.sourceApps.collectAsState()
    val context = LocalContext.current
    var retentionMenu by remember { mutableStateOf(false) }
    var sourceDialog by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Private by default. Adjustable by you.", style = MaterialTheme.typography.bodyLarge, color = EasyLinkTheme.extraColors.mutedText, modifier = Modifier.padding(vertical = 8.dp))
        }
        item {
            SettingsCard("Appearance", Icons.Outlined.DarkMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppearanceMode.entries.forEach { mode -> FilterChip(settings.appearanceMode == mode, { viewModel.setAppearance(mode) }, { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }) }
                }
            }
        }
        item {
            SettingsCard("Data and previews", Icons.Outlined.Storage) {
                SettingToggle("Rich link previews", "Contacts linked websites directly to fetch titles and images.", settings.richPreviews, viewModel::setRichPreviews)
                Row(Modifier.fillMaxWidth().clickable { retentionMenu = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Automatic cleanup"); Text(retentionLabel(settings.retentionDays), style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }
                    Icon(Icons.Default.ChevronRight, null)
                    DropdownMenu(retentionMenu, { retentionMenu = false }) {
                        listOf(0, 30, 90, 365).forEach { days -> DropdownMenuItem({ Text(retentionLabel(days)) }, { viewModel.setRetentionDays(days); retentionMenu = false }) }
                    }
                }
                Row(Modifier.fillMaxWidth().clickable { sourceDialog = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Source applications"); Text("Choose apps EasyLink should ignore", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        item {
            SettingsCard("Permissions", Icons.Default.Notifications) {
                Text("EasyLink only works while Android notification access is enabled.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Manage notification access") }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("Manage EasyLink notifications") }
            }
        }
        item {
            SettingsCard("Privacy and support", Icons.Outlined.PrivacyTip) {
                WebsiteRow("Privacy policy (placeholder)", AppLinks.PRIVACY_POLICY)
                WebsiteRow("Terms (placeholder)", AppLinks.TERMS)
                WebsiteRow("EasyLink website (placeholder)", AppLinks.WEBSITE)
                Text("Support: ${AppLinks.SUPPORT_EMAIL}", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item { Text("EasyLink ${BuildConfig.VERSION_NAME} • Local-first link manager", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, modifier = Modifier.padding(16.dp)) }
    }
    if (sourceDialog) {
        AlertDialog(
            onDismissRequest = { sourceDialog = false }, title = { Text("Source applications") },
            text = {
                Column {
                    if (sources.isEmpty()) Text("Source apps appear here after EasyLink saves a notification link.")
                    sources.forEach { source ->
                        val ignored = source.packageName in settings.ignoredPackages
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(source.appName); Text("${source.linkCount} saved", style = MaterialTheme.typography.bodySmall) }
                            Switch(!ignored, { enabled -> viewModel.setPackageIgnored(source.packageName, !enabled) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sourceDialog = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleLarge) }
            content()
        }
    }
}

@Composable
private fun SettingToggle(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(body, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText) }
        Switch(checked, onChange)
    }
}

@Composable
private fun WebsiteRow(label: String, url: String) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, Modifier.padding(start = 10.dp).weight(1f))
        Icon(Icons.Default.ChevronRight, null)
    }
}

private fun retentionLabel(days: Int): String = if (days == 0) "Keep until I delete" else "Delete after $days days"
