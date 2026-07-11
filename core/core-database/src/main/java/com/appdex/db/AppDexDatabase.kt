package com.appdex.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BookmarkEntity::class,
        RecentPathEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDexDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun recentPathDao(): RecentPathDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
