package io.appdex.core.io

/**
 * 内存中的 [FileSystem]。所有数据存内存,关闭后丢失。
 *
 * 用途:测试 / DEX / APK 内存编辑场景。线程不安全。
 */
class InMemoryFileSystem : FileSystem {

    private sealed class Node {
        abstract val name: String
        abstract var lastModified: Long

        class Dir(override val name: String) : Node() {
            override var lastModified: Long = 0L
            val children: MutableMap<String, Node> = LinkedHashMap()
        }

        class File(override val name: String) : Node() {
            override var lastModified: Long = 0L
            var content: ByteArray = ByteArray(0)
        }
    }

    private val root = Node.Dir("")

    override fun list(dir: String): List<Entry> {
        val d = requireDir(dir)
        return d.children.values.map { node ->
            val name = node.name
            val type = when (node) {
                is Node.Dir -> EntryType.Directory
                is Node.File -> EntryType.File
            }
            val size = if (node is Node.File) node.content.size.toLong() else 0L
            Entry("$dir/$name".normalizePath(), name, type, size, node.lastModified)
        }
    }

    override fun open(path: String, mode: Mode): SeekableChannel {
        val (parent, name) = splitParent(path)
        val parentNode = requireDir(parent)
        val existing = parentNode.children[name]
        val file = when (mode) {
            Mode.Read -> {
                val f = existing as? Node.File ?: throw NoSuchFileException(path)
                f
            }
            Mode.Write, Mode.ReadWrite -> {
                if (existing is Node.File) existing
                else {
                    val f = Node.File(name)
                    parentNode.children[name] = f
                    f
                }
            }
            Mode.Append -> {
                val f = existing as? Node.File ?: Node.File(name).also {
                    parentNode.children[name] = it
                }
                f
            }
        }
        return when (mode) {
            Mode.Read -> InMemorySeekableChannel(file.content)
            Mode.Write -> {
                file.content = ByteArray(0)
                InMemorySeekableChannel(ByteArray(0)).also {
                    // 写回需要包装,见下
                }
            }
            Mode.ReadWrite -> InMemorySeekableChannel(file.content)
            Mode.Append -> InMemorySeekableChannel(file.content).also {
                // Append 模式:保留原内容,把游标移到末尾,后续写入在末尾追加
                it.position(it.size)
            }
        }.let { channel ->
            // 包装为可写回的 channel:close 时把内容写回 Node
            WriteBackChannel(channel, file)
        }
    }

    override fun stat(path: String): Entry? {
        if (path == "/" || path.isEmpty()) {
            return Entry("/", "/", EntryType.Directory, 0L, root.lastModified)
        }
        val node = lookup(path) ?: return null
        val parent = parentOf(path)
        val name = nameOf(path)
        return Entry(
            path = path,
            name = name,
            type = when (node) {
                is Node.Dir -> EntryType.Directory
                is Node.File -> EntryType.File
            },
            size = if (node is Node.File) node.content.size.toLong() else 0L,
            lastModified = node.lastModified,
        )
    }

    override fun mkdir(path: String) {
        val (parent, name) = splitParent(path)
        val parentNode = requireDir(parent)
        if (parentNode.children.containsKey(name)) {
            throw FileAlreadyExistsException(path)
        }
        parentNode.children[name] = Node.Dir(name).also { it.lastModified = System.currentTimeMillis() }
    }

    override fun delete(path: String) {
        val (parent, name) = splitParent(path)
        val parentNode = requireDir(parent)
        if (!parentNode.children.remove(name).let { it != null }) {
            throw NoSuchFileException(path)
        }
    }

    override fun rename(from: String, to: String) {
        val (fromParent, fromName) = splitParent(from)
        val (toParent, toName) = splitParent(to)
        val fromNode = requireDir(fromParent).children.remove(fromName)
            ?: throw NoSuchFileException(from)
        requireDir(toParent).children[toName] = fromNode
    }

    // ---- helpers ----

    private fun requireDir(path: String): Node.Dir {
        val n = lookup(path) ?: throw NoSuchFileException(path)
        return n as? Node.Dir ?: throw NotDirectoryException(path)
    }

    private fun lookup(path: String): Node? {
        val parts = path.normalizePath().split("/").filter { it.isNotEmpty() }
        var current: Node = root
        for (p in parts) {
            current = when (current) {
                is Node.Dir -> current.children[p] ?: return null
                is Node.File -> return null
            }
        }
        return current
    }

    private fun splitParent(path: String): Pair<String, String> {
        val p = path.normalizePath()
        val idx = p.lastIndexOf('/')
        val parent = if (idx <= 0) "/" else p.substring(0, idx)
        val name = p.substring(idx + 1)
        return parent to name
    }

    private fun parentOf(path: String): String {
        val p = path.normalizePath()
        val idx = p.lastIndexOf('/')
        return if (idx <= 0) "/" else p.substring(0, idx)
    }

    private fun nameOf(path: String): String {
        val p = path.normalizePath()
        return p.substring(p.lastIndexOf('/') + 1)
    }

    private fun String.normalizePath(): String =
        if (endsWith('/')) dropLast(1) else this

    /** 写回包装:close 时把 channel 内容同步到 Node.File。 */
    private class WriteBackChannel(
        private val inner: InMemorySeekableChannel,
        private val file: Node.File,
    ) : SeekableChannel by inner {
        override fun close() {
            file.content = inner.toByteArray()
            file.lastModified = System.currentTimeMillis()
            inner.close()
        }
    }
}
