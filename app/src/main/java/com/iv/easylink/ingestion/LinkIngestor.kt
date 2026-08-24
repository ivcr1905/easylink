package com.iv.easylink.ingestion

import com.iv.easylink.data.local.AutomationRuleEntity
import com.iv.easylink.data.repository.LinkRepository
import com.iv.easylink.data.repository.NewLink
import com.iv.easylink.utils.LinkCategorizer
import com.iv.easylink.utils.LinkCleaner
import com.iv.easylink.utils.LinkExtractor
import com.iv.easylink.utils.SecurityScanner
import java.net.URI
import java.util.Locale

data class LinkSource(val packageName: String?, val appName: String)

data class IngestedLink(
    val id: Long,
    val url: String,
    val suspicious: Boolean,
)

data class PreparedLink(
    val url: String,
    val host: String,
    val category: Int,
    val suspicious: Boolean,
    val wasCleaned: Boolean,
)

class LinkIngestor(private val repository: LinkRepository) {
    fun prepare(text: CharSequence?): List<PreparedLink> {
        val urls = LinkExtractor.extractLinks(text)
        if (urls.isEmpty()) return emptyList()
        val prepared = ArrayList<PreparedLink>(urls.size)
        urls.forEach { extracted ->
            val processed = LinkExtractor.process(extracted) ?: return@forEach
            prepared.add(
                PreparedLink(
                    url = processed.url,
                    host = processed.host,
                    category = LinkCategorizer.categorize(processed.host, processed.url).code,
                    suspicious = SecurityScanner.isSuspiciousUrl(processed.url),
                    wasCleaned = processed.wasCleaned,
                ),
            )
        }
        return prepared
    }

    suspend fun ingest(text: CharSequence?, source: LinkSource): List<IngestedLink> =
        ingestPrepared(prepare(text), source)

    suspend fun ingestPrepared(prepared: List<PreparedLink>, source: LinkSource): List<IngestedLink> {
        if (prepared.isEmpty()) return emptyList()
        val rules = repository.enabledRules()
        val saved = ArrayList<IngestedLink>(prepared.size)
        prepared.forEach { processed ->
            val rule = rules.firstOrNull { it.matches(processed.host, processed.url) }
            val category = rule?.category ?: processed.category
            val id = repository.saveLink(
                NewLink(
                    url = processed.url,
                    host = processed.host,
                    sourcePackage = source.packageName,
                    sourceApp = source.appName.take(80),
                    category = category,
                    isSuspicious = processed.suspicious,
                    wasCleaned = processed.wasCleaned,
                ),
            ) ?: return@forEach
            rule?.collectionId?.let { repository.addToCollection(id, it) }
            saved.add(IngestedLink(id, processed.url, processed.suspicious))
        }
        return saved
    }

    private fun AutomationRuleEntity.matches(host: String, url: String): Boolean {
        val normalizedPattern = hostPattern.trim().lowercase(Locale.ROOT).removePrefix("https://").removePrefix("http://").trim('/')
        if (normalizedPattern.isBlank() || !LinkCleaner.isHost(host, normalizedPattern)) return false
        val requiredPath = pathContains?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault("")
        return path.contains(requiredPath, ignoreCase = true)
    }
}
