package com.iv.easylink

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iv.easylink.data.local.EasyLinkDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EasyLinkDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun migrationTwoToThreePreservesDataAndRemovesDevelopmentCategory() {
        helper.createDatabase(DATABASE_NAME, 2).apply {
            execSQL("INSERT INTO collections (id, name, colorToken, createdAt) VALUES (1, 'Reading', 1, 10)")
            execSQL("INSERT INTO links (id, url, host, sourcePackage, sourceApp, timestamp, category, isSuspicious, isFavorite, wasCleaned, title, siteName, imageUrl, metadataState, metadataUpdatedAt) VALUES (1, 'https://github.com/openai', 'github.com', NULL, 'Manual', 10, 8, 0, 0, 0, NULL, NULL, NULL, 0, NULL)")
            execSQL("INSERT INTO automation_rules (id, name, hostPattern, pathContains, category, collectionId, enabled, priority) VALUES (1, 'GitHub', 'github.com', NULL, 8, 1, 1, 0)")
            close()
        }
        helper.runMigrationsAndValidate(DATABASE_NAME, 3, true, EasyLinkDatabase.MIGRATION_2_3).use { database ->
            database.query("SELECT category, senderName, archivedAt, deletedAt FROM links WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
            }
            database.query("SELECT isPinned FROM collections WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test fun migrationThreeToFourPreservesLinksAndAddsRecoverableUrlState() {
        helper.createDatabase(DATABASE_NAME, 3).apply {
            execSQL("INSERT INTO links (id, url, host, sourcePackage, sourceApp, senderName, conversationName, timestamp, category, isSuspicious, isFavorite, wasCleaned, title, siteName, imageUrl, metadataState, metadataUpdatedAt, archivedAt, deletedAt) VALUES (1, 'https://example.com/', 'example.com', 'pkg', 'Example', 'Rohit T', NULL, 10, 0, 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL)")
            close()
        }
        helper.runMigrationsAndValidate(DATABASE_NAME, 4, true, EasyLinkDatabase.MIGRATION_3_4).use { database ->
            database.query("SELECT url, originalUrl, isReadLater FROM links WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("https://example.com/", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(0, cursor.getInt(2))
            }
        }
    }

    private companion object { const val DATABASE_NAME = "migration-test" }
}
