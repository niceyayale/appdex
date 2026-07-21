package io.appdex.core.io

/**
 * 内存中的 [SeekableChannel] 实现。用 [ByteArray] 存储,可增长。
 *
 * 主要用途:测试、APK/DEX 在内存中编辑。不适合大文件(> 数 MB),
 * 大文件应使用平台对应的实现(jvmMain 用 FileChannel)。
 *
 * 注:计划原用 `System.arraycopy(...)`,但 `java.lang.System` 在 commonMain 不可用,
 * 故改用等价的 `Array.copyInto(...)` 扩展函数(kotlin.collections 标准 API)。
 */
class InMemorySeekableChannel(
    initialContent: ByteArray = ByteArray(0)
) : SeekableChannel {

    private var content: ByteArray = initialContent.copyOf()
    private var cursor: Long = 0L
    private var closed: Boolean = false

    override val size: Long
        get() = checkOpen().size.toLong()

    override fun position(newPos: Long) {
        checkOpen()
        require(newPos >= 0L) { "position cannot be negative: $newPos" }
        require(newPos <= content.size.toLong()) {
            "position beyond size: $newPos > ${content.size}"
        }
        cursor = newPos
    }

    override fun read(buf: ByteArray, len: Int): Int {
        checkOpen()
        require(len >= 0) { "len cannot be negative: $len" }
        require(len <= buf.size) { "len $len exceeds buffer size ${buf.size}" }
        if (cursor >= content.size) return 0
        val toRead = minOf(len, (content.size - cursor).toInt())
        content.copyInto(buf, 0, cursor.toInt(), cursor.toInt() + toRead)
        cursor += toRead
        return toRead
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        checkOpen()
        require(off >= 0 && len >= 0 && off + len <= buf.size) {
            "invalid off=$off len=$len for buffer size ${buf.size}"
        }
        val writeEnd = cursor + len
        if (writeEnd > content.size) {
            val grown = ByteArray(writeEnd.toInt())
            content.copyInto(grown, 0, 0, content.size)
            content = grown
        }
        buf.copyInto(content, cursor.toInt(), off, off + len)
        cursor = writeEnd
    }

    /** 返回当前所有内容的快照(测试用)。 */
    fun toByteArray(): ByteArray = checkOpen().copyOf()

    override fun close() {
        closed = true
    }

    private fun checkOpen(): ByteArray {
        check(!closed) { "channel is closed" }
        return content
    }
}
