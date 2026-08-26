package com.iv.easylink.utils

import com.iv.easylink.model.LinkCategory

object LinkCategorizer {
    private val video = arrayOf("youtube.com", "youtu.be", "vimeo.com", "twitch.tv", "dailymotion.com")
    private val social = arrayOf("instagram.com", "twitter.com", "x.com", "facebook.com", "reddit.com", "linkedin.com", "threads.net")
    private val shopping = arrayOf("flipkart.com", "fkrt.cc", "ebay.com", "etsy.com", "myntra.com", "meesho.com")
    private val maps = arrayOf("maps.app.goo.gl", "maps.apple.com", "openstreetmap.org")
    private val music = arrayOf("spotify.com", "music.apple.com", "soundcloud.com", "music.youtube.com")
    private val articles = arrayOf("medium.com", "substack.com", "wikipedia.org", "bbc.com", "reuters.com")
    private val documentSuffixes = arrayOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx")

    fun categorize(host: String, url: String): LinkCategory = when {
        documentSuffixes.any { url.substringBefore('?').endsWith(it, true) } -> LinkCategory.DOCUMENT
        music.any { LinkCleaner.isHost(host, it) } -> LinkCategory.MUSIC
        video.any { LinkCleaner.isHost(host, it) } -> LinkCategory.VIDEO
        social.any { LinkCleaner.isHost(host, it) } -> LinkCategory.SOCIAL
        shopping.any { LinkCleaner.isHost(host, it) } || isAmazonHost(host) -> LinkCategory.SHOPPING
        maps.any { LinkCleaner.isHost(host, it) } || host.startsWith("maps.google.") -> LinkCategory.MAPS
        articles.any { LinkCleaner.isHost(host, it) } -> LinkCategory.ARTICLE
        else -> LinkCategory.OTHER
    }

    private fun isAmazonHost(host: String): Boolean = host == "amazon.com" ||
        host.startsWith("amazon.") || host.contains(".amazon.")
}
