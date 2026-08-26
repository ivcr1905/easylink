package com.iv.easylink

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iv.easylink.data.transfer.BackupCodec
import com.iv.easylink.data.transfer.BackupKind
import com.iv.easylink.data.transfer.BackupLink
import com.iv.easylink.data.transfer.EasyLinkBackup
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.StringReader
import java.io.StringWriter

@RunWith(AndroidJUnit4::class)
class BackupCodecInstrumentedTest {
    @Test fun roundTripsVersionedBackup() {
        val source = EasyLinkBackup(
            kind = BackupKind.COLLECTION,
            exportedAt = 123,
            name = "Reading",
            links = listOf(
                BackupLink(
                    ref = "l1", url = "https://example.com/article?utm_source=test", sourcePackage = null,
                    sourceApp = "Manual", senderName = "Rohit", conversationName = null, timestamp = 100,
                    category = 2, isSuspicious = false, isFavorite = true, wasCleaned = false,
                    title = "Article", siteName = "Example", archivedAt = null, deletedAt = null,
                    collectionRefs = emptyList(),
                    originalUrl = "https://example.com/article?utm_source=test",
                    isReadLater = true,
                ),
            ),
        )
        val output = StringWriter()
        BackupCodec.write(source, output)
        val restored = BackupCodec.read(StringReader(output.toString()))
        assertEquals(BackupKind.COLLECTION, restored.kind)
        assertEquals("Reading", restored.name)
        assertEquals("https://example.com/article", restored.links.single().url)
        assertEquals("Rohit", restored.links.single().senderName)
        assertEquals("https://example.com/article?utm_source=test", restored.links.single().originalUrl)
        assertEquals(true, restored.links.single().isReadLater)
    }
}
