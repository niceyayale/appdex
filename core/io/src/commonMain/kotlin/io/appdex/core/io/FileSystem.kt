package io.appdex.core.io

/**
 * 文件系统抽象。所有路径使用 `/` 分隔符(POSIX 风格),根为 `/`。
 * 路径不允许 `..`(调用方应先规范化)。
 */
interface FileSystem {

    /** 列出 [dir] 下的条目。dir 必须存在且为目录。 */
    fun list(dir: String): List<Entry>

    /** 打开文件,按 [mode] 读写。文件不存在时,Write/append 模式会创建。 */
    fun open(path: String, mode: Mode): SeekableChannel

    /** 返回条目元信息;不存在返回 null。 */
    fun stat(path: String): Entry?

    /** 创建目录(非递归)。父目录必须存在。 */
    fun mkdir(path: String)

    /** 删除文件或空目录。不存在抛 [NoSuchFileException]。 */
    fun delete(path: String)

    /** 重命名/移动。源不存在抛 [NoSuchFileException]。 */
    fun rename(from: String, to: String)
}

/** 文件打开模式。 */
enum class Mode {
    Read,
    Write,
    ReadWrite,
    Append,
}

/** 文件类型。 */
enum class EntryType {
    File,
    Directory,
}

/** 文件条目元信息。 */
data class Entry(
    val path: String,
    val name: String,
    val type: EntryType,
    val size: Long,
    val lastModified: Long,
)

/** 路径不存在。 */
class NoSuchFileException(path: String) : RuntimeException("no such file: $path")

/** 路径已存在,但操作要求它不存在。 */
class FileAlreadyExistsException(path: String) : RuntimeException("already exists: $path")

/** 路径是目录,但操作要求文件;反之亦然。 */
class NotFileException(path: String) : RuntimeException("not a file: $path")
class NotDirectoryException(path: String) : RuntimeException("not a directory: $path")
