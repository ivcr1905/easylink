package com.iv.easylink.utils

import java.net.URI
import java.util.Locale

data class NormalizedLink(
    val url: String,
    val host: String,
    val wasCleaned: Boolean,
    val originalUrl: String? = null,
)

object LinkExtractor {
    private val urlPattern = Regex(
        """(?i)\b((?:https?://|www\.)[^\s<>\[\]{}\"']+|(?:[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?\.)+(?:com|org|net|in|co|io|me|app|dev|tv|gov|edu|ai|info|biz|xyz|ly)(?::\d{1,5})?(?:/[^\s<>\[\]{}\"']*)?)""",
    )
    private val trailingCharacters = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')

    fun extractLinks(text: CharSequence?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val results = ArrayList<String>(2)
        urlPattern.findAll(text).forEach { match ->
            val value = match.value.trimEnd(*trailingCharacters)
            normalize(value)?.url?.let { normalized ->
                if (results.none { it == normalized }) results.add(normalized)
            }
        }
        return results
    }

    fun process(rawUrl: String): NormalizedLink? {
        val normalized = normalize(rawUrl) ?: return null
        val cleaned = LinkCleaner.clean(normalized.url)
        val final = normalize(cleaned) ?: return normalized
        val changed = final.url != normalized.url
        return final.copy(
            wasCleaned = changed,
            originalUrl = normalized.url.takeIf { changed },
        )
    }

    fun cleanTrackingUrl(dirtyUrl: String): String = process(dirtyUrl)?.url ?: dirtyUrl

    private fun normalize(rawUrl: String): NormalizedLink? {
        val candidate = rawUrl.trim().let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
        }
        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            if (scheme != "http" && scheme != "https") return null
            if (uri.userInfo != null) return null
            val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return null
            if (host.length > 253 || !host.contains('.')) return null
            val port = when {
                uri.port == 80 && scheme == "http" -> -1
                uri.port == 443 && scheme == "https" -> -1
                else -> uri.port
            }
            val path = uri.rawPath.orEmpty().ifEmpty { "/" }
            val authority = if (port >= 0) "$host:$port" else host
            val rebuilt = buildString(candidate.length + 8) {
                append(scheme).append("://").append(authority).append(path)
                uri.rawQuery?.let { append('?').append(it) }
                uri.rawFragment?.let { append('#').append(it) }
            }
            NormalizedLink(rebuilt, host, false)
        } catch (_: Exception) {
            null
        }
    }
}

object LinkCleaner {
    private val universalTracking = setOf(
        "fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid", "vero_id", "ref_src",
        "ref_url", "mkt_tok", "yclid", "_hsenc", "_hsmi",
    )
    private val youtubeTracking = setOf("si", "feature", "embeds_referring_euri", "source_ve_path")
    private val instagramTracking = setOf("igsh", "igshid")
    private val xTracking = setOf("s", "t")
    private val tiktokTracking = setOf("_r", "_t")
    private val amazonTracking = setOf("tag", "linkcode", "camp", "creative", "creativeasin", "ascsubtag", "ref")

    fun clean(url: String): String = try {
        val uri = URI(url)
        val host = uri.host?.lowercase(Locale.ROOT) ?: return url
        val rawQuery = uri.rawQuery ?: return url
        val stripInstagramQuery = isInstagramContentUrl(host, uri.rawPath.orEmpty())
        val kept = rawQuery.split('&').filter { pair ->
            val rawKey = pair.substringBefore('=').lowercase(Locale.ROOT)
            val isUniversal = rawKey.startsWith("utm_") || rawKey in universalTracking
            val isYouTube = isHost(host, "youtube.com") || isHost(host, "youtu.be")
            val isInstagram = isHost(host, "instagram.com")
            val isX = isHost(host, "twitter.com") || isHost(host, "x.com")
            val isTikTok = isHost(host, "tiktok.com")
            val isSpotify = isHost(host, "spotify.com")
            val isThreads = isHost(host, "threads.net")
            val isAmazon = host == "amazon.com" || host.startsWith("amazon.") || host.contains(".amazon.")
            !stripInstagramQuery && !isUniversal &&
                !(isYouTube && rawKey in youtubeTracking) &&
                !(isInstagram && rawKey in instagramTracking) &&
                !(isX && rawKey in xTracking) &&
                !(isTikTok && rawKey in tiktokTracking) &&
                !(isSpotify && rawKey == "si") &&
                !(isThreads && rawKey == "xmt") &&
                !(isAmazon && rawKey in amazonTracking)
        }
        buildString(url.length) {
            append(uri.scheme).append("://").append(uri.rawAuthority).append(uri.rawPath.orEmpty().ifEmpty { "/" })
            kept.joinToString("&").takeIf(String::isNotEmpty)?.let { append('?').append(it) }
            uri.rawFragment?.let { append('#').append(it) }
        }
    } catch (_: Exception) {
        url
    }

    private fun isInstagramContentUrl(host: String, rawPath: String): Boolean {
        if (host != "instagram.com" && host != "www.instagram.com" && host != "m.instagram.com") return false
        return rawPath.startsWith("/p/") || rawPath.startsWith("/reel/") ||
            rawPath.startsWith("/reels/") || rawPath.startsWith("/tv/") ||
            rawPath.startsWith("/stories/") || rawPath.startsWith("/share/")
    }

    internal fun isHost(host: String, domain: String): Boolean = host == domain || host.endsWith(".$domain")
}
