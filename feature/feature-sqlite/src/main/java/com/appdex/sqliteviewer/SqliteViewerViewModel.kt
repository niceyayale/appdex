package com.appdex.sqliteviewer

import android.util.Log

import com.appdex.arch.BaseViewModel
import com.appdex.arch.MviEffect
import com.appdex.arch.MviIntent
import com.appdex.arch.MviState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SqliteViewerState(
    val tables: List<TableInfo> = emptyList(),
    val selectedTable: String = "",
    val columns: List<ColumnInfo> = emptyList(),
    val queryResult: QueryResult? = null,
    val sqlInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val fileName: String = "",
) : MviState

sealed interface SqliteViewerIntent : MviIntent {
    data class OpenDatabase(val dbPath: String) : SqliteViewerIntent
    data class OpenFromApk(val apkPath: String, val dbEntryName: String) : SqliteViewerIntent
    data class SelectTable(val tableName: String) : SqliteViewerIntent
    data class UpdateSql(val sql: String) : SqliteViewerIntent
    data object ExecuteSql : SqliteViewerIntent
}

sealed interface SqliteViewerEffect : MviEffect {
    data class ShowMessage(val message: String) : SqliteViewerEffect
}

@HiltViewModel
class SqliteViewerViewModel @Inject constructor(
    private val repository: SqliteViewerRepository,
) : BaseViewModel<SqliteViewerIntent, SqliteViewerState, SqliteViewerEffect>(SqliteViewerState()) {

    override fun handleIntent(intent: SqliteViewerIntent) {
        when (intent) {
            is SqliteViewerIntent.OpenDatabase -> openDatabase(intent.dbPath)
            is SqliteViewerIntent.OpenFromApk -> openFromApk(intent.apkPath, intent.dbEntryName)
            is SqliteViewerIntent.SelectTable -> selectTable(intent.tableName)
            is SqliteViewerIntent.UpdateSql -> update { it.copy(sqlInput = intent.sql) }
            SqliteViewerIntent.ExecuteSql -> executeSql()
        }
    }

    private fun openDatabase(dbPath: String) {
        update { it.copy(isLoading = true, error = null, fileName = dbPath.substringAfterLast("/")) }
        launchEffect {
            try {
                repository.openDatabase(dbPath)
                val tables = repository.getTables()
                update { it.copy(tables = tables, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(SqliteViewerEffect.ShowMessage("打开失败: ${e.message}"))
            }
        }
    }

    private fun openFromApk(apkPath: String, dbEntryName: String) {
        update { it.copy(isLoading = true, error = null, fileName = dbEntryName) }
        launchEffect {
            try {
                repository.openFromApk(apkPath, dbEntryName)
                val tables = repository.getTables()
                update { it.copy(tables = tables, isLoading = false) }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(SqliteViewerEffect.ShowMessage("打开失败: ${e.message}"))
            }
        }
    }

    private fun selectTable(tableName: String) {
        update { it.copy(selectedTable = tableName, isLoading = true, error = null) }
        launchEffect {
            try {
                val columns = repository.getTableSchema(tableName)
                val result = repository.queryTable(tableName)
                update {
                    it.copy(
                        columns = columns,
                        queryResult = result,
                        sqlInput = "SELECT * FROM `$tableName` LIMIT 100",
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(SqliteViewerEffect.ShowMessage("查询失败: ${e.message}"))
            }
        }
    }

    private fun executeSql() {
        val sql = currentState.sqlInput.trim()
        if (sql.isEmpty()) return

        update { it.copy(isLoading = true, error = null) }
        launchEffect {
            try {
                val result = repository.executeQuery(sql)
                update { it.copy(queryResult = result, isLoading = false) }
                emitEffect(SqliteViewerEffect.ShowMessage("查询成功: ${result.rows.size} 行"))
            } catch (e: Exception) {
                Log.w("AppX", "Suppressed exception", e)
                update { it.copy(isLoading = false, error = e.message) }
                emitEffect(SqliteViewerEffect.ShowMessage("查询失败: ${e.message}"))
            }
        }
    }
}
