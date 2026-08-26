package com.iv.easylink

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iv.easylink.data.local.EasyLinkDatabase
import com.iv.easylink.data.repository.LinkQuery
import com.iv.easylink.data.repository.LinkRepositoryImpl
import com.iv.easylink.data.repository.LinkLocation
import com.iv.easylink.data.repository.NewLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkRepositoryInstrumentedTest {
    private lateinit var database: EasyLinkDatabase
    private lateinit var repository: LinkRepositoryImpl

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EasyLinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LinkRepositoryImpl(database.linkDao())
    }

    @After fun tearDown() = database.close()

    @Test fun savesAndDeduplicatesRecentLinks() = runBlocking {
        val value = NewLink("https://example.com/", "example.com", "pkg", "Example", 2, false, false, 1_000_000)
        assertNotNull(repository.saveLink(value))
        assertNull(repository.saveLink(value.copy(capturedAt = 1_001_000)))
        assertEquals(1, repository.observeLinks(LinkQuery()).first().size)
    }

    @Test fun collectionCountsUpdate() = runBlocking {
        val linkId = repository.saveLink(NewLink("https://example.com/", "example.com", null, "Manual", 2, false, false))!!
        val collectionId = repository.createCollection("Reading", 1)
        repository.addToCollection(linkId, collectionId)
        assertEquals(1, repository.observeCollections().first().single().linkCount)
    }

    @Test fun archiveAndBinAreSeparateRecoverableStates() = runBlocking {
        val id = repository.saveLink(NewLink("https://example.com/", "example.com", null, "Manual", 0, false, false))!!
        repository.archiveLinks(listOf(id))
        assertEquals(0, repository.observeLinks(LinkQuery()).first().size)
        assertEquals(1, repository.observeLinks(LinkQuery(location = LinkLocation.ARCHIVED)).first().size)
        repository.deleteLinks(listOf(id))
        assertEquals(0, repository.observeLinks(LinkQuery(location = LinkLocation.ARCHIVED)).first().size)
        assertEquals(1, repository.observeLinks(LinkQuery(location = LinkLocation.BIN)).first().size)
        repository.restoreFromBin(listOf(id))
        assertEquals(1, repository.observeLinks(LinkQuery(location = LinkLocation.ARCHIVED)).first().size)
        repository.unarchiveLinks(listOf(id))
        assertEquals(1, repository.observeLinks(LinkQuery()).first().size)
    }

    @Test fun libraryCategoryCountsExactlyMatchActiveCategoryDestinations() = runBlocking {
        var sequence = 0
        suspend fun save(category: Int): Long = repository.saveLink(
            NewLink(
                url = "https://example.com/category-${category}-${sequence++}",
                host = "example.com",
                sourcePackage = null,
                sourceApp = "Manual",
                category = category,
                isSuspicious = false,
                wasCleaned = false,
            ),
        )!!

        save(1)
        val archivedVideo = save(1)
        val binnedSocial = save(3)
        save(3)
        repository.archiveLinks(listOf(archivedVideo))
        repository.deleteLinks(listOf(binnedSocial))

        val counts = repository.observeActiveCategoryCounts().first().associate { it.key.toInt() to it.count }
        assertEquals(repository.observeLinks(LinkQuery(category = 1)).first().size, counts[1])
        assertEquals(repository.observeLinks(LinkQuery(category = 3)).first().size, counts[3])
        assertEquals(1, counts[1])
        assertEquals(1, counts[3])
    }

    @Test fun readLaterAndEditedUrlsRemainQueryableAndRecoverable() = runBlocking {
        val id = repository.saveLink(
            NewLink(
                url = "https://example.com/article",
                host = "example.com",
                sourcePackage = null,
                sourceApp = "Manual",
                category = 2,
                isSuspicious = false,
                wasCleaned = false,
            ),
        )!!
        repository.setReadLater(id, true)
        assertEquals(1, repository.observeLinks(LinkQuery(readLaterOnly = true)).first().size)

        assertEquals(true, repository.updateLinkUrl(id, "https://example.com/article?utm_source=test"))
        val edited = repository.getLink(id)!!
        assertEquals("https://example.com/article", edited.url)
        assertEquals("https://example.com/article?utm_source=test", edited.originalUrl)
        assertEquals(true, edited.wasCleaned)
        assertEquals(true, edited.isReadLater)
    }

    @Test fun senderCleanupUsesExactAppAndSenderName() = runBlocking {
        var sequence = 0
        suspend fun save(packageName: String, sender: String) = repository.saveLink(
            NewLink(
                url = "https://example.com/${packageName.hashCode()}-${sender.hashCode()}-${sequence++}",
                host = "example.com",
                sourcePackage = packageName,
                sourceApp = packageName,
                category = 0,
                isSuspicious = false,
                wasCleaned = false,
                senderName = sender,
            ),
        )
        val archived = save("whatsapp", "Rohit T")!!
        save("whatsapp", "Rohit TT")
        save("telegram", "Rohit T")
        save("whatsapp", "Rohit T")
        repository.archiveLinks(listOf(archived))
        assertEquals(1, repository.countActiveSenderLinks("whatsapp", "Rohit T"))
        repository.moveSenderLinksToBin("whatsapp", "Rohit T")
        assertEquals(2, repository.observeLinks(LinkQuery()).first().size)
        assertEquals(1, repository.observeLinks(LinkQuery(location = LinkLocation.BIN)).first().size)
        assertEquals(1, repository.observeLinks(LinkQuery(location = LinkLocation.ARCHIVED)).first().size)
    }
}
