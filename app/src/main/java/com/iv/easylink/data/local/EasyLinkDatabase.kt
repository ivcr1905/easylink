package com.iv.easylink.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for EasyLink.
 * Version 1 — initial schema with the [LinkEntity] table.
 */
@Database(entities = [LinkEntity::class], version = 1, exportSchema = false)
abstract class EasyLinkDatabase : RoomDatabase() {

    abstract fun linkDao(): LinkDao

    companion object {
        private const val DATABASE_NAME = "easylink_db"

        @Volatile
        private var INSTANCE: EasyLinkDatabase? = null

        /**
         * Returns the singleton database instance, creating it on first access.
         * Thread-safe via double-checked locking.
         */
        fun getInstance(context: Context): EasyLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EasyLinkDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { INSTANCE = it }
            }
        }
    }
}
