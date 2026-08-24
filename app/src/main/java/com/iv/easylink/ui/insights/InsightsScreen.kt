package com.iv.easylink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iv.easylink.data.local.CountByKey
import com.iv.easylink.model.LinkCategory
import com.iv.easylink.ui.theme.EasyLinkTheme

@Composable
fun InsightsScreen(viewModel: EasyLinkViewModel) {
    val total by viewModel.weekTotal.collectAsState()
    val categories by viewModel.categoryCounts.collectAsState()
    val domains by viewModel.topDomains.collectAsState()
    val sources by viewModel.topSources.collectAsState()
    val favorites by viewModel.favoriteCount.collectAsState()
    val suspicious by viewModel.suspiciousCount.collectAsState()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Your week in links", style = MaterialTheme.typography.headlineLarge)
            Text("Computed privately from your on-device inbox.", color = EasyLinkTheme.extraColors.mutedText)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(total.toString(), "Last 7 days", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                MetricCard(favorites.toString(), "Favorites", Modifier.weight(1f), EasyLinkTheme.extraColors.success)
                MetricCard(suspicious.toString(), "Warnings", Modifier.weight(1f), MaterialTheme.colorScheme.error)
            }
        }
        item { InsightSection("Categories", categories.map { it.copy(key = LinkCategory.fromCode(it.key.toIntOrNull() ?: 0).label) }, EasyLinkTheme.extraColors.categoryColors) }
        item { InsightSection("Top domains", domains, listOf(MaterialTheme.colorScheme.primary)) }
        item { InsightSection("Source apps", sources, listOf(EasyLinkTheme.extraColors.success)) }
        item { Text("Counts update as links are added or removed. EasyLink does not send insight data anywhere.", style = MaterialTheme.typography.bodySmall, color = EasyLinkTheme.extraColors.mutedText, modifier = Modifier.padding(vertical = 18.dp)) }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier, color: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .14f))) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InsightSection(title: String, rows: List<CountByKey>, colors: List<Color>) {
    Card(colors = CardDefaults.cardColors(containerColor = EasyLinkTheme.extraColors.card)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (rows.isEmpty()) Text("No data yet", color = EasyLinkTheme.extraColors.mutedText)
            val maximum = rows.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            rows.forEachIndexed { index, row ->
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(row.key, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(row.count.toString(), color = EasyLinkTheme.extraColors.mutedText)
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 5.dp).height(7.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .25f), MaterialTheme.shapes.extraSmall)) {
                        val barColor = if (colors.isEmpty()) MaterialTheme.colorScheme.primary else colors[index % colors.size]
                        Box(Modifier.fillMaxWidth(row.count.toFloat() / maximum).height(7.dp).background(barColor, MaterialTheme.shapes.extraSmall))
                    }
                }
            }
        }
    }
}
