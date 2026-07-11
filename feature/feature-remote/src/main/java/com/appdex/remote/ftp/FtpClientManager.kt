package com.appdex.remote.ftp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.io.FileOutputStream

/**
 * FTP client wrapper for remote file operations.
 * Supports connect, browse, download, and disconnect.
 */
class FtpClientManager(
    private val context: Context
) {
    private var ftpClient: FTPClient? = null

    val isConnected: Boolean
        get() = ftpClient?.isConnected == true

    data class FtpEntry(
        val name: String,
        val size: Long,
        val isDirectory: Boolean,
        val modifiedTime: Long,
        val permission: String?
    )

    data class FtpConnectionConfig(
        val host: String,
        val port: Int = 21,
        val username: String = "anonymous",
        val password: String = "",
        val encoding: String = "UTF-8"
    )

    suspend fun connect(config: FtpConnectionConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = FTPClient()
            client.controlEncoding = config.encoding
            client.connect(config.host, config.port)
            client.login(config.username, config.password)

            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.enterLocalPassiveMode()
            client.bufferSize = 1024 * 1024

            ftpClient = client
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listFiles(path: String): List<FtpEntry> = withContext(Dispatchers.IO) {
        val client = ftpClient ?: return@withContext emptyList()
        try {
            val files = client.listFiles(path)
            files.map { f ->
                FtpEntry(
                    name = f.name,
                    size = f.size,
                    isDirectory = f.isDirectory,
                    modifiedTime = f.timestamp?.timeInMillis ?: 0L,
                    permission = null
                )
            }.filter { it.name != "." && it.name != ".." }
                .sortedWith(compareByDescending<FtpEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun downloadFile(
        remotePath: String,
        localDir: String
    ): String? = withContext(Dispatchers.IO) {
        val client = ftpClient ?: return@withContext null
        try {
            val fileName = remotePath.substringAfterLast("/")
            val localFile = File(localDir, fileName)
            FileOutputStream(localFile).use { fos ->
                client.retrieveFile(remotePath, fos)
            }
            localFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentDirectory(): String? = withContext(Dispatchers.IO) {
        try {
            ftpClient?.printWorkingDirectory()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changeDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            ftpClient?.changeWorkingDirectory(path) == true
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        try {
            ftpClient?.let {
                if (it.isConnected) {
                    it.logout()
                    it.disconnect()
                }
            }
        } catch (_: Exception) {
        } finally {
            ftpClient = null
        }
    }
}
