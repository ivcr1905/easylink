package com.iv.easylink.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.local.LinkEntity
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Dense, precision representation for a saved link with quiet craft and clear hierarchy. */
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
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = { if (selectionMode) onSelect() else onClick() },
                onLongClick = onSelect,
            ),
    ) {
        Row(
            Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(8.dp),
                color = categoryColor.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        EasyLinkIcons.category(category),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = categoryColor,
                    )
                }
            }
            Column(
                Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = link.title ?: link.siteName ?: link.host,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (link.isSuspicious) {
                        Icon(
                            EasyLinkIcons.Warning,
                            "Potentially unsafe",
                            Modifier.padding(start = 4.dp).size(15.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(
                    sourceLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = EasyLinkTheme.extraColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = category.label,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EasyLinkTheme.extraColors.mutedText,
                        )
                    }
                    if (link.wasCleaned) {
                        Surface(
                            color = EasyLinkTheme.extraColors.success.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                "Cleaned",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = EasyLinkTheme.extraColors.success,
                            )
                        }
                    }
                    if (link.isReadLater) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                "Read later",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        timeLabel(link.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = EasyLinkTheme.extraColors.mutedText,
                    )
                }
            }
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    if (link.isFavorite) EasyLinkIcons.Favorite else EasyLinkIcons.FavoriteBorder,
                    if (link.isFavorite) "Remove from favorites" else "Add to favorites",
                    modifier = Modifier.size(19.dp),
                    tint = if (link.isFavorite) MaterialTheme.colorScheme.primary else EasyLinkTheme.extraColors.mutedText.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private val compactTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun timeLabel(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(compactTimeFormatter)
