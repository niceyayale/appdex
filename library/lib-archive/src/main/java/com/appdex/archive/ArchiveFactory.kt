package com.appdex.archive

import java.io.File
import java.io.InputStream
import java.io.OutputStream

enum class ArchiveFormat(val extension: String) {
    ZIP("zip"),
    TAR("tar"),
    TAR_GZ("tar.gz"),
    TAR_BZ2("tar.bz2"),
    SEVEN_Z("7z");

    companion object {
        fun fromFileName(name: String): ArchiveFormat? {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".zip") -> ZIP
                lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> TAR_GZ
                lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> TAR_BZ2
                lower.endsWith(".tar") -> TAR
                lower.endsWith(".7z") -> SEVEN_Z
                else -> null
            }
        }
    }
}

data class ArchiveEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long
)

interface ArchiveReader {
    fun listEntries(): List<ArchiveEntry>
    fun getInputStream(entryPath: String): InputStream?
    fun close()
}

interface ArchiveWriter {
    fun addEntry(path: String, inputStream: InputStream)
    fun close()
}

object ArchiveFactory {
    fun createReader(file: File): ArchiveReader {
        val format = ArchiveFormat.fromFileName(file.name)
            ?: throw IllegalArgumentException("Unsupported archive format: ${file.name}")
        // TODO: Implement per-format readers
        throw NotImplementedError("Archive reading not yet implemented")
    }

    fun createWriter(file: File, format: ArchiveFormat): ArchiveWriter {
        // TODO: Implement per-format writers
        throw NotImplementedError("Archive writing not yet implemented")
    }
}
