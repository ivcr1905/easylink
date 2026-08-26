package com.iv.easylink

import com.iv.easylink.model.LinkCategory
import com.iv.easylink.utils.LinkCategorizer
import com.iv.easylink.utils.LinkCleaner
import com.iv.easylink.utils.LinkExtractor
import com.iv.easylink.utils.SecurityScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkProcessingTest {
    @Test
    fun `extracts multiple links and removes sentence punctuation`() {
        val links = LinkExtractor.extractLinks("Read example.com/a, then https://developer.android.com/guide.")
        assertEquals(listOf("https://example.com/a", "https://developer.android.com/guide"), links)
    }

    @Test
    fun `deduplicates the same normalized link in one message`() {
        val links = LinkExtractor.extractLinks("https://example.com https://example.com/")
        assertEquals(listOf("https://example.com/"), links)
    }

    @Test
    fun `cleaner removes trackers without removing functional query values`() {
        val cleaned = LinkCleaner.clean("https://www.youtube.com/watch?v=abc123&si=share&utm_source=chat")
        assertEquals("https://www.youtube.com/watch?v=abc123", cleaned)
    }

    @Test
    fun `processing retains the normalized original when cleaning changes a link`() {
        val processed = LinkExtractor.process("example.com/article?utm_source=chat")!!
        assertEquals("https://example.com/article", processed.url)
        assertEquals("https://example.com/article?utm_source=chat", processed.originalUrl)
        assertTrue(processed.wasCleaned)
    }

    @Test
    fun `cleaner uses hostname boundaries`() {
        val cleaned = LinkCleaner.clean("https://attackerx.com/path?s=required")
        assertEquals("https://attackerx.com/path?s=required", cleaned)
    }

    @Test
    fun `instagram content links discard the complete tracking query`() {
        val cleaned = LinkCleaner.clean(
            "https://www.instagram.com/reel/ABC123/?igsh=tracking&unknown_share_token=also_tracking",
        )
        assertEquals("https://www.instagram.com/reel/ABC123/", cleaned)
    }

    @Test
    fun `instagram redirect links preserve their functional destination`() {
        val dirty = "https://l.instagram.com/?u=https%3A%2F%2Fexample.com%2Farticle&igsh=tracking"
        assertEquals("https://l.instagram.com/?u=https%3A%2F%2Fexample.com%2Farticle", LinkCleaner.clean(dirty))
    }

    @Test
    fun `known social trackers are removed without dropping functional values`() {
        assertEquals(
            "https://www.tiktok.com/@creator/video/123",
            LinkCleaner.clean("https://www.tiktok.com/@creator/video/123?_r=1&_t=tracking"),
        )
        assertEquals(
            "https://open.spotify.com/track/123?context=album",
            LinkCleaner.clean("https://open.spotify.com/track/123?si=tracking&context=album"),
        )
    }

    @Test
    fun `facebook functional parameters survive universal tracker removal`() {
        val cleaned = LinkCleaner.clean("https://www.facebook.com/story.php?story_fbid=123&id=456&fbclid=tracking")
        assertEquals("https://www.facebook.com/story.php?story_fbid=123&id=456", cleaned)
    }

    @Test
    fun `categorizer recognizes common destinations`() {
        assertEquals(LinkCategory.VIDEO, LinkCategorizer.categorize("youtu.be", "https://youtu.be/id"))
        assertEquals(LinkCategory.OTHER, LinkCategorizer.categorize("github.com", "https://github.com/openai"))
        assertEquals(LinkCategory.DOCUMENT, LinkCategorizer.categorize("example.com", "https://example.com/file.pdf"))
        assertEquals(LinkCategory.SHOPPING, LinkCategorizer.categorize("fkrt.cc", "https://fkrt.cc/hILNss1"))
        assertEquals(LinkCategory.MUSIC, LinkCategorizer.categorize("music.youtube.com", "https://music.youtube.com/watch?v=id"))
        assertEquals(LinkCategory.OTHER, LinkCategorizer.categorize("unknown.example.com", "https://unknown.example.com/item"))
    }

    @Test
    fun `scanner flags risky hosts and allows ordinary domains`() {
        assertTrue(SecurityScanner.isSuspiciousUrl("https://secure-login-paypal.top/"))
        assertTrue(SecurityScanner.isSuspiciousUrl("https://127.0.0.1/account"))
        assertFalse(SecurityScanner.isSuspiciousUrl("https://accounts.google.com/"))
        assertFalse(SecurityScanner.isSuspiciousUrl("https://example.com/"))
    }
}
