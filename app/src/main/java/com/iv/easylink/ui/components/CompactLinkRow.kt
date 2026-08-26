package com.iv.easylink.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Dense, reusable representation for a saved link. Full URLs remain in the action sheet. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactLinkRow(
    link: LinkEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onSelect: () -> Unit = {},
    onMetadata: () -> Unit = {},
    onFavorite: () -> Unit = {},
) {
    LaunchedEffect(link.id, link.metadataState) { onMetadata() }
    val category = LinkCategory.fromCode(link.category)
    val categoryColor = EasyLinkTheme.extraColors.categoryColors.getOrNull(category.code)
        ?: MaterialTheme.colorScheme.primary
    val sourceLine = remember(link.host, link.senderName, link.sourceApp) {
        buildString {
            append(link.host)
            link.senderName?.takeIf(String::isNotBlank)?.let { append(" · ").append(it) }
            link.sourceApp.takeIf(String::isNotBlank)?.let { append(" · ").append(it) }
        }
    }
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = { if (selectionMode) onSelect() else onClick() },
                onLongClick = onSelect,
            ),
    ) {
        Row(
            Modifier.heightIn(min = 74.dp).padding(start = 4.dp, end = 0.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = categoryColor.copy(alpha = .14f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(EasyLinkIcons.category(category), null, Modifier.size(21.dp), tint = categoryColor)
                }
            }
            Column(
                Modifier.padding(start = 12.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = link.title ?: link.siteName ?: link.host,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (link.isSuspicious) Icon(EasyLinkIcons.Warning, "Potentially unsafe", Modifier.padding(start = 4.dp).size(17.dp), tint = MaterialTheme.colorScheme.error)
                }
                Text(sourceLine, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(categoryColor, CircleShape))
                    Text(category.label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, color = EasyLinkTheme.extraColors.mutedText)
                    if (link.wasCleaned) Text(" · Cleaned", style = MaterialTheme.typography.labelMedium, color = EasyLinkTheme.extraColors.success)
                    if (link.isReadLater) Text(" · Read later", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    Text(timeLabel(link.timestamp), style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
                }
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (link.isFavorite) EasyLinkIcons.Favorite else EasyLinkIcons.FavoriteBorder,
                    if (link.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (link.isFavorite) MaterialTheme.colorScheme.primary else EasyLinkTheme.extraColors.mutedText,
                )
            }
        }
    }
}

private val compactTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun timeLabel(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(compactTimeFormatter)
