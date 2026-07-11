package com.appdex.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val iconKey: String = "folder",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_paths")
data class RecentPathEntity(
    @PrimaryKey val path: String,
    val lastAccessed: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
