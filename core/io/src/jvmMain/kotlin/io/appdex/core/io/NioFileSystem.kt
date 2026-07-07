package io.appdex.core.io

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

/**
 * JVM 平台 [FileSystem] 实现,基于 java.nio.file。
 *
 * [root] 是文件系统根目录,所有路径都相对于此解析。
 * 这样测试可以用临时目录,生产可以用 `/sdcard/` 或 SAF 缓存目录。
 */
class NioFileSystem(
    val root: java.io.File,
) : FileSystem {

    private val basePath: Path = root.toPath()

    override fun list(dir: String): List<Entry> {
        val p = resolve(dir)
        if (!Files.isDirectory(p)) throw NotDirectoryException(dir)
        return Files.list(p).use { stream ->
            stream.map { it.toEntry(dir) }.toList()
        }
    }

    override fun open(path: String, mode: Mode): SeekableChannel {
        val p = resolve(path)
        when (mode) {
            Mode.Read -> if (!Files.exists(p)) throw NoSuchFileException(path)
            else -> Files.createDirectories(p.parent)
        }
        return NioFileChannel.open(p, mode)
    }

    override fun stat(path: String): Entry? {
        val p = resolve(path)
        if (!Files.exists(p)) return null
        return p.toEntry(parentOf(path))
    }

    override fun mkdir(path: String) {
        val p = resolve(path)
        if (Files.exists(p)) throw FileAlreadyExistsException(path)
        val parent = p.parent
        if (parent == null || !Files.exists(parent)) throw NoSuchFileException(path)
        Files.createDirectory(p)
    }

    override fun delete(path: String) {
        val p = resolve(path)
        if (!Files.exists(p)) throw NoSuchFileException(path)
        Files.delete(p)
    }

    override fun rename(from: String, to: String) {
        val fromP = resolve(from)
        val toP = resolve(to)
        if (!Files.exists(fromP)) throw NoSuchFileException(from)
        if (Files.exists(toP)) throw FileAlreadyExistsException(to)
        Files.move(fromP, toP)
    }

    // ---- helpers ----

    private fun resolve(path: String): Path =
        basePath.resolve(path.removePrefix("/"))

    private fun Path.toEntry(parentPath: String): Entry {
        val attrs = readAttributes<BasicFileAttributes>()
        return Entry(
            path = "$parentPath/${name}".removePrefix("//"),
            name = name,
            type = if (attrs.isDirectory) EntryType.Directory else EntryType.File,
            size = attrs.size(),
            lastModified = attrs.lastModifiedTime().toMillis(),
        )
    }

    private fun parentOf(path: String): String {
        val idx = path.lastIndexOf('/')
        return if (idx <= 0) "/" else path.substring(0, idx)
    }
}
