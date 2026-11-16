package com.iv.easylink.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.local.CountByKey
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkDimensions
import com.iv.easylink.ui.theme.EasyLinkTheme
import com.iv.easylink.ui.components.EasyLinkIcons

@Composable
fun InsightsScreen(
    viewModel: EasyLinkViewModel,
    onReviewFavorites: () -> Unit,
    onReviewWarnings: () -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val total by viewModel.weekTotal.collectAsState()
    val categories by viewModel.categoryCounts.collectAsState()
    val domains by viewModel.topDomains.collectAsState()
    val sources by viewModel.topSources.collectAsState()
    val favorites by viewModel.favoriteCount.collectAsState()
    val suspicious by viewModel.suspiciousCount.collectAsState()
    val leadingCategory = categories.firstOrNull()
    val leadingSource = sources.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = EasyLinkDimensions.screenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(EasyLinkDimensions.sectionSpacing),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your week in links", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (total == 0) "Your private weekly recap will appear as links arrive."
                    else "$total link${if (total == 1) "" else "s"} saved in the last 7 days.",
                    color = EasyLinkTheme.extraColors.mutedText,
                )
            }
        }
        if (total > 0) {
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("This week's pattern", style = MaterialTheme.typography.titleMedium)
                        leadingCategory?.let {
                            Text("You saved mostly ${LinkCategory.fromCode(it.key.toIntOrNull() ?: 0).label.lowercase()} links.")
                        }
                        leadingSource?.let { Text("${it.key} was your most active source.") }
                    }
                }
            }
        }
        if (favorites > 0 || suspicious > 0) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Review", style = MaterialTheme.typography.titleLarge)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column {
                            if (favorites > 0) ActionRow(EasyLinkIcons.FavoriteBorder, "$favorites favorite${if (favorites == 1) "" else "s"}", "Open saved favorites", onReviewFavorites)
                            if (favorites > 0 && suspicious > 0) HorizontalDivider()
                            if (suspicious > 0) ActionRow(EasyLinkIcons.Warning, "$suspicious warning${if (suspicious == 1) "" else "s"}", "Review links flagged by local checks", onReviewWarnings)
                        }
                    }
                }
            }
        }
        if (categories.isNotEmpty()) {
            item {
                InsightList(
                    title = "Top categories",
                    rows = categories.take(4).map { it.copy(key = LinkCategory.fromCode(it.key.toIntOrNull() ?: 0).label) },
                    onClick = { label -> LinkCategory.displayEntries.firstOrNull { it.label == label }?.let { onOpenCategory(it.code) } },
                )
            }
        }
        if (domains.isNotEmpty()) item { InsightList("Top domains", domains.take(5)) }
        if (sources.isNotEmpty()) item { InsightList("Source apps", sources.take(5)) }
        item {
            Text(
                "Computed on this device. EasyLink never uploads insight data.",
                style = MaterialTheme.typography.bodySmall,
                color = EasyLinkTheme.extraColors.mutedText,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText)
        }
        Icon(EasyLinkIcons.Chevron, null, tint = EasyLinkTheme.extraColors.mutedText)
    }
}

@Composable
private fun InsightList(title: String, rows: List<CountByKey>, onClick: ((String) -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    Row(
                        Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable { onClick(row.key) }).padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.key, Modifier.weight(1f), maxLines = 1)
                        Text(row.count.toString(), style = MaterialTheme.typography.labelLarge, color = EasyLinkTheme.extraColors.mutedText)
                        if (onClick != null) Icon(EasyLinkIcons.Chevron, null, Modifier.padding(start = 6.dp), tint = EasyLinkTheme.extraColors.mutedText)
                    }
                    if (index != rows.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}
