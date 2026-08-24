package com.iv.easylink.utils

import com.iv.easylink.model.LinkCategory

object LinkCategorizer {
    private val video = arrayOf("youtube.com", "youtu.be", "vimeo.com", "twitch.tv", "dailymotion.com")
    private val social = arrayOf("instagram.com", "twitter.com", "x.com", "facebook.com", "reddit.com", "linkedin.com", "threads.net")
    private val shopping = arrayOf("amazon.", "flipkart.com", "ebay.", "etsy.com", "myntra.com", "meesho.com")
    private val maps = arrayOf("maps.google.", "goo.gl", "maps.apple.com", "openstreetmap.org")
    private val music = arrayOf("spotify.com", "music.apple.com", "soundcloud.com", "music.youtube.com")
    private val development = arrayOf("github.com", "gitlab.com", "stackoverflow.com", "developer.android.com", "medium.com")
    private val documentSuffixes = arrayOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx")

    fun categorize(host: String, url: String): LinkCategory = when {
        video.any { LinkCleaner.isHost(host, it) } -> LinkCategory.VIDEO
        social.any { LinkCleaner.isHost(host, it) } -> LinkCategory.SOCIAL
        shopping.any { host == it.removeSuffix(".") || host.contains(it) } -> LinkCategory.SHOPPING
        maps.any { host == it.removeSuffix(".") || host.contains(it) } -> LinkCategory.MAPS
        music.any { LinkCleaner.isHost(host, it) } -> LinkCategory.MUSIC
        development.any { LinkCleaner.isHost(host, it) } -> LinkCategory.DEVELOPMENT
        documentSuffixes.any { url.substringBefore('?').endsWith(it, true) } -> LinkCategory.DOCUMENT
        else -> LinkCategory.ARTICLE
    }
}
