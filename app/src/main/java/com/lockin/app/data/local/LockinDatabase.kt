package com.lockin.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedLinkEntity::class], version = 1, exportSchema = true)
abstract class LockinDatabase : RoomDatabase() {
    abstract fun savedLinkDao(): SavedLinkDao
}
