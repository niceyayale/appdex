package com.appdex.sqliteviewer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SQLite 查看器 Repository:打开 .db 文件,列出表,查询数据。
 *
 * 支持从 APK 内提取 .db 文件或直接打开本地 .db。
 * 使用 Android 内置 SQLiteDatabase (基于 sqlite3 C 库)。
 */
@Singleton
class SqliteViewerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var database: SQLiteDatabase? = null
    private var tempDbFile: File? = null

    fun openDatabase(dbPath: String) {
        close()
        val file = File(dbPath)
        database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }

    /**
     * 从 APK 中提取 .db 文件并打开。
     */
    fun openFromApk(apkPath: String, dbEntryName: String) {
        close()
        val tempFile = File.createTempFile("AppX_db", ".db", context.cacheDir)
        java.util.zip.ZipFile(apkPath).use { zip ->
            val entry = zip.getEntry(dbEntryName)
                ?: throw IllegalArgumentException("Entry not found: $dbEntryName")
            zip.getInputStream(entry).use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        tempDbFile = tempFile
        database = SQLiteDatabase.openDatabase(
            tempFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }

    fun getTables(): List<TableInfo> {
        val db = database ?: return emptyList()
        val tables = mutableListOf<TableInfo>()

        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
            null,
        )
        cursor.use {
            while (it.moveToNext()) {
                val tableName = it.getString(0)
                val countCursor = db.rawQuery("SELECT COUNT(*) FROM `$tableName`", null)
                val rowCount = countCursor.use { c ->
                    if (c.moveToFirst()) c.getInt(0) else 0
                }
                tables.add(TableInfo(tableName, rowCount))
            }
        }
        return tables
    }

    fun getTableSchema(tableName: String): List<ColumnInfo> {
        val db = database ?: return emptyList()
        val columns = mutableListOf<ColumnInfo>()

        val cursor = db.rawQuery("PRAGMA table_info(`$tableName`)", null)
        cursor.use {
            while (it.moveToNext()) {
                columns.add(
                    ColumnInfo(
                        cid = it.getInt(0),
                        name = it.getString(1),
                        type = it.getString(2) ?: "ANY",
                        notNull = it.getInt(3) == 1,
                        defaultValue = it.getString(4),
                        isPrimaryKey = it.getInt(5) == 1,
                    ),
                )
            }
        }
        return columns
    }

    fun queryTable(tableName: String, limit: Int = 100, offset: Int = 0): QueryResult {
        val db = database ?: return QueryResult(emptyList(), emptyList())

        val columnsCursor = db.rawQuery("PRAGMA table_info(`$tableName`)", null)
        val columnNames = mutableListOf<String>()
        columnsCursor.use {
            while (it.moveToNext()) {
                columnNames.add(it.getString(1))
            }
        }

        val cursor = db.rawQuery(
            "SELECT * FROM `$tableName` LIMIT $limit OFFSET $offset",
            null,
        )

        val rows = mutableListOf<List<String?>>()
        cursor.use {
            while (it.moveToNext()) {
                val row = mutableListOf<String?>()
                for (i in 0 until it.columnCount) {
                    val value = when (it.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        android.database.Cursor.FIELD_TYPE_BLOB -> {
                            val blob = it.getBlob(i)
                            "[BLOB ${blob.size} bytes]"
                        }
                        else -> it.getString(i)
                    }
                    row.add(value)
                }
                rows.add(row)
            }
        }

        return QueryResult(columnNames, rows)
    }

    fun executeQuery(sql: String): QueryResult {
        val db = database ?: return QueryResult(emptyList(), emptyList())
        val cursor = db.rawQuery(sql, null)

        val columnNames = mutableListOf<String>()
        for (i in 0 until cursor.columnCount) {
            columnNames.add(cursor.getColumnName(i))
        }

        val rows = mutableListOf<List<String?>>()
        cursor.use {
            while (it.moveToNext()) {
                val row = mutableListOf<String?>()
                for (i in 0 until it.columnCount) {
                    val value = when (it.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        android.database.Cursor.FIELD_TYPE_BLOB -> {
                            val blob = it.getBlob(i)
                            "[BLOB ${blob.size} bytes]"
                        }
                        else -> it.getString(i)
                    }
                    row.add(value)
                }
                rows.add(row)
            }
        }

        return QueryResult(columnNames, rows)
    }

    fun close() {
        database?.close()
        database = null
        tempDbFile?.delete()
        tempDbFile = null
    }
}

data class TableInfo(
    val name: String,
    val rowCount: Int,
)

data class ColumnInfo(
    val cid: Int,
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?,
    val isPrimaryKey: Boolean,
)

data class QueryResult(
    val columns: List<String>,
    val rows: List<List<String?>>,
)
