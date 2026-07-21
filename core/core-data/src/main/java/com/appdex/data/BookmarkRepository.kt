package com.appdex.data

import com.appdex.db.BookmarkDao
import com.appdex.db.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {
    fun observeAll(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

    suspend fun add(name: String, path: String) {
        bookmarkDao.insert(BookmarkEntity(name = name, path = path))
    }

    suspend fun remove(path: String) {
        bookmarkDao.deleteByPath(path)
    }

    suspend fun exists(path: String): Boolean {
        return bookmarkDao.observeAll().first().any { it.path == path }
    }
}
