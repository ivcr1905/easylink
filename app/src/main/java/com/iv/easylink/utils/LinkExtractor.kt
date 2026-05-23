package com.iv.easylink.utils

import android.net.Uri
import android.util.Patterns
import androidx.core.net.toUri

object LinkExtractor{
    fun extractLinks(text: String?) : List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val matcher = Patterns.WEB_URL.matcher(text)
        val link = if(matcher.find()){
            var rawMatch = matcher.group().trimEnd('.', ',', ';', '!', '?')
            val hasScheme = rawMatch.startsWith("http://", ignoreCase = true) ||
                    rawMatch.startsWith("https://", ignoreCase = true)
            if (!hasScheme) {
                // The Combined Super-List
                val commonTlds = listOf(".com", ".in", ".org", ".net", ".co", ".io", ".be", ".me", ".app", ".dev", ".tv", ".gov", ".edu")

                // If it's naked AND doesn't end in a common domain, it's a typo. Ignore it.
                if (commonTlds.none { rawMatch.lowercase().endsWith(it) }) {
                    return emptyList()
                }
            }
            var finalUrl = cleanTrackingUrl(rawMatch)
            if(!finalUrl.startsWith("http://", ignoreCase = true) && !finalUrl.startsWith("https://", ignoreCase = true)){
                finalUrl = "https://$finalUrl"
            }
            finalUrl
        } else {
            return emptyList()
        }
        return listOf(link)
    }

    fun cleanTrackingUrl(dirtyUrl: String): String {
        try {
            val uri = dirtyUrl.toUri()
            val host = uri.host ?: return dirtyUrl
            val query = uri.query
            if (host.contains("instagram.com") || host.contains("twitter.com") || host.contains("x.com")){
                if(query != null) {
                    return uri.buildUpon().clearQuery().build().toString()
                }
            }
            return dirtyUrl
        } catch (e: Exception) {
            return dirtyUrl
        }
    }
}