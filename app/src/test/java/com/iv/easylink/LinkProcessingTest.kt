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
    fun `cleaner uses hostname boundaries`() {
        val cleaned = LinkCleaner.clean("https://attackerx.com/path?s=required")
        assertEquals("https://attackerx.com/path?s=required", cleaned)
    }

    @Test
    fun `categorizer recognizes common destinations`() {
        assertEquals(LinkCategory.VIDEO, LinkCategorizer.categorize("youtu.be", "https://youtu.be/id"))
        assertEquals(LinkCategory.DEVELOPMENT, LinkCategorizer.categorize("github.com", "https://github.com/openai"))
        assertEquals(LinkCategory.DOCUMENT, LinkCategorizer.categorize("example.com", "https://example.com/file.pdf"))
    }

    @Test
    fun `scanner flags risky hosts and allows ordinary domains`() {
        assertTrue(SecurityScanner.isSuspiciousUrl("https://secure-login-paypal.top/"))
        assertTrue(SecurityScanner.isSuspiciousUrl("https://127.0.0.1/account"))
        assertFalse(SecurityScanner.isSuspiciousUrl("https://accounts.google.com/"))
        assertFalse(SecurityScanner.isSuspiciousUrl("https://example.com/"))
    }
}
