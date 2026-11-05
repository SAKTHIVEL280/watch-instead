package com.watchinstead.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SavedLinkEntity::class, PromptEventEntity::class], version = 2, exportSchema = true)
abstract class WatchInsteadDatabase : RoomDatabase() {
    abstract fun savedLinkDao(): SavedLinkDao
    abstract fun promptEventDao(): PromptEventDao
}

/** Adds the stats table. Saved links are untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `prompt_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                "`packageName` TEXT NOT NULL, `outcome` TEXT NOT NULL, `reason` TEXT)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_prompt_events_timestamp_packageName` " +
                "ON `prompt_events` (`timestamp`, `packageName`)",
        )
    }
}
