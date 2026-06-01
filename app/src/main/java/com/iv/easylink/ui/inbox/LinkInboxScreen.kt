package com.iv.easylink.ui.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Public entry point ───────────────────────────────────────────────────────

@Composable
fun LinkInboxScreen(
    viewModel: LinkInboxViewModel = viewModel(),
) {
    val links by viewModel.links.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    // Group links by display date
    val groupedLinks = remember(links) { groupLinksByDate(links) }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            InboxTopBar(
                linkCount = links.size,
                onClearAll = { showClearDialog = true },
            )
        },
    ) { padding ->
        if (links.isEmpty()) {
            EmptyInboxState(modifier = Modifier.padding(padding))
        } else {
            LinkList(
                groupedLinks = groupedLinks,
                onDelete = { viewModel.deleteLink(it) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    // ── Clear-all confirmation dialog ────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = SurfaceCard,
            titleContentColor = Ink,
            textContentColor = Body,
            title = { Text("Clear All Links?") },
            text = { Text("This will permanently delete your entire link history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllLinks()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Mulberry),
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Muted),
                ) { Text("Cancel") }
            },
        )
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxTopBar(linkCount: Int, onClearAll: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Recent Links",
                    style = MaterialTheme.typography.displayMedium,
                    color = Ink,
                )
                if (linkCount > 0) {
                    Text(
                        text = "$linkCount link${if (linkCount != 1) "s" else ""} saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        },
        actions = {
            if (linkCount > 0) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all links",
                        tint = Muted,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas),
    )
}

// ── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyInboxState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MulberryMuted,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No links yet",
                style = MaterialTheme.typography.displayMedium,
                color = Ink,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Links from your notifications will appear here automatically.",
                style = MaterialTheme.typography.bodyLarge,
                color = Body,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ── Link List with date headers ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkList(
    groupedLinks: Map<String, List<LinkEntity>>,
    onDelete: (LinkEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groupedLinks.forEach { (dateLabel, linksInGroup) ->
            // Sticky date header
            stickyHeader(key = dateLabel) {
                DateHeader(label = dateLabel)
            }
            items(
                items = linksInGroup,
                key = { it.id },
            ) { link ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                    exit = fadeOut(tween(200)),
                ) {
                    LinkCard(
                        link = link,
                        onDelete = { onDelete(link) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        // Bottom spacer so FAB doesn't overlap last item
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DateHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Canvas)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Mulberry,
        )
    }
}

// ── Link Card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkCard(
    link: LinkEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = { openUrl(context, link.url) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    },
                )
                .padding(16.dp),
        ) {
            // ── Top row: sender + source app badge ───────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (link.isSuspicious) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Suspicious link",
                        tint = AccentRose,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = link.senderName,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (link.isSuspicious) AccentRose else BodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SourceAppBadge(appName = link.sourceApp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── URL in monospace ──────────────────────────────────────────────
            Text(
                text = link.url,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.2.sp,
                ),
                color = Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Timestamp ────────────────────────────────────────────────────
            Text(
                text = formatTime(link.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }

        // ── Context menu (long-press) ────────────────────────────────────────
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = SurfaceCard,
        ) {
            DropdownMenuItem(
                text = { Text("Open", color = Ink) },
                leadingIcon = {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Mulberry)
                },
                onClick = {
                    openUrl(context, link.url)
                    showMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text("Copy", color = Ink) },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Mulberry)
                },
                onClick = {
                    copyToClipboard(context, link.url)
                    showMenu = false
                },
            )
            HorizontalDivider(color = Hairline)
            DropdownMenuItem(
                text = { Text("Delete", color = AccentRose) },
                leadingIcon = {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AccentRose)
                },
                onClick = {
                    onDelete()
                    showMenu = false
                },
            )
        }
    }
}

// ── Source App Badge ─────────────────────────────────────────────────────────

@Composable
private fun SourceAppBadge(appName: String) {
    Text(
        text = appName,
        style = MaterialTheme.typography.labelSmall,
        color = Mulberry,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MulberryMuted)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Group a flat list of links into date-labelled buckets (Today, Yesterday, or date string). */
private fun groupLinksByDate(links: List<LinkEntity>): Map<String, List<LinkEntity>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = today - 86_400_000L // 24h in millis

    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    return links.groupBy { link ->
        when {
            link.timestamp >= today     -> "Today"
            link.timestamp >= yesterday -> "Yesterday"
            else                        -> dateFormat.format(Date(link.timestamp))
        }
    }
}

/** Format a timestamp to a human-readable time string (e.g. "3:42 PM"). */
private fun formatTime(timestamp: Long): String {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return timeFormat.format(Date(timestamp))
}

/** Open a URL in the default browser. */
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}

/** Copy a URL to the system clipboard. */
private fun copyToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("EasyLink URL", url))
    // Android 13+ shows its own clipboard notification
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
    }
}
