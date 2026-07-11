package com.appdex.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val permissions: String = "",
    val mimeType: String? = null,
    val extension: String = ""
) {
    val isHidden: Boolean get() = name.startsWith(".")
    val isApk: Boolean get() = extension.equals("apk", ignoreCase = true)
    val isArchive: Boolean get() = extension.lowercase() in setOf("zip", "rar", "7z", "tar", "gz", "bz2")
    val isText: Boolean get() = extension.lowercase() in setOf(
        "txt", "kt", "java", "xml", "json", "yaml", "yml", "md", "py", "js", "ts",
        "c", "cpp", "h", "hpp", "cs", "go", "rs", "rb", "php", "sh", "bat", "ps1",
        "html", "css", "scss", "sql", "gradle", "properties", "cfg", "conf", "ini"
    )
}

@Serializable
data class Bookmark(
    val id: Long = 0,
    val name: String,
    val path: String,
    val iconKey: String = "folder"
)

@Serializable
data class FileOperation(
    val type: FileOperationType,
    val current: Int,
    val total: Int,
    val currentFile: String
)

enum class FileOperationType { COPY, MOVE, DELETE, COMPRESS, EXTRACT }
