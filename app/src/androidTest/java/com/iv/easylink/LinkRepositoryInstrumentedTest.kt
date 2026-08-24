package com.iv.easylink

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iv.easylink.data.local.EasyLinkDatabase
import com.iv.easylink.data.repository.LinkQuery
import com.iv.easylink.data.repository.LinkRepositoryImpl
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
}
