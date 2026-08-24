package com.iv.easylink.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LinkEntity::class, CollectionEntity::class, LinkCollectionEntity::class, AutomationRuleEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class EasyLinkDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao

    companion object {
        private const val DATABASE_NAME = "easylink_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS links_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, url TEXT NOT NULL, host TEXT NOT NULL, sourcePackage TEXT, sourceApp TEXT NOT NULL, timestamp INTEGER NOT NULL, category INTEGER NOT NULL, isSuspicious INTEGER NOT NULL, isFavorite INTEGER NOT NULL, wasCleaned INTEGER NOT NULL, title TEXT, siteName TEXT, imageUrl TEXT, metadataState INTEGER NOT NULL, metadataUpdatedAt INTEGER)""")
                db.execSQL("""INSERT INTO links_new (id, url, host, sourcePackage, sourceApp, timestamp, category, isSuspicious, isFavorite, wasCleaned, metadataState)
                    SELECT id, url,
                    CASE WHEN instr(substr(url, instr(url, '://') + 3), '/') > 0
                      THEN lower(substr(substr(url, instr(url, '://') + 3), 1, instr(substr(url, instr(url, '://') + 3), '/') - 1))
                      ELSE lower(substr(url, instr(url, '://') + 3)) END,
                    NULL, sourceApp, timestamp, 0, isSuspicious, 0, 0, 0 FROM links""")
                db.execSQL("DROP TABLE links")
                db.execSQL("ALTER TABLE links_new RENAME TO links")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_timestamp ON links(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_url_timestamp ON links(url, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_host ON links(host)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_category ON links(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_sourcePackage ON links(sourcePackage)")
                db.execSQL("CREATE TABLE IF NOT EXISTS collections (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, colorToken INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS link_collections (linkId INTEGER NOT NULL, collectionId INTEGER NOT NULL, PRIMARY KEY(linkId, collectionId), FOREIGN KEY(linkId) REFERENCES links(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(collectionId) REFERENCES collections(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_link_collections_collectionId ON link_collections(collectionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS automation_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, hostPattern TEXT NOT NULL, pathContains TEXT, category INTEGER, collectionId INTEGER, enabled INTEGER NOT NULL, priority INTEGER NOT NULL, FOREIGN KEY(collectionId) REFERENCES collections(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_automation_rules_collectionId ON automation_rules(collectionId)")
            }
        }

        @Volatile private var instance: EasyLinkDatabase? = null

        fun getInstance(context: Context): EasyLinkDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EasyLinkDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
